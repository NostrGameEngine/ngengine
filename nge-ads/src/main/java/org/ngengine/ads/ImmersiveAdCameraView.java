package org.ngengine.ads;

import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.HashMap;
import java.util.Map;

import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.gui.win.NWindowManagerComponent;
import org.ngengine.gui.win.NToast;
import org.ngengine.gui.win.NToast.ToastType;
import org.ngengine.runner.Runner;
import org.ngengine.store.DataStoreProvider;

import com.jme3.bounding.BoundingVolume;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.Camera.FrustumIntersect;

public class ImmersiveAdCameraView implements ImmersiveAdViewer  {

    private final Camera cam;
    private float maxDistance = 1000.0f;
    private float screenToolTipTriggerDistance = 0.05f;
    private float  worldTooltipDistanceTrigger = 10f;

    private ComponentManager mng;
    
    private NToast currentToast;
    private ImmersiveAdSpace currentSpace;

    public ImmersiveAdCameraView(ComponentManager mng, Camera cam){
        this.cam = cam;    
        this.mng = mng;
    }

    public void setMaxDistance(float maxDistance) {
        this.maxDistance = maxDistance;
    }

    @Override
    public boolean isNear(ImmersiveAdSpace space) {
        BoundingVolume bvol = space.getBounds();
        float d = bvol.getCenter().distance(cam.getLocation());
        return d <= maxDistance;
    }

    @Override
    public boolean isVisible(ImmersiveAdSpace space){
        if(!isNear(space)) return false;
        FrustumIntersect frustumState = cam.contains(space.getBounds());
        if(frustumState==FrustumIntersect.Outside) return false;

        return true;              
    }


    private float getScreenCenterDistance(ImmersiveAdSpace space) {
        // Get the center of the ad space bounds
        Vector3f worldPos = space.getBounds().getCenter();
        
        // Project to screen coordinates
        Vector3f screenPos = cam.getScreenCoordinates(worldPos);
        
        // Get screen dimensions
        float screenWidth = cam.getWidth();
        float screenHeight = cam.getHeight();
        
        // Calculate normalized coordinates where (0,0) is screen center
        float normX = (screenPos.x - (screenWidth / 2)) / (screenWidth / 2);
        float normY = (screenPos.y - (screenHeight / 2)) / (screenHeight / 2);
        
        // Distance from center (0 = center, 1 = corner of the screen)
        float distance = Math.min(1.0f, FastMath.sqrt(normX * normX + normY * normY));
        
        return distance;
    }

    @Override
    public void showInfo(ImmersiveAdSpace space, String description, String callToAction, String link) {
        NWindowManagerComponent windowManager = mng.getComponent(NWindowManagerComponent.class);
        if (windowManager != null) {
            float d = getScreenCenterDistance(space);
            Vector3f worldPos = space.getBounds().getCenter();
            
            if (currentToast == null || d < screenToolTipTriggerDistance && space != currentSpace && worldPos.distance(cam.getLocation()) <= worldTooltipDistanceTrigger) {
                currentSpace = space;
                if (currentToast != null) currentToast.removeFromParent();
                windowManager.showToast(ToastType.INFO,
                        description + "\n> " + callToAction, Duration.ofMinutes(1),
                        (toast, err) -> {
                            currentToast = toast;
                        });
                
            }
        }
    }

    @Override
    public void beginUpdate() {

    }

    @Override
    public void endUpdate() {
     
    }
 
    
}
