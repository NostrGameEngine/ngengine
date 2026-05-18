package io.github.jmecn.tiled.components;

import java.lang.ref.WeakReference;

import org.ngengine.components.ComponentManager;
import org.ngengine.components.jme3.audio.RandomBackgroundMusicComponent;

import io.github.jmecn.tiled.core.TiledMap;

public class BackgroundMusicComponent extends RandomBackgroundMusicComponent  {
    private WeakReference<TiledMap> map;

    @Override
    public void updateAppLogic(ComponentManager mng, float tpf) {
        TiledMap lastMap = this.map==null?null:this.map.get();
        TiledMap currentMap = mng.getInstanceOf(TiledMap.class);
        if(currentMap!=lastMap){
            this.selectMusic(currentMap.hashCode());
            this.map = new WeakReference<>(currentMap);
        }
        super.updateAppLogic(mng, tpf);
    }

    
}
