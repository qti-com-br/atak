
package com.atakmap.map.layer.feature.opengl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import com.atakmap.coremap.log.Log;

import android.database.sqlite.SQLiteException;
import android.util.Pair;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.MapControl;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.layer.Layer2;
import com.atakmap.map.layer.control.AttributionControl;
import com.atakmap.map.layer.control.Controls;
import com.atakmap.map.layer.control.TimeSpanControl;
import com.atakmap.map.layer.feature.Adapters;
import com.atakmap.map.layer.feature.AttributeSet;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.FeatureCursor;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureDataStore2.FeatureSetQueryParameters;
import com.atakmap.map.layer.feature.FeatureDefinition;
import com.atakmap.map.layer.feature.FeatureDefinition2;
import com.atakmap.map.layer.feature.FeatureLayer;
import com.atakmap.map.layer.feature.FeatureLayer2;
import com.atakmap.map.layer.feature.FeatureLayer3;
import com.atakmap.map.layer.feature.style.Style;
import com.atakmap.map.layer.feature.FeatureDataStore2.FeatureQueryParameters;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.GeometryCollection;
import com.atakmap.map.layer.feature.geometry.GeometryFactory;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.feature.geometry.Polygon;
import com.atakmap.map.layer.feature.geometry.opengl.GLBatchGeometry;
import com.atakmap.map.layer.feature.geometry.opengl.GLBatchGeometryCollection;
import com.atakmap.map.layer.feature.geometry.opengl.GLBatchGeometryRenderer;
import com.atakmap.map.layer.feature.geometry.opengl.GLBatchLineString;
import com.atakmap.map.layer.feature.geometry.opengl.GLBatchMultiLineString;
import com.atakmap.map.layer.feature.geometry.opengl.GLBatchMultiPoint;
import com.atakmap.map.layer.feature.geometry.opengl.GLBatchMultiPolygon;
import com.atakmap.map.layer.feature.geometry.opengl.GLBatchPolygon;
import com.atakmap.map.layer.feature.geometry.opengl.GLBatchPoint;
import com.atakmap.map.layer.feature.ogr.style.FeatureStyleParser;
import com.atakmap.map.layer.feature.service.FeatureHitTestControl;
import com.atakmap.map.layer.opengl.GLAsynchronousLayer2;
import com.atakmap.map.layer.raster.osm.OSMUtils;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.math.Statistics;

