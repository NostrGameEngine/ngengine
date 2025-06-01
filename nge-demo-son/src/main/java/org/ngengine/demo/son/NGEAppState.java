package org.ngengine.demo.son;

 
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.platform.NGEPlatform;
import org.ngengine.platform.VStore;
import org.ngengine.store.DataStore;

import com.jme3.app.Application;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.renderer.RenderManager;
import com.jme3.system.AppSettings;

public abstract class NGEAppState implements AppState{
    private static final Logger log = Logger.getLogger(NGEAppState.class.getName());

    private Application app;
    private boolean initialized;
    private boolean enabled = true;
    private String id;
    private Object unit;

    protected NGEAppState() {
    }

    protected NGEAppState(String id) {
        this.id = id;
    }

    protected NGEAppState(String id, Object unit) {
        this.id = id;
        this.unit = unit;
    }

    protected Object getUnit() {
        return unit;
    }

    protected void setUnit(Object unit) {
        this.unit = unit;
    }

    /**
     *  Called after the app state is detached or during
     *  application shutdown if the state is still attached.
     *  onDisable() is called before this cleanup() method if
     *  the state is enabled at the time of cleanup.
     * @param app the application
     */
    protected void cleanup(Application app){

    }

    /**
     *  Called when the state is fully enabled, ie: is attached
     *  and isEnabled() is true or when the setEnabled() status
     *  changes after the state is attached.
     */
    protected abstract void onEnable();

    /**
     *  Called when the state was previously enabled but is
     *  now disabled either because setEnabled(false) was called
     *  or the state is being cleaned up.
     */
    protected abstract void onDisable();

    /**
     *  Do not call directly: Called by the state manager to initialize this
     *  state post-attachment.
     *  This implementation calls initialize(app) and then onEnable() if the
     *  state is enabled.
     */
    @Override
    public final void initialize(AppStateManager stateManager, Application app) {
        log.log(Level.FINEST, "initialize():{0}", this);

        this.app = app;
        initialized = true;
        if (isEnabled()) {
            log.log(Level.FINEST, "onEnable():{0}", this);
            disableStatesWithSameUnit();
            onEnable();
        }
    }

    @Override
    public final boolean isInitialized() {
        return initialized;
    }

    /**
     *  Sets the unique ID of this app state.  Note: that setting
     *  this while an app state is attached to the state manager will
     *  have no effect on ID-based lookups.
     *
     * @param id the desired ID
     */
    protected void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public final Application getApplication() {
        return app;
    }

    public final AppStateManager getStateManager() {
        return app.getStateManager();
    }

  
 

    @Override
    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled)
            return;
        this.enabled = enabled;
        if (!isInitialized())
            return;
        if (enabled) {
            log.log(Level.FINEST, "onEnable():{0}", this);
            disableStatesWithSameUnit();
            onEnable();
        } else {
            log.log(Level.FINEST, "onDisable():{0}", this);
            onDisable();
        }
    }

    protected void disableStatesWithSameUnit() {
        if (unit != null) {
            AppStateManager mng = getApplication().getStateManager();
            for (AppState s : mng.getStates()) {
                if (s.isEnabled() && s instanceof GameAppState && unit !=null && unit.equals(((NGEAppState) s).getUnit())
                        && s != this) {
                    log.log(Level.FINEST, "Disabling same unit state: {0}", s);
                    s.setEnabled(false);
                }
            }
        }
    }

    @Override
    public final boolean isEnabled() {
        return enabled;
    }

    @Override
    public void stateAttached(AppStateManager stateManager) {
    }

    @Override
    public void stateDetached(AppStateManager stateManager) {
    }

    @Override
    public void update(float tpf) {
    }

    @Override
    public void render(RenderManager rm) {
    }

    @Override
    public void postRender() {
    }

    /**
     *  Do not call directly: Called by the state manager to terminate this
     *  state post-detachment or during state manager termination.
     *  This implementation calls onDisable() if the state is enabled and
     *  then cleanup(app).
     */
    @Override
    public final void cleanup() {
        log.log(Level.FINEST, "cleanup():{0}", this);

        if (isEnabled()) {
            log.log(Level.FINEST, "onDisable():{0}", this);
            onDisable();
        }
        cleanup(app);
        initialized = false;
    }


    public DataStore getDataStore(String name){
        return new DataStore(app, name, false);
    }

    public DataStore getCache(String name){
        return new DataStore(app, name, true);
    }
}
