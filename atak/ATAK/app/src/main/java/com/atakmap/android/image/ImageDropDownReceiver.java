
package com.atakmap.android.image;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.atakmap.android.hashtags.HashtagManager;
import com.atakmap.android.hashtags.attachments.AttachmentContent;
import com.atakmap.android.hashtags.util.HashtagUtils;
import com.atakmap.android.hashtags.view.HashtagEditText;
import com.atakmap.android.image.quickpic.QuickPicReceiver;
import com.atakmap.android.imagecapture.CapturePrefs;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.missionpackage.file.MissionPackageConfiguration;
import com.atakmap.android.missionpackage.file.MissionPackageContent;
import com.atakmap.android.user.PlacePointTool;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.util.FileProviderHelper;
import com.atakmap.android.util.SimpleItemSelectedListener;
import com.atakmap.android.attachment.DeleteAfterSendCallback;
import com.atakmap.android.contact.Contact;
import com.atakmap.android.contact.ContactPresenceDropdown;
import com.atakmap.android.contact.Contacts;
import com.atakmap.android.image.nitf.NITFHelper;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.missionpackage.MissionPackageMapComponent;
import com.atakmap.android.missionpackage.api.MissionPackageApi;
import com.atakmap.android.missionpackage.file.MissionPackageManifest;
import com.atakmap.android.missionpackage.file.NameValuePair;
import com.atakmap.android.util.AttachmentManager;
import com.atakmap.android.video.manager.VideoFileWatcher;
import com.atakmap.coremap.locale.LocaleUtil;

import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;
import org.gdal.gdal.Dataset;

import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.menu.MapMenuReceiver;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.atakmap.filesystem.HashingUtils;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;
import org.gdal.gdal.gdal;

