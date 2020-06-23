
package com.atakmap.android.importfiles.http;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.atakmap.net.AtakAuthenticationHandlerHTTP;
import javax.net.ssl.TrustManager;
import com.atakmap.net.CertificateManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.io.InputStream;
import java.io.FileOutputStream;

import com.atakmap.android.http.rest.NetworkOperationManager;
import com.atakmap.android.http.rest.operation.GetFilesOperation;
import com.atakmap.android.http.rest.operation.NetworkOperation;
import com.atakmap.android.http.rest.request.GetFileRequest;
import com.atakmap.android.importfiles.resource.RemoteResource;
import com.atakmap.android.importfiles.task.ImportNetworkLinksTask;
import com.atakmap.android.importfiles.task.KMLNetworkLinkRefresh;
import com.atakmap.android.util.NotificationUtil;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.spatial.file.SpatialDbContentSource;
import com.atakmap.spatial.kml.FeatureHandler;
import com.atakmap.spatial.kml.KMLMatcher;
import com.atakmap.spatial.kml.KMLUtil;
import com.ekito.simpleKML.model.Kml;
import com.ekito.simpleKML.model.NetworkLink;
import com.foxykeep.datadroid.requestmanager.Request;
import com.foxykeep.datadroid.requestmanager.RequestManager;
import com.foxykeep.datadroid.requestmanager.RequestManager.RequestListener;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * HTTP download support for remote KML import. Supports nested NetworkLinks Leverages Android
 * Service to offload async HTTP requests
 * 
 * 
 */
public class KMLNetworkLinkDownloader implements RequestListener {

    protected static final String TAG = "KMLNetworkLinkDownloader";

    private int curNotificationId = 84000;

    /**
     * Core class members
     */
    private Context _context;

    private KMLNetworkLinkRefresh _linkRefresh;

    /**
     * Cache for performance
     */
    private Serializer _serializer;

    /**
     * ctor
     * 
     * @param context
     */
    public KMLNetworkLinkDownloader(Context context) {
        if (!(context instanceof Activity)) {
            throw new IllegalArgumentException(
                    "KMLNetworkLinkRefresh requires Activity context");
        }

        _context = context;
        _serializer = new Persister(new KMLMatcher());
    }

    public Context getContext() {
        return _context;
    }

    /**
     * Keep for legacy behavior.
     */
    public synchronized KMLNetworkLinkRefresh getLinkRefresh() {
        if (_linkRefresh == null)
            _linkRefresh = new KMLNetworkLinkRefresh((Activity) _context);
        return _linkRefresh;
    }

    public int getNotificationId() {
        return curNotificationId;
    }

    /**
     * Download specified file asynchronously
     * 
     * @param resource the remote resource to download.
     */
    public void download(RemoteResource resource) {

        if (FileSystemUtils.isEmpty(resource.getUrl())) {
            Log.e(TAG, "Unable to determine URL");
            NotificationUtil
                    .getInstance()
                    .postNotification(
                            SpatialDbContentSource.getNotificationId(),
                            R.drawable.ic_network_error_notification_icon,
                            NotificationUtil.RED,
                            _context.getString(
                                    R.string.importmgr_remote_kml_download_failed),
                            _context.getString(
                                    R.string.importmgr_unable_to_determine_url),
                            _context.getString(
                                    R.string.importmgr_unable_to_determine_url));
            return;
        }

        if (FileSystemUtils.isEmpty(resource.getName())) {
            Log.e(TAG, "Unable to determine KML filename label");
            NotificationUtil
                    .getInstance()
                    .postNotification(
                            SpatialDbContentSource.getNotificationId(),
                            R.drawable.ic_network_error_notification_icon,
                            NotificationUtil.RED,
                            _context.getString(
                                    R.string.importmgr_remote_kml_download_failed),
                            _context.getString(
                                    R.string.importmgr_unable_to_determine_kml_filename),
                            _context.getString(
                                    R.string.importmgr_unable_to_determine_kml_filename));
            return;
        }

        // all files (this and child Network Links) are stored in temp dir named based on UID
        String uid = UUID.randomUUID().toString();
        File tmpDir = new File(
                FileSystemUtils.getItem(FileSystemUtils.TMP_DIRECTORY), uid);
        if (!tmpDir.mkdirs()) {
            Log.w(TAG,
                    "Failed to create directories: "
                            + tmpDir.getAbsolutePath());
        }
        GetFileRequest request = new GetFileRequest(resource.getUrl(),
                resource.getName(), tmpDir.getAbsolutePath(),
                curNotificationId++);

        List<GetFileRequest> requests = new ArrayList<>();
        requests.add(request);
        download(new RemoteResourcesRequest(resource, uid, requests,
                request.getNotificationId()));
    }