public class GLBatchGeometryFeatureDataStoreRenderer extends
        GLAsynchronousLayer2<LinkedList<GLBatchGeometry>> implements
        FeatureDataStore2.OnDataStoreContentChangedListener,
        FeatureHitTestControl {

    private final static Set<Class<? extends MapControl>> filteredControls = new HashSet<Class<? extends MapControl>>();
    static {
        filteredControls.add(TimeSpanControl.class);
        filteredControls.add(AttributionControl.class);
    }

    private final static String TAG = "GLBatchGeometryFeatureDataStoreRenderer";

    private Map<Integer, Statistics> queryStats = new HashMap<Integer, Statistics>();
    private Collection<GLBatchGeometryRenderer> renderList;

    private GLBatchGeometryRenderer batchRenderer;

    private Map<Long, GLBatchGeometry> glSpatialItems;
    
    private final Layer2 subject;
    private final FeatureDataStore2 dataStore;
    private final TimespanControlImpl timespanControl;
    private final AttributionControlImpl attributionControl;
    private final AttributionControl srcAttributionControl;
    private final Set<MapControl> controls;

    public GLBatchGeometryFeatureDataStoreRenderer(MapRenderer surface, FeatureLayer subject) {
        this(surface, subject, Adapters.adapt(subject.getDataStore()));
    }
    
    public GLBatchGeometryFeatureDataStoreRenderer(MapRenderer surface, FeatureLayer2 subject) {
        this(surface, subject, Adapters.adapt(subject.getDataStore()));
    }
    
    public GLBatchGeometryFeatureDataStoreRenderer(MapRenderer surface, FeatureLayer3 subject) {
        this(surface, subject, subject.getDataStore());
    }

    public GLBatchGeometryFeatureDataStoreRenderer(MapRenderer surface, Layer2 subject, FeatureDataStore2 dataStore) {
        super(surface, subject);
        
        this.subject = subject;
        this.dataStore = dataStore;

        this.batchRenderer = new GLBatchGeometryRenderer(surface);

        this.renderList = new ArrayList<GLBatchGeometryRenderer>();

        this.glSpatialItems = new HashMap<Long, GLBatchGeometry>();
        
        if(this.dataStore.hasTimeReference())
            this.timespanControl = new TimespanControlImpl();
        else
            this.timespanControl = null;
        
        this.srcAttributionControl = (this.dataStore instanceof Controls) ? ((Controls)this.dataStore).getControl(AttributionControl.class) : null;
        if(this.srcAttributionControl != null)
            this.attributionControl = new AttributionControlImpl();
        else
            this.attributionControl = null;
        
        this.controls = new HashSet<MapControl>();
        if(this.timespanControl != null)
            this.controls.add(this.timespanControl);
        if(this.attributionControl != null)
            this.controls.add(this.attributionControl);

        if(this.dataStore instanceof Controls) {
            Collection<Object> dataStoreControls = new LinkedList<Object>();
            ((Controls)this.dataStore).getControls(dataStoreControls);
            for(Object ctrl : dataStoreControls) {
                if(!(ctrl instanceof MapControl))
                    continue;
                boolean filtered = false;
                for(Class<? extends MapControl> ctrlClazz : filteredControls) {
                    if(ctrlClazz.isAssignableFrom(ctrl.getClass())) {
                        filtered = true;
                        break;
                    }
                }
                if(filtered)
                    continue;

                this.controls.add((MapControl)ctrl);
            }
        }
    }
    
    /**************************************************************************/
    // GL Layer
    
    @Override
    public synchronized void start() {
        super.start();
        
        this.dataStore.addOnDataStoreContentChangedListener(this);
        this.renderContext.registerControl(this.subject, this);
        for(MapControl ctrl : this.controls)
            this.renderContext.registerControl(this.subject, ctrl);        
    }
    
    @Override
    public synchronized void stop() {
        super.stop();
        
        this.renderContext.unregisterControl(this.subject, this);
        for(MapControl ctrl : this.controls)
            this.renderContext.unregisterControl(this.subject, ctrl);

        this.dataStore.removeOnDataStoreContentChangedListener(this);
    }

    @Override
    public int getRenderPass() {
        return GLMapView.RENDER_PASS_SPRITES|GLMapView.RENDER_PASS_SURFACE;
    }

    /**************************************************************************/
    // GL Asynchronous Map Renderable

    @Override
    protected void releaseImpl() {        
        this.batchRenderer.release();
        
        // release all renderables in the cache -- per the implementation of
        // GLAsynchronousMapRenderable, the query thread MUST be stopped by the
        // time we hit this method
        this.glSpatialItems.clear();
    }

    @Override
    protected Collection<GLBatchGeometryRenderer> getRenderList() {
        return this.renderList;
    }

    @Override
    protected void resetPendingData(LinkedList<GLBatchGeometry> pendingData) {
        pendingData.clear();
    }

    @Override
    protected void releasePendingData(LinkedList<GLBatchGeometry> pendingData) {
        this.resetPendingData(pendingData);
    }

    @Override
    protected LinkedList<GLBatchGeometry> createPendingData() {
        return new LinkedList<GLBatchGeometry>();
    }

    @Override
    protected boolean updateRenderList(ViewState state, LinkedList<GLBatchGeometry> pendingData) {
        if (this.renderList.size() < 1)
            this.renderList.add(this.batchRenderer);
        this.batchRenderer.setBatch(pendingData);

        return true;
    }

    @Override
    protected String getBackgroundThreadName() {
        return "Feature [" + this.subject.getName() + "] (Batch) GL worker@" + Integer.toString(this.hashCode());
    }

    @Override
    protected void query(ViewState state, LinkedList<GLBatchGeometry> result) {
        
        ViewState scratch = this.newViewStateInstance();
        scratch.copy(state);
        try {
            if (state.crossesIDL) {
                Set<GLBatchGeometry> resultSet = new HashSet<>();

                ViewState unwrapped = new ViewState();
                unwrapped.copy(state);
                unwrapped.westBound = state.westBound;
                unwrapped.eastBound = state.westBound+360d;
                queryImpl(unwrapped, resultSet);

                unwrapped.copy(state);
                unwrapped.westBound = state.eastBound-360d;
                unwrapped.eastBound = state.eastBound;
                queryImpl(unwrapped, resultSet);

                result.addAll(resultSet);
            } else {
                queryImpl(state, result);
            }

            // XXX - depth sort

            if(this.attributionControl != null) {
                Set<Pair<String, String>> attrs;
                if(!result.isEmpty())
                    attrs = this.srcAttributionControl.getContentAttribution();
                else
                    attrs = Collections.<Pair<String, String>>emptySet();
                this.attributionControl.setAttributions(attrs);
            }
        } finally {
            state.copy(scratch);
        }
    }

    private void queryImpl(
            ViewState state,
            Collection<GLBatchGeometry> result) {

        final int lod = OSMUtils.mapnikTileLevel(state.drawMapResolution);
        
        double simplifyFactor = (state.drawMapResolution/111111d) * 2;
        
        if(this.checkQueryThreadAbort())
            return;

        FeatureQueryParameters params = new FeatureQueryParameters();
        params.visibleOnly = true;
        params.spatialFilter = GeometryFactory.fromEnvelope(new Envelope(state.westBound, state.southBound, 0d, state.eastBound, state.northBound, 0d));
        params.featureSetFilter = new FeatureSetQueryParameters();
        params.featureSetFilter.maxResolution = state.drawMapResolution;
        params.spatialOps = Collections.<FeatureQueryParameters.SpatialOp>singleton(new FeatureQueryParameters.SpatialOp.Simplify(simplifyFactor));
        params.ignoredFeatureProperties = FeatureDataStore2.PROPERTY_FEATURE_ATTRIBUTES;
        if(this.timespanControl != null) {
            params.minimumTimestamp = this.timespanControl.currentMin;
            params.maximumTimestamp = this.timespanControl.currentMax;
        }

        try {        
            Map<String, Style> styleMap = new HashMap<String, Style>();

            Long featureId;
            ByteBuffer blob;
            Geometry geom;
            Style style;
            
            long s = System.currentTimeMillis();

            FeatureCursor cursor = null;
            try {
                cursor = this.dataStore.queryFeatures(params);
                while (cursor.moveToNext()) {
                    if(this.checkQueryThreadAbort())
                        break;

                    featureId = cursor.getId();

                    GLBatchGeometry glitem = this.glSpatialItems.get(featureId);
                    if (glitem != null) {
                        // XXX - this isn't perfect as it doesn't take into
                        // account the point scale factor, however
                        // performance is probably more important than
                        // the small amount of detail we may end up
                        // losing

                        // if we've already simplified the geometry at the
                        // current level of detail, just go with what we have.
                        if (glitem.lod == lod) {
                            result.add(glitem);
                            continue;
                        }
                    }

                    final int type;
                    boolean useBlobGeom = false;
                    blob = null;
                    geom = null;
                    if(cursor.getGeomCoding() == FeatureDefinition.GEOM_SPATIALITE_BLOB) {
                        byte[] rawGeom = (byte[])cursor.getRawGeometry();
                        if (rawGeom == null)
                            continue;
    
                        blob = ByteBuffer.wrap(rawGeom);
                        if ((blob.get() & 0xFF) != 0x00) // marker byte
                            continue;
                        // endian
                        switch (blob.get() & 0xFF) {
                            case 0x00:
                                blob.order(ByteOrder.BIG_ENDIAN);
                                break;
                            case 0x01:
                                blob.order(ByteOrder.LITTLE_ENDIAN);
                                break;
                            default:
                                continue;
                        }
                        // skip the SRID and MBR
                        blob.position(blob.position() + 36);
                        if ((blob.get() & 0xFF) != 0x7C) // marker byte
                            // XXX - drop through and process as Geometry class
                            continue;
                        
                        type = blob.getInt();
                        useBlobGeom = true;
                    } else {
                        if(cursor.getGeomCoding() == FeatureDefinition.GEOM_ATAK_GEOMETRY)
                            geom = (Geometry)cursor.getRawGeometry();
                        else
                            geom = cursor.get().getGeometry();

                        if(geom instanceof Point) {
                            type = 1;
                        } else if(geom instanceof LineString) {
                            type = 2;
                        } else if(geom instanceof Polygon) {
                            type = 3;
                        } else if(geom instanceof GeometryCollection) {
                            type = 7;
                        } else if(geom == null) {
                            continue;
                        } else {
                            throw new IllegalStateException();
                        }
                    }

                    if (glitem == null) {
                        switch (type % 1000) {
                            case 1:
                                glitem = new GLBatchPoint(this.renderContext);
                                break;
                            case 2:
                                glitem = new GLBatchLineString(this.renderContext);
                                break;
                            case 3:
                                glitem = new GLBatchPolygon(this.renderContext);
                                break;
                            case 4:
                                glitem = new GLBatchMultiPoint(this.renderContext);
                                break;
                            case 5:
                                glitem = new GLBatchMultiLineString(this.renderContext);
                                break;
                            case 6:
                                glitem = new GLBatchMultiPolygon(this.renderContext);
                                break;
                            case 7:
                                glitem = new GLBatchGeometryCollection(this.renderContext);
                                break;
                            default:
                                Log.d(TAG, "Geometry type not supported, skipping feature "
                                        + featureId);
                                continue;
                        }

                        if(this.checkQueryThreadAbort())
                            break;

                        glitem.init(featureId.longValue(), cursor.getName());

                        style = null;
                        if(cursor.getStyleCoding() == FeatureDefinition.STYLE_OGR) {
                            final String styleString = (String)cursor.getRawStyle();
                            if (styleString != null) {
                                style = styleMap.get(styleString);
                                if (style == null) {
                                    style = FeatureStyleParser.parse2(styleString);
                                    if(style != null)
                                        styleMap.put(styleString, style);
                                }
                            }
                        } else if(cursor.getStyleCoding() == FeatureDefinition.STYLE_ATAK_STYLE) {
                            style = (Style)cursor.getRawStyle();
                        } else {
                            style = cursor.get().getStyle();
                        }
                        if (style != null)
                            glitem.setStyle(style);

                        this.glSpatialItems.put(featureId, glitem);
                    }

                    if(this.checkQueryThreadAbort())
                        break;

                    // XXX - geometry only needs to get set on GLBatchPoint once
                    if(useBlobGeom) {
                        glitem.setGeometry(blob, type, lod);
                    } else {
                        switch (type % 1000) {
                            case 1:
                                if(!(glitem instanceof GLBatchPoint))
                                    continue;
                                glitem.setGeometry(geom, lod);
                                break;
                            case 2:
                                if(!(glitem instanceof GLBatchLineString))
                                    continue;
                                glitem.setGeometry(geom, lod);
                                break;
                            case 3:
                                if(!(glitem instanceof GLBatchPolygon))
                                    continue;
                                glitem.setGeometry(geom, lod);
                                break;
                            case 4:
                            case 5:
                            case 6:
                            case 7:
                                if(!(glitem instanceof GLBatchGeometryCollection))
                                    continue;
                                glitem.setGeometry(geom, lod);
                                break;
                            default:
                                throw new IllegalStateException();
                        }
                    }

                    // only add the result if it's visible
                    result.add(glitem);
                }
            } catch(DataStoreException e) {
                Log.w(TAG, "Unexpected exception occurred during feature query on " + this.subject.getName(), e);
            } finally {
                if(cursor != null)
                    cursor.close();
            }

            long e = System.currentTimeMillis();
            Statistics stats = queryStats.get(lod);
            if (stats == null)
                queryStats.put(lod, stats = new Statistics());
            stats.observe(e - s);
/*            
            Log.d(TAG, "SPATIAL DB [" + this.subject.getName() + "] QUERY in " + (e-s) + "ms (avg=" + stats.average +
             ") " + result.size() + " RESULTS lod=" +
             OSMUtils.mapnikTileLevel(state.drawMapResolution) + " simplify factor=" +
             simplifyFactor);
 */
        } catch (SQLiteException e) {
            // XXX - seeing this get raised pretty consistently on shutdown as
            //       result of race between Database.close() and the worker
            //       thread exiting. Worker thread needs to be join()'d as part
            //       of release(), but this will require some significant effort
            //       to ensure query is aborted in non-blocking way across all
            //       asynchronous renderables. Log exception for 2.2 release.
            
            //throw new RuntimeException(e);
            Log.e("GLPersistentDataSourceFeatureDataStore", "Unexpected SQLite exception", e);
        }
    }

    @Override
    public void onDataStoreContentChanged(FeatureDataStore2 dataStore) {
        this.invalidate();
    }
    
    // XXX - next 4 -- for now, just invoke content changed. in the future we
    //       should update individual features for more interactive editing
    @Override
    public void onFeatureInserted(FeatureDataStore2 dataStore, long fid, FeatureDefinition2 def, long version) {
        this.invalidate();
    }
    @Override
    public void onFeatureUpdated(FeatureDataStore2 dataStore, long fid, int modificationMask, String name, Geometry geom, Style style, AttributeSet attribs, int attribsUpdateType) {
        this.invalidate();
    }
    @Override
    public void onFeatureDeleted(FeatureDataStore2 dataStore, long fid) {
        this.invalidate();
    }
    @Override
    public void onFeatureVisibilityChanged(FeatureDataStore2 dataStore, long fid, boolean visible) {
        this.invalidate();
    }

    /**************************************************************************/
    // Hit Test Service

    @Override
    public void hitTest(Collection<Long> fids,
                        float screenX,
                        float screenY,
                        final GeoPoint touch,
                        double resolution,
                        float radius,
                        final int limit) {

        double mercatorscale = Math.cos(Math.toRadians(touch.getLatitude()));
        if (mercatorscale < 0.0001)
            mercatorscale = 0.0001;
        final double meters = radius * resolution * mercatorscale;

        // geometries are modified on the GL thread so we need to do the
        // hit-testing on that thread
        final Collection<Long> glfids = new LinkedList<Long>();
        final boolean[] signaled = new boolean[] {false};
        
        this.renderContext.queueEvent(new Runnable() {
            @Override
            public void run() {
                synchronized(GLBatchGeometryFeatureDataStoreRenderer.this) {
                    if(GLBatchGeometryFeatureDataStoreRenderer.this.batchRenderer != null) {
                        //long s = System.currentTimeMillis();
                        GLBatchGeometryFeatureDataStoreRenderer.this.batchRenderer.hitTest2(
                                glfids,
                                new Point(touch.getLongitude(),
                                          touch.getLatitude()),
                                meters,
                                limit);
                        //long e = System.currentTimeMillis();
                        //Log.d(TAG, "HIT TEST [" + subject.getName() + "] in " + (e-s) + "ms");
                    }

                }
                
                synchronized(signaled) {
                    signaled[0] = true;
                    signaled.notify();
                }
            }
        });
        
        synchronized(signaled) {
            while(!signaled[0])
                try {
                    signaled.wait();
                } catch(InterruptedException ignored) {} 
        }

        fids.addAll(glfids);
    }
    
    private class TimespanControlImpl implements TimeSpanControl {

        private TimeSpan bounds = TimeSpan.createInterval(dataStore.getMinimumTimestamp(), dataStore.getMaximumTimestamp());
        private TimeSpan current = bounds;
        long currentMin = FeatureDataStore2.TIMESTAMP_NONE;
        long currentMax = FeatureDataStore2.TIMESTAMP_NONE;
        
        @Override
        public int getSupportedTimespanTypes() {
            return TIMESPAN_TYPE_INTERVAL;
        }

        @Override
        public long getMinimumTime() {
            return GLBatchGeometryFeatureDataStoreRenderer.this.dataStore.getMinimumTimestamp();
        }

        @Override
        public long getMaximumTime() {
            return GLBatchGeometryFeatureDataStoreRenderer.this.dataStore.getMaximumTimestamp();
        }

        @Override
        public synchronized TimeSpan getCurrentTimeSpan() {
            return this.current;
        }

        @Override
        public void setTimeSpan(TimeSpan timespan) {
            final long srcStart = this.bounds.getTimeStamp();
            final long srcStop = this.bounds.getTimeStamp()+this.bounds.getDuration();
            final long dstStart = timespan.getTimeStamp();
            final long dstStop = timespan.getTimeStamp()+timespan.getDuration();
            
            if(dstStart <= srcStart && dstStop >= srcStop) {
                this.currentMin = FeatureDataStore2.TIMESTAMP_NONE;
                this.currentMax = FeatureDataStore2.TIMESTAMP_NONE;
            } else {
                this.currentMin = dstStart;
                this.currentMax = dstStop;
            }
            
            this.current = timespan;
            
            invalidate();
        }
        
    }
    
    private static class AttributionControlImpl implements AttributionControl {

        private Set<OnAttributionUpdatedListener> listeners;
        private Set<Pair<String, String>> attributions;
        
        public AttributionControlImpl() {
            this.listeners = Collections.newSetFromMap(new IdentityHashMap<OnAttributionUpdatedListener, Boolean>());
            this.attributions = Collections.<Pair<String, String>>emptySet();
        }

        synchronized void setAttributions(Set<Pair<String, String>> attrs) {
            this.attributions = attrs;
            for(OnAttributionUpdatedListener l : this.listeners)
                l.onAttributionUpdated(this);
        }

        @Override
        public synchronized Set<Pair<String, String>> getContentAttribution() {
            return this.attributions;
        }

        @Override
        public synchronized void addOnAttributionUpdatedListener(OnAttributionUpdatedListener l) {
            this.listeners.add(l);
        }

        @Override
        public synchronized void removeOnAttributionUpdatedListener(OnAttributionUpdatedListener l) {
            this.listeners.remove(l);
        }
        
    }
}
