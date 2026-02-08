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

package org.ngengine.ads;

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.ngengine.nostrads.protocol.AdBidEvent;
import org.ngengine.nostrads.protocol.types.AdPriceSlot;
import org.ngengine.nostrads.protocol.types.AdSize;
import org.ngengine.nostrads.protocol.types.AdTaxonomy;

public class ImmersiveAdControl extends AbstractControl implements ImmersiveAdGroup {

    private static final Logger logger = Logger.getLogger(ImmersiveAdControl.class.getName());
    private static final String[] DEFAULT_TEXTURE_KEYS = new String[] { "ColorMap", "BaseColorMap", "DiffuseMap" };

    private boolean needUpdate = true;
    private String[] textureKeys = DEFAULT_TEXTURE_KEYS;

    private String categoryIds = null;
    private String languages = null;
    private String context = null;
    private String priceSlot = null;
    private Material activeMaterialReplacement;
    private Material deactiveMaterialReplacement;
    private boolean replaceActiveMaterial = true;
    private boolean replaceDeactiveMaterial = true;
    private AssetManager assetManager;
    private Function<AdBidEvent, Boolean> filter = null;

    private transient List<ImmersiveAdSpace> adSpaces = new ArrayList<>();

    public ImmersiveAdControl(
        @Nonnull AssetManager assetManager,
        @Nullable List<AdTaxonomy.Term> categoryIds,
        @Nullable List<String> languages,
        @Nullable AdPriceSlot priceSlot,
        @Nullable String context
    ) {
        if (languages != null) this.languages = String.join(",", languages);
        if (categoryIds != null) this.categoryIds =
            String.join(",", categoryIds.stream().map(t -> t.id()).collect(Collectors.toList()));
        this.priceSlot = priceSlot.toString();
        this.context = context;
    }

    public void setFilter(Function<AdBidEvent, Boolean> filter) {
        this.filter = filter;
    }

    public ImmersiveAdControl(@Nonnull AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    protected ImmersiveAdControl() {}

    public void setActiveMaterialReplacement(Material material) {
        this.activeMaterialReplacement = material;
        this.replaceActiveMaterial = material != null;
    }

    public Material getActiveMaterialReplacement() {
        if (!replaceActiveMaterial) return null;
        if (activeMaterialReplacement == null && replaceActiveMaterial) {
            activeMaterialReplacement = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        }
        return activeMaterialReplacement != null ? activeMaterialReplacement.clone() : null;
    }

    public void setDeactiveMaterialReplacement(Material material) {
        this.deactiveMaterialReplacement = material;
        this.replaceDeactiveMaterial = material != null;
    }

    public Material getDeactiveMaterialReplacement() {
        if (!replaceDeactiveMaterial) return null;
        if (deactiveMaterialReplacement == null && replaceDeactiveMaterial) {
            deactiveMaterialReplacement = new Material(assetManager, Materials.PBR);
            deactiveMaterialReplacement.setColor("BaseColor", ColorRGBA.Black);
        }
        return deactiveMaterialReplacement;
    }

    protected String getSpatialProperty(Spatial sp, String key) {
        String value = null;
        if (sp != null) {
            value = ImmersiveAdProperties.getProperty(sp, key);
        }
        if (value == null || value.isEmpty()) {
            value = ImmersiveAdProperties.getProperty(spatial, key);
        }
        if (value == null || value.isEmpty()) {
            switch (key) {
                case ImmersiveAdProperties.categories:
                    {
                        value = categoryIds;
                        break;
                    }
                case ImmersiveAdProperties.languages:
                    {
                        value = languages;
                        break;
                    }
                case ImmersiveAdProperties.context:
                    {
                        value = context;
                        break;
                    }
                case ImmersiveAdProperties.priceslot:
                    {
                        value = priceSlot;
                        break;
                    }
            }
        }
        if (value != null && value.isEmpty()) {
            value = null;
        }
        return value;
    }

    public void setTextureKeys(String... keys) {
        this.textureKeys = keys;
    }

    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);
        needUpdate = true;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(textureKeys, "textureKeys", DEFAULT_TEXTURE_KEYS);
        oc.write(categoryIds, "categoryIds", null);
        oc.write(languages, "languages", null);
        oc.write(priceSlot, "priceSlot", null);
        oc.write(context, "context", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        assetManager = im.getAssetManager();
        textureKeys = ic.readStringArray("textureKeys", DEFAULT_TEXTURE_KEYS);
        categoryIds = ic.readString("categoryIds", null);
        languages = ic.readString("languages", null);
        priceSlot = ic.readString("priceSlot", null);
        context = ic.readString("context", null);
        needUpdate = true;
    }

    protected void applyTexture(Spatial sp, Texture tx) {
        sp.depthFirstTraversal(sx -> {
            if (!(sx instanceof Geometry)) return;
            Geometry geo = (Geometry) sx;
            Material mat = geo.getMaterial();

            Material matReplacement = getActiveMaterialReplacement();
            if (matReplacement != null) {
                geo.setMaterial(matReplacement);
                mat = matReplacement;
            } else {
                geo.setMaterial(geo.getMaterial().clone());
            }

            for (MatParam param : mat.getMaterialDef().getMaterialParams()) {
                if (param.getVarType() != VarType.Texture2D) continue;
                String name = param.getName();
                if (!Arrays.asList(textureKeys).contains(name)) continue;

                mat.setTexture(name, tx);
                break;
            }
        });
    }

    protected void prepareAdSpaces(Spatial sp, List<ImmersiveAdSpace> spaces) {
        String adspaceV = getSpatialProperty(sp, ImmersiveAdProperties.adspace);
        if (adspaceV != null) {
            AdSize size = AdSize.fromString(adspaceV);
            if (size != null) {
                try {
                    Material matReplacement = getDeactiveMaterialReplacement();
                    if (matReplacement != null) {
                        sp.depthFirstTraversal(sx -> {
                            if (!(sx instanceof Geometry)) return;
                            Geometry geo = (Geometry) sx;
                            geo.setMaterial(matReplacement);
                        });
                    }

                    ImmersiveAdSpace adSpace = new ImmersiveAdSpace(
                        () -> sp.getWorldBound(),
                        tx -> {
                            tx.setMinFilter(Texture.MinFilter.Trilinear);
                            tx.setMagFilter(Texture.MagFilter.Bilinear);
                            tx.setWrap(Texture.WrapMode.EdgeClamp);
                            tx.setAnisotropicFilter(4);
                            tx.setName("AdTexture_" + sp.getName() + "_" + size.toString());
                            applyTexture(sp, tx);
                        },
                        key -> getSpatialProperty(sp, key),
                        filter
                    );

                    spaces.add(adSpace);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to create ad space for " + sp.getName() + " with size " + size, e);
                }
            }
        } else if (sp instanceof Node) {
            for (Spatial child : ((Node) sp).getChildren()) {
                prepareAdSpaces(child, spaces);
            }
        }
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (needUpdate) {
            needUpdate = false;
            adSpaces.clear();
            if (spatial != null) {
                prepareAdSpaces(spatial, adSpaces);
            } else {
                logger.warning("Spatial is null, cannot update ad spaces.");
            }
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}

    public List<ImmersiveAdSpace> getSpaces() {
        return adSpaces;
    }
}
