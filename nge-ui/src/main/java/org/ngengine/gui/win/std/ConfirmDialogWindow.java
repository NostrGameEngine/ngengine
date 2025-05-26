package org.ngengine.gui.win.std;

import java.util.function.Consumer;

import org.ngengine.gui.win.Window;

import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.BorderLayout;

 
public class ConfirmDialogWindow extends Window<ConfirmDialogOptions>{

    @Override
    protected void compose(Vector3f size, ConfirmDialogOptions args) throws Throwable {
        Button confirmButton = new Button(args.getConfirmButtonText());
        confirmButton.addClickCommands((b) -> {
            args.getConfirmAction().accept(this);
        });
        Button cancelButton = new Button(args.getCancelButtonText());
        cancelButton.addClickCommands((b) -> {
            args.getCancelAction().accept(this);
        });
        
        Label textLabel = new Label(args.getText());
        
        Container content = getContent(new BorderLayout());
        content.addChild(textLabel, BorderLayout.Position.North);
        content.addChild(confirmButton, BorderLayout.Position.West);
        content.addChild(cancelButton, BorderLayout.Position.East);
        
    }
 
    
}
