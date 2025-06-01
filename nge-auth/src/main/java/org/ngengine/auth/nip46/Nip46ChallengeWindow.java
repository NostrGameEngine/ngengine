package org.ngengine.auth.nip46;

import java.util.logging.Logger;

import org.ngengine.gui.win.NWindow;
import org.ngengine.gui.components.containers.NColumn;
import org.ngengine.gui.win.NToast.ToastType;
import org.ngengine.platform.NGEPlatform;

import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.VAlignment;

public class Nip46ChallengeWindow extends NWindow<String[]> {
    private static final Logger log = Logger.getLogger(Nip46ChallengeWindow.class.getName());

    @Override
    protected void compose(Vector3f size, String[] args) throws Throwable {
        log.info("Composing Nip46ChallengeWindow with args: " + String.join(", ", args));
        String type = args[0];
        if(!type.equals("auth_url")){
            throw new IllegalArgumentException("Unknown challenge type: "+type);    
        }
        String challenge = args[1]; 
        setTitle("Resolve challenge");
        getContent().clearChildren();
        NColumn content = getContent().addCol();

        Label expl = new Label("Please complete the challenge on your remote signer");
        content.addChild(expl);

        Button open = new Button("Open");
        open.setTextHAlignment(HAlignment.Center);
        open.setTextVAlignment(VAlignment.Center);

        open.addClickCommands((b) -> {
            try {
                NGEPlatform platform = NGEPlatform.get();
                platform.openInWebBrowser(challenge);
            } catch (Exception e) {
              getManager().showToast(e);
            }
        });
        content.addChild(open);

        Button copy = new Button("Copy link");
        copy.setTextHAlignment(HAlignment.Center);
        copy.setTextVAlignment(VAlignment.Center);

        copy.addClickCommands((b) -> {
            try {
                NGEPlatform platform = NGEPlatform.get();
                platform.setClipboardContent(challenge);
                getManager().showToast(ToastType.INFO, "Link copied to clipboard");
            } catch (Exception e) {
                getManager().showToast(e);
            }
        });
        content.addChild(copy);

    }
    
}
