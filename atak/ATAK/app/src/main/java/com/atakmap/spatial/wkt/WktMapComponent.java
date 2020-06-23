
package com.atakmap.spatial.wkt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;

import com.atakmap.android.data.DataMgmtReceiver;
import com.atakmap.android.features.FeatureDataStoreDeepMapItemQuery;
import com.atakmap.android.features.FeatureDataStoreMapOverlay;
import com.atakmap.android.features.FeaturesDetailsDropdownReceiver;
import com.atakmap.android.importexport.ExporterManager;
import com.atakmap.android.importexport.Importer;
import com.atakmap.android.importexport.ImporterManager;
import com.atakmap.android.importexport.MarshalManager;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.AbstractMapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.update.AppVersionUpgrade;
import com.atakmap.app.DeveloperOptions;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.comms.CommsMapComponent.ImportResult;
import com.atakmap.map.layer.feature.DataSourceFeatureDataStore;
import com.atakmap.map.layer.feature.FeatureDataSourceContentFactory;
import com.atakmap.map.layer.feature.FeatureLayer;
import com.atakmap.map.layer.feature.PersistentDataSourceFeatureDataStore;
import com.atakmap.map.layer.feature.PersistentDataSourceFeatureDataStore2;
import com.atakmap.map.layer.feature.ogr.OgrFeatureDataSource;
import com.atakmap.spatial.file.DrwFileDatabase;
import com.atakmap.spatial.file.FileDatabase;
import com.atakmap.spatial.file.GpxContentResolver;
import com.atakmap.spatial.file.GpxFileSpatialDb;
import com.atakmap.spatial.file.KmlContentResolver;
import com.atakmap.spatial.file.KmlFileSpatialDb;
import com.atakmap.spatial.file.LptFileDatabase;
import com.atakmap.spatial.file.ShapefileContentResolver;
import com.atakmap.spatial.file.ShapefileMarshal;
import com.atakmap.spatial.file.ShapefileSpatialDb;
import com.atakmap.spatial.file.SpatialDbContentResolver;
import com.atakmap.spatial.file.SpatialDbContentSource;
import com.atakmap.spatial.file.export.GPXExportMarshal;
import com.atakmap.spatial.file.export.KMLExportMarshal;
import com.atakmap.spatial.file.export.KMZExportMarshal;
import com.atakmap.spatial.file.export.SHPExportMarshal;

import org.gdal.gdal.gdal;
import org.gdal.ogr.ogr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import com.atakmap.coremap.locale.LocaleUtil;
import java.util.Map;
import java.util.Set;

public class WktMapComponent extends AbstractMapComponent {

    private final static File SPATIAL_DB_FILE = FileSystemUtils
            .getItem("Databases/spatial.sqlite");
    private final static File SPATIAL_DB_DIR = FileSystemUtils
            .getItem("Databases/spatial2.sqlite");

    private static final String TAG = "WktMapComponent";

    private DataSourceFeatureDataStore spatialDb;
    private Set<FileDatabase> fileDatabases;
    private Map<String, SpatialDbContentSource> contentSources;
    private FeatureLayer layer;
    private MapView mapView;

