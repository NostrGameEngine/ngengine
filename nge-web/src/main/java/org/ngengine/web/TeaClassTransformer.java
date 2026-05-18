/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Nostr Game Engine is a fork of the jMonkeyEngine, which is licensed under
 * the BSD 3-Clause License. 
 */

package org.ngengine.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationContainer;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;

import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.MethodHolder;
import org.teavm.model.ReferenceCache;
import org.teavm.model.ValueType;
import org.teavm.model.util.ModelUtils;
import org.teavm.parsing.ClassRefsRenamer;

import org.ngengine.web.patches.*;

public class TeaClassTransformer implements ClassHolderTransformer {

    public TeaClassTransformer() {

    }

    private static ClassHolder getClassHolder(Class<?> clazz, ClassHolderTransformerContext context) {
        ClassReaderSource innerSource = context.getHierarchy().getClassSource();
        ClassReader classReader = innerSource.get(clazz.getName());
        ClassHolder classHolder = (ClassHolder) classReader;
        return classHolder;
    }

    private static ReferenceCache referenceCache = new ReferenceCache();

    private static void transferMethods(Class<?> clazz, ClassHolder a, ClassHolder b,
            ClassHolderTransformerContext context) {
        Collection<MethodHolder> methods = a.getMethods();
        Collection<MethodHolder> patchableMethods = new ArrayList<MethodHolder>();

        for (MethodHolder methodHolder : methods) {
            if (!methodHolder.hasModifier(ElementModifier.ABSTRACT)
                    && !methodHolder.getName().equals("<init>")) {
                patchableMethods.add(methodHolder);
            }
        }

        ClassRefsRenamer refPatcher = new ClassRefsRenamer(referenceCache, n -> {
            if (n.equals(a.getName())) {
                return b.getName();
            }
            return n;
        });

        for (MethodHolder methodHolder : patchableMethods) {

            methodHolder = ModelUtils.copyMethod(methodHolder);
            methodHolder = refPatcher.rename(methodHolder);
            b.addMethod(methodHolder);
        }

    }

    private static void transferFields(ClassHolder a, ClassHolder b, ClassHolderTransformerContext context) {
        Collection<FieldHolder> fields = a.getFields();
        Collection<FieldHolder> patchableFields = new ArrayList<FieldHolder>();

        for (FieldHolder fieldHolder : fields) {
            if (!fieldHolder.hasModifier(ElementModifier.ABSTRACT)) {
                patchableFields.add(fieldHolder);
            }
        }
        for (FieldHolder fieldHolder : patchableFields) {
            if (b.getField(fieldHolder.getName()) != null) {
                b.removeField(b.getField(fieldHolder.getName()));
            }
            a.removeField(fieldHolder);
            b.addField(fieldHolder);
        }

    }

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        String clzName = cls.getName();

        try {
            Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(clzName);

            if (clzName.equals("java.io.Reader")) {
                ClassHolder classPatchHolder = getClassHolder(ReaderPatch.class, context);
                cls.getInterfaces().add("java.lang.Readable");
                transferMethods(clazz, classPatchHolder, cls, context);
                System.out.println(clazz.getName() + " patched");
            } else if (clzName.equals("java.lang.reflect.Field")) {
                ClassHolder classPatchHolder = getClassHolder(FieldPatch.class, context);
                transferMethods(clazz, classPatchHolder, cls, context);
                System.out.println(clazz.getName() + " patched");
            } else if (clzName.equals("java.lang.Class")) {
                ClassHolder classPatchHolder = getClassHolder(ClassPatch.class, context);
                transferMethods(clazz, classPatchHolder, cls, context);
                System.out.println(clazz.getName() + " patched");
            }

        } catch (ClassNotFoundException e) {
        }

        if (hasAnnotation(context, cls, "org.ngengine.gui.style.StyleAttribute")) {
            try {
                String attributeMapJson = extractLemurStyleAttributeAnnotationsMeta(context, cls);
                if (attributeMapJson != null) {
                    addMapField(cls, "lemurStyleAttributeMap", attributeMapJson);
                }
            } catch (Exception e) {
                System.err.println("Error processing StyleAttributes for " + clzName + ": " + e.getMessage());
            }
        }


