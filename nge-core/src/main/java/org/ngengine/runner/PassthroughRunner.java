package org.ngengine.runner;

public class PassthroughRunner implements Runner {

    @Override
    public void run(Runnable task) {
        if (task != null) {
            task.run();
        }
    }

 
    
}
