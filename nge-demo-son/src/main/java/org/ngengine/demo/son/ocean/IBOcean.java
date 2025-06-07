/**
 * Copyright (c) 2025, Nostr Game Engine
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
 * the BSD 3-Clause License. The original jMonkeyEngine license is as follows:
 */
package org.ngengine.demo.son.ocean;

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
import com.jme3.texture.TextureArray;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class IBOcean implements Savable {

    private ArrayList<IBOceanLayer> layers;
    private Vector3f scale = new Vector3f(1, 1, 1);
    private Vector3f baseScale = new Vector3f(1, 1, 1);
    private TextureArray textureArray;
    private Vector2f scrolls[];
    private Vector2f tileSize;
    private transient Vector3f wind = new Vector3f(0, 0, 1f);
    private transient Material material;

    public IBOcean() {}

    public IBOcean(Vector2f tileSize, ArrayList<IBOceanLayer> layers, Vector3f patternScale) {
        this.layers = layers;
        this.tileSize = tileSize;
        List<Image> images = new ArrayList<>(layers.size());
        for (IBOceanLayer layer : layers) {
            images.add(layer.getImage());
        }
        this.textureArray = new TextureArray(images);
        this.textureArray.setWrap(Texture.WrapMode.Repeat);
        this.textureArray.setMagFilter(Texture.MagFilter.Bilinear);
        this.textureArray.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        this.textureArray.setName("OceanLayerArray");
        this.scrolls = new Vector2f[layers.size()];
        for (int i = 0; i < layers.size(); i++) {
            scrolls[i] = new Vector2f(0, 0);
        }
        this.baseScale.set(patternScale);
    }

    private Vector2f uv = new Vector2f(0, 0);

    public float getWaterHeight(Vector3f worldPos) {
        float height = 0;
        for (int i = 0; i < layers.size(); i++) {
            uv.set(worldPos.x, worldPos.z);
            uv.x += scrolls[i].x;
            uv.y += scrolls[i].y;
            uv.x *= scale.x * baseScale.x;
            uv.y *= scale.z * baseScale.z;
            uv.x = ((uv.x % tileSize.x) + tileSize.x) % tileSize.x;
            uv.y = ((uv.y % tileSize.y) + tileSize.y) % tileSize.y;
            uv.x /= tileSize.x;
            uv.y /= tileSize.y;
            IBOceanLayer layer = layers.get(i);
            height += layer.sample(uv.x, uv.y) * scale.y * baseScale.y;
        }
        return height / layers.size();
    }

    public void update(
        Instant time,
        AssetManager assetManager,
        Texture2D reflectionMap,
        Matrix4f refViewProjection,
        Vector3f wind
    ) {
        double tt = (double) Instant.now().toEpochMilli() / 1000.0;
        tt %= 60 * 60 * 10;
        Vector3f windDirection = wind.normalize();
        float windFactor = wind.length();
        windFactor *= 1.02f;
        windFactor = FastMath.clamp(windFactor, 0.1f, 10f);
        for (int i = 0; i < layers.size(); i++) {
            float layerFrequency = getLayerFrequency(i);
            float waveLength = 2.0f * FastMath.PI / layerFrequency;
            float baseSpeed = FastMath.sqrt(9.81f * waveLength / (2.0f * FastMath.PI));
            float windInfluence = FastMath.clamp(windFactor * 0.3f, 0.2f, 1.5f);
            float scrollSpeed = baseSpeed * windInfluence;
            float angleVariation = (i - 1) * 0.15f; // Small angle differences between layers
            Vector3f layerWindDir = rotateVector(windDirection, angleVariation);
            float speedMultiplier = getLayerSpeedMultiplier(i);
            scrollSpeed *= speedMultiplier;
            Vector2f offset = scrolls[i];
            offset.x = (float) (layerWindDir.x * scrollSpeed * tt);
            offset.y = (float) (layerWindDir.z * scrollSpeed * tt);
            offset.x = ((offset.x % tileSize.x) + tileSize.x) % tileSize.x;
            offset.y = ((offset.y % tileSize.y) + tileSize.y) % tileSize.y;
        }

        scale.set(1f, 13, 1f);
        Material mat = getMaterial(assetManager);
        wind.set(wind);
        mat.setTexture("RefMap", reflectionMap);
        mat.setMatrix4("ReflViewProj", refViewProjection);
        mat.setVector3("Wind", wind);
        mat.setVector3("BaseScale", baseScale);
    }

    public Material getMaterial(AssetManager assetManager) {
        if (material == null) {
            material = new Material(assetManager, "ibocean/Ocean.j3md");
            material.getAdditionalRenderState().setBlendMode(BlendMode.AlphaSumA);
            material.setVector3("Scale", scale);
            material.setVector3("BaseScale", baseScale);
            material.setVector3("Wind", wind);
            material.setTexture("OceanMap", textureArray);
            material.setVector2("TileSize", tileSize);
            material.setParam("Offsets", scrolls);
            material.setInt("NumLayers", layers.size());
            material
                .getAdditionalRenderState()
                .setStencil(
                    true,
                    RenderState.StencilOperation.Keep, //front triangle fails stencil test
                    RenderState.StencilOperation.Keep, //front triangle fails depth test
                    RenderState.StencilOperation.Keep, //front triangle passes depth test
                    RenderState.StencilOperation.Keep, //back triangle fails stencil test
                    RenderState.StencilOperation.Keep, //back triangle fails depth test
                    RenderState.StencilOperation.Keep, //back triangle passes depth test
                    RenderState.TestFunction.NotEqual, //front triangle stencil test function
                    RenderState.TestFunction.NotEqual
                ); //back triangle stencil test function

            material.getAdditionalRenderState().setFrontStencilReference(1);
            material.getAdditionalRenderState().setBackStencilReference(1);
            material.getAdditionalRenderState().setFrontStencilMask(0xFF);
            material.getAdditionalRenderState().setBackStencilMask(0xFF);

            Texture2D foamTexture = (Texture2D) assetManager.loadTexture("Common/MatDefs/Water/Textures/foam2.jpg");
            foamTexture.setWrap(WrapMode.Repeat);
            material.setTexture("FoamTexture", foamTexture);
        }
        return material;
    }

    public int getLayersCount() {
        return layers.size();
    }

    public void setScale(Vector3f scale) {
        this.scale.set(scale);
    }

    public Vector3f getScale() {
        return scale;
    }

    public IBOceanLayer getLayer(int index) {
        if (index < 0 || index >= layers.size()) {
            throw new IndexOutOfBoundsException("Layer index out of bounds: " + index);
        }
        return layers.get(index);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.writeSavableArrayList(layers, "layers", null);
        oc.write(scale, "scale", new Vector3f(1, 1, 1));
        oc.write(baseScale, "baseScale", new Vector3f(1, 1, 1));
        oc.write(textureArray, "textureArray", null);
        oc.write(scrolls, "offsets", null);
        oc.write(tileSize, "tileSize", new Vector2f(1, 1));
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        layers = ic.readSavableArrayList("layers", new ArrayList<>());
        scale = (Vector3f) ic.readSavable("scale", new Vector3f(1, 1, 1));
        baseScale = (Vector3f) ic.readSavable("baseScale", new Vector3f(1, 1, 1));
        textureArray = (TextureArray) ic.readSavable("textureArray", null);
        Savable scrollSavables[] = ic.readSavableArray("offsets", new Vector2f[layers.size()]);
        scrolls = new Vector2f[scrollSavables.length];
        System.arraycopy(scrollSavables, 0, scrolls, 0, scrollSavables.length);

        tileSize = (Vector2f) ic.readSavable("tileSize", new Vector2f(1, 1));
    }

    // Helper methods to add:
    private float getLayerFrequency(int layerIndex) {
        switch (layerIndex) {
            case 0:
                return 0.02f; // Large waves - low frequency
            case 1:
                return 0.08f; // Medium waves
            case 2:
                return 0.25f; // Small waves - high frequency
            default:
                return 0.1f * (layerIndex + 1);
        }
    }

    private float getLayerSpeedMultiplier(int layerIndex) {
        switch (layerIndex) {
            case 0:
                return 1.0f; // Large waves move at calculated speed
            case 1:
                return 0.85f; // Medium waves slightly slower
            case 2:
                return 0.65f; // Small waves move slower
            default:
                return 1.0f / FastMath.sqrt(layerIndex + 1);
        }
    }

    private Vector3f rotateVector(Vector3f vector, float angleRad) {
        float cos = FastMath.cos(angleRad);
        float sin = FastMath.sin(angleRad);
        return new Vector3f(vector.x * cos - vector.z * sin, vector.y, vector.x * sin + vector.z * cos);
    }
}
