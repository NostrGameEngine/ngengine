package org.ngengine.gui.win.std;

import java.util.function.Consumer;

import org.ngengine.gui.components.NButton;
import org.ngengine.gui.components.NLabel;
import org.ngengine.gui.components.containers.NPanel;
import org.ngengine.gui.win.NWindow;

import com.jme3.math.Vector3f;
import com.simsilica.lemur.component.BorderLayout;

 
public class NConfirmDialogWindow extends NWindow<NConfirmDialogOptions> {

    @Override
    protected void compose(Vector3f size, NConfirmDialogOptions args) throws Throwable {
        NButton confirmButton = new NButton(args.getConfirmButtonText());
        confirmButton.addClickCommands((b) -> {
            args.getConfirmAction().accept(this);
        });
        NButton cancelButton = new NButton(args.getCancelButtonText());
        cancelButton.addClickCommands((b) -> {
            args.getCancelAction().accept(this);
        });
        
         
        NPanel content = getContent( );
        content.addChild(new NLabel(args.getText()), BorderLayout.Position .North);
        content.addChild(confirmButton, BorderLayout.Position.West);
        content.addChild(cancelButton, BorderLayout.Position.East);
        
    }
 
    
}