    /**
     * Download specified files and parse each one, iterate to download any child NetworkLinks
     * Somewhat recursive with all Network Links for all files in a request being downloaded
     * together and then parsed together. If any of those files in turn have child Network Links,
     * all will be processed together, and so on
     * 
     * @param request the request to use.
     */
    private void download(final RemoteResourcesRequest request) {

        if (request == null || !request.isValid()) {
            Log.e(TAG, "Cannot download invalid request");
            return;
        }

        // notify user
        Log.d(TAG, "Import KML Network Link download request created for: "
                + request.toString());

        NotificationUtil
                .getInstance()
                .postNotification(
                        request.getNotificationId(),
                        R.drawable.ic_kml_file_notification_icon,
                        NotificationUtil.BLUE,
                        _context.getString(
                                R.string.importmgr_remote_kml_download_started),
                        String.format(
                                _context.getString(
                                        R.string.importmgr_downloading_count_files),
                                request.getCount()),
                        String.format(
                                _context.getString(
                                        R.string.importmgr_downloading_count_files),
                                request.getCount()));

        // Kick off async HTTP request to get file
        ndl(request);
    }

    public void ndl(final RemoteResourcesRequest requests) {
        Thread t = new Thread(TAG) {
            @Override
            public void run() {
                Log.d(TAG, "start download... ");
                try {
                    for (GetFileRequest r : requests.getRequests()) {
                        GetFileRequest request = r;

                        URL url = new URL(request.getUrl());
                        URLConnection conn = url.openConnection();
                        conn.setRequestProperty("User-Agent", "TAK");
                        conn.setUseCaches(true);
                        conn.setConnectTimeout(10000);
                        conn.setReadTimeout(30000);
                        setAcceptAllVerifier(conn);

                        conn = processRedirect(conn);

                        // support authenticated connections
                        InputStream input;
                        if (conn instanceof HttpURLConnection) {
                            AtakAuthenticationHandlerHTTP.Connection connection;
                            connection = AtakAuthenticationHandlerHTTP
                                    .makeAuthenticatedConnection(
                                            (HttpURLConnection) conn, 3);
                            conn = connection.conn;
                            input = connection.stream;
                        } else {
                            conn.connect();
                            input = conn.getInputStream();
                        }

                        File fout = new File(request.getDir(),
                                request.getFileName());
                        FileOutputStream fos = new FileOutputStream(fout);

                        try {
                            FileSystemUtils.copy(input, fos);
                            Log.d(TAG, "success: " + request.getDir() + "/"
                                    + request.getFileName());
                        } catch (Exception e) {
                            Log.d(TAG, "failure: " + request.getFileName());
                            onRequestConnectionError(new Request(
                                    NetworkOperationManager.REQUEST_TYPE_GET_FILES),
                                    new RequestManager.ConnectionError(900,
                                            "unable to write download"));
                        }

                    }
                    Bundle b = new Bundle();
                    b.putParcelable(GetFilesOperation.PARAM_GETFILES, requests);
                    onRequestFinished(new Request(
                            NetworkOperationManager.REQUEST_TYPE_GET_FILES), b);

                } catch (Exception e) {
                    Log.e(TAG, "error occured", e);
                    onRequestConnectionError(new Request(
                            NetworkOperationManager.REQUEST_TYPE_GET_FILES),
                            new RequestManager.ConnectionError(-1,
                                    "unable to download network source"));

                }
                Log.d(TAG, "end download... ");
            }
        };
        t.start();

        //HTTPRequestManager.from(_context).execute(
        //        request.createGetFileRequests(), this);
    }

