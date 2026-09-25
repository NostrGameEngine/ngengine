package org.jmonkeyengine.jme3androidexamples;

import android.os.Bundle;
import android.content.Context;
import com.jme3.app.AndroidHarnessFragment;
import com.jme3.app.LegacyApplication;
import com.jme3.system.AppSettings;
import java.util.logging.Level;
import java.util.logging.LogManager;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.platform.android.AndroidThreadedPlatform;

/**
 * A placeholder fragment containing a jME GLSurfaceView.
 */
public class JmeFragment extends AndroidHarnessFragment {
    private String appClass;

    @Override
    public void onAttach(Context context) {
        if (!NGEPlatform.isSet()) {
            NGEPlatform.set(new AndroidThreadedPlatform(context));
        }
        super.onAttach(context);
    }

    public JmeFragment() {
        finishOnAppStop = true;
        LogManager.getLogManager().getLogger("").setLevel(Level.INFO);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Bundle bundle=getArguments();

        appClass = bundle.getString(MainActivity.SELECTED_APP_CLASS);
//        Log.d(this.getClass().getSimpleName(), "AppClass: " + appClass);
        boolean verboseLogging = bundle.getBoolean(MainActivity.VERBOSE_LOGGING,
                MainActivity.DEFAULT_VERBOSE_LOGGING);
//        Log.d(this.getClass().getSimpleName(), "VerboseLogging: " + verboseLogging);
        if (verboseLogging) {
            // Set the default logging level (default=Level.INFO, Level.ALL=All Debug Info)
            LogManager.getLogManager().getLogger("").setLevel(Level.ALL);
        } else {
            // Set the default logging level (default=Level.INFO, Level.ALL=All Debug Info)
            LogManager.getLogManager().getLogger("").setLevel(Level.INFO);
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    protected LegacyApplication createApplication() throws Exception {
        Class<?> clazz = Class.forName(appClass);
        LegacyApplication application = (LegacyApplication) clazz.getDeclaredConstructor().newInstance();
        AppSettings settings = new AppSettings(true);
        settings.setEmulateMouse(true);
        settings.setEmulateKeyboard(true);
        application.setSettings(settings);
        return application;
    }

}
