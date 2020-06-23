
package com.atakmap.android.gpkg;

import android.content.Context;
import android.content.Intent;
import android.util.Pair;

import com.atakmap.android.importexport.ImportExportMapComponent;
import com.atakmap.android.importexport.ImporterManager;
import com.atakmap.android.maps.AbstractMapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.feature.FeatureDataStore;
import com.atakmap.map.layer.feature.FeatureLayer;
import com.atakmap.map.layer.feature.gpkg.GeoPackageFeatureDataStore;
import com.atakmap.map.layer.feature.opengl.GLBatchGeometryFeatureDataStoreRenderer;
import com.atakmap.map.layer.opengl.GLLayer2;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import com.atakmap.map.layer.opengl.GLLayerSpi2;

public class GeopackageMapComponent
        extends AbstractMapComponent {

    public static final String TAG = "GeopackageMapComponent";
    private GeoPackageImporter packageImporter;

    @Override
    public void onCreate(Context context,
            Intent intent,
            MapView view) {

        GLLayerFactory.register(new GLLayerSpi2() {
            @Override
            public GLLayer2 create(Pair<MapRenderer, Layer> arg) {
                if (!(arg.second instanceof FeatureLayer)) {
                    return null;
                }

                FeatureLayer layer = (FeatureLayer) arg.second;
                FeatureDataStore dataStore = layer.getDataStore();
                if (!(dataStore instanceof GeoPackageFeatureDataStore))
                    return null;

                return new GLBatchGeometryFeatureDataStoreRenderer(
                        arg.first, layer);
            }

            @Override
            public int getPriority() {
                return 3;
            }
        });

        ImportExportMapComponent.getInstance()
                .addImporterClass(GeoPackageImporter.getImportResolver());

        packageImporter = new GeoPackageImporter(view, "Geopackage",
                "resource://" + R.drawable.gpkg);

        //
        // Load prior imports and any available overlays.  Register for imports.
        //
        packageImporter.loadImports();
        packageImporter.loadOverlays(FileSystemUtils.getItems("overlays"));
        ImporterManager.registerImporter(packageImporter);
    }

    @Override
    protected void onDestroyImpl(Context context,
            MapView view) {

        if (packageImporter != null) {
            ImporterManager.unregisterImporter(packageImporter);
            packageImporter.dispose();
            packageImporter = null;
        }
    }

    @Override
    public void onStart(Context context,
            MapView view) {
    }

    @Override
    public void onStop(Context context,
            MapView view) {
    }

    @Override
    public void onPause(Context context,
            MapView view) {
    }

    @Override
    public void onResume(Context context,
            MapView view) {
    }
}
