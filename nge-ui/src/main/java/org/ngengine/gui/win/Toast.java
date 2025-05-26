package org.ngengine.gui.win;

import java.time.Duration;
import java.time.Instant;

import org.ngengine.gui.components.IconButton;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.terrain.noise.Color;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.VAlignment;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.BoxLayout;
import com.simsilica.lemur.component.DynamicInsetsComponent;
import com.simsilica.lemur.core.GuiControl;
import com.simsilica.lemur.core.GuiControlListener;
import com.simsilica.lemur.core.GuiLayout;
import com.simsilica.lemur.core.GuiUpdateListener;
import com.simsilica.lemur.style.ElementId;

public class Toast extends Container   {
    public static final String ELEMENT_ID  = "toast";
    
    public enum ToastType {
        INFO,
        WARNING,
        ERROR
    }

    protected ToastType type;
    protected Label message;
    protected Duration duration;
    protected Instant creationTime;
    protected WindowManagerAppState appState;
    protected boolean closeable = false;
    protected IconButton closeBtn;

    protected Toast(ToastType type, String message, Duration duration) {
        super(new BorderLayout(), new ElementId(type.name().toLowerCase()+"."+ELEMENT_ID));
        this.type = type;
        this.message = new Label(message, new ElementId(type.name().toLowerCase() + "." + ELEMENT_ID+".label"));
        addChild(this.message, BorderLayout.Position.Center);
        
        IconButton icon = new IconButton("icons/outline/activity.svg", 
                type.name().toLowerCase() + "." +"toast."+IconButton.ELEMENT_ID);
        
        addChild(icon, BorderLayout.Position.West);

        closeBtn = new IconButton("icons/outline/x.svg", 
                type.name().toLowerCase() + "." +"toast.close." + IconButton.ELEMENT_ID);
        closeBtn.setInsetsComponent(new DynamicInsetsComponent(0,1f,1f,0f));
        creationTime = Instant.now();

        setCloseable(false);
        setDuration(duration);
        setCloseAction(()->{
            close();
        });

    }

    public void setCloseable(boolean closeable) {
        closeBtn.removeFromParent();
        this.closeable = closeable;
        if(closeable){
            addChild(closeBtn, BorderLayout.Position.East);
        }
    }   

    public boolean isCloseable() {
        return closeable;
    }

    public void setCloseAction(Runnable action) {
        closeBtn.addClickCommands((src)->{
            if(action != null){
                action.run();
            }
        });
    }

    public void close(){
        if(appState != null){
            appState.closeToast(this);
        } else{
            removeFromParent();
        }
    }

    protected void initialize(WindowManagerAppState appState) { 
        this.appState = appState;
    }

    protected WindowManagerAppState getManager(){
        return appState;
    }

    public  Instant getCreationTime() {
        return creationTime;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
        if(!closeable&&duration ==null){
            setCloseable(true);
        }
    }

    public Duration getDuration() {
        return duration;
    }

    public Toast(){
        this(ToastType.INFO, "", Duration.ofSeconds(5));
    }

    public void setType(ToastType type) {
        this.type = type;
    }

    public void setMessage(String message) {
        this.message.setText(message);
    }

    public ToastType getType() {
        return type;
    }

    public String getMessage() {
        return message.getText();
    }


    
}
