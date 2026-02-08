package ngetests.gui;

import org.ngengine.gui.components.NButton;
import org.ngengine.gui.components.NLabel;
import org.ngengine.gui.components.NTextInput;
import org.ngengine.gui.components.NVSpacer;
import org.ngengine.gui.components.containers.NColumn;
import org.ngengine.gui.components.containers.NPanel;
import org.ngengine.gui.components.containers.NRow;
import org.ngengine.gui.win.NWindow;

import com.jme3.math.Vector3f;
import com.simsilica.lemur.FillMode;

public class MainWindow extends NWindow<Object> {

    @Override
    protected void compose(Vector3f size, Object args) throws Throwable {
        setWithTitleBar(false);
        
        NPanel panel = getContent();
        
        NRow r = panel.addRow();
        NLabel label = new NLabel("Welcome to this demo");
        r.addChild(label);
        
        r = panel.addRow();
        NColumn c1 = r.addCol();
        NColumn c2 = r.addCol();

        label = new NLabel("Write something:");
        c1.addChild(label);

        
        
        NTextInput input = new NTextInput();
        c2.addChild(input);

        r = panel.addRow();
        r.addChild(new NVSpacer());

        NButton btn = new NButton("Click me!");
        r.addChild(btn);
        btn.addClickCommands((b)->{
            System.out.println("Button clicked! Input text: " + input.getText());
        });

    }
    
}
