
package com.atakmap.android.video;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.video.manager.VideoManager;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Display a list of videos
 * 
 * 
 */
public class VideoListDialog {

    private static final String TAG = "VideoListDialog";

    public interface Callback {
        void onVideosSelected(List<ConnectionEntry> selected);
    }

    private final Context _context;

    private VideoAdapter _adapter;
    private Callback _callback;
    private AlertDialog _dialog;

    /**
     * Create a new video list dialog
     * @param mapView The map view - This is used over Context because it forces
     *                plugin developers to use the proper app context
     */
    public VideoListDialog(MapView mapView) {
        _context = mapView.getContext();
    }

    /**
     * Display dialog to select from a list of videos
     *
     * @param title Dialog title
     * @param entries List of connection entries to choose from
     * @param callback Selection callback
     */
    public boolean show(String title, List<ConnectionEntry> entries,
            boolean multiSelect, final Callback callback) {
        if (FileSystemUtils.isEmpty(entries)) {
            Log.w(TAG, "No videos provided");
            return false;
        }

        _callback = callback;
        _adapter = new VideoAdapter(entries, multiSelect);
        AlertDialog.Builder b = new AlertDialog.Builder(_context);
        b.setIcon(R.drawable.ic_video_alias);
        if (!FileSystemUtils.isEmpty(title))
            b.setTitle(title);
        else // XXX - These string resource IDs are horrendous...
            b.setTitle(multiSelect ? R.string.video_text6
                    : R.string.point_dropper_text18);
        b.setAdapter(_adapter, null);
        if (multiSelect) {
            b.setPositiveButton(R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int w) {
                            finish();
                        }
                    });
        }
        b.setNegativeButton(R.string.cancel, null);
        _dialog = b.show();
        _dialog.getListView().setOnItemClickListener(_adapter);
        return true;
    }

    public boolean show(boolean multiSelect, final Callback callback) {
        List<ConnectionEntry> remote = VideoManager.getInstance()
                .getRemoteEntries();
        if (remote.isEmpty()) {
            Toast.makeText(_context, R.string.point_dropper_text17,
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        return show(null, remote, multiSelect, callback);
    }

    private void finish() {
        if (_callback != null && _adapter != null)
            _callback.onVideosSelected(_adapter.getSelected());
        if (_dialog != null)
            _dialog.dismiss();
    }

    private class VideoAdapter extends BaseAdapter implements
            ListView.OnItemClickListener {

        private final List<ConnectionEntry> _entries;
        private final Set<ConnectionEntry> _selected = new HashSet<>();
        private final boolean _multiSelect;

        private VideoAdapter(List<ConnectionEntry> entries,
                boolean multiSelect) {
            _entries = entries;
            _multiSelect = multiSelect;
        }

        List<ConnectionEntry> getSelected() {
            return new ArrayList<>(_selected);
        }

        @Override
        public int getCount() {
            return _entries.size();
        }

        @Override
        public ConnectionEntry getItem(int position) {
            return _entries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int pos, View row, ViewGroup parent) {
            ViewHolder h = row != null ? (ViewHolder) row.getTag() : null;
            if (h == null) {
                row = LayoutInflater.from(_context).inflate(
                        R.layout.videolist_select_items, parent, false);
                h = new ViewHolder();
                h.alias = row.findViewById(
                        R.id.videolist_select_alias);
                h.url = row.findViewById(R.id.videolist_select_url);
                h.selected = row.findViewById(
                        R.id.videolist_select_checkbox);
                row.setTag(h);
            }
            ConnectionEntry ce = getItem(pos);
            if (ce == null)
                return LayoutInflater.from(_context).inflate(R.layout.empty,
                        parent, false);
            h.alias.setText(ce.getAlias());
            h.url.setText(ConnectionEntry.getURL(ce));
            if (_multiSelect) {
                h.selected.setVisibility(View.VISIBLE);
                h.selected.setChecked(_selected.contains(ce));
            } else
                h.selected.setVisibility(View.GONE);
            return row;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View v, int p, long id) {
            ConnectionEntry ce = getItem(p);
            if (ce != null) {
                if (_selected.contains(ce))
                    _selected.remove(ce);
                else
                    _selected.add(ce);
                notifyDataSetChanged();
                if (!_multiSelect)
                    finish();
            }
        }
    }

    private static class ViewHolder {
        TextView alias;
        TextView url;
        CheckBox selected;
    }

    // Deprecated methods/callback - ugly and not as intuitive

    @Deprecated
    public static boolean selectVideo(Context context, String message,
            List<ConnectionEntry> videoConnections,
            final VideoSelectionCallback callback) {
        MapView mv = MapView.getMapView();
        if (mv == null)
            return false;
        return new VideoListDialog(mv).show(message, videoConnections, false,
                new Callback() {
                    @Override
                    public void onVideosSelected(
                            List<ConnectionEntry> selected) {
                        if (selected == null)
                            return;
                        callback.onSelection(selected.get(0));
                    }
                });
    }

    @Deprecated
    public static boolean selectVideos(Context context, String message,
            List<ConnectionEntry> videoConnections,
            final VideoSelectionCallback callback) {
        MapView mv = MapView.getMapView();
        if (mv == null)
            return false;
        return new VideoListDialog(mv).show(message, videoConnections, true,
                new Callback() {
                    @Override
                    public void onVideosSelected(
                            List<ConnectionEntry> selected) {
                        callback.onSelection(new HashSet<>(selected));
                    }
                });
    }

    @Deprecated
    public interface VideoSelectionCallback {

        void onSelection(HashSet<ConnectionEntry> selected);

        void onSelection(ConnectionEntry selected);
    }
}
