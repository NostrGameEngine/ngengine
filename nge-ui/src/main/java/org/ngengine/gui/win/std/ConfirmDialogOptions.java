package org.ngengine.gui.win.std;

import java.util.function.Consumer;

public class ConfirmDialogOptions {
    public String text;
    public String confirmButton = "Confirm";
    public String cancelButton = "Cancel";
    public Consumer<ConfirmDialogWindow> confirmAction;
    public Consumer<ConfirmDialogWindow> cancelAction = win->win.close();
    public ConfirmDialogOptions(){

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

    public Consumer<ConfirmDialogWindow> getConfirmAction() {
        return confirmAction;
    }

    public Consumer<ConfirmDialogWindow> getCancelAction() {
        return cancelAction;
    }




    public ConfirmDialogOptions setText(String text) {
        this.text = text;
        return this;
    }

    public ConfirmDialogOptions setConfirmButtonText(String confirmButton) {
        this.confirmButton = confirmButton;
        return this;
    }

    public ConfirmDialogOptions setCancelButtonText(String cancelButton) {
        this.cancelButton = cancelButton;
        return this;
    }

    public ConfirmDialogOptions setConfirmAction(Consumer<ConfirmDialogWindow> confirmAction) {
        this.confirmAction = confirmAction;
        return this;
    }
    public ConfirmDialogOptions setCancelAction(Consumer<ConfirmDialogWindow> cancelAction) {
        this.cancelAction = cancelAction;
        return this;
    }
}
