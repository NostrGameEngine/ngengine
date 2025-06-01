package org.ngengine.demo.son;

import com.jme3.input.InputManager;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.system.SystemListener;
import com.jme3.system.SystemListenerAggregator;
public class FocusGuardAppState extends NGEAppState implements SystemListener, RawInputListener {
    private boolean readyToObtainFocus = false;
    private boolean readyToLoseFocus = false;
    private boolean focus = true;
    private Boolean cursorVisible = null;

    @Override
    protected void onEnable() {
        SystemListener lis = getApplication().getContext().getSystemListener();
        if(lis instanceof SystemListenerAggregator) {
            SystemListenerAggregator aggregator = (SystemListenerAggregator) lis;
            aggregator.addListener(this);
        }
        InputManager inputManager = getApplication().getInputManager();
        inputManager.addRawInputListener(this);
    }

    @Override
    protected void onDisable() {
        InputManager inputManager = getApplication().getInputManager();
        inputManager.removeRawInputListener(this);
         
    }

    @Override
    public void initialize() {
      
    }

    @Override
    public void reshape(int width, int height) {
        
    }

    @Override
    public void update() {
        
        if(readyToLoseFocus){
            setFocus(false);
            readyToLoseFocus = false;
        }
        if(readyToObtainFocus){
            setFocus(true);
            readyToObtainFocus = false;
        }
       
    }

    @Override
    public void requestClose(boolean esc) {

    }

    @Override
    public void gainFocus() {
        
    }

    @Override
    public void loseFocus() {
        readyToLoseFocus = true;
        
    }

    public void setFocus(boolean v) {
        
        InputManager inputManager = getApplication().getInputManager();
        if(v){
            focus = v;
            if(cursorVisible == null) return;
            inputManager.setCursorVisible(cursorVisible);
            cursorVisible = null;
            
        } else{
            if(cursorVisible == null) cursorVisible = inputManager.isCursorVisible();
            inputManager.setCursorVisible(true);
            focus = v;
        }
        
    }

    @Override
    public void handleError(String errorMsg, Throwable t) {
        
    }

    @Override
    public void destroy() {
       
    }
 

    @Override
    public void beginInput() {
     
    }

    @Override
    public void endInput() {
 
    }

    @Override
    public void onJoyAxisEvent(JoyAxisEvent evt) {
      
    }

    @Override
    public void onJoyButtonEvent(JoyButtonEvent evt) {
        

    }

    @Override
    public void onMouseMotionEvent(MouseMotionEvent evt) {
        
    }

    @Override
    public void onMouseButtonEvent(MouseButtonEvent evt) {
        if(!focus){
            readyToObtainFocus = true;
        }

    }

    @Override
    public void onKeyEvent(KeyInputEvent evt) {
        if(!focus){
            readyToObtainFocus = true;
        }

    }

    @Override
    public void onTouchEvent(TouchEvent evt) {
        if(!focus){
            readyToObtainFocus = true;
        }
     
    }

}