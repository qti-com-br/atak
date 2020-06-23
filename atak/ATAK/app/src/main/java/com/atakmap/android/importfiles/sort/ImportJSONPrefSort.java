
package com.atakmap.android.importfiles.sort;

import android.content.Context;

import com.atakmap.app.R;
import com.atakmap.app.preferences.PreferenceControl;
import com.atakmap.app.preferences.json.JSONPreferenceControl;
import com.atakmap.coremap.log.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;

/**
 * Import JSON preferences
 */
public class ImportJSONPrefSort extends ImportInternalSDResolver {

    private static final String TAG = "ImportJSONPrefSort";

    public ImportJSONPrefSort(Context context, boolean validateExt,
            boolean copyFile) {
        super(".pref", PreferenceControl.DIRNAME, validateExt, copyFile,
                context.getString(R.string.preference_file));
    }

    @Override
    public boolean match(File file) {
        if (!super.match(file))
            return false;

        try {
            return isPreference(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Failed to match Pref file: " + file.getAbsolutePath(),
                    e);
            return false;
        }
    }

    public boolean isPreference(InputStream stream) {
        try {
            char[] buffer = new char[64];
            BufferedReader reader = null;
            int numRead;
            try {
                reader = new BufferedReader(new InputStreamReader(
                        stream));
                numRead = reader.read(buffer);
            } finally {
                if (reader != null)
                    reader.close();
            }

            if (numRead < 1) {
                Log.d(TAG, "Failed to read .pref stream");
                return false;
            }

            String content = String.valueOf(buffer, 0, numRead);
            return content.startsWith("{") && content.contains(
                    JSONPreferenceControl.PREFERENCE_CONTROL);
        } catch (Exception e) {
            Log.d(TAG, "Failed to match .pref", e);
            return false;
        }
    }

    @Override
    protected void onFileSorted(File src, File dst, Set<SortFlags> flags) {
        super.onFileSorted(src, dst, flags);
        try {
            JSONPreferenceControl.getInstance().load(dst, false);
        } catch (Exception e) {
            Log.e(TAG, "exception in onFileSorted!", e);
        }
    }
}
