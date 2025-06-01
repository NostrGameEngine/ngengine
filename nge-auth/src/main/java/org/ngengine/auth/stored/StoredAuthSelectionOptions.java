package org.ngengine.auth.stored;

import java.util.function.Consumer;

import com.jme3.texture.Texture2D;

public class StoredAuthSelectionOptions {
    protected Consumer<StoredAuthSelectionWindow> onConfirm;
    protected Consumer<StoredAuthSelectionWindow> onCancel ;
    protected Consumer<StoredAuthSelectionWindow> onRemove;
    protected String alias;
    protected Texture2D icon;

    public StoredAuthSelectionOptions(String alias, Texture2D icon) {
        this.alias = alias;
        this.icon = icon;
     
    }

    public Texture2D getIcon() {
        return icon;
    }
    public String getAlias() {
        return alias;
    }

    public StoredAuthSelectionOptions setConfirmAction(Consumer<StoredAuthSelectionWindow> onConfirm) {
        this.onConfirm = onConfirm;
        return this;
    }

    public StoredAuthSelectionOptions setCancelAction(Consumer<StoredAuthSelectionWindow> onCancel) {
        this.onCancel = onCancel;
        return this;
    }

    public StoredAuthSelectionOptions setRemoveAction(Consumer<StoredAuthSelectionWindow> onRemove) {
        this.onRemove = onRemove;
        return this;
    }

    public Consumer<StoredAuthSelectionWindow> getConfirmAction() {
        return onConfirm;
    }

    public Consumer<StoredAuthSelectionWindow> getCancelAction() {
        return onCancel;
    }

    public Consumer<StoredAuthSelectionWindow> getRemoveAction() {
        return onRemove;
    }


}
