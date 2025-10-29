package ngetest.tests.world2d;

import org.ngengine.Components;
import org.ngengine.NGEApplication;
import org.ngengine.gui.win.NWindowManagerComponent;

public class TestWorld2d {
    
    public static void main(String arg[]){
        NGEApplication.createApp(
            app -> {
                Components.mount(app, new NWindowManagerComponent()).enable();
                Components.mount(app, new TiledWorld2DComponent("maps/jungle/jungle.tmx"), NWindowManagerComponent.class).enable();
            }
        ).run();
    }
}
