package org.ngengine.world2d;

import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;

/**
 * Provides the scene and GUI targets used to render one point of view of a
 * {@link TiledWorld2d}.
 */
public interface PovRenderer {
    /**
     * Returns the viewport that receives tiled world scene geometry.
     *
     * @return the scene viewport, or {@code null} when the POV is not currently renderable
     */
    public ViewPort getSceneViewPort();

    /**
     * Returns the viewport that receives GUI fragments associated with the tiled world.
     *
     * @return the GUI viewport, or {@code null} when no GUI viewport is available
     */
    public ViewPort getGuiViewPort();

    /**
     * Returns or creates a general-purpose GUI scene node for this POV.
     *
     * @param i the GUI scene index
     * @return the GUI node, or {@code null} when no GUI viewport is available
     */
    public Node getGuiNode(int i);

    /**
     * Returns or creates a general-purpose scene node for this POV.
     *
     * @param i the scene index
     * @return the scene node, or {@code null} when no scene viewport is available
     */
    public Node getSceneNode(int i);
}
