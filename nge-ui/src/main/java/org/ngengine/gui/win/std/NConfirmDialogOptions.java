package org.ngengine.gui.win.std;

import java.util.function.Consumer;

public class NConfirmDialogOptions {
    public String text;
    public String confirmButton = "Confirm";
    public String cancelButton = "Cancel";
    public Consumer<NConfirmDialogWindow> confirmAction;
    public Consumer<NConfirmDialogWindow> cancelAction = win->win.close();
    public NConfirmDialogOptions(){

    }

    public String getConfirmButtonText() {
        return confirmButton;
    }

    public String getCancelButtonText() {
        return cancelButton;
    }

    public String getText() {
        return text;
    }

    public Consumer<NConfirmDialogWindow> getConfirmAction() {
        return confirmAction;
    }

    public Consumer<NConfirmDialogWindow> getCancelAction() {
        return cancelAction;
    }




    public NConfirmDialogOptions setText(String text) {
        this.text = text;
        return this;
    }

    public NConfirmDialogOptions setConfirmButtonText(String confirmButton) {
        this.confirmButton = confirmButton;
        return this;
    }

    public NConfirmDialogOptions setCancelButtonText(String cancelButton) {
        this.cancelButton = cancelButton;
        return this;
    }

    public NConfirmDialogOptions setConfirmAction(Consumer<NConfirmDialogWindow> confirmAction) {
        this.confirmAction = confirmAction;
        return this;
    }
    public NConfirmDialogOptions setCancelAction(Consumer<NConfirmDialogWindow> cancelAction) {
        this.cancelAction = cancelAction;
        return this;
    }
}