    private void initComponents(Context context, MapView view) {
        FeatureDataSourceContentFactory
                .register(ShapefileSpatialDb.ZIPPED_SHP_DATA_SOURCE);

        this.mapView = view;

        // Initialize the Spatial DB
        Log.d(TAG, "Initializing SpatialDb");

        if (DeveloperOptions.getIntOption("feature-metadata-enabled", 1) == 0) {
            if (SPATIAL_DB_DIR.exists())
                FileSystemUtils.deleteDirectory(SPATIAL_DB_DIR, false);

            if (DeveloperOptions.getIntOption("force-overlays-rebuild",
                    0) == 1) {
                FileSystemUtils.deleteFile(SPATIAL_DB_FILE);
            }

            this.spatialDb = new PersistentDataSourceFeatureDataStore(
                    SPATIAL_DB_FILE);
        } else {
            if (SPATIAL_DB_FILE.exists())
                FileSystemUtils.deleteFile(SPATIAL_DB_FILE);

            if (DeveloperOptions.getIntOption("force-overlays-rebuild", 0) == 1)
                FileSystemUtils.deleteDirectory(SPATIAL_DB_DIR, true);

            OgrFeatureDataSource.metadataEnabled = true;
            this.spatialDb = new PersistentDataSourceFeatureDataStore2(
                    SPATIAL_DB_DIR);
        }
        this.layer = new FeatureLayer("Spatial Database", this.spatialDb);

        this.contentSources = new HashMap<>();

        SpatialDbContentSource contentSource;

        contentSource = new KmlFileSpatialDb(this.spatialDb);
        contentSource.setContentResolver(new KmlContentResolver(view,
                this.spatialDb));
        this.contentSources.put(contentSource.getType(), contentSource);

        contentSource = new ShapefileSpatialDb(this.spatialDb);
        contentSource.setContentResolver(new ShapefileContentResolver(view,
                this.spatialDb));
        this.contentSources.put(contentSource.getType(), contentSource);

        contentSource = new GpxFileSpatialDb(this.spatialDb);
        contentSource.setContentResolver(new GpxContentResolver(view,
                this.spatialDb));
        this.contentSources.put(contentSource.getType(), contentSource);

        FeatureDataStoreDeepMapItemQuery query = new FeatureDataStoreDeepMapItemQuery(
                this.layer);
        for (SpatialDbContentSource spatialDbContentSource : this.contentSources
                .values()) {
            contentSource = spatialDbContentSource;
            FeatureDataStoreMapOverlay overlay = new FeatureDataStoreMapOverlay(
                    context, spatialDb, contentSource.getType(),
                    contentSource.getGroupName(), contentSource.getIconPath(),
                    query,
                    contentSource.getContentType(),
                    contentSource.getFileMimeType());
            mapView.getMapOverlayManager().addFilesOverlay(overlay);
        }

        this.fileDatabases = new HashSet<>();

        DocumentedIntentFilter intentFilter = new DocumentedIntentFilter();
        intentFilter.addAction(DataMgmtReceiver.ZEROIZE_CONFIRMED_ACTION);
        AtakBroadcast.getInstance().registerReceiver(dataMgmtReceiver,
                intentFilter);

        //register Overlay Manager exporters
        ExporterManager.registerExporter(
                KmlFileSpatialDb.KML_TYPE.toUpperCase(LocaleUtil.getCurrent()),
                KmlFileSpatialDb.KML_FILE_ICON_ID, KMLExportMarshal.class);
        ExporterManager.registerExporter(
                KmlFileSpatialDb.KMZ_TYPE.toUpperCase(LocaleUtil.getCurrent()),
                KmlFileSpatialDb.KML_FILE_ICON_ID, KMZExportMarshal.class);
        ExporterManager.registerExporter(ShapefileSpatialDb.SHP_CONTENT_TYPE,
                ShapefileSpatialDb.SHP_FILE_ICON_ID, SHPExportMarshal.class);
        ExporterManager.registerExporter(
                GpxFileSpatialDb.GPX_CONTENT_TYPE.toUpperCase(LocaleUtil
                        .getCurrent()),
                GpxFileSpatialDb.GPX_FILE_ICON_ID, GPXExportMarshal.class);

        DocumentedIntentFilter i = new DocumentedIntentFilter();
        i.addAction(FeaturesDetailsDropdownReceiver.SHOW_DETAILS,
                "Show feature details");
        i.addAction(FeaturesDetailsDropdownReceiver.TOGGLE_VISIBILITY,
                "Toggle feature visibility");
        this.registerReceiver(context,
                new FeaturesDetailsDropdownReceiver(view, this.spatialDb), i);
    }

