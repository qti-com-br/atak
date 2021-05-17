
package com.atakmap.spatial.file;

import android.graphics.drawable.Drawable;

import com.atakmap.android.hierarchy.action.Visibility;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapView;
import com.atakmap.annotations.DeprecatedApi;
import com.atakmap.app.R;
import com.atakmap.map.layer.feature.geometry.Envelope;

import java.io.File;

/**
 * @deprecated Transitioned to Map Engine Features API
 * Replaced by {@link FalconViewContentHandler}
 */
@Deprecated
@DeprecatedApi(since = "4.3", forRemoval = true, removeAt = "4.6")
public class FileDatabaseContentHandler extends FileOverlayContentHandler
        implements Visibility {

    protected final MapGroup _group;
    protected final String _contentType, _mimeType;

    protected FileDatabaseContentHandler(MapView mv, File file,
            MapGroup group, Envelope bounds, String contentType,
            String mimeType) {
        super(mv, file, bounds);
        _group = group;
        _contentType = contentType;
        _mimeType = mimeType;
    }

    @Override
    public Drawable getIcon() {
        return _context.getDrawable(
                R.drawable.ic_geojson_file_notification_icon);
    }

    @Override
    public String getContentType() {
        return _contentType;
    }

    @Override
    public String getMIMEType() {
        return _mimeType;
    }

    @Override
    public boolean setVisibleImpl(boolean visible) {
        _group.setVisible(visible);
        return true;
    }

    @Override
    public boolean isVisible() {
        return isConditionVisible() && _group.getVisible();
    }
}
