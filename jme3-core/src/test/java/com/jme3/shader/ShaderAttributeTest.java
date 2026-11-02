package com.jme3.shader;

import com.jme3.scene.VertexBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShaderAttributeTest {
    @Test
    void enumAndNameShareTheSameAttributeLocation() {
        Shader shader = new Shader();
        Attribute position = shader.getAttribute(VertexBuffer.Type.Position);
        position.setLocation(3);
        assertSame(position, shader.getAttribute("inPosition"));
        assertEquals(3, shader.getAttribute("inPosition").getLocation());
    }

    @Test
    void namedLookupBeforeEnumLookupAlsoSharesTheLocation() {
        Shader shader = new Shader();
        Attribute instanceData = shader.getAttribute("inInstanceData");
        instanceData.setLocation(4);
        assertSame(instanceData, shader.getAttribute(VertexBuffer.Type.InstanceData));
        assertEquals(4, shader.getAttribute(VertexBuffer.Type.InstanceData).getLocation());
        assertNotSame(instanceData, shader.getAttribute("inTileData"));
        shader.setUpdateNeeded();
        assertEquals(-2, instanceData.getLocation());
    }
}
