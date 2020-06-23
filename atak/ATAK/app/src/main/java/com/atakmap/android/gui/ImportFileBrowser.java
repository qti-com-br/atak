
package com.atakmap.android.gui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.atakmap.android.filesystem.ResourceFile;
import com.atakmap.android.filesystem.ResourceFile.MIMEType;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.math.MathUtils;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;

import java.util.TimeZone;

//Based on code from
//https://github.com/mburman/Android-File-Explore

public class ImportFileBrowser extends LinearLayout {

    private static final String TAG = "ImportFileBrowser";

    /*************************** PRIVATE FIELDS **************************/
    private String[] _extensions;
    private String _userStartDirectory;
    private File _retFile;
    private View _up;
    private View _phoneButton;
    private View _sdcardButton;
    private static final String FILE_SEPARATOR = File.separator;
    private static final String INITIAL_DIRECTORY = FileSystemUtils.getRoot()
            + FILE_SEPARATOR;
    protected AlertDialog _alert;

    /*************************** PROTECTED FIELDS **************************/
    protected final ArrayList<String> _pathDirsList = new ArrayList<>();
    protected final List<FileItem> _fileList = new ArrayList<>();
    protected File _path;
    protected String _currFile;
    protected ArrayAdapter<FileItem> _adapter;
    protected boolean _directoryEmpty;

    /*************************** PUBLIC FIELDS **************************/
    public static final String WILDCARD = "*";

    /*************************** CONSTRUCTORS **************************/

    public ImportFileBrowser(Context context) {
        super(context);
    }

