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

package org.ngengine;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.RendererException;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.util.MaterialDebugAppState;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.ReloadableComponent;

public class DevMode extends BaseAppState implements RawInputListener {

    private static final int SELECTOR_ROWS = 10;
    private static final float SELECTOR_X = 24f;
    private static final float SELECTOR_TOP = 24f;
    private static final float SELECTOR_WIDTH = 360f;
    private static final float SELECTOR_TITLE_HEIGHT = 28f;
    private static final float SELECTOR_ROW_HEIGHT = 24f;
    private static final float SELECTOR_PADDING = 10f;

    private static Map<Object, Runnable> onReloadMap = new WeakHashMap<>();
    private static ArrayList<WeakReference<Spatial>> reloadableSpatials = new ArrayList<>();
    private static ArrayList<WeakReference<Material>> reloadableMaterials = new ArrayList<>();
    private static volatile boolean needsReload = false;
    private static ArrayList<WeakReference<ComponentManager>> reloadableComponentManagers = new ArrayList<>();
    private static Map<String, Supplier<Map<String, Runnable>>> menuSections = new LinkedHashMap<>();
    private static volatile boolean enableDevMode = false;

    private Node selectorRoot;
    private Geometry selectorBackground;
    private BitmapText selectorTitle;
    private ArrayList<BitmapText> selectorRows = new ArrayList<>();
    private ArrayList<MenuSection> selectorSections = new ArrayList<>();
    private ArrayList<MenuEntry> selectorEntries = new ArrayList<>();
    private boolean selectorVisible = false;
    private boolean selectorAttachedToGuiViewPort = false;
    private int selectorIndex = 0;
    private int selectorFirstRow = 0;

    public static boolean isDevModeEnabled() {
        return enableDevMode;
    }

    public static void setDevModeEnabled(boolean enabled) {
        DevMode.enableDevMode = enabled;
    }

    public static void registerReloadCallback(Object ref, Runnable callback) {
        if(!enableDevMode) return;
        onReloadMap.put(ref, callback);
    }

    public static void registerForReload(Spatial spatial) {
        if(!enableDevMode) return;
        reloadableSpatials.add(new WeakReference<>(spatial));
    }

    public static void registerForReload(ComponentManager componentManager) {
        if(!enableDevMode) return;
        reloadableComponentManagers.add(new WeakReference<>(componentManager));
    }

    public static void registerForReload(Material material) {
        if(!enableDevMode) return;
        reloadableMaterials.add(new WeakReference<>(material));
    }

    public static void reload() {
        if(!enableDevMode) return;
        needsReload = true;
    }

    public static void addMenuSection(String title, Supplier<Map<String, Runnable>> optionsSupplier) {
        if(!enableDevMode) return;
        menuSections.put(title, optionsSupplier);
    }

    public static void addMenuSection(String title, Map<String, Runnable> options) {
        addMenuSection(title, () -> options);
    }

    public static void removeMenuSection(String title) {
        if(!enableDevMode) return;
        menuSections.remove(title);
    }

    @Override
    protected void initialize(Application app) {}

