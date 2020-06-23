
package com.atakmap.android.importfiles.sort;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.routes.RouteMapReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.app.R;

import java.io.File;
import java.util.Set;

/**
 * Imports GPX Route Files
 * 
 */
public class ImportGPXRouteSort extends ImportGPXSort {

    private static final String TAG = "ImportGPXRouteSort";

    public ImportGPXRouteSort(Context context, boolean validateExt,
            boolean copyFile, boolean importInPlace) {
        super(validateExt, copyFile, importInPlace, context
                .getString(R.string.gpx_route_file));
    }

    @Override
    protected void onFileSorted(File src, File dst, Set<SortFlags> flags) {
        Intent i = new Intent(RouteMapReceiver.ROUTE_IMPORT);
        i.putExtra("filename", src.toString());
        AtakBroadcast.getInstance().sendBroadcast(i);
    }
}
