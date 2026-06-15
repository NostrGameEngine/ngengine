/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.app;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Statistics;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The <code>StatsView</code> provides a heads-up display (HUD) of various
 * statistics of rendering. The data is retrieved every frame from a
 * {@link com.jme3.renderer.Statistics} and then displayed on screen.
 * <p>
 * To use the stats view, you need to retrieve the
 * {@link com.jme3.renderer.Statistics} from the
 * {@link com.jme3.renderer.Renderer} used by the application. Then, attach
 * the <code>StatsView</code> to the scene graph.
 * <pre>
 * Statistics stats = renderer.getStatistics();
 * StatsView statsView = new StatsView("MyStats", assetManager, stats);
 * rootNode.attachChild(statsView);
 * </pre>
 */
public class StatsView extends Node implements Control, JmeCloneable {
    private final BitmapText statText;
    private final Statistics statistics;

    private final String[] statLabels;
    private final int[] statData;
    private final List<Supplier<String>> customStatLines = new ArrayList<>();

    private boolean enabled = true;
    private int lineCount;

    private final StringBuilder stringBuilder = new StringBuilder();
    private final double[] uploadMbpsHistory = new double[5];
    private long uploadSampleBytes;
    private float uploadSampleTime;
    private double smoothedUploadMbps;
    private int uploadSampleCount;
    private int uploadHistoryIndex;

    public StatsView(String name, AssetManager manager, Statistics stats) {
        super(name);

        setQueueBucket(Bucket.Gui);
        setCullHint(CullHint.Never);

        statistics = stats;
        statistics.setEnabled(enabled);

        statLabels = statistics.getLabels();
        statData = new int[statLabels.length];
        lineCount = statLabels.length;

        BitmapFont font = manager.loadFont("Interface/Fonts/Console.j3o");
        statText = new BitmapText(font);
        statText.setLocalTranslation(0, statText.getLineHeight() * statLabels.length, 0);
        attachChild(statText);

        addControl(this);
    }

    public float getHeight() {
        return statText.getLineHeight() * lineCount;
    }

    public void addCustomStatLine(Supplier<String> supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("Custom stat supplier cannot be null.");
        }
        customStatLines.add(supplier);
        lineCount = statLabels.length + customStatLines.size();
    }

    public boolean removeCustomStatLine(Supplier<String> supplier) {
        boolean removed = customStatLines.remove(supplier);
        if (removed) {
            lineCount = statLabels.length + customStatLines.size();
        }
        return removed;
    }

    public void clearCustomStatLines() {
        customStatLines.clear();
        lineCount = statLabels.length;
    }

    @Override
    public void update(float tpf) {
        if (!isEnabled()) {
            return;
        }

        statistics.getData(statData);
        stringBuilder.setLength(0);

        int lines = statLabels.length + customStatLines.size() + 1;
        // Need to walk through it backwards, as the first label
        // should appear at the bottom, not the top.
        for (int i = statLabels.length - 1; i >= 0; i--) {
            stringBuilder.append(statLabels[i]).append(" = ").append(statData[i]);
            if ("Objects".equals(statLabels[i]) && statistics.getNumMeshInstances() > 0) {
                stringBuilder.append(" \\#ddd9#+").append(statistics.getNumMeshInstances()).append("\\#ffff#");
            }
            stringBuilder.append('\n');
        }
        stringBuilder.append("CPU Upload = ");
        updateUploadBandwidth(tpf);
        appendMbps(smoothedUploadMbps);
        stringBuilder.append(" (^");
        appendMbps(getUploadMbpsPeak());
        stringBuilder.append(')');
        stringBuilder.append('\n');
        for (Supplier<String> supplier : customStatLines) {
            String line = supplier.get();
            if (line != null && !line.isEmpty()) {
                stringBuilder.append(line).append('\n');
            } else {
                lines--;
            }
        }
        lineCount = Math.max(1, lines);
        statText.setLocalTranslation(0, statText.getLineHeight() * lineCount, 0);
        statText.setText(stringBuilder);

        // Moved to ResetStatsState to make sure it is
        // done even if there is no StatsView or the StatsView
        // is disabled.
        //statistics.clearFrame();
    }

    private void updateUploadBandwidth(float tpf) {
        uploadSampleBytes += statistics.getCpuToGpuUploadBytes();
        uploadSampleTime += tpf;
        if (uploadSampleTime < 1f) {
            return;
        }

        double mbps = uploadSampleBytes * 8.0 / uploadSampleTime / 1_000_000.0;
        smoothedUploadMbps = uploadSampleCount == 0 ? mbps : smoothedUploadMbps * 0.65 + mbps * 0.35;
        uploadMbpsHistory[uploadHistoryIndex] = mbps;
        uploadHistoryIndex = (uploadHistoryIndex + 1) % uploadMbpsHistory.length;
        if (uploadSampleCount < uploadMbpsHistory.length) {
            uploadSampleCount++;
        }
        uploadSampleBytes = 0;
        uploadSampleTime = 0f;
    }

    private double getUploadMbpsPeak() {
        double peak = smoothedUploadMbps;
        for (int i = 0; i < uploadSampleCount; i++) {
            peak = Math.max(peak, uploadMbpsHistory[i]);
        }
        return peak;
    }

    private void appendMbps(double mbps) {
        if (mbps >= 100.0) {
            stringBuilder.append((long) mbps);
        } else {
            long tenths = Math.round(mbps * 10.0);
            stringBuilder.append(tenths / 10).append('.').append(tenths % 10);
        }
        stringBuilder.append(" Mbps");
    }

    @Deprecated
    @Override
    public Control cloneForSpatial(Spatial spatial) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StatsView jmeClone() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void cloneFields(Cloner cloner, Object original) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void setSpatial(Spatial spatial) {
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        statistics.setEnabled(enabled);
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void render(RenderManager rm, ViewPort vp) {
    }
}
