package org.ngengine.demo.son.anim;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class AnimGroupController {
    protected List<AnimTweenController> controllers = new ArrayList<>();
    protected Float step = null;
    protected boolean play = true;

    public void forEach(Consumer<AnimTweenController> consumer) {
        for (AnimTweenController controller : controllers) {
            consumer.accept(controller);  
        }      
    }

    public void addIfAbsent(AnimTweenController controller) {
        if(controllers.contains(controller)) {
            return; // Already exists, do not add again
        }
        controllers.add(controller);

        // Iterator<AnimTweenController> iterator = controllers.iterator();

        // while (iterator.hasNext()) {
        //     AnimTweenController c = iterator.next();
        //     // AnimTweenController c = ref.get();
        //     if (c == null) {
        //         iterator.remove(); // Clean up null references
        //     } else  if(c== controller) {
        //         return; // Already exists, do not add again
        //     }
        // }

        // controllers.add(new WeakReference<>(controller));
    }

    
    public void add(AnimTweenController controller) {
        controllers.add(controller);
    }

    public void remove(AnimTweenController controller) {
        // Iterator<WeakReference<AnimTweenController>> iterator = controllers.iterator();
        // while (iterator.hasNext()) {
        //     WeakReference<AnimTweenController> ref = iterator.next();
        //     AnimTweenController c = ref.get();
        //     if (c == null || c == controller) {
        //         iterator.remove(); // Remove null references or the specified controller
        //     }
        // }
        controllers.remove(controller); // Remove the specified controller
    }
    public void clear() {
        controllers.clear(); // Clear all controllers
    }


    public void unsetStep() {
        step=null;
        forEach(controller -> controller.unsetStep());
    }

    public void setStep(Float i) {
        step=i;
        forEach(controller -> controller.setStep(i));
    }

    public Float getStep() {
        return step;
    }

    public void play(){
        play = true;
        forEach(AnimTweenController::play);
    }

    public void pause() {
        play = false;
        forEach(AnimTweenController::pause);
    }

    public boolean isPlaying() {
        return play;
    }
    

}
