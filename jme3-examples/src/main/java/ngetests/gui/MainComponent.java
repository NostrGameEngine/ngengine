package ngetests.gui;

import org.ngengine.components.AbstractComponent;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.gui.win.NWindowManagerComponent;

public class MainComponent extends AbstractComponent {

    @Override
    protected void onEnable(ComponentManager mng, boolean firstTime) {
        NWindowManagerComponent win = getInstanceOf(NWindowManagerComponent.class);
        win.showWindow(MainWindow.class);
    }

    @Override
    protected void onDisable(ComponentManager mng) {
    }
    
}
