package org.ngengine;

import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.jme3.ComponentManagerControl;

import com.jme3.scene.Spatial;

public class Components {
    public static ComponentRef mount(Spatial sp, Component component, Object... dependencies){
        ComponentManagerControl c = sp.getControl(ComponentManagerControl.class);
        if(c==null){
            c = new ComponentManagerControl();
            sp.addControl(c);
        }
        c.addComponent(component, dependencies);
        return new ComponentRef(c, component);
    }

    public static ComponentRef mount(NGEApplication app, Component component, Object... dependencies){
        ComponentManager c = app.getComponentManager();
        c.addComponent(component, dependencies);
        return new ComponentRef(c, component);
    }

    public static ComponentRef mount(ComponentManager cm, Component component, Object... dependencies){
        cm.addComponent(component, dependencies);
        return new ComponentRef(cm, component);
    }

    
    public static void unmount(Spatial sp, ComponentRef cref){
        unmount(sp, cref.get());
    }

    public static void unmount(NGEApplication app, ComponentRef cref){
        unmount(app, cref.get());
    }

    public static void unmount(Spatial sp, Component component){
        ComponentManagerControl c = sp.getControl(ComponentManagerControl.class);
        if(c!=null){
            c.removeComponent(component);
        }
    }

    public static void unmount(NGEApplication app, Component component){
        ComponentManager c = app.getComponentManager();
        c.removeComponent(component);
    }

    public static void unmount(ComponentManager cm, Component component){
        cm.removeComponent(component);
    }

    public static boolean has(Spatial sp, Class<? extends Component> cls) {
        ComponentManagerControl c = sp.getControl(ComponentManagerControl.class);
        if(c!=null){
            return c.hasComponent(cls);
        }
        return false;
    }

    public static boolean has(Spatial sp, Component cc) {
        ComponentManagerControl c = sp.getControl(ComponentManagerControl.class);
        if(c!=null){
            return c.hasComponent(cc);
        }
        return false;
    }

    public static boolean has(Spatial sp, String id) {
        ComponentManagerControl c = sp.getControl(ComponentManagerControl.class);
        if(c!=null){
            return c.hasComponent(id);
        }
        return false;
    }


    public static boolean has(NGEApplication app, Class<? extends Component> cls) {
        ComponentManager c = app.getComponentManager();
        if(c!=null){
            return c.hasComponent(cls);
        }
        return false;
    }

    public static boolean has(NGEApplication app, Component cc) {
        ComponentManager c = app.getComponentManager();
        if(c!=null){
            return c.hasComponent(cc);
        }
        return false;
    }


    public static boolean has(NGEApplication app, String id) {
        ComponentManager c = app.getComponentManager();
        if(c!=null){
            return c.hasComponent(id);
        }
        return false;
    }

    public static boolean has(ComponentManager cm, Class<? extends Component> cls) {
        return cm.hasComponent(cls);
    }

    public static ComponentRef get(Spatial sp, Class<? extends Component> cls) {
        ComponentManagerControl c = sp.getControl(ComponentManagerControl.class);
        if(c!=null){
            Component comp = c.getComponent(cls);
            if(comp!=null){
                return new ComponentRef(c, comp);
            }
        }
        return null;
    }

    public static ComponentRef get(NGEApplication app, Class<? extends Component> cls) {
        ComponentManager c = app.getComponentManager();
        if(c!=null){
            Component comp = c.getComponent(cls);
            if(comp!=null){
                return new ComponentRef(c, comp);
            }
        }
        return null;
    }

    public static ComponentRef get(Spatial sp, String id) {
        ComponentManagerControl c = sp.getControl(ComponentManagerControl.class);
        if(c!=null){
            Component comp = (Component)c.getComponentById(id);
            if(comp!=null){
                return new ComponentRef(c, comp);
            }
        }
        return null;
    }

    public static ComponentRef get(NGEApplication app, String id) {
        ComponentManager c = app.getComponentManager();
        if(c!=null){
            Component comp = (Component)c.getComponentById(id);
            if(comp!=null){
                return new ComponentRef(c, comp);
            }
        }
        return null;
    } 

    public static ComponentRef get(ComponentManager cm, String id) {
        Component comp = (Component)cm.getComponentById(id);
        if(comp!=null){
            return new ComponentRef(cm, comp);
        }
        return null;
    }

    public static ComponentRef get(ComponentManager cm, Class<? extends Component> cls) {
        Component comp = cm.getComponent(cls);
        if(comp!=null){
            return new ComponentRef(cm, comp);
        }
        return null;
    }

    public static ComponentRef get(ComponentManager cm, Component component) {
        if(cm.hasComponent(component)){
            return new ComponentRef(cm, component);
        }
        return null;
    }

    
}
