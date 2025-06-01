package org.ngengine.auth.stored;

import org.ngengine.gui.components.NVSpacer;
import org.ngengine.gui.components.containers.NColumn;
import org.ngengine.gui.win.NWindow;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.DynamicInsetsComponent;
import com.simsilica.lemur.component.IconComponent;
import com.simsilica.lemur.style.ElementId;

public class StoredAuthSelectionWindow extends NWindow<StoredAuthSelectionOptions> {

    @Override
    protected void compose(Vector3f size, StoredAuthSelectionOptions args) throws Throwable {
        setTitle("Identity Selected");
        Label label = new Label(args.getAlias(), new ElementId("highlighted").child(Label.ELEMENT_ID));
        label.setTextHAlignment(HAlignment.Center);

        Button confirmButton = new Button("Continue as "+args.getAlias(), new ElementId("highlighted").child(Button.ELEMENT_ID));
        confirmButton.addClickCommands((b) -> {
            if (args.getConfirmAction() != null) {
                args.getConfirmAction().accept(this);
            }
            close();
        });
        

        Button cancelButton = new Button("Go back");
        cancelButton.addClickCommands((b) -> {
            if (args.getCancelAction() != null) {
                args.getCancelAction().accept(this);
            }
            close();
        });

        Button removeButton = new Button("Delete identity", new ElementId("danger").child(Button.ELEMENT_ID));
        removeButton.addClickCommands((b) -> {
            if (args.getRemoveAction() != null) {
                args.getRemoveAction().accept(this);
            }
            close();
        });
        getContent().clearChildren();
        NColumn windowContent = getContent().addCol();

        if(args.getIcon()!=null){
            IconComponent icon = new IconComponent(
                    args.getIcon(),
                    new Vector2f(1, 1),0,0,0,false);
            float iconSize = Math.min(size.x,size.y) * 0.5f;

            icon.setIconSize(new Vector2f(iconSize, iconSize));
            icon.setMargin(0,0);
            Label iconLabel = new Label("");
            iconLabel.setIcon(icon);
            windowContent.addChild(iconLabel);
            iconLabel.setInsetsComponent(new DynamicInsetsComponent(1,1,1,1));

        }
       
        windowContent.addChild(label);
        windowContent.addChild(new NVSpacer());

        windowContent.addChild(confirmButton);
        windowContent.addChild(cancelButton);
        windowContent.addChild(new NVSpacer());
        windowContent.addChild(removeButton);
    }
    
}
