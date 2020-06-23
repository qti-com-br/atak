
package com.atakmap.android.gui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.atakmap.android.importfiles.ui.ImportManagerFileBrowser;
import com.atakmap.app.R;
import com.atakmap.coremap.log.Log;

// to properly inflate the view no matter what is displaying it 
import com.atakmap.android.maps.MapView;

import java.io.File;

public class ImportFileBrowserDialog {

    public static final String TAG = "ImportFileBrowserDialog";

    public interface DialogDismissed {
        /**
         * When the file dialog is dismissed, return the selected file, null if no file was selected.
         */
        void onFileSelected(File f);

        /**
         * The dialog can be closed via back press - nothing selected, but onFileSelected not 
         * triggered.  Provides a way to be aware of dialog closing.
         */
        void onDialogClosed();

    }

    /**
     * Produce a file browser without a defined location, uses the last recorded location.
     */
    synchronized static public void show(final String title,
            final String[] extensionTypes,
            final DialogDismissed dismissed,
            final Context context) {
        show(title, null, extensionTypes, dismissed, context);
    }

    /**
     * Produce a file browser with a defined location.
     */
    synchronized static public void show(final String title,
            final String location,
            final String[] extensionTypes,
            final DialogDismissed dismissed,
            final Context context) {

        MapView mv = MapView.getMapView();
        if (mv == null)
            return;

        Context mapCtx = mv.getContext();
        final ImportManagerFileBrowser importFileBrowser = ImportManagerFileBrowser
                .inflate(mv);
        final SharedPreferences defaultPrefs = PreferenceManager
                .getDefaultSharedPreferences(mv.getContext());

        if (location != null) {
            importFileBrowser.setStartDirectory(location);
        } else {
            final String _lastDirectory = defaultPrefs
                    .getString("lastDirectory",
                            Environment.getExternalStorageDirectory()
                                    .getPath());
            importFileBrowser.setStartDirectory(_lastDirectory);
        }

        importFileBrowser.setExtensionTypes(extensionTypes);
        importFileBrowser.setMultiSelect(false);

        // use the user supplied context for display purposes
        AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setTitle(title);
        b.setView(importFileBrowser);
        b.setNegativeButton(mapCtx.getString(R.string.cancel), null);
        AlertDialog d = b.create();
        importFileBrowser.setAlertDialog(d);

        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(final DialogInterface dialog) {
                final File f = importFileBrowser.getReturnFile();
                if (f != null) {
                    // the location was not passed in, so we want to save it for next time.
                    if (location == null) {
                        defaultPrefs.edit()
                                .putString("lastDirectory", f.getParent())
                                .apply();
                    }
                    // provide for notification of the file selected, null if no file selected
                    if (dismissed != null)
                        dismissed.onFileSelected(f);
                } else {
                    if (dismissed != null)
                        dismissed.onDialogClosed();
                }
            }
        });

        // XXX - https://atakmap.com/bugzilla3/show_bug.cgi?id=2838
        // Based on the literature, people recommend using isFinishing() to overcome this, but the 
        // context that has been passed in is the MapView context, so I really doubt that at the point of this 
        // code it is really finishing.     (but possibly?)   Low risk try catch block for 2.2.
        try {
            if (!((Activity) context).isFinishing())
                d.show();
        } catch (Exception e) {
            Log.e(TAG, "bad bad bad", e);
        }
    }
}