    /**
     * Given an existing connection, process a redirect.
     * @param conn the connection
     * @return the redirected connection
     * @throws IOException interactions with the connection failed.
     */
    private URLConnection processRedirect(URLConnection conn)
            throws IOException {
        URLConnection retval = conn;
        if (conn instanceof HttpURLConnection) {
            boolean redirect = false;

            retval = duplicate((HttpURLConnection) conn);

            int status = ((HttpURLConnection) conn)
                    .getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                if (status == HttpURLConnection.HTTP_MOVED_TEMP
                        || status == HttpURLConnection.HTTP_MOVED_PERM
                        || status == HttpURLConnection.HTTP_SEE_OTHER)
                    redirect = true;
            }

            if (redirect) {

                // get redirect url from "location" header field
                String newUrl = conn.getHeaderField("Location");

                // get the cookie if need, for login
                //String cookies = conn
                //        .getHeaderField("Set-Cookie");

                // open the new connnection again
                boolean useCaches = conn.getUseCaches();
                String userAgent = conn.getRequestProperty("User-Agent");
                int connectionTimeout = conn.getConnectTimeout();
                int readTimeout = conn.getReadTimeout();
                retval = new URL(newUrl)
                        .openConnection();
                retval.setRequestProperty("User-Agent", userAgent);
                retval.setUseCaches(useCaches);
                retval.setConnectTimeout(connectionTimeout);
                retval.setReadTimeout(readTimeout);
                setAcceptAllVerifier(retval);
                Log.d(TAG, "Redirect to URL : " + newUrl);

            }
        }

