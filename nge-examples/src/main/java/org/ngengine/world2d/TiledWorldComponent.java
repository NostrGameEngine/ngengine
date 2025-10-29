package org.ngengine.world2d;

import org.ngengine.ComponentRef;
import org.ngengine.Components;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;

import ngetest.tests.world2d.TiledWorld2DComponent;

public interface TiledWorldComponent extends Component{
    
    public default TiledWorld2DComponent getTiledWorld(ComponentManager mng){
        ComponentManager parent = mng.getParent();
        if (parent==null) return null;
        ComponentRef ref = Components.get(parent,TiledWorldComponent.class);
        if(ref!=null){
            return (TiledWorld2DComponent)ref.get();
        }
        return null;        
        
    }
}
