package org.ngengine.gui.win.std;

import java.util.function.Consumer;

import org.ngengine.gui.components.NTextInput;
import org.ngengine.gui.components.containers.NPanel;
 import org.ngengine.gui.win.NWindow;

import com.jme3.math.Vector3f;
import com.simsilica.lemur.component.BorderLayout;

 

public class NErrorWindow extends NWindow<Throwable> {

    @Override
    protected void compose(Vector3f size, Throwable args) throws Exception {
        String title = "Error: " + args.getClass().getSimpleName();
        setTitle(title);
        setFitContent(false);

        NPanel content = getContent();

        
        NTextInput errorLogField = new NTextInput();

        StringBuilder errorLog = new StringBuilder();
        Consumer<String> append = (str)->{
            // split lines longer than size.x/2;
            int charsPerLine = (int)(
                (size.x * 1.8f) /  errorLogField.getFontSize()
            );
            if (str.length() > charsPerLine) {
                int start = 0;
                while (start < str.length()) {
                    int end = Math.min(start + charsPerLine, str.length());
                    errorLog.append(str, start, end).append("\n");
                    start = end;
                }
            } else {
                errorLog.append(str).append("\n");
            }
            

        };

        append.accept(args.getMessage());
        append.accept("  \n\n");
  
        for (StackTraceElement element : args.getStackTrace()) {
            append.accept("    "+element.toString());
            append.accept("\n");
        }
        errorLogField.setText(errorLog.toString());
        errorLogField.setSingleLine(false);
        // errorLogField.setTextVAlignment(VAlignment.Top);
        errorLogField.setPasteAction(null);
        content.addChild(errorLogField, BorderLayout.Position .Center);

        
    }

    
}
