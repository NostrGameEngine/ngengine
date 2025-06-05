package org.ngengine.runner;

/**
 * A runner runs tasks in its own thread.
 */
public interface Runner {

    /**
     * Run a task in the runner's thread, asap.
     * 
     * @param task
     */
    public void run(Runnable task);

    /**
     * Enqueue a task to be run in the runner's thread.
     * 
     * This is the same as {@link #run(Runnable)} but the task will be enqueued even if the call happens in
     * the runner's thread.
     * 
     * <p>
     * Generally, this is useful only for special use cases, like ensuring the thread doesn't get overloaded
     * with too many tasks all at once. Otherwise just use {@link #run(Runnable)}.
     * 
     * @param task
     */
    public void enqueue(Runnable task);
}