    @Override
    protected void cleanup(Application app) {
        if (selectorRoot != null) {
            if (selectorAttachedToGuiViewPort) {
                app.getGuiViewPort().detachScene(selectorRoot);
            } else {
                selectorRoot.removeFromParent();
            }
            selectorRoot = null;
            selectorRows.clear();
            selectorAttachedToGuiViewPort = false;
        }
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
        if (selectorVisible) {
            updateSelectorOverlay();
        }
        if (needsReload) {
            needsReload = false;
            AssetManager assetManager = getApplication().getAssetManager();
            RenderManager renderManager = getApplication().getRenderManager();
            for (Runnable callback : onReloadMap.values()) {
                try {
                    callback.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            reloadRegisteredSpatials(assetManager, renderManager);
            reloadRegisteredMaterials(assetManager, renderManager);
            reloadRegisteredComponents();
        }
    }

    @Override
    protected void onEnable() {
        InputManager im = getApplication().getInputManager();
        im.addRawInputListener(this);

        addMenuSection("Materials", this::collectMaterialMenuOptions);
        addMenuSection("Spatials", this::collectSpatialMenuOptions);
        addMenuSection("Components", this::collectComponentMenuOptions);

        registerReloadCallback(
            this,
            () -> {
                getApplication().getAssetManager().clearCache();
            }
        );
    }

    @Override
    protected void onDisable() {
        InputManager im = getApplication().getInputManager();
        im.removeRawInputListener(this);
        hideSelectorOverlay();
        removeMenuSection("Materials");
        removeMenuSection("Spatials");
        removeMenuSection("Components");
    }

    @Override
    public void beginInput() {}

    @Override
    public void endInput() {}

    @Override
    public void onJoyAxisEvent(JoyAxisEvent evt) {}

    @Override
    public void onJoyButtonEvent(JoyButtonEvent evt) {}

    @Override
    public void onMouseMotionEvent(MouseMotionEvent evt) {}

    @Override
    public void onMouseButtonEvent(MouseButtonEvent evt) {}

    @Override
    public void onTouchEvent(TouchEvent evt) {}

    @Override
    public void onKeyEvent(KeyInputEvent evt) {
        int key = evt.getKeyCode();
        if (!enableDevMode) {
            return;
        }

        if (key == KeyInput.KEY_F5 || key == KeyInput.KEY_F6 || key == KeyInput.KEY_F7) {
            evt.setConsumed();
            if (!evt.isPressed() || evt.isRepeating()) {
                return;
            }

            if (key == KeyInput.KEY_F5) {
                reload();
            } else if (key == KeyInput.KEY_F6) {
                Application app = getApplication();
                app.getInputManager().setCursorVisible(!app.getInputManager().isCursorVisible());
            } else {
                Application app = getApplication();
                if (app instanceof SimpleApplication) {
                    SimpleApplication simpleApp = (SimpleApplication) app;
                    simpleApp.getFlyByCamera().setEnabled(!simpleApp.getFlyByCamera().isEnabled());
                }
            }
            return;
        }

        if (key == KeyInput.KEY_F11) {
            evt.setConsumed();
            if (evt.isRepeating()) {
                return;
            }
            if (evt.isPressed()) {
                selectorVisible = true;
                selectorSections = collectMenuSections();
                selectorEntries = flattenMenuSections(selectorSections);
                if (selectorIndex >= selectorEntries.size()) {
                    selectorIndex = Math.max(0, selectorEntries.size() - 1);
                }
                updateSelectorOverlay();
            } else if (selectorVisible) {
                MenuEntry entry = selectorIndex < selectorEntries.size() ? selectorEntries.get(selectorIndex) : null;
                hideSelectorOverlay();
                if (entry != null && entry.action != null) {
                    try {
                        entry.action.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return;
        }

        if (!selectorVisible || (key != KeyInput.KEY_UP && key != KeyInput.KEY_DOWN)) {
            return;
        }

        evt.setConsumed();
        if (!evt.isPressed() || selectorEntries.isEmpty()) {
            return;
        }

        if (key == KeyInput.KEY_UP) {
            selectorIndex = Math.max(0, selectorIndex - 1);
        } else {
            selectorIndex = Math.min(selectorEntries.size() - 1, selectorIndex + 1);
        }
        updateSelectorOverlay();
    }

    private ArrayList<MenuSection> collectMenuSections() {
        ArrayList<MenuSection> sections = new ArrayList<>();
        for (Map.Entry<String, Supplier<Map<String, Runnable>>> sectionEntry : menuSections.entrySet()) {
            Map<String, Runnable> options;
            try {
                Supplier<Map<String, Runnable>> supplier = sectionEntry.getValue();
                options = supplier == null ? null : supplier.get();
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            if (options == null || options.isEmpty()) {
                continue;
            }

            MenuSection section = new MenuSection(sectionEntry.getKey());
            for (Map.Entry<String, Runnable> option : options.entrySet()) {
                section.add(option.getKey(), option.getValue());
            }
            if (!section.entries.isEmpty()) {
                sections.add(section);
            }
        }
        return sections;
    }

    private Map<String, Runnable> collectMaterialMenuOptions() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Reload all materials", () -> reloadRegisteredMaterials(getApplication().getAssetManager(), getApplication().getRenderManager()));
        Iterator<WeakReference<Material>> materialIt = reloadableMaterials.iterator();
        while (materialIt.hasNext()) {
            Material material = materialIt.next().get();
            if (material == null) {
                materialIt.remove();
                continue;
            }
            String label = material.getName();
            if (label == null && material.getMaterialDef() != null) {
                label = material.getMaterialDef().getAssetName();
            }
            Material target = material;
            addMenuOption(options, "Reload " + (label == null ? material.getClass().getSimpleName() : label), () -> reloadTarget(target));
        }
        return options;
    }

    private Map<String, Runnable> collectSpatialMenuOptions() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Reload all spatials", () -> reloadRegisteredSpatials(getApplication().getAssetManager(), getApplication().getRenderManager()));
        Iterator<WeakReference<Spatial>> spatialIt = reloadableSpatials.iterator();
        while (spatialIt.hasNext()) {
            Spatial spatial = spatialIt.next().get();
            if (spatial == null) {
                spatialIt.remove();
                continue;
            }
            String label = spatial.getName();
            Spatial target = spatial;
            addMenuOption(options, "Reload " + (label == null ? spatial.getClass().getSimpleName() : label), () -> reloadTarget(target));
        }
        return options;
    }

    private Map<String, Runnable> collectComponentMenuOptions() {
        LinkedHashMap<String, Runnable> options = new LinkedHashMap<>();
        options.put("Reload all components", this::reloadRegisteredComponents);
        Iterator<WeakReference<ComponentManager>> managerIt = reloadableComponentManagers.iterator();
        while (managerIt.hasNext()) {
            ComponentManager manager = managerIt.next().get();
            if (manager == null) {
                managerIt.remove();
                continue;
            }
            for (Component component : manager.getAllComponents()) {
                if (component instanceof ReloadableComponent) {
                    ReloadableComponent reloadable = (ReloadableComponent) component;
                    String label = reloadable.getReloadLabel();
                    addMenuOption(options, "Reload " + (label == null ? component.getClass().getSimpleName() : label), reloadable::reload);
                }
            }
        }
        return options;
    }

    private void addMenuOption(Map<String, Runnable> options, String label, Runnable action) {
        if (label == null) {
            label = "(unnamed)";
        }
        String uniqueLabel = label;
        int duplicate = 2;
        while (options.containsKey(uniqueLabel)) {
            uniqueLabel = label + " (" + duplicate + ")";
            duplicate++;
        }
        options.put(uniqueLabel, action);
    }

    private void reloadRegisteredMaterials(AssetManager assetManager, RenderManager renderManager) {
        Iterator<WeakReference<Material>> it = reloadableMaterials.iterator();
        while (it.hasNext()) {
            try {
                WeakReference<Material> ref = it.next();
                Material mat = ref.get();
                if (mat == null) {
                    it.remove();
                    continue;
                }
                reloadMaterial(assetManager, renderManager, mat);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void reloadRegisteredSpatials(AssetManager assetManager, RenderManager renderManager) {
        Iterator<WeakReference<Spatial>> it = reloadableSpatials.iterator();
        while (it.hasNext()) {
            try {
                WeakReference<Spatial> ref = it.next();
                Spatial spatial = ref.get();
                if (spatial == null) {
                    it.remove();
                    continue;
                }
                spatial.depthFirstTraversal(sx -> {
                    if (sx instanceof Geometry) {
                        Geometry geom = (Geometry) sx;
                        Material mat = geom.getMaterial();
                        if (mat != null) {
                            reloadMaterial(assetManager, renderManager, mat);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void reloadRegisteredComponents() {
        Iterator<WeakReference<ComponentManager>> it = reloadableComponentManagers.iterator();
        while (it.hasNext()) {
            try {
                WeakReference<ComponentManager> ref = it.next();
                ComponentManager cm = ref.get();
                if (cm == null) {
                    it.remove();
                    continue;
                }
                List<Component> components = cm.getAllComponents();
                for (Component component : components) {
                    if (component instanceof ReloadableComponent) {
                        ((ReloadableComponent) component).reload();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private ArrayList<MenuEntry> flattenMenuSections(ArrayList<MenuSection> sections) {
        ArrayList<MenuEntry> entries = new ArrayList<>();
        for (MenuSection section : sections) {
            entries.addAll(section.entries);
        }
        return entries;
    }

    private void reloadTarget(Object target) {
        AssetManager assetManager = getApplication().getAssetManager();
        RenderManager renderManager = getApplication().getRenderManager();
        try {
            if (target instanceof Material) {
                reloadMaterial(assetManager, renderManager, (Material) target);
            } else if (target instanceof Spatial) {
                ((Spatial) target).depthFirstTraversal(sx -> {
                    if (sx instanceof Geometry) {
                        Material mat = ((Geometry) sx).getMaterial();
                        if (mat != null) {
                            reloadMaterial(assetManager, renderManager, mat);
                        }
                    }
                });
            } else if (target instanceof ReloadableComponent) {
                ((ReloadableComponent) target).reload();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSelectorOverlay() {
        if (selectorRoot == null) {
            selectorRoot = new Node("DevModeReloadSelector");
            selectorBackground = new Geometry("DevModeReloadSelectorBackground", new Quad(SELECTOR_WIDTH, SELECTOR_PADDING * 2 + SELECTOR_TITLE_HEIGHT + SELECTOR_ROWS * SELECTOR_ROW_HEIGHT));
            selectorBackground.setMaterial(uiMaterial(ColorRGBA.White));
            selectorRoot.attachChild(selectorBackground);

            BitmapFont font = getApplication().getAssetManager().loadFont("Interface/Fonts/Default.fnt");
            selectorTitle = new BitmapText(font);
            selectorTitle.setSize(16f);
            selectorTitle.setColor(ColorRGBA.Black);
            selectorRoot.attachChild(selectorTitle);
            for (int i = 0; i < SELECTOR_ROWS; i++) {
                BitmapText row = new BitmapText(font);
                row.setSize(14f);
                row.setColor(ColorRGBA.Black);
                selectorRows.add(row);
                selectorRoot.attachChild(row);
            }

            Application app = getApplication();
            if (app instanceof SimpleApplication) {
                ((SimpleApplication) app).getGuiNode().attachChild(selectorRoot);
            } else {
                app.getGuiViewPort().attachScene(selectorRoot);
                selectorAttachedToGuiViewPort = true;
            }
        }

        float height = getApplication().getGuiViewPort().getCamera().getHeight();
        float panelHeight = SELECTOR_PADDING * 2 + SELECTOR_TITLE_HEIGHT + SELECTOR_ROWS * SELECTOR_ROW_HEIGHT;
        float baseY = height - SELECTOR_TOP - panelHeight;
        selectorBackground.setLocalTranslation(SELECTOR_X, baseY, 0f);

        if (selectorEntries.isEmpty()) {
            selectorFirstRow = 0;
            selectorTitle.setText("Dev menu");
            selectorTitle.setLocalTranslation(SELECTOR_X + SELECTOR_PADDING, baseY + panelHeight - SELECTOR_PADDING - 6f, 2f);
            selectorRows.get(0).setText("(no menu options)");
            selectorRows.get(0).setLocalTranslation(SELECTOR_X + SELECTOR_PADDING + 6f, baseY + panelHeight - SELECTOR_PADDING - SELECTOR_TITLE_HEIGHT - 8f, 2f);
            selectorRows.get(0).setColor(ColorRGBA.Black);
            for (int i = 1; i < selectorRows.size(); i++) {
                selectorRows.get(i).setText("");
            }
        } else {
            MenuEntry selected = selectorEntries.get(selectorIndex);
            ArrayList<MenuRow> menuRows = new ArrayList<>();
            int selectedRow = 0;
            for (MenuSection section : selectorSections) {
                menuRows.add(new MenuRow(section.title, null));
                for (MenuEntry entry : section.entries) {
                    if (entry == selected) {
                        selectedRow = menuRows.size();
                    }
                    menuRows.add(new MenuRow("  " + entry.label, entry));
                }
            }

            if (selectorFirstRow > Math.max(0, menuRows.size() - SELECTOR_ROWS)) {
                selectorFirstRow = Math.max(0, menuRows.size() - SELECTOR_ROWS);
            }
            if (selectedRow < selectorFirstRow) {
                selectorFirstRow = selectedRow;
            } else if (selectedRow >= selectorFirstRow + SELECTOR_ROWS) {
                selectorFirstRow = selectedRow - SELECTOR_ROWS + 1;
            }

            selectorTitle.setText("Dev menu");
            selectorTitle.setLocalTranslation(SELECTOR_X + SELECTOR_PADDING, baseY + panelHeight - SELECTOR_PADDING - 6f, 2f);
            for (int rowIndex = 0; rowIndex < SELECTOR_ROWS; rowIndex++) {
                int menuRowIndex = selectorFirstRow + rowIndex;
                BitmapText row = selectorRows.get(rowIndex);
                if (menuRowIndex >= menuRows.size()) {
                    row.setText("");
                    continue;
                }
                MenuRow menuRow = menuRows.get(menuRowIndex);
                row.setText(menuRow.label);
                row.setColor(menuRow.entry == selected ? ColorRGBA.Red : ColorRGBA.Black);
                row.setLocalTranslation(SELECTOR_X + SELECTOR_PADDING + 6f, baseY + panelHeight - SELECTOR_PADDING - SELECTOR_TITLE_HEIGHT - rowIndex * SELECTOR_ROW_HEIGHT - 8f, 2f);
            }
        }
        selectorRoot.setCullHint(Spatial.CullHint.Never);
    }

    private Material uiMaterial(ColorRGBA color) {
        Material material = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", color);
        material.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        material.getAdditionalRenderState().setDepthTest(false);
        material.getAdditionalRenderState().setDepthWrite(false);
        return material;
    }

    private void hideSelectorOverlay() {
        selectorVisible = false;
        if (selectorRoot != null) {
            selectorRoot.setCullHint(Spatial.CullHint.Always);
        }
    }

    private static class MenuSection {
        final String title;
        final ArrayList<MenuEntry> entries = new ArrayList<>();

        MenuSection(String title) {
            this.title = title;
        }

        void add(String label, Runnable action) {
            entries.add(new MenuEntry(this, label, action));
        }
    }

    private static class MenuEntry {
        final MenuSection section;
        final String label;
        final Runnable action;

        MenuEntry(MenuSection section, String label, Runnable action) {
            this.section = section;
            this.label = label;
            this.action = action;
        }
    }

    private static class MenuRow {
        final String label;
        final MenuEntry entry;

        MenuRow(String label, MenuEntry entry) {
            this.label = label;
            this.entry = entry;
        }
    }

    public static Material reloadMaterial(AssetManager assetManager, RenderManager renderManager, Material mat) {
        // clear the entire cache, there might be more clever things to do, like
        // clearing only the matdef, and the associated shaders.
        assetManager.clearCache();

        // creating a dummy mat with the mat def of the mat to reload
        // Force the reloading of the asset, otherwise the new shader code will not be applied.
        Material dummy = new Material(assetManager, mat.getMaterialDef().getAssetName());

        for (MatParam matParam : mat.getParams()) {
            dummy.setParam(matParam.getName(), matParam.getVarType(), matParam.getValue());
        }

        dummy.getAdditionalRenderState().set(mat.getAdditionalRenderState());

        // creating a dummy geom and assigning the dummy material to it
        Geometry dummyGeom = new Geometry("dummyGeom", new Box(1f, 1f, 1f));
        dummyGeom.setMaterial(dummy);

        try {
            // preloading the dummyGeom, this call will compile the shader again
            renderManager.preloadScene(dummyGeom);
        } catch (RendererException e) {
            // compilation error, the shader code will be output to the console
            // the following code will output the error
            Logger.getLogger(MaterialDebugAppState.class.getName()).log(Level.SEVERE, e.getMessage());
            return null;
        }

        Logger.getLogger(MaterialDebugAppState.class.getName()).log(Level.INFO, "Material successfully reloaded");
        return dummy;
    }
}
