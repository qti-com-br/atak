
package com.atakmap.android.features;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;

import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceResponse;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import com.atakmap.io.ZipVirtualFile;
import java.io.IOException;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import com.atakmap.app.R;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.items.MapItemDetailsView;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.dropdown.DropDown.OnStateListener;

import com.atakmap.coremap.log.Log;
import com.atakmap.map.layer.feature.DataSourceFeatureDataStore;

public class FeaturesDetailsDropdownReceiver extends DropDownReceiver implements
        OnStateListener {
    private final DataSourceFeatureDataStore spatialDb;
    private final MapItemDetailsView itemViewer;
    private final WebView htmlViewer;
    private TabHost tabs;
    private MapItem item;
    private double currWidth = HALF_WIDTH;
    private double currHeight = HALF_HEIGHT;

    public static final String TAG = "FeaturesDetailsDropdownReceiver";
    public static final String SHOW_DETAILS = "com.atakmap.android.features.SHOW_DETAILS";
    public static final String TOGGLE_VISIBILITY = "com.atakmap.android.features.TOGGLE_VISIBILITY";

    public FeaturesDetailsDropdownReceiver(MapView mapView,
            DataSourceFeatureDataStore spatialDb) {
        super(mapView);
        this.spatialDb = spatialDb;

        LayoutInflater inflater = (LayoutInflater) getMapView().getContext()
                .getSystemService(
                        Service.LAYOUT_INFLATER_SERVICE);

        this.htmlViewer = new WebView(mapView.getContext());
        this.htmlViewer.setVerticalScrollBarEnabled(true);
        this.htmlViewer.setHorizontalScrollBarEnabled(true);

        WebSettings webSettings = this.htmlViewer.getSettings();

        // do not enable per security guidelines
        //webSettings.setAllowFileAccessFromFileURLs(true);
        //webSettings.setAllowUniversalAccessFromFileURLs(true);

        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        this.htmlViewer.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view,
                    String url) {
                //Log.d(TAG, "shouldInterceptRequest: " + url);
                if (url.startsWith("file:///android_asset/")) {
                    if (item == null)
                        return null;

                    String fName = item.getMetaString("file", null);
                    if (fName != null) {
                        fName = FileSystemUtils
                                .sanitizeWithSpacesAndSlashes(fName);
                        url = url.replaceAll("file:///android_asset/", "");
                        Log.d(TAG, "zipFile: " + fName + " file: " + url);

                        try {
                            ZipVirtualFile vf = new ZipVirtualFile(fName, url);
                            return new WebResourceResponse("text/html",
                                    FileSystemUtils.UTF8_CHARSET, vf
                                            .openStream());
                        } catch (IOException ioe) {
                            Log.d(TAG, "error reading: " + fName + " entry: "
                                    + url, ioe);
                            return null;
                        } catch (Exception e) {
                            Log.d(TAG, "error reading: " + fName + " entry: "
                                    + url, e);
                            return null; // general exception occured
                        }
                    }
                }

                return null;
            }
        });

        this.itemViewer = (MapItemDetailsView) inflater.inflate(
                R.layout.map_item_view, null);
        this.itemViewer.setDropDown(this);
        this.itemViewer.setGalleryAsAttachments(false);
    }

    @Override
    public void disposeImpl() {
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

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        final MapItem item = findTarget(intent.getStringExtra("targetUID"));
        if (action == null)
            return;

        if (action.equals(SHOW_DETAILS)) {
            if (this.isVisible() && item == null) {
                this.closeDropDown();
            } else if (item != null) {

                this.item = item;
                setRetain(true);

                if (this.tabs == null) {
                    LayoutInflater inflater = (LayoutInflater) getMapView()
                            .getContext().getSystemService(
                                    Service.LAYOUT_INFLATER_SERVICE);

                    this.tabs = (TabHost) inflater.inflate(
                            R.layout.feature_details_tabhost, null);
                    this.tabs.setup();

                    TabHost.TabSpec detailsTab = this.tabs
                            .newTabSpec("details");
                    detailsTab.setIndicator("Details");
                    detailsTab.setContent(new TabContentFactory() {
                        @Override
                        public View createTabContent(String tag) {
                            return itemViewer;
                        }
                    });
                    TabHost.TabSpec metadataTab = this.tabs
                            .newTabSpec("metadata");
                    metadataTab.setIndicator("Metadata");
                    metadataTab.setContent(new TabContentFactory() {
                        @Override
                        public View createTabContent(String tag) {
                            return htmlViewer;
                        }
                    });

                    this.tabs.addTab(metadataTab);
                    this.tabs.addTab(detailsTab);
                }

                this.itemViewer.setItem(getMapView(), item);

                // cause subsequent calls to loadData not to fail - without this 
                // the web view would remain inconsistent on subsequent concurrent opens
                this.htmlViewer.loadUrl("about:blank");

                String html = item.getMetaString("html", "No Metadata");
                this.htmlViewer.loadDataWithBaseURL("file:///android_asset/",
                        html, "text/html", FileSystemUtils.UTF8_CHARSET, null);
                setSelected(item, "asset:/icons/outline.png");

                if (!this.isVisible()) {
                    showDropDown(this.tabs,
                            HALF_WIDTH,
                            FULL_HEIGHT,
                            FULL_WIDTH,
                            HALF_HEIGHT, this);
                }
            }
        } else if (action.equals(TOGGLE_VISIBILITY)) {
            if (item == null || !item.hasMetaValue("fid"))
                return;
            long fid = item.getMetaLong("fid", 0);
            this.spatialDb.setFeatureVisible(fid, false);
        }
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
        Log.d(TAG, "resizing width=" + width + " height=" + height);
        currWidth = width;
        currHeight = height;
    }

    @Override
    public void onDropDownClose() {
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownVisible(boolean v) {
    }

    private MapItem findTarget(final String targetUID) {
        if (targetUID != null) {
            MapGroup rootGroup = getMapView().getRootGroup();
            return rootGroup.deepFindItem("uid", targetUID);
        } else {
            return null;
        }
    }
}
