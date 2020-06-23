
package com.atakmap.app;

import android.app.Activity;
import android.os.Bundle;

import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.log.Log;

import org.acra.ACRA;
import org.acra.ACRAConfiguration;
import org.acra.ReportField;
import org.acra.annotation.ReportsCrashes;

import java.io.File;
import com.atakmap.jnicrash.JNICrash;

import plugins.core.model.Plugin;
import plugins.host.PluginRegistry;
import transapps.commons.rtcl.RTCLApplication;

@ReportsCrashes
public class ATAKApplication extends RTCLApplication {

    static final String TAG = "ATAKApplication";

    private PluginRegistry plugins;

    @Override
    public void onCreate() {
        super.onCreate();

        // important first step, initialize the native loader
        // otherwise as classes are loader, the static blocks
        // are run which might load native libraries.
        com.atakmap.coremap.loader.NativeLoader.init(this);

        // Turn on ACRA defaults, plus enable STACK_HASH
        ReportField[] fields = new ReportField[] {
                ReportField.REPORT_ID,
                ReportField.APP_VERSION_CODE,
                ReportField.APP_VERSION_NAME,
                ReportField.PACKAGE_NAME,
                ReportField.FILE_PATH,
                ReportField.PHONE_MODEL,
                ReportField.ANDROID_VERSION,
                ReportField.BUILD,
                ReportField.BRAND,
                ReportField.PRODUCT,
                ReportField.TOTAL_MEM_SIZE,
                ReportField.AVAILABLE_MEM_SIZE,
                ReportField.BUILD_CONFIG,
                ReportField.CUSTOM_DATA,
                ReportField.STACK_TRACE,
                ReportField.STACK_TRACE_HASH,
                ReportField.INITIAL_CONFIGURATION,
                ReportField.CRASH_CONFIGURATION,
                ReportField.DISPLAY,
                ReportField.USER_COMMENT,
                ReportField.USER_APP_START_DATE,
                ReportField.USER_CRASH_DATE,
                ReportField.DUMPSYS_MEMINFO,
                ReportField.LOGCAT,
                ReportField.IS_SILENT,
                ReportField.INSTALLATION_ID,
                ReportField.USER_EMAIL,
                ReportField.DEVICE_FEATURES,
                ReportField.ENVIRONMENT,
                ReportField.SHARED_PREFERENCES,
                ReportField.BUILD_CONFIG
        };
        ACRAConfiguration config = new ACRAConfiguration()
                .setCustomReportContent(fields);

        // start listening for any crash to report it
        ACRA.init(this, config);
        ACRA.getErrorReporter()
                .setReportSender(ATAKCrashHandler.instance());

        this.registerActivityLifecycleCallbacks(new ActivityLifecycleHandler());
        plugins = PluginRegistry.getInstance(this);
        plugins.registerPlugin(new Plugin(this));
        long time = android.os.SystemClock.elapsedRealtime();
        plugins.preloadClasses();
        Log.d(TAG, "Preloaded plugin classes in "
                + (android.os.SystemClock.elapsedRealtime() - time));

        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler());

        File logsDir = ATAKCrashHandler.getLogsDir();
        com.atakmap.coremap.loader.NativeLoader
                .loadLibrary("jnicrash");
        String hdr = ATAKCrashHandler.getHeaderText(null, null, null, this);
        try {
            String gpu = ATAKCrashHandler.getGPUInfo();
            hdr += ",\n" + gpu;
        } catch (Exception ignore) {
        }
        JNICrash.initialize(logsDir, hdr);
    }

    /**
     * Custom handler that stops the CommsMapComponent prior to actually crashing through which
     * should prevent a native crash when the callbacks are no longer valid.
     */
    public static class CustomExceptionHandler implements
            Thread.UncaughtExceptionHandler {

        private final Thread.UncaughtExceptionHandler defaultUEH;

        CustomExceptionHandler() {
            this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        }

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            //Log.e(TAG, "uncaughtException", e);

            // Stop commo first to prevent native callbacks once JVM starts
            // to tear down.  ACRA is idiotic and threads the uncaught exception handler work off
            // so we cannot do it in a callback from ACRA as JVM will have already begun to
            // shut down and native callbacks will cause hard crashes
            // (see https://jira.pargovernment.net/browse/ATAK-8098?filter=15300)
            CommsMapComponent cmc = CommsMapComponent.getInstance();
            if (cmc != null)
                cmc.onCrash();

            // I believe that the background service not getting stopped is probably a big reason
            // for the occurance of secondary crash logs.
            try {
                Log.d(TAG, "shutting down services");
                BackgroundServices.stopService();
            } catch (Exception err) {
                Log.d(TAG, "error occurred: " + err);
            }

            // Chain to ACRA
            defaultUEH.uncaughtException(t, e);
        }
    }

    @Override
    public PluginRegistry getPluginRegistry() {
        return plugins;
    }

    /**
     * This Callback receives callbacks for each type of Activity lifecycle change that occurs
     * within this Application.
     */
    private static class ActivityLifecycleHandler implements
            ActivityLifecycleCallbacks {

        @Override
        public void onActivityCreated(Activity activity,
                Bundle savedInstanceState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            Log.e(TAG, "activity destroyed = " + activity.getClass());
            try {
                if (activity.getClass().equals(ATAKActivity.class)) {
                    Log.d(TAG, "turning off the oreo gps service");
                    BackgroundServices.stopService();
                    Log.d(TAG, "successfully turned off the oreo gps service");
                }
            } catch (Exception e) {
                Log.d(TAG, "error", e);
            }
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity,
                Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }
    }

    public static void addCrashListener(CrashListener listener) {
        ATAKCrashHandler.instance().addListener(listener);
    }

    public static void removeCrashListener(CrashListener listener) {
        ATAKCrashHandler.instance().removeListener(listener);
    }
}