        return retval;
    }

    private static void setAcceptAllVerifier(URLConnection connection) {
        if (connection instanceof javax.net.ssl.HttpsURLConnection) {
            try {
                javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext
                        .getInstance("SSL");
                sc.init(null,
                        new TrustManager[] {
                                CertificateManager.SelfSignedAcceptingTrustManager
                        },
                        new java.security.SecureRandom());
                ((javax.net.ssl.HttpsURLConnection) connection)
                        .setSSLSocketFactory(sc.getSocketFactory());
            } catch (Exception ignored) {
            }
        }
    }

    private static HttpURLConnection duplicate(HttpURLConnection conn)
            throws IOException {
        HttpURLConnection retval = (HttpURLConnection) conn.getURL()
                .openConnection();
        retval.setRequestProperty("User-Agent", "TAK");
        retval.setUseCaches(conn.getUseCaches());
        retval.setConnectTimeout(conn.getConnectTimeout());
        retval.setReadTimeout(conn.getReadTimeout());
        setAcceptAllVerifier(retval);
        return retval;
    }

    int count = 0;

    @Override
    public void onRequestFinished(Request request, Bundle resultData) {

        // HTTP response received successfully
        if (request
                .getRequestType() == NetworkOperationManager.REQUEST_TYPE_GET_FILES) {
            if (resultData == null) {
                Log.e(TAG,
                        "Remote KML Download Failed - Unable to obtain results");
                NotificationUtil
                        .getInstance()
                        .postNotification(
                                SpatialDbContentSource.getNotificationId(),
                                R.drawable.ic_network_error_notification_icon,
                                NotificationUtil.RED,
                                _context.getString(
                                        R.string.importmgr_remote_kml_download_failed),
                                _context.getString(
                                        R.string.importmgr_unable_to_obtain_results),
                                _context.getString(
                                        R.string.importmgr_unable_to_obtain_results));
                return;
            }

            // the initial request that was sent out
            final RemoteResourcesRequest initialRequest = resultData
                    .getParcelable(GetFilesOperation.PARAM_GETFILES);
            if (initialRequest == null || !initialRequest.isValid()) {
                Log.e(TAG,
                        "Remote KML Download Failed - Unable to parse request");
                NotificationUtil
                        .getInstance()
                        .postNotification(
                                SpatialDbContentSource.getNotificationId(),
                                R.drawable.ic_network_error_notification_icon,
                                NotificationUtil.RED,
                                _context.getString(
                                        R.string.importmgr_remote_kml_download_failed),
                                _context.getString(
                                        R.string.importmgr_unable_to_parse_request),
                                _context.getString(
                                        R.string.importmgr_unable_to_parse_request));
                return;
            }

            // loop all requested files
            final RemoteResourcesRequest childRequests = new RemoteResourcesRequest(
                    initialRequest.getResource(),
                    initialRequest.getUID(),
                    new ArrayList<GetFileRequest>(),
                    initialRequest.getNotificationId());
            Log.d(TAG, "Parsing child requests size: "
                    + initialRequest.getRequests().size());
            for (final GetFileRequest curRequest : initialRequest
                    .getRequests()) {
                File downloadedFile = new File(curRequest.getDir(),
                        curRequest.getFileName());
                if (!FileSystemUtils.isFile(downloadedFile)) {
                    Log.e(TAG,
                            "Remote KML Download Failed - Failed to create local file: "
                                    + downloadedFile);
                    NotificationUtil
                            .getInstance()
                            .postNotification(
                                    initialRequest.getNotificationId(),
                                    R.drawable.ic_network_error_notification_icon,
                                    NotificationUtil.RED,
                                    _context.getString(
                                            R.string.importmgr_remote_kml_download_failed),
                                    String.format(
                                            _context.getString(
                                                    R.string.importmgr_failed_to_create_local_file_filename),
                                            curRequest.getFileName()),
                                    String.format(
                                            _context.getString(
                                                    R.string.importmgr_failed_to_create_local_file_filename),
                                            curRequest.getFileName()));
                    continue;
                    //return;
                }

                // Attempt to set file extension, user specified this is KML/Z
                String filename = curRequest.getFileName().toLowerCase(
                        LocaleUtil.getCurrent());
                if (!filename.endsWith(".kml") && !filename.endsWith(".kmz")) {
                    if (FileSystemUtils.isZip(new File(downloadedFile
                            .getParent(), filename))) {
                        filename += ".kmz";
                    } else {
                        filename += ".kml";
                    }

                    Log.d(TAG, curRequest.getFileName() + " set filename: "
                            + filename);
                    File renamed = new File(downloadedFile.getParent(),
                            filename);
                    if (!FileSystemUtils.renameTo(downloadedFile, renamed)) {
                        Log.e(TAG,
                                "Remote KML Download Failed - Failed to rename local file: "
                                        + renamed.getAbsolutePath());
                        NotificationUtil
                                .getInstance()
                                .postNotification(
                                        initialRequest.getNotificationId(),
                                        R.drawable.ic_network_error_notification_icon,
                                        NotificationUtil.RED,
                                        _context.getString(
                                                R.string.importmgr_remote_kml_download_failed),
                                        String.format(
                                                _context.getString(
                                                        R.string.importmgr_failed_to_create_local_file_filename),
                                                curRequest.getFileName()),
                                        String.format(
                                                _context.getString(
                                                        R.string.importmgr_failed_to_create_local_file_filename),
                                                curRequest.getFileName()));
                        return;
                    }

                    downloadedFile = renamed;
                    curRequest.setFileName(filename);

                }

                if (!FileSystemUtils.isFile(downloadedFile)) {
                    Log.e(TAG,
                            "Remote KML Download Failed - Failed to create local file: "
                                    + downloadedFile);
                    NotificationUtil
                            .getInstance()
                            .postNotification(
                                    initialRequest.getNotificationId(),
                                    R.drawable.ic_network_error_notification_icon,
                                    NotificationUtil.RED,
                                    _context.getString(
                                            R.string.importmgr_remote_kml_download_failed),
                                    String.format(
                                            _context.getString(
                                                    R.string.importmgr_failed_to_create_local_file_filename),
                                            curRequest.getFileName()),
                                    String.format(
                                            _context.getString(
                                                    R.string.importmgr_failed_to_create_local_file_filename),
                                            curRequest.getFileName()));
                    return;
                }

                // Extract KML from KMZ if necessary
                try {
                    File tempKML = KMLUtil
                            .getKmlFileFromKmzFile(downloadedFile,
                                    _context.getCacheDir());
                    if (tempKML != null) {
                        Log.d(TAG, "Extracting KMZ downloaded file: "
                                + curRequest.toString());
                        downloadedFile = tempKML;
                    } else {
                        Log.d(TAG,
                                "Network Link is not KMZ, processing KML...");
                    }
                } catch (IOException e1) {
                    Log.d(TAG,
                            "Network Link is not KMZ, processing KML...",
                            e1);
                }

                Kml kml;
                try {
                    Log.d(TAG,
                            "Parsing downloaded file: "
                                    + downloadedFile.getAbsolutePath());
                    kml = _serializer
                            .read(Kml.class, downloadedFile, false);
                } catch (Exception e) {
                    Log.e(TAG,
                            "Remote KML Download Failed - Unable to de-serialize KML",
                            e);
                    NotificationUtil
                            .getInstance()
                            .postNotification(
                                    initialRequest.getNotificationId(),
                                    R.drawable.ic_network_error_notification_icon,
                                    NotificationUtil.RED,
                                    _context.getString(
                                            R.string.importmgr_remote_kml_download_failed),
                                    String.format(
                                            _context.getString(
                                                    R.string.importmgr_invalid_kml),
                                            curRequest.getFileName()),
                                    String.format(
                                            _context.getString(
                                                    R.string.importmgr_invalid_kml),
                                            curRequest.getFileName()));
                    return;
                }

                if (kml == null || kml.getFeature() == null) {
                    Log.w(TAG,
                            "Remote KML Download Failed - KML has no Feature");
                    NotificationUtil
                            .getInstance()
                            .postNotification(
                                    initialRequest.getNotificationId(),
                                    R.drawable.ic_network_error_notification_icon,
                                    NotificationUtil.RED,
                                    _context.getString(
                                            R.string.importmgr_remote_kml_download_failed),
                                    String.format(
                                            _context.getString(
                                                    R.string.importmgr_kml_has_no_features),
                                            curRequest.getFileName()),
                                    String.format(
                                            _context.getString(
                                                    R.string.importmgr_kml_has_no_features),
                                            curRequest.getFileName()));
                    return;
                }

                KMLUtil.deepFeatures(kml,
                        new FeatureHandler<NetworkLink>() {

                            @Override
                            public boolean process(NetworkLink link) {
                                if (link == null || link.getLink() == null
                                        || link.getLink().getHref() == null) {
                                    Log.w(TAG,
                                            "Remote KML Download Failed - KML has invalid Link");
                                    NotificationUtil
                                            .getInstance()
                                            .postNotification(
                                                    initialRequest
                                                            .getNotificationId(),
                                                    R.drawable.ic_network_error_notification_icon,
                                                    NotificationUtil.RED,
                                                    _context.getString(
                                                            R.string.importmgr_remote_kml_download_failed),
                                                    String.format(
                                                            _context.getString(
                                                                    R.string.importmgr_kml_has_invalid_link),
                                                            curRequest
                                                                    .getFileName()),
                                                    String.format(
                                                            _context.getString(
                                                                    R.string.importmgr_kml_has_invalid_link),
                                                            curRequest
                                                                    .getFileName()));
                                    return false;
                                }

                                String url = KMLUtil.getURL(link.getLink());
                                if (FileSystemUtils.isEmpty(url)) {
                                    Log.e(TAG,
                                            "Unsupported NetworkLink URL: : "
                                                    + url);
                                    NotificationUtil
                                            .getInstance()
                                            .postNotification(
                                                    initialRequest
                                                            .getNotificationId(),
                                                    R.drawable.ic_network_error_notification_icon,
                                                    NotificationUtil.RED,
                                                    _context.getString(
                                                            R.string.importmgr_remote_kml_download_failed),
                                                    String.format(
                                                            _context.getString(
                                                                    R.string.importmgr_only_http_s_network_links_supported),
                                                            curRequest
                                                                    .getFileName()),
                                                    String.format(
                                                            _context.getString(
                                                                    R.string.importmgr_only_http_s_network_links_supported),
                                                            curRequest
                                                                    .getFileName()));
                                    return false;
                                }

                                String filename = curRequest.getFileName();
                                if (FileSystemUtils.isEmpty(filename)) {
                                    Log.e(TAG,
                                            "Unable to determine local filename for Network Link: "
                                                    + url);
                                    NotificationUtil
                                            .getInstance()
                                            .postNotification(
                                                    initialRequest
                                                            .getNotificationId(),
                                                    R.drawable.ic_network_error_notification_icon,
                                                    NotificationUtil.RED,
                                                    _context.getString(
                                                            R.string.importmgr_remote_kml_download_failed),
                                                    _context.getString(
                                                            R.string.importmgr_unable_to_determine_filename),
                                                    _context.getString(
                                                            R.string.importmgr_unable_to_determine_filename));
                                    return false;
                                }

                                // get label for this Network Link

                                Log.d(TAG, "download: " + link);
                                filename = filename + "-"
                                        + link.getLink().getHref()
                                                .replaceAll(":", "")
                                                .replaceAll("/", "-");
                                Log.d(TAG, "to: " + filename);

                                // filename = KMLUtil.getNetworkLinkName(
                                //         filename, link) + (count++);
                                childRequests
                                        .getRequests()
                                        .add(
                                                new GetFileRequest(
                                                        url,
                                                        filename,
                                                        curRequest.getDir(),
                                                        initialRequest
                                                                .getNotificationId()));
                                return false;
                            }
                        }, NetworkLink.class);

            } // end request loop

            // see if we have more Network Links to download
            if (childRequests.getCount() > 0) {
                // psuedo-recurse
                download(childRequests);
            } else {
                Log.d(TAG,
                        "Finished downloading Network Links for: "
                                + initialRequest.toString());

                // all data is on file system, now move them into KML folder to be imported
                // we are storing all in the same temp dir, so just grab off first one
                File tempFolder = new File(initialRequest.getRequests()
                        .get(0).getDir());

                new ImportNetworkLinksTask(_serializer, _context,
                        initialRequest.getResource(),
                        initialRequest.getNotificationId())
                                .execute(tempFolder);
            }
        } else {
            Log.w(TAG,
                    "Unhandled request response: " + request.getRequestType());
        }
    }

    @Override
    public void onRequestConnectionError(Request request,
            RequestManager.ConnectionError ce) {
        String detail = NetworkOperation.getErrorMessage(ce);
        Log.e(TAG, "Remote KML Download Failed - Request Connection Error: "
                + detail);
        NotificationUtil
                .getInstance()
                .postNotification(
                        SpatialDbContentSource.getNotificationId(),
                        R.drawable.ic_network_error_notification_icon,
                        NotificationUtil.RED,
                        _context.getString(
                                R.string.importmgr_remote_kml_download_failed),
                        String.format(_context
                                .getString(R.string.importmgr_check_your_url),
                                detail),
                        String.format(_context
                                .getString(R.string.importmgr_check_your_url),
                                detail));
    }

    @Override
    public void onRequestDataError(Request request) {
        Log.e(TAG, "Remote KML Download Failed - Request Data Error");
        NotificationUtil
                .getInstance()
                .postNotification(
                        SpatialDbContentSource.getNotificationId(),
                        R.drawable.ic_network_error_notification_icon,
                        NotificationUtil.RED,
                        _context.getString(
                                R.string.importmgr_remote_kml_download_failed),
                        _context.getString(
                                R.string.importmgr_request_data_error),
                        _context.getString(
                                R.string.importmgr_request_data_error));
    }

    @Override
    public void onRequestCustomError(Request request, Bundle resultData) {
        Log.e(TAG, "Remote KML Download Failed - Request Custom Error");
        NotificationUtil
                .getInstance()
                .postNotification(
                        SpatialDbContentSource.getNotificationId(),
                        R.drawable.ic_network_error_notification_icon,
                        NotificationUtil.RED,
                        _context.getString(
                                R.string.importmgr_remote_kml_download_failed),
                        _context.getString(
                                R.string.importmgr_request_custom_error),
                        _context.getString(
                                R.string.importmgr_request_custom_error));
    }

    public void shutdown() {
        // TODO what to do? Shutdown RequestService? cancel any ongoing downloads?

        if (_linkRefresh != null) {
            _linkRefresh.shutdown();
            _linkRefresh = null;
        }
    }
}
