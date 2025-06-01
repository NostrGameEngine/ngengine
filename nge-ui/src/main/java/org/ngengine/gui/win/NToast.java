package org.ngengine.gui.win;

import java.time.Duration;
import java.time.Instant;

import org.ngengine.gui.components.NIconButton;

import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.DynamicInsetsComponent;
import com.simsilica.lemur.style.ElementId;

public class NToast extends Container   {
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
    protected NWindowManagerAppState appState;
    protected boolean closeable = false;
    protected NIconButton closeBtn;

    protected NToast(ToastType type, String message, Duration duration) {
        super(new BorderLayout(), new ElementId(type.name().toLowerCase()+"."+ELEMENT_ID));
        this.type = type;
        this.message = new Label(message, new ElementId(type.name().toLowerCase() + "." + ELEMENT_ID+".label"));
        addChild(this.message, BorderLayout.Position.Center);
        
        NIconButton icon = new NIconButton("icons/outline/activity.svg", 
                type.name().toLowerCase() + "." +"toast."+NIconButton.ELEMENT_ID);
        
        addChild(icon, BorderLayout.Position.West);

        closeBtn = new NIconButton("icons/outline/x.svg", 
                type.name().toLowerCase() + "." +"toast.close." + NIconButton.ELEMENT_ID);
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

    protected void initialize(NWindowManagerAppState appState) { 
        this.appState = appState;
    }

    protected NWindowManagerAppState getManager(){
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

    public NToast(){
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
