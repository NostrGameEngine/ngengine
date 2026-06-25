package org.ngengine.components;

public interface ReloadableComponent extends Component {
    void reload();

    default String getReloadLabel(){
        return getClass().getSimpleName();
    }
}