    public ImportFileBrowser(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /*************************** PUBLIC METHODS **************************/

    public void setUpButton(View upBtn) {
        _up = upBtn;
        _up.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                _navigateUpOneDirectory();
            }
        });
    }

    public void setInternalButton(View phoneButton) {
        _phoneButton = phoneButton;
        _phoneButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                _path = Environment.getExternalStorageDirectory();
                _parseDirectoryPath();
                _loadFileList();
                _adapter.notifyDataSetChanged();
                _updateCurrentDirectoryTextView();
                _scrollListToTop();
            }
        });
    }

    public void setExternalButton(View sdcardButton) {
        _sdcardButton = sdcardButton;
        _sdcardButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                String android_storage = System.getenv("ANDROID_STORAGE");
                _path = new File(android_storage);
                _parseDirectoryPath();
                _loadFileList();
                _adapter.notifyDataSetChanged();
                _updateCurrentDirectoryTextView();
                _scrollListToTop();
            }
        });
    }

    public File getReturnFile() {
        return _retFile;
    }

    public void setAlertDialog(AlertDialog alert) {
        _alert = alert;
    }

    /** 
     * Clears a previously selected file.
     */
    public void clear() {
        _retFile = null;
    }

    public void allowAnyExtenstionType() {
        this.setExtensionType(WILDCARD);
    }

    /**
     * Note must currently be called before _init();
     * 
     * @param path
     */
    public void setStartDirectory(String path) {
        _userStartDirectory = path;
    }

    /**
     * Get the current path shown in the browser
     *
     * @return Current path
     */
    public String getCurrentPath() {
        return _path != null ? _path.getAbsolutePath()
                : FileSystemUtils.getRoot().getAbsolutePath();
    }

    /**
     * Note, setExtensionType or setExtensionTypes must be called prior to using the view
     *
     * @param extension
     */
    public void setExtensionType(String extension) {
        this.setExtensionTypes(new String[] {
                extension
        });
    }

    /**
     * Note, setExtensionType or setExtensionTypes must be called prior to using the view
     *
     * @param extensions
     */
    public void setExtensionTypes(String[] extensions) {
        _extensions = extensions;
        _init();
    }

    public static String getModifiedDate(File file) {
        return getModifiedDate(file.lastModified());
    }

    public static String getModifiedDate(Long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss",
                LocaleUtil.getCurrent());
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(time);
    }

    /*************************** PROTECTED METHODS **************************/

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (_extensions != null) {
            _init();
        }
    }

    protected void _init() {
        _setInitialDirectory();
        _parseDirectoryPath();
        _loadFileList();
        _createAdapter();
        _initButtons();
        _initFileListView();
        _updateCurrentDirectoryTextView();
    }

    protected void _loadFileList() {
        MapView mv = MapView.getMapView();
        Context ctx = mv != null ? mv.getContext() : getContext();
        _fileList.clear();
        if (_path.exists() && _path.canRead()) {
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String fileName) {
                    File sel = new File(dir, fileName);
                    String ext = StringUtils.substringAfterLast(fileName, ".");
                    if (ext != null && sel.isFile()) {
                        return (_testExtension(ext.toLowerCase(LocaleUtil
                                .getCurrent())))
                                && sel.canRead();
                    }
                    return sel.canRead() && sel.isDirectory();
                }
            };
            File[] fList = _path.listFiles(filter);
            _directoryEmpty = false;

            if (fList != null) {
                for (File f : fList) {
                    Drawable icon = ctx
                            .getDrawable(R.drawable.import_file_icon);
                    int type = FileItem.FILE;
                    if (f.isDirectory()) {
                        icon = ctx.getDrawable(R.drawable.import_folder_icon);
                        type = FileItem.DIRECTORY;
                    } else {
                        MIMEType mt = ResourceFile.getMIMETypeForFile(
                                f.getName());
                        if (mt != null) {
                            Bitmap b = ATAKUtilities.getUriBitmap(mt.ICON_URI);
                            if (b != null)
                                icon = new BitmapDrawable(
                                        ctx.getResources(), b);
                        }
                    }
                    _fileList.add(createItem(f.getName(), icon, type));
                }
            }
            if (_fileList.size() == 0) {
                _directoryEmpty = true;
                _fileList.add(createItem(getContext().getString(
                        R.string.directory_empty), null,
                        FileItem.FILE));
            } else {
                Collections.sort(_fileList, new FileItemComparator());
            }
        }
    }

    protected FileItem createItem(String fileName, Drawable icon,
            Integer type) {
        return new FileItem(fileName, icon, type);
    }

    protected void _updateCurrentDirectoryTextView() {
        StringBuilder currDirString = new StringBuilder();
        for (String d : _pathDirsList) {
            currDirString.append(d).append(FILE_SEPARATOR);
        }

        if (_pathDirsList.size() == 0) {
            if (_up != null)
                _up.setEnabled(false);
        } else {
            if (_up != null)
                _up.setEnabled(true);
        }
        TextView tv = this
                .findViewById(R.id.importBrowserCurrentDirectory);
        if (tv != null)
            tv.setText(currDirString.toString());
    }

    protected boolean _testExtension(String ext) {
        for (String e : _extensions) {
            if (e.equals(WILDCARD) || e.equalsIgnoreCase(ext))
                return true;
        }
        return false;
    }

    protected void _scrollListToTop() {
        ListView lv = this
                .findViewById(R.id.importBrowserFileItemList);
        if (lv != null) {
            lv.setSelectionFromTop(0, 0);
        }
    }

    protected void _createAdapter() {
        _adapter = new FileItemAdapter(getContext(),
                R.layout.import_file_browser_fileitem,
                _fileList);
    }

    protected void _navigateUpOneDirectory() {
        _loadDirectoryUp();
        _loadFileList();
        _adapter.notifyDataSetChanged();
        _updateCurrentDirectoryTextView();

        _scrollListToTop();
    }

    /*************************** PRIVATE METHODS **************************/

    private void _setInitialDirectory() {
        // first check if user provided a directory to start in
        if (_userStartDirectory != null && _userStartDirectory.length() > 0) {
            File userDir = new File(_userStartDirectory);
            if (userDir.exists() && userDir.isDirectory()) {
                _path = userDir;
                return;
            }
        }

        // start in default directory
        File temp = new File(INITIAL_DIRECTORY);
        if (temp.isDirectory())
            _path = temp;
        if (_path == null) {
            if (Environment.getExternalStorageDirectory().isDirectory()
                    && Environment.getExternalStorageDirectory().canRead()) {
                _path = Environment.getExternalStorageDirectory();
            } else {
                _path = new File(FILE_SEPARATOR);
            }
        }
    }

    private void _parseDirectoryPath() {
        _pathDirsList.clear();
        String pathString = FileSystemUtils.prettyPrint(_path);
        String[] parts = pathString.split(Pattern.quote(FILE_SEPARATOR));
        Collections.addAll(_pathDirsList, parts);
    }

    private void _initButtons() {
        if (_up != null) {
            _up.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    _navigateUpOneDirectory();
                }
            });
        }

    }

    private void _loadDirectoryUp() {
        if (_pathDirsList.size() <= 1)
            return;
        String s = _pathDirsList.remove(_pathDirsList.size() - 1);
        int lastIndex = _path.toString().lastIndexOf(s);
        if (lastIndex > -1)
            _path = new File(_path.toString().substring(0, lastIndex));
        _fileList.clear();
    }

    protected void _initFileListView() {
        ListView list = this
                .findViewById(R.id.importBrowserFileItemList);
        if (list != null)
            list.setAdapter(_adapter);
    }

    protected void _returnFile(File file) {
        _retFile = file;
        if (_alert != null) {
            View doneBtn = _alert.getButton(DialogInterface.BUTTON_POSITIVE);
            if (doneBtn != null)
                doneBtn.callOnClick();
            else
                _alert.dismiss();
        }
    }

    /*************************** PROTECTED CLASSES **************************/

    public static class FileItem {
        public final String file;
        public final Drawable iconDr;
        public static final int FILE = 0x1;
        public static final int DIRECTORY = 0x1 << 1;
        public final int type;

        @Deprecated
        public final int icon;

        public FileItem(String file, Drawable icon, Integer type) {
            this.file = file;
            this.iconDr = icon;
            this.icon = 0;
            this.type = type;
        }

        @Deprecated
        public FileItem(String file, Integer icon, Integer type) {
            this.file = file;
            this.icon = icon;
            this.iconDr = null;
            this.type = type;
        }

        @Override
        public String toString() {
            return file;
        }

    }

    /*************************** PRIVATE CLASSES **************************/

    private static class FileItemComparator implements Comparator<FileItem>,
            Serializable {
        @Override
        public int compare(FileItem i0, FileItem i1) {
            if (i0.type == i1.type) {
                return i0.file.toLowerCase(LocaleUtil.getCurrent()).compareTo(
                        i1.file.toLowerCase(LocaleUtil.getCurrent()));
            } else if (i0.type == FileItem.FILE) {
                return -1;
            } else if (i0.type == FileItem.DIRECTORY) {
                return 1;
            }
            return i0.file.toLowerCase(LocaleUtil.getCurrent()).compareTo(
                    i1.file.toLowerCase(LocaleUtil.getCurrent()));
        }
    }

    /**
     * 
     */
    private class FileItemAdapter extends ArrayAdapter<FileItem> {

        final Context context;

        public FileItemAdapter(Context context, int resourceId,
                List<FileItem> items) {
            super(context, resourceId, items);
            this.context = context;
        }

        /* private view holder class */
        private class ViewHolder {
            Button icon;
            TextView txtFilename;
            TextView txtModifiedDate;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView,
                @NonNull ViewGroup parent) {
            ViewHolder holder = null;
            final FileItem fileItem = getItem(position);

            LayoutInflater mInflater = (LayoutInflater) context
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            if (convertView == null) {
                convertView = mInflater.inflate(
                        R.layout.import_file_browser_fileitem, null);
                holder = new ViewHolder();
                holder.txtFilename = convertView
                        .findViewById(R.id.importBrowserFileName);
                holder.txtModifiedDate = convertView
                        .findViewById(R.id.importBrowserModifiedDate);
                holder.icon = convertView
                        .findViewById(R.id.importBrowserIcon);
                convertView.setTag(holder);
            } else
                holder = (ViewHolder) convertView.getTag();

            File f = new File(_path, fileItem.file);
            holder.txtFilename.setText(f.getName());
            if (f.exists())
                holder.txtModifiedDate.setText(getModifiedDate(f));
            else
                holder.txtModifiedDate.setText("");

            Drawable drawable = fileItem.iconDr;
            if (drawable == null && fileItem.icon != 0) {
                try {
                    drawable = context.getDrawable(fileItem.icon);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to find icon for " + fileItem.file
                            + ": " + e.getMessage());
                }
            }

            holder.icon.setCompoundDrawablesWithIntrinsicBounds(null, drawable,
                    null,
                    null);
            if (fileItem.type == FileItem.FILE) {
                if (f.exists())
                    holder.icon.setText(MathUtils.GetLengthString(f.length()));
                else {
                    holder.icon.setText("");
                    holder.txtModifiedDate.setText("");
                }
            } else {
                FilenameFilter filter = new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String fileName) {
                        File sel = new File(dir, fileName);
                        String ext = StringUtils.substringAfterLast(fileName,
                                ".");
                        if (sel.isFile()) {
                            return (_testExtension(ext)) && sel.canRead();
                        }
                        return sel.canRead() && sel.isDirectory();
                    }
                };
                String[] children = f.list(filter);
                if (children == null || children.length < 1) {
                    holder.icon.setText("");
                } else {
                    holder.icon.setText(String.format(LocaleUtil.getCurrent(),
                            getContext()
                                    .getString(R.string.items),
                            children.length));
                }
            }

            // handle user clicks
            holder.icon.setClickable(false);
            convertView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    _currFile = fileItem.file;
                    File sel = new File(_path, _currFile);
                    if (sel.isDirectory()) {
                        if (sel.canRead()) {
                            _pathDirsList.add(_currFile);
                            _path = new File(sel + "");
                            _loadFileList();
                            _adapter.notifyDataSetChanged();
                            _updateCurrentDirectoryTextView();

                            _scrollListToTop();
                        }
                    } else {
                        if (!_directoryEmpty) {
                            _returnFile(sel);
                        }
                    }
                }
            });
            return convertView;
        }
    }

}
