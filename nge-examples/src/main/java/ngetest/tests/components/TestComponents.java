package ngetest.tests.components;

import org.ngengine.Components;
import org.ngengine.NGEApplication;
import org.ngengine.gui.win.NWindowManagerComponent;

public class TestComponents {
    
    public static void main(String arg[]){
        NGEApplication.createApp(
            app -> {
                Components.mount(app, new NWindowManagerComponent()).enable();
                Components.mount(app, new AppComponent(), NWindowManagerComponent.class).enable();
            }
        ).run();
    }
}
