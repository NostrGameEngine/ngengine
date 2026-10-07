package com.jme3.shadow;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.OpaqueComparator;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShadowUtilTraversalTest {
    @Test
    void nestedTraversalKeepsVisibilityAndShadowModeFiltering() {
        Camera camera = new Camera(64, 64);
        camera.setFrustumPerspective(45, 1, .1f, 50);
        camera.lookAt(new Vector3f(0, 0, -1), Vector3f.UNIT_Y);
        Node root = new Node();
        Node nested = new Node();
        root.attachChild(nested);
        Geometry visible = new Geometry("visible", new Box(.2f, .2f, .2f));
        Geometry outside = new Geometry("outside", new Box(.2f, .2f, .2f));
        Geometry receiver = new Geometry("receiver", new Box(.2f, .2f, .2f));
        Geometry hidden = new Geometry("hidden", new Box(.2f, .2f, .2f));
        visible.setLocalTranslation(0, 0, -5);
        outside.setLocalTranslation(100, 0, -5);
        receiver.setLocalTranslation(0, 0, -5);
        hidden.setLocalTranslation(0, 0, -5);
        visible.setShadowMode(ShadowMode.Cast);
        outside.setShadowMode(ShadowMode.Cast);
        receiver.setShadowMode(ShadowMode.Receive);
        hidden.setShadowMode(ShadowMode.Cast);
        hidden.setCullHint(Spatial.CullHint.Always);
        nested.attachChild(visible);
        nested.attachChild(outside);
        root.attachChild(receiver);
        root.attachChild(hidden);
        root.updateGeometricState();
        GeometryList output = new GeometryList(new OpaqueComparator());
        ShadowUtil.getGeometriesInCamFrustum(root, camera, ShadowMode.Cast, output);
        assertEquals(1, output.size());
        assertSame(visible, output.get(0));
    }
}
