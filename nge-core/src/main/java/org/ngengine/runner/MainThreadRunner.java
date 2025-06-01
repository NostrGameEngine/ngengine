package org.ngengine.runner;

import java.util.Objects;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;

public class MainThreadRunner extends BaseAppState implements Runner {
    private Thread mainThread;


    public static MainThreadRunner of(Application app) {
        Objects.requireNonNull(app, "Application cannot be null");
        MainThreadRunner state = app.getStateManager().getState(MainThreadRunner.class);
        if(state==null){
            state = new MainThreadRunner(app);
        }
        return state;
    }
 
    public MainThreadRunner(Application app) {
        if(!app.getStateManager().hasState(this)){
            app.getStateManager().attach(this);
        }
    }

    @Override
    public void run(Runnable task) {
        if (Thread.currentThread() == mainThread) {
            task.run();
        } else {
            getApplication().enqueue(task);
        }
    }

    @Override
    protected void initialize(Application app) {
        mainThread = Thread.currentThread();

    }

    @Override
    protected void cleanup(Application app) {
       
    }

    @Override
    protected void onEnable() {
     }

    @Override
    protected void onDisable() {
         
    }

   
    
}
