
package com.atakmap.android.features;

import android.content.Intent;
import android.graphics.Color;
import android.view.View;

import com.atakmap.android.elev.dt2.Dt2ElevationModel;
import com.atakmap.android.hierarchy.HierarchyListItem;
import com.atakmap.android.hierarchy.action.Action;
import com.atakmap.android.hierarchy.action.Delete;
import com.atakmap.android.hierarchy.action.GoTo;
import com.atakmap.android.hierarchy.action.Visibility;
import com.atakmap.android.hierarchy.items.AbstractHierarchyListItem;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.Location;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.overlay.MapOverlay;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.app.R;

import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoPoint;

import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataStore;
import com.atakmap.map.layer.feature.style.Style;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.feature.style.BasicFillStyle;
import com.atakmap.map.layer.feature.style.BasicPointStyle;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.CompositeStyle;
import com.atakmap.map.layer.feature.style.IconPointStyle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class FeatureHierarchyListItem extends
        AbstractHierarchyListItem implements GoTo,
        Visibility, Location, Delete {

    private final FeatureDataStore spatialDb;
    private final long fid;
    private final String name;
    private final GeoBounds bounds;
    private final boolean isPoint;
    private final GeoPointMetaData altitude;

    private final String iconUri;
    private final int iconColor;
    private final String uid;

    private final Set<Class<? extends Action>> actions;

    public FeatureHierarchyListItem(
            FeatureDataStore spatialDb,
            Feature feature) {

        this(
                spatialDb,
                feature.getId(),
                feature.getName(),
                iconUriFromStyle(feature.getStyle()),
                colorFromStyle(feature.getStyle()),
                feature.getGeometry() instanceof Point,
                feature.getGeometry().getEnvelope().minX,
                feature.getGeometry().getEnvelope().minY,
                feature.getGeometry().getEnvelope().maxX,
                feature.getGeometry().getEnvelope().maxY);
    }

    public FeatureHierarchyListItem(
            FeatureDataStore spatialDb,
            long fid,
            String name,
            String iconUri,
            int iconColor,
            boolean isPoint,
            double minX,
            double minY,
            double maxX,
            double maxY) {

        this.spatialDb = spatialDb;
        this.fid = fid;
        this.name = name;
        this.isPoint = isPoint;
        this.bounds = new GeoBounds(maxY, minX, minY, maxX);

        this.iconUri = iconUri;
        this.iconColor = iconColor;

        this.uid = "spatialdb::" + this.spatialDb.getUri()
                + "::" + this.fid;

        // XXX - icon

        this.actions = new HashSet<>();
        if ((spatialDb.getVisibilitySettingsFlags()
                & FeatureDataStore.VISIBILITY_SETTINGS_FEATURE) == FeatureDataStore.VISIBILITY_SETTINGS_FEATURE)
            this.actions.add(Visibility.class);
        if ((spatialDb.getModificationFlags()
                & FeatureDataStore.MODIFY_FEATURESET_FEATURE_DELETE) == FeatureDataStore.MODIFY_FEATURESET_FEATURE_DELETE)
            this.actions.add(Delete.class);

        // XXX - do bounds check here?  not sure how bounds is ever null...
        this.actions.add(GoTo.class);

        if (isPoint) {
            GeoPoint center = this.bounds.getCenter(null);
            altitude = Dt2ElevationModel.getInstance().queryPoint(
                    center.getLatitude(), center.getLongitude());
        } else {
            altitude = new GeoPointMetaData();
        }
    }

    public static String iconUriFromStyle(Style style) {
        if (style != null) {
            IconPointStyle istyle = (style instanceof IconPointStyle)
                    ? (IconPointStyle) style
                    : null;
            BasicPointStyle bstyle = (style instanceof BasicPointStyle)
                    ? (BasicPointStyle) style
                    : null;

            if (style instanceof CompositeStyle) {
                istyle = (IconPointStyle) CompositeStyle
                        .find((CompositeStyle) style, IconPointStyle.class);
                bstyle = (BasicPointStyle) CompositeStyle
                        .find((CompositeStyle) style, BasicPointStyle.class);
            }

            if (style != null) {
                if (istyle != null) {
                    String uri = istyle.getIconUri();
                    if (uri != null)
                        return uri;
                    // Fallback to generic point
                    style = new BasicPointStyle(-1, 0);
                }
                if (bstyle != null)
                    return "android.resource://"
                            + MapView.getMapView().getContext()
                                    .getPackageName()
                            + "/"
                            + R.drawable.generic;
                else if (style instanceof BasicStrokeStyle)
                    return "android.resource://"
                            + MapView.getMapView().getContext()
                                    .getPackageName()
                            + "/"
                            + R.drawable.polyline;
                else if (style instanceof BasicFillStyle)
                    return "android.resource://"
                            + MapView.getMapView().getContext()
                                    .getPackageName()
                            + "/" + R.drawable.shape;
            }
        }
        return null;
    }

    public static int colorFromStyle(Style style) {
        if (style != null) {
            IconPointStyle istyle = (style instanceof IconPointStyle)
                    ? (IconPointStyle) style
                    : null;
            BasicPointStyle bstyle = (style instanceof BasicPointStyle)
                    ? (BasicPointStyle) style
                    : null;

            if (style instanceof CompositeStyle) {
                istyle = (IconPointStyle) CompositeStyle
                        .find((CompositeStyle) style, IconPointStyle.class);
                bstyle = (BasicPointStyle) CompositeStyle
                        .find((CompositeStyle) style, BasicPointStyle.class);
            }

            if (style != null) {
                if (istyle != null)
                    return istyle.getColor();
                else if (bstyle != null)
                    return bstyle.getColor();
                else if (style instanceof BasicStrokeStyle)
                    return ((BasicStrokeStyle) style).getColor();
                else if (style instanceof BasicFillStyle)
                    return ((BasicFillStyle) style).getColor();
            }
        }
        return Color.WHITE;
    }

    protected GeoBounds getBounds() {
        return bounds;
    }

    @Override
    public int getIconColor() {
        return this.iconColor;
    }

    @Override
    public String getIconUri() {
        return this.iconUri;
    }

    @Override
    public String getTitle() {
        return this.name;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public int getDescendantCount() {
        return 0;
    }

    @Override
    public HierarchyListItem getChildAt(int index) {
        return null;
    }

    @Override
    public boolean isChildSupported() {
        return false;
    }

    @Override
    public <T extends Action> T getAction(Class<T> clazz) {
        // XXX - legacy guard -- need to find out why we would get null bounds?
        if (clazz.equals(GoTo.class) && this.bounds == null)
            return null;

        if (this.actions.contains(clazz)) {
            return clazz.cast(this);
        } else {
            return null;
        }
    }

    @Override
    public Object getUserObject() {
        return null;
    }

    @Override
    public View getExtraView() {
        return null;
    }

    @Override
    public Sort refresh(Sort sort) {
        return sort;
    }

    /**************************************************************************/
    // Visibility

    @Override
    public boolean setVisible(boolean visible) {
        this.spatialDb.setFeatureVisible(this.fid, visible);

        return true;
    }

    @Override
    public boolean isVisible() {
        return this.spatialDb.isFeatureVisible(this.fid);
    }

    /**************************************************************************/
    // Go To

    @Override
    public boolean goTo(boolean select) {
        if (this.bounds == null)
            return false;

        MapView mv = MapView.getMapView();
        if (mv != null) {
            GeoPoint[] p;
            if (this.isPoint) {
                p = new GeoPoint[] {
                        bounds.getCenter(null)
                };
            } else {
                p = new GeoPoint[] {
                        new GeoPoint(bounds.getNorth(), bounds.getWest()),
                        new GeoPoint(bounds.getSouth(), bounds.getEast())
                };
            }
            ATAKUtilities.scaleToFit(mv, p, mv.getWidth(), mv.getHeight());
        }
        ArrayList<Intent> intents = new ArrayList<>(2);
        intents.add(new Intent("com.atakmap.android.maps.SHOW_MENU")
                .putExtra("uid", uid));
        intents.add(new Intent("com.atakmap.android.maps.SHOW_DETAILS")
                .putExtra("uid", uid));
        AtakBroadcast.getInstance().sendIntents(intents);
        return true;
    }

    /**************************************************************************/
    // Delete

    @Override
    public boolean delete() {
        this.spatialDb.deleteFeature(this.fid);
        return true;
    }

    /**************************************************************************/

    @Override
    public GeoPointMetaData getLocation() {
        GeoPoint location = GeoPoint.createMutable();
        location.set(Double.NaN, Double.NaN);
        location.set(altitude.get().getAltitude());
        if (this.bounds != null)
            this.bounds.getCenter(location);
        return GeoPointMetaData.wrap(location);
    }

    @Override
    public String getFriendlyName() {
        return this.getTitle();
    }

    @Override
    public String getUID() {
        return uid;
    }

    @Override
    public MapOverlay getOverlay() {
        return null;
    }
}
