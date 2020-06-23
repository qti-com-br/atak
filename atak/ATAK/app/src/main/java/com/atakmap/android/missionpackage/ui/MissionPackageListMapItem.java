
package com.atakmap.android.missionpackage.ui;

import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.missionpackage.file.MissionPackageContent;
import com.atakmap.android.missionpackage.file.MissionPackageManifest;
import com.atakmap.android.missionpackage.file.NameValuePair;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.assets.Icon;
import com.atakmap.coremap.maps.coords.GeoPoint;

/**
 * UI convenience wrapper around MapItem MissionPackageContent
 * 
 * 
 */
public class MissionPackageListMapItem extends MissionPackageListItem {

    private static final String TAG = "MissionPackageListMapItem";

    public GeoPoint point;

    public MissionPackageListMapItem(Icon icon, String type, String name,
            MissionPackageContent content) {
        super(icon, type, name, content);
    }

    @Override
    public boolean isFile() {
        return false;
    }

    public String getUID() {
        if (_content == null)
            return null;

        NameValuePair p = _content
                .getParameter(MissionPackageContent.PARAMETER_UID);
        if (p == null || !p.isValid())
            return null;

        return p.getValue();
    }

    @Override
    public boolean exists(MapView mapView) {
        String uid = getUID();
        if (FileSystemUtils.isEmpty(uid))
            return false;

        MapItem item = mapView.getRootGroup().deepFindUID(uid);
        return item != null;
    }

    @Override
    public void removeContent() {
        String uid = getUID();
        if (FileSystemUtils.isEmpty(uid)) {
            Log.d(TAG, "UID not set");
            return;
        }
        MapView mv = MapView.getMapView();
        if (mv == null)
            return;

        MapItem item = mv.getRootGroup().deepFindUID(uid);
        if (item != null)
            item.removeFromGroup();
    }

    @Override
    public String toString() {
        return getUID();
    }

    @Override
    public long getsizeInBytes() {
        // TODO currently estimating, also include size of attached images?
        return MissionPackageManifest.MAP_ITEM_ESTIMATED_SIZE;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object rhs) {
        if (!(rhs instanceof MissionPackageListMapItem))
            return false;

        MissionPackageListMapItem rhsc = (MissionPackageListMapItem) rhs;

        if (!FileSystemUtils.isEquals(getUID(), rhsc.getUID()))
            return false;

        return super.equals(rhs);
    }
}