        if (isSerializable(cls, context)) {
            removeSerializationMethods(cls);
        }

    }

    private void addMapField(ClassHolder cls, String fieldName, String attributeMapJson) {
        FieldHolder attributeMapField = new FieldHolder(fieldName);
        attributeMapField.setType(ValueType.object("java.lang.String"));
        attributeMapField.setLevel(AccessLevel.PUBLIC);
        attributeMapField.getModifiers().add(ElementModifier.FINAL);
        attributeMapField.setInitialValue(attributeMapJson);
        cls.addField(attributeMapField);
        System.out.println("Added StyleAttribute map to: " + fieldName);
    }

    private boolean hasAnnotation(ClassHolderTransformerContext context,ClassHolder cls, String t) {
        for (MethodHolder m : cls.getMethods()) {
            AnnotationContainer annos = m.getAnnotations();
            for (AnnotationHolder anno : annos.all()) {
                if (anno.getType().equals(t)) {
                    return true;
                }
            }

        }
        String parent = cls.getParent();
        if (parent != null && !parent.equals("java.lang.Object")) {
            ClassReader superReader = context.getHierarchy().getClassSource().get(parent);
            if (superReader != null) {
                return hasAnnotation(context,(ClassHolder) superReader, t);
            }
        }
        return false;
    }

    private boolean isSerializable(ClassHolder cls, ClassHolderTransformerContext context) {
        for (String iface : cls.getInterfaces()) {
            if (iface.equals("java.io.Serializable")) {
                return true;
            }
            ClassReader ifaceReader = context.getHierarchy().getClassSource().get(iface);
            if (ifaceReader != null && isSerializable((ClassHolder) ifaceReader, context)) {
                return true;
            }
        }
        String superclass = cls.getParent();
        if (superclass != null && !superclass.equals("java.lang.Object")) {
            ClassReader superReader = context.getHierarchy().getClassSource().get(superclass);
            if (superReader != null) {
                return isSerializable((ClassHolder) superReader, context);
            }
        }
        return false;
    }

    private void extractLemurStyleAttributeAnnotationsMeta(ClassHolderTransformerContext ctx, ClassHolder root, ClassHolder cls, Map<String, Map<String, Object>> attributeMap ) {
        for (MethodHolder m : cls.getMethods()) {
            AnnotationContainer annos = m.getAnnotations();
            for (AnnotationHolder anno : annos.all()) {
                if (anno.getType().equals("org.ngengine.gui.style.StyleAttribute")) {
                    Map<String, Object> data = new HashMap<>();
                    AnnotationValue valueA = anno.getValue("value");
                    AnnotationValue lookupDefaultA = anno.getValue("lookupDefault");
                    data.put("value", valueA == null ? null : valueA.getString());
                    data.put("lookupDefault", lookupDefaultA == null ? true : lookupDefaultA.getBoolean());
                    attributeMap.putIfAbsent(m.getName(), data);
                    System.out.println("Found StyleAttribute: " + m.getName() + " in " + root.getName());
                }
            }
        }
        
        String parent = cls.getParent();
        if (parent != null && !parent.equals("java.lang.Object")) {
            ClassReader superReader = ctx.getHierarchy().getClassSource().get(parent);
            if (superReader != null) {
                extractLemurStyleAttributeAnnotationsMeta(ctx,root, (ClassHolder) superReader, attributeMap);
            }
        }
    }


    private String extractLemurStyleAttributeAnnotationsMeta(ClassHolderTransformerContext ctx, ClassHolder cls) {
        Map<String, Map<String, Object>> attributeMap = new HashMap<>();

        extractLemurStyleAttributeAnnotationsMeta(ctx, cls,cls, attributeMap);

        if (attributeMap.isEmpty()) {
            return null;
        }

        return toSimpleJson(attributeMap);

    }



    private String toSimpleJson(Map<String, Map<String, Object>> attributeMap) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            boolean firstEntry = true;
            for (Map.Entry<String, Map<String, Object>> entry : attributeMap.entrySet()) {
                if (!firstEntry) {
                    sb.append(",");
                }
                firstEntry = false;
                sb.append("\"").append(entry.getKey()).append("\":{");
                boolean firstAttr = true;
                for (Map.Entry<String, Object> attr : entry.getValue().entrySet()) {
                    if (!firstAttr) {
                        sb.append(",");
                    }
                    firstAttr = false;
                    sb.append("\"").append(attr.getKey()).append("\":");
                    Object value = attr.getValue();
                    if (value instanceof String) {
                        sb.append("\"").append(value).append("\"");
                    } else {
                        sb.append(value);
                    }
                }
                sb.append("}");
            }
            sb.append("}");
            return sb.toString();
        } catch (Exception e) {
            System.err.println("Error creating StyleAttribute map");
            return null;
        }
    }

    private void removeSerializationMethods(ClassHolder cls) {
        ArrayList<MethodHolder> methodsToRemove = new ArrayList<>();
        for (MethodHolder method : cls.getMethods()) {
            String name = method.getName();
            if (name.equals("writeObject")) {
                methodsToRemove.add(method);
            } else if (name.equals("readObject")) {
                methodsToRemove.add(method);
            }
        }
        for (MethodHolder method : methodsToRemove) {
            cls.removeMethod(method);
            System.out.println("Removed serialization method " + method.getName() + " from " + cls.getName());
        }
    }

}