public class ImageDropDownReceiver
        extends DropDownReceiver
        implements OnStateListener {
    //==================================
    //
    //  PUBLIC INTERFACE
    //
    //==================================

    //==================================
    //  PUBLIC CONSTANTS
    //==================================

    public static final String TAG = "ImageDropDownReceiver";
    public static final String IMAGE_DISPLAY = "com.atakmap.maps.images.DISPLAY";
    public static final String IMAGE_REFRESH = "com.atakmap.maps.images.REFRESH";
    public static final String IMAGE_UPDATE = "com.atakmap.maps.images.FILE_UPDATE";
    public static final String IMAGE_SELECT_RESOLUTION = "com.atakmap.maps.images.SELECT_RESOLUTION";

    public static final FilenameFilter ImageFileFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir,
                String filename) {
            filename = filename.toLowerCase(LocaleUtil.getCurrent());

            return filename.endsWith(".jpg")
                    || filename.endsWith(".jpeg")
                    || filename.endsWith(".png")
                    || filename.endsWith(".bmp")
                    || filename.endsWith(".gif")
                    || filename.endsWith(".lnk")
                    || filename.endsWith(".ntf")
                    || filename.endsWith(".nitf")
                    || filename.endsWith(".nsf");
        }
    };

    public static final FilenameFilter VideoFileFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            if (filename == null)
                return false;
            // 'dir' may be null - can't perform an /atak/tools/video check
            return VideoFileWatcher.VIDEO_FILTER.accept(new File(filename))
                    && !filename.toLowerCase(LocaleUtil.getCurrent())
                            .endsWith(".xml");
        }
    };

    //==================================
    //  PUBLIC METHODS
    //==================================

    public ImageDropDownReceiver(MapView mapView) {
        super(mapView);
        _mapView = mapView;
        _context = mapView.getContext();
    }

    /**
     * Get unique filename for an attachment to the specified UID/MapItem Create
     *  paren directories
     * if they does exist
     * 
     * @param uid
     * @param ext file extension not including the '.'
     * @return
     */
    public static File createAndGetPathToImageFromUID(String uid,
            String ext) {
        final String[] possible = {
                "", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k",
                "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w",
                "x", "y", "z"
        };

        File result = null;

        try {
            final String dirPath = FileSystemUtils.getItem("attachments")
                    .getPath()
                    + File.separator
                    + URLEncoder.encode(uid, FileSystemUtils.UTF8_CHARSET);
            final File dir = new File(dirPath);

            Log.d(TAG, "creating an attachment directory: " + dirPath);
            if (dir.isDirectory() || !dir.exists() && dir.mkdirs()) {
                final String name = getDateTimeString();

                for (int i = 0; result == null && i < possible.length; ++i) {
                    File file = new File(dir, name + possible[i] + "." + ext);

                    if (!file.exists()) {
                        result = file;
                    }
                }

                // Too many images attached, overwrite one
                if (result == null) {
                    result = new File(dir, name + "." + ext);
                }
            } else {
                Log.d(TAG, "could not wrap: " + dirPath);
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "createAndGetPathToImageFromUID", e);
        }

        return result;
    }

    //==================================
    //  DropDownReceiver INTERFACE
    //==================================

    @Override
    public void disposeImpl() {
    }

    //==================================
    //  BroadcastReceiver INTERFACE
    //==================================

    @Override
    public void onReceive(final Context ignoreCtx,
            Intent intent) {
        try {
            Log.d(TAG, "received intent: " + intent.getAction());

            final String action = intent.getAction();
            final String newUID = intent.getStringExtra("uid");

            if (action.equals(IMAGE_REFRESH) && newUID != null
                    && newUID.equals(_uid)) {
                // This intent is received from the imageMarkupTool to refresh
                // the image view to reflect the changes in the image.  Only
                // file URIs are currently supported.

                File imageFile = null;

                if (ic != null) {
                    String imageURI = ic.getCurrentImageURI();
                    if (imageURI != null && imageURI.startsWith("file://"))
                        imageFile = new File(Uri.parse(imageURI).getPath());
                }

                Uri imageURI = intent.hasExtra("imageURI")
                        ? Uri.parse(intent.getStringExtra("imageURI"))
                        : null;

                if (imageURI != null && imageURI.getScheme().equals("file")) {
                    String f = imageURI.getPath();
                    if (FileSystemUtils.isFile(f)) {
                        File newFile = new File(f);

                        if (ImageContainer.NITF_FilenameFilter.accept(
                                newFile.getParentFile(), newFile.getName())
                                && imageFile != null
                                && ImageContainer.JPEG_FilenameFilter
                                        .accept(imageFile.getParentFile(),
                                                imageFile.getName()))
                            FileSystemUtils.delete(imageFile);
                    }

                    String title = intent.getStringExtra("title");
                    boolean noFunctionality = intent.getBooleanExtra(
                            "noFunctionality", false);
                    inflateDropDown(_context, imageURI.toString(), null,
                            noFunctionality, title, null);
                } else if (_uid != null) {
                    inflateDropDown(_context, intent);
                }
            } else if (action.equals(IMAGE_DISPLAY)
                    || action.equals(IMAGE_UPDATE)
                            && newUID != null
                            && newUID.equals(_uid)) {
                _uid = newUID;

                //
                // If the imageURIs extra is supplied, the imageURI extra
                // indicates which of the URIs is to be displayed.  The imageURI
                // extra may be supplied without imageURIs if only one image is
                // to be displayed.  All URIs must share the same scheme.
                //
                String[] imageURIs = intent.getStringArrayExtra("imageURIs");
                String imageURI = intent.getStringExtra("imageURI");
                String title = intent.getStringExtra("title");
                String[] titles = intent.getStringArrayExtra("titles");

                boolean noFunctionality = intent.getBooleanExtra(
                        "noFunctionality", false);
                String scheme = imageURI != null
                        ? Uri.parse(imageURI).getScheme()
                        : null;

                if (imageURIs != null) {
                    if (scheme != null) {
                        boolean foundURI = false;

                        for (String uri : imageURIs) {
                            if (!scheme.equals(Uri.parse(uri).getScheme())) {
                                Log.e(TAG, "Non-homogeneous URI schemes");
                                break;
                            }
                            if (uri.equals(imageURI)) {
                                foundURI = true;
                            }
                        }
                        if (!foundURI) {
                            Log.e(TAG, "imageURI not found in imageURIs");
                        }
                    } else if (imageURI == null) {
                        Log.e(TAG,
                                "imageURI required when imageURIs is supplied");
                    } else {
                        Log.e(TAG, "Invalid URI scheme: " + imageURI);
                    }
                }

                //
                // Attempt to navigate to the image if it's in the current set.
                //
                if (isVisible() && ic != null && (scheme != null
                        && ic.setCurrentImageByURI(imageURI)
                        || _uid != null
                                && ic.setCurrentImageByUID(_uid))) {
                    resetSelection(ic.getCurrentImageUID());
                } else {
                    if (scheme != null) {
                        inflateDropDown(_context, imageURI, imageURIs,
                                noFunctionality, title, titles);
                    } else if (_uid != null) {
                        // show all images for UID
                        inflateDropDown(_context, intent);
                    }
                }
            } else if (action.equals(IMAGE_SELECT_RESOLUTION)) {
                String[] contactUIDs = intent.getStringArrayExtra("sendTo");
                String f = intent.getStringExtra("filepath");
                if (FileSystemUtils.isEmpty(contactUIDs)) {
                    // Must select contacts before prompting for resolution
                    Intent sendList = new Intent(
                            ContactPresenceDropdown.SEND_LIST)
                                    .putExtra("uid", newUID)
                                    .putExtra("filepath", f)
                                    .putExtra("sendCallback",
                                            IMAGE_SELECT_RESOLUTION)
                                    .putExtra("disableBroadcast", true);
                    AtakBroadcast.getInstance().sendBroadcast(sendList);
                    return;
                }
                //Prompt for selection of resolution.
                _uid = newUID;
                if (FileSystemUtils.isFile(f))
                    decideImageType(new File(f), contactUIDs);

            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to process " + intent.getAction() + " intent",
                    e);
        }
    }

    @Override
    protected void onStateRequested(int state) {
        if (state == DROPDOWN_STATE_FULLSCREEN) {
            if (!isPortrait()) {
                if (Double.compare(currWidth, HALF_WIDTH) == 0) {
                    resize(FULL_WIDTH - HANDLE_THICKNESS_LANDSCAPE,
                            FULL_HEIGHT);
                }
            } else {
                if (Double.compare(currHeight, HALF_HEIGHT) == 0) {
                    resize(FULL_WIDTH, FULL_HEIGHT - HANDLE_THICKNESS_PORTRAIT);
                }
            }
        } else if (state == DROPDOWN_STATE_NORMAL) {
            if (!isPortrait()) {
                resize(HALF_WIDTH, FULL_HEIGHT);
            } else {
                resize(FULL_WIDTH, HALF_HEIGHT);
            }
        }
    }

    //==================================
    //  OnStateListener INTERFACE
    //==================================

    @Override
    public void onDropDownClose() {
        if (ic != null) {
            ic.dispose();
            ic = null;
        }
        if (icPending != null) {
            // In case drop-down is re-opened
            ImageContainer newIC = icPending;
            icPending = null;
            if (newIC instanceof ImageFileContainer)
                inflateDropDown(_context, (ImageFileContainer) newIC, false);
            else if (newIC instanceof ImageBlobContainer)
                inflateDropDown(_context, (ImageBlobContainer) newIC);
        } else {
            // So refresh doesn't open the drop-down again
            _uid = null;
        }
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
        Log.d(TAG, "resizing width=" + width + " height=" + height);
        currWidth = width;
        currHeight = height;
    }

    @Override
    public void onDropDownVisible(boolean v) {
    }

    //==================================
    //
    //  PRIVATE IMPLEMENTATION
    //
    //==================================

    /**
     * Select new image size for sending over network
     * @param file Image file
     * @param contactUIDs Contact UIDs to send file to
     */
    private void confirmImageResize(final File file,
            final String[] contactUIDs) {
        final String[] sizeValues = {
                _context.getString(R.string.original_size),
                _context.getString(R.string.large_size),
                _context.getString(R.string.medium_size),
                _context.getString(R.string.thumbnail_size)
        };
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(_context,
                android.R.layout.simple_spinner_item,
                sizeValues);

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);

        // Setup spinner for image size choices (use preference by default)
        @SuppressLint("InflateParams")
        final View view = LayoutInflater.from(_context)
                .inflate(R.layout.image_resize_dialog,
                        null);
        final Spinner spinner = view.findViewById(R.id.image_sizes);

        spinner.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View v,
                    int pos, long id) {
                if (v instanceof TextView)
                    ((TextView) v).setTextColor(Color.WHITE);
            }
        });

        spinner.setAdapter(adapter);
        spinner.setSelection(0);

        // Present dialog
        new AlertDialog.Builder(_context)
                .setTitle(R.string.send_image)
                .setView(view)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int id) {
                                File img = resizeImageFile(file,
                                        spinner.getSelectedItemPosition(),
                                        _context);
                                if (!sendImage(img, contactUIDs))
                                    Toast.makeText(_context,
                                            R.string.failed_to_send_file,
                                            Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Decide image type before sending file
     * @param file File to send
     * @param contactUIDs Contact UIDs to send file to
     */
    private void decideImageType(final File file, final String[] contactUIDs) {
        if (ImageContainer.NITF_FilenameFilter.accept(file.getParentFile(),
                file.getName())) {
            final String[] sizeValues = {
                    "NITF (Editable)",
                    "JPEG (Uneditable)"
            };
            final ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    _context,
                    android.R.layout.simple_spinner_item,
                    sizeValues);

            adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item);

            // Setup spinner for image size choices (use preference by default)
            @SuppressLint("InflateParams")
            final View view = LayoutInflater.from(_context)
                    .inflate(R.layout.image_type_select_dialog,
                            null);
            final Spinner spinner = view
                    .findViewById(R.id.image_types);
            spinner.setOnItemSelectedListener(new SimpleItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View v,
                        int pos, long id) {
                    if (v instanceof TextView)
                        ((TextView) v).setTextColor(Color.WHITE);
                }
            });

            spinner.setAdapter(adapter);
            spinner.setSelection(0);

            // Present dialog
            new AlertDialog.Builder(_context)
                    .setTitle(R.string.send_image_as)
                    .setView(view)
                    .setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    if (spinner.getSelectedItemPosition() == 1)
                                        confirmImageResize(file, contactUIDs);
                                    else
                                        sendImage(file, contactUIDs);
                                    dialog.dismiss();
                                }
                            })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else
            confirmImageResize(file, contactUIDs);
    }

    /**
     * Returns the date string in a format that mimics what Android does when
     * saving a new file.
     */
    private static String getDateTimeString() {
        return (new SimpleDateFormat("yyyyMMdd_HHmmss", LocaleUtil.getCurrent())
                .format(CoordinatedTime.currentDate()));
    }

    private void inflateDropDown(final Context context,
            ImageBlobContainer ibc) {
        if (ic != null) {
            icPending = ibc;
            closeDropDown();
            return;
        }
        ic = ibc;

        View view = ic.getView();

        prepareView(view);

        final View sendButton = view.findViewById(R.id.cotInfoSendButton);
        final View editImageButton = view.findViewById(R.id.editImage);
        final View caption = view.findViewById(R.id.image_caption);

        view.post(new Runnable() {
            @Override
            public void run() {
                sendButton.setVisibility(View.GONE);
                editImageButton.setVisibility(View.GONE);
                caption.setVisibility(View.GONE);
            }
        });
        if (_uid != null) {
            setSelected(_uid);
        }

        setRetain(false);
        showDropDown(view,
                HALF_WIDTH, FULL_HEIGHT,
                FULL_WIDTH, HALF_HEIGHT,
                this);
    }

    private void inflateDropDown(final Context context,
            final ImageFileContainer ifc, boolean noFunctionality) {
        if (ic != null) {
            icPending = ifc;
            closeDropDown();
            return;
        }
        ic = ifc;

        View view = ic.getView();

        prepareView(view);

        final ImageButton sendButton = view
                .findViewById(R.id.cotInfoSendButton);

        sendButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final File imageFile = ifc.getCurrentImageFile();

                if (imageFile != null) {
                    // Keep drop-down on stack
                    setRetain(true);
                    // Bring up image resize confirmation dialog
                    //decideImageType(context, imageFile);
                    openSendList(imageFile);
                }
            }
        });

        final ImageButton editImageButton = view
                .findViewById(R.id.editImage);

        editImageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri imageUri = Uri.parse(ic.getCurrentImageURI());
                Log.d(TAG, "ImageUri: " + imageUri);

                String team = PreferenceManager
                        .getDefaultSharedPreferences(context)
                        .getString("locationTeam", "Cyan");
                String scheme = imageUri.getScheme();
                Intent editIntent = new Intent("com.atakmap.maps.images.EDIT")
                        .putExtra("uid", _uid)
                        .putExtra("team", team)
                        .putExtra("callsign", _mapView.getDeviceCallsign());
                if (scheme != null && scheme.startsWith("file")) {
                    FileProviderHelper.setDataAndType(
                            context, editIntent, new File(imageUri.getPath()),
                            "image/*");
                } else {
                    editIntent.setDataAndType(imageUri, "image/*");
                }

                List<ResolveInfo> apps = context.getPackageManager()
                        .queryIntentActivities(editIntent, 0);
                if (apps == null || apps.isEmpty())
                    Toast.makeText(_context, R.string.no_image_editors,
                            Toast.LENGTH_SHORT).show();
                else if (apps.size() == 1)
                    context.startActivity(editIntent);
                else
                    context.startActivity(Intent.createChooser(editIntent,
                            context.getString(R.string.select_image_editor)));
            }
        });

        final EditText caption = view
                .findViewById(R.id.image_caption);

        caption.setFocusableInTouchMode(true);
        caption.setFocusable(false);

        caption.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final HashtagEditText captionInput = new HashtagEditText(
                        context);
                captionInput.setMixedInput(true);
                captionInput.setText(caption.getText().toString());
                captionInput.setTextSize(18);
                AlertDialog.Builder b = new AlertDialog.Builder(context);
                if (_context.getResources().getBoolean(R.bool.isTablet)
                        || CapturePrefs.inPortraitMode())
                    b.setTitle(R.string.enter_image_caption);
                b.setCancelable(false);
                b.setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int id) {
                                // Update caption text
                                final String newCaption = captionInput
                                        .getTextString();
                                caption.setText(newCaption);
                                // Update exif
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateImageDescription(newCaption);
                                    }
                                }, TAG + "-UpdateCaption").start();
                            }
                        });
                b.setNegativeButton(R.string.cancel, null);
                b.setView(captionInput);
                b.show();
                captionInput.post(new Runnable() {
                    @Override
                    public void run() {
                        // Bring up keyboard
                        captionInput.requestFocusFromTouch();
                        captionInput
                                .setSelection(captionInput.getText().length());
                        InputMethodManager imm = (InputMethodManager) _context
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(captionInput,
                                InputMethodManager.SHOW_IMPLICIT);
                    }
                });
            }
        });

        // check if ImageMarkUpTool is installed
        int tmpVis = View.GONE;

        try {
            context.getPackageManager()
                    .getPackageInfo("com.par.imagemarkup",
                            PackageManager.GET_ACTIVITIES);
            tmpVis = View.VISIBLE;
            Log.d(TAG, "ImageMarkUpTool found");
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "ImageMarkUpTool not found");
        }

        final int editVisibility = tmpVis;

        view.post(new Runnable() {
            @Override
            public void run() {
                editImageButton.setVisibility(editVisibility);
            }
        });

        //If the intent specifies no functionality just display the image for viewing
        if (noFunctionality) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    sendButton.setVisibility(View.INVISIBLE);
                    editImageButton.setVisibility(View.INVISIBLE);
                }
            });
        }

        if (_uid != null) {
            setSelected(_uid);
        }

        setRetain(false);
        showDropDown(view,
                HALF_WIDTH, FULL_HEIGHT,
                FULL_WIDTH, HALF_HEIGHT,
                this);
    }

    /**
     * This method inflates the image container view for an intent that doesn't
     * supply imageURI.
     *
     * @param context
     * @param intent
     */
    private void inflateDropDown(final Context context,
            Intent intent) {
        List<File> files = AttachmentManager.getAttachments(_uid);
        for (int i = 0; i < files.size(); i++) {
            File f = files.get(i);
            if (!ImageFileFilter.accept(f.getParentFile(), f.getName()))
                files.remove(i--);
        }
        final boolean sortByTime = FileSystemUtils.isEquals(
                intent.getStringExtra("sort"), "time");
        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                if (sortByTime)
                    return Long.compare(f2.lastModified(), f1.lastModified());
                else
                    return f1.getName().compareTo(f2.getName());
            }
        });
        if (!files.isEmpty()) {
            String[] imageURIs = new String[files.size()];

            for (int i = 0; i < files.size(); ++i) {
                imageURIs[i] = Uri.fromFile(files.get(i)).toString();
            }

            String title = intent.getStringExtra("title");
            String[] titles = intent.getStringArrayExtra("titles");
            boolean noFunctionality = intent.getBooleanExtra(
                    "noFunctionality", false);
            inflateDropDown(context, imageURIs[0], imageURIs,
                    noFunctionality, title, titles);
        } else {
            Toast.makeText(context,
                    context.getString(R.string.no_pic),
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void inflateDropDown(final Context context,
            String imageURI,
            String[] imageURIs,
            boolean noFunctionality,
            String title,
            String[] titles) {
        String scheme = Uri.parse(imageURI).getScheme();

        //if a single title, use it
        if (FileSystemUtils.isEmpty(titles)
                && !FileSystemUtils.isEmpty(title)) {
            titles = new String[] {
                    title
            };
        }

        if ("file".equals(scheme)) {
            inflateDropDown(context,
                    new ImageFileContainer(context, _mapView,
                            _uid, imageURI, imageURIs, titles),
                    noFunctionality);
        } else if ("sqlite".equals(scheme)) {
            inflateDropDown(context,
                    new ImageBlobContainer(context, _mapView,
                            _uid, imageURI, imageURIs, titles));
        }
    }

    private void prepareView(View view) {
        // Navigation controls
        view.findViewById(R.id.nav_prev).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (ic != null && ic.prevImage())
                            resetSelection(ic.getCurrentImageUID());
                    }
                });
        view.findViewById(R.id.nav_next).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (ic != null && ic.nextImage())
                            resetSelection(ic.getCurrentImageUID());
                    }
                });
    }

    private void resetSelection(String imageUID) {
        setSelected(imageUID);
        AtakBroadcast.getInstance()
                .sendBroadcast(
                        new Intent("com.atakmap.android.maps.ZOOM_TO_LAYER")
                                .putExtra("uid", imageUID)
                                .putExtra("noZoom", true));
        AtakBroadcast.getInstance()
                .sendBroadcast(new Intent(MapMenuReceiver.HIDE_MENU));
    }

    private File resizeImageFile(File file,
            int size,
            Context context) {
        String filePath = file.getAbsolutePath();
        String fileName = file.getName();

        // Remove extension and resized suffix
        fileName = fileName.substring(0, fileName.lastIndexOf('.'));
        if (fileName.endsWith("_l")
                || fileName.endsWith("_m")
                || fileName.endsWith("_t")) {
            fileName = fileName.substring(0, fileName.lastIndexOf("_"));
        }

        boolean isNITF = ImageContainer.NITF_FilenameFilter.accept(
                file.getParentFile(), file.getName());

        // Extract size from size preference index
        int width, height;

        switch (size) {
            case 1:
                width = 1632;
                height = 1224;
                fileName += "_l.jpg";
                break;
            case 2:
                width = 1024;
                height = 768;
                fileName += "_m.jpg";
                break;
            case 3:
                width = 320;
                height = 240;
                fileName += "_t.jpg";
                break;
            default:
                // Original size
                if (!isNITF)
                    return file;
                fileName += ".jpg";
                size = width = height = 0;
                break;
        }

        Bitmap bmp = null;
        TiffOutputSet tos = null;

        if (isNITF) {
            bmp = ImageContainer.readNITF(file, context.getResources());
            if (bmp != null) {
                int inWidth = bmp.getWidth();
                int inHeight = bmp.getHeight();
                int outWidth = inWidth;
                int outHeight = inHeight;
                if (size > 0) {
                    if (inWidth > inHeight) {
                        outWidth = width;
                        outHeight = (inHeight * width) / inWidth;
                    } else {
                        outHeight = height;
                        outWidth = (inWidth * height) / inHeight;
                    }
                    bmp = Bitmap.createScaledBitmap(bmp, outWidth, outHeight,
                            false);
                }
                // Convert NITF metadata to EXIF
                Dataset nitf = gdal.Open(file.toString());
                if (nitf != null) {
                    tos = NITFHelper.getExifOutput(nitf, outWidth, outHeight);
                    nitf.delete();
                }
            }
        } else {
            // Get image dimensions
            BitmapFactory.Options opts = new BitmapFactory.Options();

            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, opts);

            // Is resizing necessary?
            if (opts.outWidth > width || opts.outHeight > height) {
                opts.inSampleSize = Math.round(Math.min((float) opts.outWidth
                        / width,
                        (float) opts.outHeight / height));
                opts.inJustDecodeBounds = false;

                // Decode subsampled result
                bmp = BitmapFactory.decodeFile(filePath, opts);

                // Copy EXIF data
                tos = ExifHelper
                        .getExifOutput(ExifHelper.getExifMetadata(file));
            }
        }

        File result = file;

        if (bmp != null) {
            FileOutputStream fos = null;

            // Store downscaled in ATAK temp directory (cleared on shutdown)
            File atakTmp = FileSystemUtils.getItemOnSameRoot(file, "tmp");
            if (!atakTmp.exists() && !atakTmp.mkdirs()) {
                Log.w(TAG, "Failed to create ATAK temp dir: " + atakTmp);
                // Fallback to system temp directory (may not be readable/writable)
                result = new File(_context.getCacheDir(),
                        fileName);
            } else
                result = new File(atakTmp, fileName);
            try {
                int jpeg_quality = 90;

                fos = new FileOutputStream(result);
                bmp.compress(CompressFormat.JPEG, jpeg_quality, fos);
                fos.flush();
                fos.close();
                fos = null;
                // Save copied EXIF output set to new image
                if (tos != null)
                    ExifHelper.saveExifOutput(tos, result);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save resized image: ", e);
                result = null;
            } finally {
                try {
                    if (fos != null) {
                        fos.close();
                    }
                } catch (Exception ignored) {
                }
            }
            bmp.recycle();
        }

        return result;
    }

    /**
     * Open the contacts list for selecting which contacts to send the attachment to
     * @param file Attachment file
     */
    private void openSendList(File file) {
        if (!MissionPackageMapComponent.getInstance().checkFileSharingEnabled())
            return;
        Intent sendList = new Intent(ContactPresenceDropdown.SEND_LIST)
                .putExtra("uid", _uid)
                .putExtra("filepath", file.getAbsolutePath())
                .putExtra("sendCallback", IMAGE_SELECT_RESOLUTION)
                .putExtra("disableBroadcast", true);
        AtakBroadcast.getInstance().sendBroadcast(sendList);
    }

    /**
     * Compress image in mission package and send to contacts
     * @param file Resized image file
     * @param contactUIDs Contact UIDs to send to
     */
    private boolean sendImage(File file, String[] contactUIDs) {
        if (file == null)
            return false;

        // Find attached marker
        boolean tempMarker = false;
        MapItem mi = _mapView.getRootGroup().deepFindUID(_uid);
        if (mi == null) {
            // Need a valid SHA-256 hash for the temp UID
            String sha256 = HashingUtils.sha256sum(file.getAbsolutePath());
            if (FileSystemUtils.isEmpty(sha256))
                return false;

            // If there's none then make create a temporary quick-pic marker
            TiffImageMetadata exif = ExifHelper.getExifMetadata(file);
            GeoPoint gp = ExifHelper.getLocation(exif);
            if (!gp.isValid()) {
                Marker self = ATAKUtilities.findSelf(_mapView);
                if (self != null && self.getGroup() != null)
                    gp = self.getPoint();
                else
                    gp = _mapView.getPoint().get();
            }
            mi = new PlacePointTool.MarkerCreator(gp)
                    .setUid(UUID.nameUUIDFromBytes(sha256.getBytes())
                            .toString())
                    .setCallsign(file.getName())
                    .setType(QuickPicReceiver.QUICK_PIC_IMAGE_TYPE)
                    .showCotDetails(false)
                    .placePoint();
            mi.setVisible(false);
            tempMarker = true;
        }

        // Get callsign and UID
        String callsign = mi.getMetaString("callsign", null);
        String uid = mi.getUID();

        MissionPackageManifest manifest = MissionPackageApi
                .CreateTempManifest(file.getName(), true, true, null);
        manifest.addMapItem(uid);
        manifest.addFile(file, uid);

        if (tempMarker) {
            // XXX - Would be nice if addMapItem returned the content we just added
            // For now we have to do it this way
            List<MissionPackageContent> contents = manifest.getMapItems();
            for (MissionPackageContent mc : contents) {
                NameValuePair nvp = mc
                        .getParameter(MissionPackageContent.PARAMETER_UID);
                if (nvp != null && uid.equals(nvp.getValue())) {
                    // Delete this marker after the MP is sent
                    mc.setParameter(new NameValuePair(
                            MissionPackageContent.PARAMETER_DeleteWithPackage,
                            "true"));
                    break;
                }
            }
        }

        // Include extra metadata if available
        if (ImageContainer.NITF_FilenameFilter.accept(
                file.getParentFile(), file.getName())) {
            File nitfXml = new File(file.getParent(), file.getName()
                    + ".aux.xml");
            if (nitfXml.exists())
                manifest.addFile(nitfXml, uid);
        }

        //set parameters to get file attached to the UID/map item
        MissionPackageConfiguration config = manifest.getConfiguration();
        config.setParameter(new NameValuePair("callsign", callsign));
        config.setParameter(new NameValuePair("uid", uid));

        // Find contacts by UID
        List<Contact> contacts = new ArrayList<>();
        if (contactUIDs != null && contactUIDs.length > 0)
            contacts.addAll(Arrays.asList(Contacts.getInstance()
                    .getIndividualContactsByUuid(Arrays.asList(contactUIDs))));

        // Send packaged image to contacts w/ scope set to "private"
        return MissionPackageApi.Send(_context, manifest,
                DeleteAfterSendCallback.class,
                contacts.toArray(new Contact[0]));
    }

    private void setSelected(String imageUID) {
        setSelected(_mapView.getRootGroup().deepFindUID(imageUID),
                "asset:/icons/outline.png");
    }

    //
    // This is run in a separate thread.
    //
    private void updateImageDescription(String newDesc) {
        if (ic == null)
            return;
        File imageFile = new File(Uri.parse(ic.getCurrentImageURI()).getPath());
        AttachmentContent content = new AttachmentContent(_mapView, imageFile);
        content.readHashtags();
        File dir = imageFile.getParentFile();
        String name = imageFile.getName();
        if (ImageContainer.JPEG_FilenameFilter.accept(dir, name)) {
            // Update EXIF image description
            TiffImageMetadata exif = ExifHelper.getExifMetadata(imageFile);
            if (exif != null) {
                TiffOutputSet tos = ExifHelper.getExifOutput(exif);
                if (ExifHelper.updateField(tos,
                        TiffConstants.TIFF_TAG_IMAGE_DESCRIPTION, newDesc))
                    ExifHelper.saveExifOutput(tos, imageFile);
            }
        } else if (ImageContainer.NITF_FilenameFilter.accept(dir, name)) {
            // Update NITF file title
            Dataset nitf = gdal.Open(imageFile.toString());
            if (nitf != null) {
                NITFHelper.setTitle(nitf, newDesc);
                nitf.delete();
            }
        } else if (name.endsWith(".png")) {
            ExifHelper.setPNGDescription(imageFile, newDesc);
        }
        HashtagManager.getInstance().updateContent(content,
                HashtagUtils.extractTags(newDesc));
    }

    //==================================
    //  PRIVATE REPRESENTATION
    //==================================

    private String _uid;
    private ImageContainer ic;
    private ImageContainer icPending;
    private final MapView _mapView;
    private final Context _context;

    private double currWidth = HALF_WIDTH;
    private double currHeight = HALF_HEIGHT;

}
