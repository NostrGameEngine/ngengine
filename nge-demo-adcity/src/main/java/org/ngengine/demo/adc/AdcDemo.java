package org.ngengine.demo.adc;

import org.ngengine.NGEApplication;
import org.ngengine.components.ComponentManager;
import org.ngengine.gui.win.NWindowManagerComponent;
import org.ngengine.nostr4j.keypair.NostrPublicKey;

import com.jme3.system.AppSettings;

public class AdcDemo {
    
    public static void main(String arg[]){
        AppSettings settings = new AppSettings(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL32);
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setGammaCorrection(true);
        settings.setSamples(2);
        settings.setStencilBits(8);
        settings.setDepthBits(24);
        settings.setVSync(true);
        settings.setGraphicsDebug(false);
        settings.setTitle("Nostr Game Engine Demo");
        settings.setX11PlatformPreferred(true);
        

        
        Runnable appBuilder = NGEApplication.createApp(settings, app -> {
            app.enableAds();


            ComponentManager mng = app.getComponentManager();
            mng.addAndEnableComponent(new AdCity());
            mng.addAndEnableComponent(new NWindowManagerComponent());
        
            
            app.getJme3App().setFlyCamEnabled(true);
            app.getJme3App().getInputManager().setCursorVisible(false);
            
            
        });
    
        appBuilder.run();
    }
}
