/*
 * Copyright (c) 2009-2022 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3tools.shader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GLSL Preprocessor
 * 
 * @author Riccardo Balbo
 */
public class Preprocessor {

    public static InputStream apply(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte chunk[] = new byte[1024];
        int read;
        while ((read = in.read(chunk)) != -1) {
            bos.write(chunk, 0, read);
        }
        bos.close();
        in.close();

        String code = bos.toString("UTF-8");

        code = Preprocessor.forMacro(code);
        code = Preprocessor.structMacro(code);

        return new ByteArrayInputStream(code.getBytes("UTF-8"));
    }

    /**
     * #for i=0..100 ( #ifdef ENABLE_INPUT_$i $0 #endif ) 
     *      do something with $i
     * #endfor
     */
    private static final Pattern FOR_REGEX = Pattern.compile(
            "\\s*(\\w+)\\s*=\\s*(\\d+)\\s*\\.\\.\\s*(\\d+)\\s*(?:\\((.+)\\))?\\s*"
    );

    public static String forMacro(String code) {
        StringBuilder expanded = new StringBuilder();

        StringBuilder currentFor = null;
        String forDec = null;
        String forLineOriginal = null;

        int depth = 0;
        boolean expandedOnce = false;

        String[] lines = code.split("\n", -1); 
        for (String line : lines) {
            String trimmed = line.trim();

            if (!expandedOnce && trimmed.startsWith("#for")) {
                if (depth++ == 0) {
                    forLineOriginal = line;
                    forDec = trimmed.substring("#for".length()).trim();
                    currentFor = new StringBuilder();
                    continue; 
                }
            }

            if (!expandedOnce && trimmed.startsWith("#endfor")) {
                depth--;
                if (depth == 0 && currentFor != null) {
                    Matcher m = FOR_REGEX.matcher(forDec);
                    if (m.matches()) {
                        String varName = "$" + m.group(1);
                        int start = Integer.parseInt(m.group(2));
                        int end = Integer.parseInt(m.group(3));

                        String inj = m.group(4); // optional
                        if (inj == null || inj.trim().isEmpty()) {
                            inj = "$0";
                        }

                        String body = currentFor.toString();

                        currentFor = null;
                        forDec = null;
                        forLineOriginal = null;

                        for (int i = start; i < end; i++) {
                            String expandedBlock =
                                    inj.replace("$0", "\n" + body)
                                    .replace(varName, Integer.toString(i));
                            expanded.append("\n").append(expandedBlock).append("\n");
                        }

                        expandedOnce = true;
                        continue; 
                    } else {
                        expanded.append(forLineOriginal).append("\n");
                        expanded.append(currentFor);
                        expanded.append(line).append("\n");

                        currentFor = null;
                        forDec = null;
                        forLineOriginal = null;
                        continue;
                    }
                }
            }

            if (currentFor != null) {
                currentFor.append(line).append("\n");
            } else {
                expanded.append(line).append("\n");
            }
        }

        if (currentFor != null) {
            expanded.append(forLineOriginal).append("\n");
            expanded.append(currentFor);
        }

        String result = expanded.toString();
        return expandedOnce ? forMacro(result) : result;
    }

    /**
     * <code>
     * #struct MyStruct extends BaseStruct, BaseStruct2
     *  int i; 
     *  int b; 
     * #endstruct
     * </code>
     */
    // match #struct MyStruct extends BaseStruct, BaseStruct2
    // extends is optional
    // private static final Pattern FOR_REGEX = Pattern
    // .compile("([^=]+)=\\s*([0-9]+)\\s*\\.\\.\\s*([0-9]+)\\s*\\((.+)\\)");

    private static final Pattern STRUCT_REGEX = Pattern
            .compile("(\\w+)(?:\\s+extends\\s+(\\w+(?:,\\s*\\w+)*))?");

    public static String structMacro(String code) {
        StringBuilder expandedCode = new StringBuilder();
        StringBuilder currentStruct = null;
        String structDec = null;
        int skip = 0;
        String[] codeLines = code.split("\n");
        boolean captured = false;
        for (String line : codeLines) {
            if (!captured) {
                String trimmedLine = line.trim();
                if (trimmedLine.startsWith("#struct")) {
                    if (skip == 0) {
                        structDec = trimmedLine;
                        currentStruct = new StringBuilder();
                        skip++;
                        continue;
                    }
                    skip++;
                } else if (trimmedLine.startsWith("#endstruct")) {
                    skip--;
                    if (skip == 0) {
                        structDec = structDec.substring("#struct ".length()).trim();

                        Matcher matcher = STRUCT_REGEX.matcher(structDec);
                        if (matcher.matches()) {
                            String structName = matcher.group(1);
                            if (structName == null) structName = "";

                            String extendsStructs = matcher.group(2);
                            String extendedStructs[];
                            if (extendsStructs != null) {
                                extendedStructs = extendsStructs.split(",\\s*");
                            } else {
                                extendedStructs = new String[0];
                            }
                            String structBody = currentStruct.toString();
                            if (structBody == null) structBody = "";
                            else {
                                // remove tail spaces
                                structBody = structBody.replaceAll("\\s+$", "");
                            }

                            currentStruct = null;
                            expandedCode.append("#define STRUCT_").append(structName).append(" \\\n");
                            for (String extendedStruct : extendedStructs) {
                                expandedCode.append("STRUCT_").append(extendedStruct).append(" \\\n");
                            }
                            String structBodyLines[] = structBody.split("\n");
                            for (int i = 0; i < structBodyLines.length; i++) {
                                String structBodyLine = structBodyLines[i];
                                structBodyLine = structBodyLine.trim();
                                if (structBodyLine == "") continue;
                                // remove comments if any
                                int commentIndex = structBodyLine.indexOf("//");
                                if (commentIndex >= 0)
                                    structBodyLine = structBodyLine.substring(0, commentIndex);
                                expandedCode.append(structBodyLine);
                                if (i < structBodyLines.length - 1) expandedCode.append(" \\");
                                expandedCode.append("\n");
                            }
                            expandedCode.append("struct ").append(structName).append(" { \nSTRUCT_")
                                    .append(structName).append("\n};\n");
                            captured = true;
                            continue;
                        }
                    }
                }
            }
            if (currentStruct != null) currentStruct.append(line).append("\n");
            else expandedCode.append(line).append("\n");
        }
        code = expandedCode.toString();
        if (captured) code = structMacro(code);
        return code;
    }

}
