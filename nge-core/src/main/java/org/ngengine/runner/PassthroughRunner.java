package org.ngengine.runner;

/**
 * This is a fake runner, it will just run the task in the current thread immediately. Used for debugging and
 * testing purposes.
 */
public class PassthroughRunner implements Runner {

    @Override
    public void run(Runnable task) {
        if (task != null) {
            task.run();
        }
    }

    @Override
    public void enqueue(Runnable task) {
        if (task != null) {
            task.run();
        }
    }
 
    
}