    @Override
    public void onCreate(Context context, Intent intent, MapView view) {
        Log.d(TAG, "Creating WktMapComponent");
        final Context appCtx = context;
        final MapView mapView = view;

        // Register all configured GDAL/OGR drivers
        gdal.SetConfigOption("LIBKML_RESOLVE_STYLE", "yes");
        ogr.RegisterAll();

        initComponents(appCtx, mapView);

        // Spawn a new thread to initialize this component,
        // so ATAK can continue to run in the background as
        // we're doing the FS/KML/DB operations.
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    long s = SystemClock.elapsedRealtime();

                    // see if the atak/overlays exists, and, if not, create it
                    File overlaysDir = FileSystemUtils.getItem("overlays");
                    if (!overlaysDir.exists())
                        if (!overlaysDir.mkdirs()) {
                            Log.e(TAG, "Failure to make directory " +
                                    overlaysDir.getAbsolutePath());
                        }

                    // if any files were moved by AppVersionUpgrade, make the data
                    // store do a refresh prior to the full scan to reduce the
                    // number of changes in the transaction
                    if (AppVersionUpgrade.OVERLAYS_MIGRATED) {
                        spatialDb.beginBulkModification();
                        try {
                            spatialDb.refresh();
                        } finally {
                            spatialDb.endBulkModification(true);
                        }
                    }

                    if (DeveloperOptions.getIntOption(
                            "feature-metadata-enabled", 1) != 0)
                        mapView.addLayer(MapView.RenderStack.VECTOR_OVERLAYS,
                                0,
                                layer);

                    synchronized (WktMapComponent.this) {
                        //TODO do we need a FileDatabaseImporter wrapper like WktImporter?

                        FileDatabase db = new LptFileDatabase(appCtx, mapView);
                        ImporterManager.registerImporter(db);
                        fileDatabases.add(db);

                        db = new DrwFileDatabase(appCtx, mapView);
                        ImporterManager.registerImporter(db);
                        fileDatabases.add(db);

                        // TODO: add this back when GeoJson actually parses the files
                        //db = new GeoJsonFileDatabase(appCtx, mapView);
                        //ImporterManager.registerImporter(db);
                    }

                    fullScan();

                    Log.d(TAG, "initialized: "
                            + (SystemClock.elapsedRealtime() - s) + "ms");
                } catch (NullPointerException e) {
                    Log.e(TAG,
                            "Initialization failed, application may have exited prior to completion",
                            e);
                }
            }
        }, "WktMapInitThread");
        t.setPriority(Thread.NORM_PRIORITY);

        for (SpatialDbContentSource source : this.contentSources.values()) {
            // XXX - resolve type v. content type post 3.3
            ImporterManager.registerImporter(new WktImporter(source.getType(),
                    source));
            ImporterManager.registerImporter(new WktImporter(source
                    .getContentType(), source));
        }
        MarshalManager.registerMarshal(ShapefileMarshal.INSTANCE);

        t.start();
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {

        if (dataMgmtReceiver != null) {
            AtakBroadcast.getInstance().unregisterReceiver(dataMgmtReceiver);
            dataMgmtReceiver = null;
        }

        mapView.removeLayer(MapView.RenderStack.VECTOR_OVERLAYS, layer);

        this.spatialDb.dispose();

        synchronized (this) {
            for (FileDatabase db : fileDatabases)
                db.close();
            fileDatabases.clear();

            if (contentSources != null) {
                for (SpatialDbContentSource s : this.contentSources.values())
                    s.dispose();
                contentSources.clear();
            }
        }
    }

    /**************************************************************************/

    private void fullScan() {
        synchronized (this) {

            try {
                // XXX - legacy implementation needs bulk modification for
                //       transaction, new implementation does not
                if (this.spatialDb instanceof PersistentDataSourceFeatureDataStore)
                    this.spatialDb.beginBulkModification();
                boolean success = false;
                try {
                    // refresh the data store and drop all invalid entries
                    this.spatialDb.refresh();

                    // Add existing content to the resolvers
                    for (SpatialDbContentSource s : this.contentSources
                            .values()) {
                        SpatialDbContentResolver res = s.getContentResolver();
                        if (res != null)
                            res.scan(s);
                    }

                    final String[] mountPoints = FileSystemUtils
                            .findMountPoints();

                    // single "Overlays" directory scan
                    File overlaysDir;
                    for (String mountPoint : mountPoints) {
                        overlaysDir = new File(mountPoint, "overlays");
                        OverlaysScanner.scan(this.spatialDb,
                                overlaysDir,
                                this.contentSources.values(),
                                this.fileDatabases);
                    }

                    success = true;
                } finally {
                    // XXX - legacy implementation needs bulk modification for
                    //       transaction, new implementation does not
                    if (this.spatialDb instanceof PersistentDataSourceFeatureDataStore)
                        this.spatialDb.endBulkModification(success);
                }

            } catch (SQLiteException e) {
                Log.e(TAG, "Scan failed.", e);
            }
        }

        if (DeveloperOptions.getIntOption("feature-metadata-enabled", 1) == 0)
            this.mapView.addLayer(MapView.RenderStack.VECTOR_OVERLAYS, 0,
                    this.layer);
    }

    /**************************************************************************/

    private class WktImporter implements Importer {
        private final String contentType;
        private final SpatialDbContentSource source;

        WktImporter(String contentType, SpatialDbContentSource source) {
            this.contentType = contentType;
            this.source = source;
        }

        @Override
        public String getContentType() {
            return this.contentType;
        }

        @Override
        public Set<String> getSupportedMIMETypes() {
            return this.source.getSupportedMIMETypes();
        }

        @Override
        public ImportResult importData(InputStream source, String mime,
                Bundle bundle) {
            return ImportResult.FAILURE;
        }

        @Override
        public ImportResult importData(Uri uri, String mime, Bundle bundle)
                throws IOException {
            if (uri.getScheme() != null && !uri.getScheme().equals("file"))
                return ImportResult.FAILURE;

            ImportResult retval;
            synchronized (WktMapComponent.this) {
                WktMapComponent.this.spatialDb.beginBulkModification();
                boolean success = false;
                try {
                    retval = this.source.importData(uri, mime, bundle);
                    success = true;
                } finally {
                    WktMapComponent.this.spatialDb.endBulkModification(success);
                }
            }
            return retval;
        }

        @Override
        public boolean deleteData(Uri uri, String mime) {
            if (uri.getScheme() != null && !uri.getScheme().equals("file"))
                return false;

            synchronized (WktMapComponent.this) {
                WktMapComponent.this.spatialDb.beginBulkModification();
                boolean success = false;
                try {
                    this.source.deleteData(uri, mime);
                    success = true;
                } finally {
                    WktMapComponent.this.spatialDb.endBulkModification(success);
                }
            }
            return true;
        }
    }

    private BroadcastReceiver dataMgmtReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Deleting spatial data");

            //Clean up Spatial DBs
            final String[] mountPoints = FileSystemUtils.findMountPoints();
            Iterator<SpatialDbContentSource> iter = contentSources.values()
                    .iterator();

            // build out the list of all files in the features database
            Set<File> spatialDbFilesToDelete = new HashSet<>();

            DataSourceFeatureDataStore.FileCursor result = null;
            try {
                result = spatialDb.queryFiles();
                if (result != null) {
                    while (result.moveToNext())
                        spatialDbFilesToDelete.add(result.getFile());
                }
            } finally {
                if (result != null)
                    result.close();
            }

            // clear out the feature DB, per content
            while (iter.hasNext()) {
                SpatialDbContentSource contentSource = iter.next();
                // delete from DB
                contentSource.deleteAll();

                //delete data directory
                for (String mountPoint : mountPoints) {
                    File scanDir = new File(mountPoint,
                            contentSource.getFileDirectoryName());
                    if (scanDir.exists() && scanDir.isDirectory())
                        FileSystemUtils.deleteDirectory(scanDir, true);
                }
            }

            // delete any remaining files that may have been externally imported
            for (File overlayFile : spatialDbFilesToDelete)
                if (overlayFile.exists())
                    FileSystemUtils.deleteFile(overlayFile);

            //Clean up File Databases
            for (FileDatabase fileDatabase : fileDatabases) {

                List<String> files = fileDatabase.queryFiles();

                //delete from DB
                fileDatabase.deleteAll();

                //delete from UI
                if (fileDatabase.getRootGroup() != null) {
                    fileDatabase.getRootGroup().clearItems();
                    fileDatabase.getRootGroup().clearGroups();
                }

                //delete files from file system (atak dir and external/linked files)
                for (String filepath : files) {
                    FileSystemUtils.deleteFile(new File(FileSystemUtils
                            .sanitizeWithSpacesAndSlashes(filepath)));
                }

                //delete data directory
                for (String mountPoint : mountPoints) {
                    File scanDir = new File(mountPoint, fileDatabase
                            .getFileDirectory().getName());
                    if (scanDir.exists() && scanDir.isDirectory())
                        FileSystemUtils.deleteDirectory(scanDir, true);
                }
            }
        }
    };
}
