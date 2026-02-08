package ngetests.gui;

import org.ngengine.Components;
import org.ngengine.NGEApplication;
import org.ngengine.NGEApplication.NGEAppRunner;
import org.ngengine.gui.win.NWindowManagerComponent;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.platform.jvm.JVMAsyncPlatform;

public class TestNGEGui {

    public static void main(String arg[]) {
        NGEPlatform.set(new JVMAsyncPlatform());
        NGEAppRunner appBuilder = NGEApplication.createApp(app -> {
            Components.mount(app, new NWindowManagerComponent()).enable();
            Components.mount(app, new MainComponent()).enable();
            
        });
        appBuilder.run();
    }

}
