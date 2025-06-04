package org.ngengine.components.fragments;

import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;

import com.jme3.input.InputManager;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;

public interface  InputHandlerFragment extends Fragment, RawInputListener {
    public static class Wrapper implements RawInputListener {
        private final InputHandlerFragment fragment;
        private final ComponentManager fragmentManager;
        public Wrapper(ComponentManager fragmentManager, InputHandlerFragment fragment) {
            this.fragment = fragment;
            this.fragmentManager = fragmentManager;
        }

        @Override
        public void beginInput() {
            if(!(fragment instanceof Component) ||fragmentManager.isComponentEnabled((Component)fragment)) {
                fragment.beginInput();
            }
        }

        @Override
        public void endInput() {
            if(!(fragment instanceof Component) ||fragmentManager.isComponentEnabled((Component)fragment)) {
                fragment.endInput();
            }
        }

        @Override
        public void onJoyAxisEvent(JoyAxisEvent evt) {
            if(!(fragment instanceof Component) ||fragmentManager.isComponentEnabled((Component)fragment)) {
                fragment.onJoyAxisEvent(evt);
            }
        }

        @Override
        public void onJoyButtonEvent(JoyButtonEvent evt) {
            if(!(fragment instanceof Component) ||fragmentManager.isComponentEnabled((Component)fragment)) {
                fragment.onJoyButtonEvent(evt);
            }
        }

        @Override
        public void onMouseMotionEvent(MouseMotionEvent evt) {
            if(!(fragment instanceof Component) ||fragmentManager.isComponentEnabled((Component)fragment)) {
                fragment.onMouseMotionEvent(evt);
            }
        }

        @Override
        public void onMouseButtonEvent(MouseButtonEvent evt) {
            if(!(fragment instanceof Component) ||fragmentManager.isComponentEnabled((Component)fragment)) {
                fragment.onMouseButtonEvent(evt);
            }
        }

        @Override
        public void onKeyEvent(KeyInputEvent evt) {
            if(!(fragment instanceof Component) ||fragmentManager.isComponentEnabled((Component)fragment)) {
                fragment.onKeyEvent(evt);
            }
        }

        @Override
        public void onTouchEvent(TouchEvent evt) {
            if(!(fragment instanceof Component)||fragmentManager.isComponentEnabled((Component)fragment)) {
                fragment.onTouchEvent(evt);
            }
        }
    
        
    }

    @Override
    public default void beginInput() {
        
    }

    @Override
    public default void endInput() {
        
    }

    public default void receiveInputManager(InputManager inputManager){

    }

    public void onJoyAxisEvent(JoyAxisEvent evt);

    public void onJoyButtonEvent(JoyButtonEvent evt);

    public void onMouseMotionEvent(MouseMotionEvent evt) ;

    public void onMouseButtonEvent(MouseButtonEvent evt) ;

    public void onKeyEvent(KeyInputEvent evt) ;

    @Override
    public default void onTouchEvent(TouchEvent evt) {

    }
    
}
