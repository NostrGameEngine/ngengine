package org.ngengine.demo.son.anim;

import com.jme3.anim.tween.Tween;

public class AnimTweenController implements Tween{
    protected final Tween source;
    protected double currentTime = 0;
    protected Float step = null;
    protected boolean play = true;

    public AnimTweenController(Tween source) {
        this.source = source;
    }
    
    @Override
    public double getLength() {
        return source.getLength();
    }
 
    public Tween getSource() {
        return source;
    }

    public void unsetStep() {
        step = null;
    }

    public void setStep(Float i) {
        if(i==null){
            unsetStep();
            return;
        }
        step = i;
    }

    public Float getStep() {
        return step;
    }


    public void pause() {
        play = false;
    }
    

    public void play() {
        play = true;
    }

    public boolean isPlaying() {
        return play;
    }

    @Override
    public boolean interpolate(double t) {
        if(!play){
            return source.interpolate(currentTime);
        }
        if(step!=null){
            double length = getLength();
            double time = length * (double) step;
            currentTime = time;

            return source.interpolate(currentTime);
        } else {
            currentTime = t;
            return source.interpolate(currentTime);
        }
    }
    
}
