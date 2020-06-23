
package com.atakmap.android.hashtags.overlay;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.hashtags.HashtagContent;
import com.atakmap.android.hierarchy.HierarchyListItem;
import com.atakmap.android.hierarchy.HierarchyListReceiver;
import com.atakmap.android.hierarchy.HierarchyListUserSelect;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class HashtagListUserSelect extends HierarchyListUserSelect {

    private static final String TAG = "HashtagListUserSelect";

    private final Context _context;

    public HashtagListUserSelect(MapView mapView) {
        super(TAG, 0L);
        _context = mapView.getContext();
    }

    @Override
    public String getTitle() {
        return _context.getString(R.string.tag_items, getTag());
    }

    @Override
    public String getButtonText() {
        return _context.getString(R.string.ok);
    }

    @Override
    public ButtonMode getButtonMode() {
        return ButtonMode.VISIBLE_WHEN_SELECTED;
    }

    @Override
    public boolean processUserSelections(Context context,
            Set<HierarchyListItem> selected) {

        String tag = getTag();
        if (FileSystemUtils.isEmpty(tag))
            return false;

        for (HierarchyListItem item : selected) {
            if (item instanceof HashtagContent) {
                HashtagContent content = (HashtagContent) item;
                Collection<String> tags = content.getHashtags();
                tags.add(tag);
                content.setHashtags(tags);
            }
        }

        AtakBroadcast.getInstance().sendBroadcast(new Intent(
                HierarchyListReceiver.CLEAR_HIERARCHY));

        ArrayList<String> paths = new ArrayList<>();
        paths.add(_context.getString(R.string.hashtags));
        paths.add(tag);
        Intent om = new Intent(HierarchyListReceiver.MANAGE_HIERARCHY);
        om.putStringArrayListExtra("list_item_paths", paths);
        om.putExtra("refresh", true);
        om.putExtra("isRootList", true);
        AtakBroadcast.getInstance().sendBroadcast(om);

        return false;
    }

    @Override
    public boolean accept(HierarchyListItem item) {
        return item.isChildSupported() || item instanceof HashtagContent;
    }
}
