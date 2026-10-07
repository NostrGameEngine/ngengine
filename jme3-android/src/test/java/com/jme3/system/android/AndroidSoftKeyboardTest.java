package com.jme3.system.android;

import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class AndroidSoftKeyboardTest {
    private final JmeAndroidSystem system = new JmeAndroidSystem();
    private final View editor = mock(View.class);
    private final InputMethodManager ime = mock(InputMethodManager.class);
    private final IBinder token = mock(IBinder.class);
    private final ArrayList<Runnable> posted = new ArrayList<>();

    @BeforeEach
    void attachEditor() {
        Context context = mock(Context.class);
        when(editor.getContext()).thenReturn(context);
        when(editor.getHandler()).thenReturn(mock(Handler.class));
        when(editor.getWindowToken()).thenReturn(token);
        when(context.getSystemService(Context.INPUT_METHOD_SERVICE)).thenReturn(ime);
        when(editor.post(any(Runnable.class))).thenAnswer(call -> {
            posted.add(call.getArgument(0));
            return true;
        });
        JmeAndroidSystem.setView(editor);
    }

    @AfterEach
    void detachEditor() {
        JmeAndroidSystem.setView(null);
    }

    @Test
    void showsOnUiThreadWithSupportedApiAndNoTimedRetry() {
        system.showSoftKeyboard(true);
        verifyNoInteractions(ime);
        posted.get(0).run();
        verify(editor).requestFocus();
        verify(ime).showSoftInput(editor, 0);
        verifyNoMoreInteractions(ime);
        verify(editor, never()).postDelayed(any(Runnable.class), anyLong());
    }

    @Test
    void hideCancelsAnOlderQueuedShow() {
        system.showSoftKeyboard(true);
        system.showSoftKeyboard(false);
        for (Runnable request : posted) request.run();
        verify(ime).hideSoftInputFromWindow(token, 0);
        verifyNoMoreInteractions(ime);
        verify(editor, never()).requestFocus();
    }

    @Test
    void detachedViewCannotReceiveAnOldRequest() {
        system.showSoftKeyboard(true);
        JmeAndroidSystem.setView(null);
        posted.get(0).run();
        verifyNoInteractions(ime);
    }

    @Test
    void detachedWindowCannotReceiveARequest() {
        when(editor.getWindowToken()).thenReturn(null);
        system.showSoftKeyboard(true);
        posted.get(0).run();
        verifyNoInteractions(ime);
    }
}
