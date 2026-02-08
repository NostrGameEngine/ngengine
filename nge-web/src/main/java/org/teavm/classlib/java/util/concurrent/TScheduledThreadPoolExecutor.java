/**
 * Copyright (c) 2025, Nostr Game Engine
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Nostr Game Engine is a fork of the jMonkeyEngine, which is licensed under
 * the BSD 3-Clause License. 
 */

package org.teavm.classlib.java.util.concurrent;

 import java.lang.Override;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
 import java.util.concurrent.TimeUnit;

public class TScheduledThreadPoolExecutor implements Executor {

    


    private class ExecutorThread implements Executor {
        private  String name = "Executor";
        private final LinkedList<Runnable> tasks = new LinkedList<>();
        private  volatile boolean running = false;

        public ExecutorThread(int n){
            
        }

        public void start() {
            if(running)return;
            running = true;
            
            Thread t = new Thread(() -> {
                while (running) {
                    Runnable task = null;
                    synchronized (tasks) {
                        if (tasks.isEmpty()) {
                            try {
                                tasks.wait(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            continue;
                        }
                        if (!tasks.isEmpty()) {
                            task = tasks.removeFirst();
                        }
                    }
                    try {
                        if (task != null) task.run();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            });
            t.setName(name + " Worker");
            t.start();

        }

        public void setName(String name){
            this.name = name;
  
        }


        @Override
        public void execute(Runnable command) {
            if(!running) throw new IllegalStateException("Executor already shutdown");
            Thread t = new Thread(()->{
                synchronized (tasks) {
                    tasks.add(command);
                    tasks.notifyAll();
                }
            });
            t.setName(name+" Scheduler");
            t.start();
        }

        public void close() {
            running = false;
            Thread t = new Thread(()->{
                synchronized (tasks) {
                    tasks.notifyAll();
                }
            });
            t.setName(name+" Closer");
            t.start();            
            tasks.clear();
        }
    }


    private final ExecutorThread thread;


    public TScheduledThreadPoolExecutor(int poolSize) {
        this.thread = new ExecutorThread(poolSize);
        this.thread.setName("ScheduledThreadPoolExecutor Thread");
        thread.start();
    }
    

    public <V> TFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        if(delay == 0){ // shortcut for tasks without delay
            TFutureTask<V> futureTask = new TFutureTask<V>();
            try{
                this.thread.execute(()->{
                    try {
                        V res = callable.call();
                        futureTask.setResult(res);
                    } catch (Exception e) {
                        futureTask.setException(e);
                    }
                });
            }catch(Exception e){
                futureTask.setException(e);
            }
            return futureTask;
        }

        long ms = unit.toMillis(delay);
        TFutureTask<V> futureTask = new TFutureTask<V>();
        Thread t = new Thread(()->{ // ensure it runs on the teavms suspendable context
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try{
                this.thread.execute(()->{
                    try {
                        V res = callable.call();
                        futureTask.setResult(res);
                    } catch (Exception e) {
                        futureTask.setException(e);
                    }
                });
            }catch(Exception e){
                futureTask.setException(e);
            }
        });
        // get stacktrace
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        t.setName("Schedule Waiter "+( stack.length>2?(" at "+stack[2].toString()):""));
        t.start();        
        return futureTask;

    }
    @Override
    public void execute(Runnable command) {
        submit(command);
    }

    public <T> TFuture<T> submit(Runnable task,T result) {
        TFuture<T> res = schedule(() -> {
            try{
                task.run();
                afterExecute(task, null);
            }catch(Exception e){
                e.printStackTrace();
                afterExecute(task, e);

            }
            return result;
        }, 0, TimeUnit.MILLISECONDS);
        return res;
    
    }
    
     public <T> TFuture<T> submit(Runnable task) {
        return submit(task, null);
    }

    public <T> TFuture<T> submit(Callable<T> task) {
        return schedule(task, 0, TimeUnit.MILLISECONDS);
    }

    protected void afterExecute(Runnable r, Throwable t) {
    }

    public  List<Runnable> shutdownNow() {
        this.thread.close();
        return new ArrayList<Runnable>();
    }
    
    public void shutdown() {
        this.thread.close();
    }
}
