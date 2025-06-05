package org.ngengine.components.fragments;

import org.ngengine.AsyncAssetManager;

import com.jme3.app.Application;

/**
 * A fragment that receives the application instance.
 * 
 * This will let you access most of the JME3 API directly, but should be avoided if possible in favor of using
 * the more specific fragments.
 * 
 * This is mostly useful for backwards compatibility with existing jmonkeyengine code.
 */
public interface AppFragment extends Fragment {
    /**
     * Receive the application instance as soon as it is available. The reference to the application can be
     * stored and used later in the component logic.
     * 
     * @param app
     *            the Application instance
     */
    public void receiveApplication(Application app) ;

}
