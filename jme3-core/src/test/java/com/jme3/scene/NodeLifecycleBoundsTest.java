package com.jme3.scene;

import com.jme3.bounding.BoundingBox;
import com.jme3.light.DirectionalLight;
import com.jme3.math.Vector3f;
import com.jme3.scene.shape.Box;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NodeLifecycleBoundsTest {
    @Test
    void globalLightRebuildPreservesUnchangedBranches() {
        Node root = new Node("root");
        Node first = new Node("first");
        Node second = new Node("second");
        root.attachChild(first);
        root.attachChild(second);
        DirectionalLight firstLight = new DirectionalLight(true);
        DirectionalLight secondLight = new DirectionalLight(true);
        first.addLight(firstLight);
        second.addLight(secondLight);
        root.updateGeometricState();
        assertEquals(2, root.getWorldLightList().size());
        first.removeLight(firstLight);
        root.updateGeometricState();
        assertEquals(1, root.getWorldLightList().size());
        assertSame(secondLight, root.getWorldLightList().get(0));
        assertSame(secondLight, first.getWorldLightList().get(0));
        first.addLight(firstLight);
        root.updateGeometricState();
        assertEquals(2, root.getWorldLightList().size());
    }

    @Test
    void detachedGlobalLightLeavesTheOldRoot() {
        Node root = new Node("root");
        Node owner = new Node("owner");
        DirectionalLight light = new DirectionalLight(true);
        owner.addLight(light);
        root.attachChild(owner);
        root.updateGeometricState();
        assertSame(light, root.getWorldLightList().get(0));
        root.detachChild(owner);
        root.updateGeometricState();
        assertEquals(0, root.getWorldLightList().size());
        Node otherRoot = new Node("other-root");
        otherRoot.attachChild(owner);
        otherRoot.updateGeometricState();
        assertSame(light, otherRoot.getWorldLightList().get(0));
    }

    @Test
    void detachedSubtreeRemovesDescendantGlobalLights() {
        Node root = new Node("root");
        Node subtree = new Node("subtree");
        Node owner = new Node("owner");
        DirectionalLight light = new DirectionalLight(true);
        owner.addLight(light);
        subtree.attachChild(owner);
        root.attachChild(subtree);
        root.updateGeometricState();
        assertSame(light, root.getWorldLightList().get(0));
        root.detachChild(subtree);
        root.updateGeometricState();
        assertEquals(0, root.getWorldLightList().size());
        subtree.updateGeometricState();
        assertSame(light, subtree.getWorldLightList().get(0));
    }

    @Test
    void emptyMovingNodeReusesItsBound() {
        Node node = new Node();
        node.updateGeometricState();
        BoundingBox bound = (BoundingBox) node.getWorldBound();
        node.setLocalTranslation(2, 3, 4);
        node.updateGeometricState();
        assertSame(bound, node.getWorldBound());
        assertEquals(new Vector3f(2, 3, 4), bound.getCenter());
        assertEquals(0f, bound.getXExtent());
        assertEquals(0f, bound.getYExtent());
        assertEquals(0f, bound.getZExtent());
    }

    @Test
    void removingLastChildClearsReusedExtents() {
        Node node = new Node();
        node.attachChild(new Geometry("box", new Box(2, 3, 4)));
        node.updateGeometricState();
        BoundingBox bound = (BoundingBox) node.getWorldBound();
        assertEquals(2f, bound.getXExtent());
        node.detachAllChildren();
        node.setLocalTranslation(5, 6, 7);
        node.updateGeometricState();
        assertSame(bound, node.getWorldBound());
        assertEquals(new Vector3f(5, 6, 7), bound.getCenter());
        assertEquals(0f, bound.getXExtent());
        assertEquals(0f, bound.getYExtent());
        assertEquals(0f, bound.getZExtent());
    }
}
