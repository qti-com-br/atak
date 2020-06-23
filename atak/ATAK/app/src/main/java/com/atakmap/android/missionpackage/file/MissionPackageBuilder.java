
package com.atakmap.android.missionpackage.file;

import com.atakmap.android.importexport.CotEventFactory;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Support for building a Mission Package
 * 
 * 
 */
public class MissionPackageBuilder {
    private static final String TAG = "MissionPackageBuilder";

    public static final String MANIFEST_PATH = "MANIFEST";
    static final String MANIFEST_XML = MANIFEST_PATH + File.separator
            + "manifest.xml";

    /**
     * Interface for callback during building of a Mission Package
     * 
     * 
     */
    public interface Progress {
        void publish(int progress);

        void cancel(String reason);

        boolean isCancelled();
    }

    private final List<String> _warnings;
    private ZipOutputStream _zos;
    private final Progress _progress;
    private final MissionPackageManifest _contents;
    private final MapGroup _mapGroup;
    final byte[] _buffer; // for efficiency reuse _buffer for all files

    private ZipFile _existing; // Used for reading an existing MP when overwriting

    public MissionPackageBuilder(Progress progress,
            MissionPackageManifest contents,
            MapGroup mapGroup) {
        _progress = progress;
        _contents = contents;
        _mapGroup = mapGroup;
        _buffer = new byte[FileSystemUtils.BUF_SIZE];
        _warnings = new ArrayList<>();
    }

    public boolean hasWarnings() {
        return _warnings != null && _warnings.size() > 0;
    }

    /**
     * Build a mission package from the specified inputs
     * 
     * @return path to Mission Package zip file
     */
    public String build() {
        // TODO if a ZipEntry fails, the manifest _may_ be out of sync with actual contents
        File tmpCopy = null;
        try {
            int currentProgress = 5; // 5 percent to setup zip file...
            int progressPerContent = 95 / Math.max(_contents._contents
                    .getContents().size(), 1);

            File f = new File(_contents.getPath());
            if (f.exists()) {
                // MP already exists - copy it to a temp file in case we need to
                // extract and pull its contents into the new package
                tmpCopy = new File(f.getAbsolutePath() + ".tmp");
                FileSystemUtils.copyFile(f, tmpCopy);
                _existing = null;
                try {
                    _existing = new ZipFile(tmpCopy);
                } catch (Exception e) {
                    Log.e(TAG,
                            "Failed to open temp copy for reading: " + tmpCopy);
                }
            }

            FileOutputStream fos = new FileOutputStream(_contents.getPath());
            _zos = new ZipOutputStream(new BufferedOutputStream(fos));

            Log.d(TAG, "Building package: " + _contents.getPath());
            // store manifest. Note in case of otherwise empty Mission Package, this
            // will be only file in .zip. If manifest fails, package creation fails
            AddManifest(_zos, _contents);

            if (_progress != null)
                _progress.publish(currentProgress);
            if (_progress != null && _progress.isCancelled())
                return null;

            // loop and compress all contents
            for (MissionPackageContent content : _contents._contents
                    .getContents()) {
                AddContent(content);

                // after each content, check for cancel, and update progress
                if (_progress != null && _progress.isCancelled())
                    return null;
                if (_progress != null)
                    _progress.publish((currentProgress += progressPerContent));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to create zip file", e);
            if (_progress != null)
                _progress.cancel(e.getMessage());
        } finally {
            if (_zos != null) {
                try {
                    _zos.close();
                } catch (Exception e) {
                    Log.w(TAG, "Failed to close Mission Package zip: "
                            + (_contents == null ? "" : _contents.getPath()));
                }
            }
            if (_existing != null) {
                try {
                    _existing.close();
                } catch (Exception ignore) {
                }
            }
            if (tmpCopy != null)
                FileSystemUtils.delete(tmpCopy);
        }

        if (_progress != null)
            _progress.publish(99);

        if (hasWarnings()) {
            Log.w(TAG,
                    "Mission Package Builder warning count: "
                            + _warnings.size());

            // TODO when done display warnings for user? currently just going to logcat
            for (String warning : _warnings)
                Log.w(TAG, warning);
        }

        return _contents.getPath();
    }

    /**
     * Create manifest or throw Exception
     * 
     * @param zos
     * @param contents
     * @throws Exception
     */
    public static void AddManifest(ZipOutputStream zos,
            MissionPackageManifest contents)
            throws IOException {
        String xml = contents.toXml(false);
        if (FileSystemUtils.isEmpty(xml)) {
            throw new IOException("Failed to serialize manifest");
        }

        zos.setComment("Created by ATAK. Mission Package version "
                + contents.getVersion());
        AddManifestEntry(zos, xml);
    }

    private static void AddManifestEntry(ZipOutputStream zos, String content)
            throws IOException {

        if (FileSystemUtils.isEmpty(content)) {
            throw new IOException("Failed to serialize Manifest content");
        }

        byte[] contentData = content.getBytes(FileSystemUtils.UTF8_CHARSET);
        if (FileSystemUtils.isEmpty(contentData)) {
            throw new IOException("Failed to serialize Manifest Data");
        }

        // create new zip entry
        ZipEntry entry = new ZipEntry(MANIFEST_XML);
        zos.putNextEntry(entry);
        Log.d(TAG, "Adding manifest: " + entry.getName() + " with size: "
                + contentData.length);

        // stream data into zipstream
        // Note, here we dont use write buffering as we've already got the whole event in RAM...
        zos.write(contentData, 0, contentData.length);

        // close zip entry
        zos.closeEntry();
    }

    private void AddContent(MissionPackageContent content) {

        try {
            if (content == null || !content.isValid()) {
                throw new Exception("Invalid Content");
            }

            // see if this is CoT
            if (content.isCoT()) {
                Log.d(TAG, "Adding COT Content: " + content.getManifestUid());
                AddCoTContent(content);
            } else {
                Log.d(TAG, "Adding FILE Content: " + content.getManifestUid());
                AddFileContent(content);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to add Content: "
                    + (content == null ? "" : content.toString()), e);
            _warnings.add("Skipping, Zip Error for Content: "
                    + (content == null ? "" : content.getManifestUid()));
        }
    }

    private void AddFileContent(MissionPackageContent content) {
        try {
            NameValuePair p = content
                    .getParameter(MissionPackageContent.PARAMETER_LOCALPATH);
            if (p == null || !p.isValid()
                    || !FileSystemUtils.isFile(p.getValue())) {
                if (p != null)
                    Log.w(TAG, "Local Path: " + p.getValue());

                _warnings.add("Skipping, file path does not exist: "
                        + content.getManifestUid());
                return;
            }

            // No point of adding a Mission Package itself
            if (FileSystemUtils.isEquals(p.getValue(), _contents.getPath())) {
                _warnings.add("Skipping, cannot add Mission Package to itself: "
                        + content.getManifestUid());
                return;
            }

            // Copy ZIP files to a temp directory before zipping
            // this is to avoid any cases of recursive archival
            boolean tmpCopy = false;
            File f = new File(p.getValue());
            if (f.exists() && FileSystemUtils.isZip(f)) {
                File tmpDir = FileSystemUtils.getItemOnSameRoot(f, "tmp");
                if (!tmpDir.exists() && !tmpDir.mkdirs())
                    Log.e(TAG, "Failed to create tmp dir: " + tmpDir);
                else {
                    File tmpFile = new File(tmpDir, f.getName());
                    if (!tmpFile.equals(f)) {
                        FileSystemUtils.copyFile(f, tmpFile);
                        if (tmpFile.exists()) {
                            f = tmpFile;
                            tmpCopy = true;
                        }
                    }
                }
            }

            // create new zip entry
            ZipEntry entry = new ZipEntry(content.getManifestUid());
            _zos.putNextEntry(entry);

            // stream file into zipstream
            FileInputStream fi = new FileInputStream(f);
            BufferedInputStream origin = new BufferedInputStream(fi,
                    FileSystemUtils.BUF_SIZE);
            FileSystemUtils.copyStream(origin, true, _zos, false, _buffer);

            // close current file & corresponding zip entry
            _zos.closeEntry();

            // Remove temp file if we created one
            if (tmpCopy)
                FileSystemUtils.deleteFile(f);
        } catch (IOException e) {
            Log.e(TAG, "Failed to add File: " + content.toString(), e);
            _warnings.add("Skipping, Zip Error for File: "
                    + content.getManifestUid());
        }
    }

    /**
     * Locate the MapItem with the specified UID and add it to the Zip stream
     * 
     * @param content
     */
    private void AddCoTContent(MissionPackageContent content) {

        if (content == null || !content.isValid()) {
            _warnings.add("Unable to adapt invalid content UID");
            return;
        }

        NameValuePair pair = content
                .getParameter(MissionPackageContent.PARAMETER_UID);
        if (pair == null || !pair.isValid()) {
            NameValuePair p = content
                    .getParameter(MissionPackageContent.PARAMETER_LOCALPATH);
            if (p != null && p.isValid())
                AddFileContent(content);
            else
                _warnings.add(
                        "Skipping missing UID: " + content.getManifestUid());
            return;
        }

        String uid = pair.getValue();
        MapItem item = _mapGroup.deepFindItem("uid", uid);
        if (item == null) {
            // Item isn't found on the map, but it might be in the existing package
            if (_existing != null) {
                ZipEntry entry = _existing.getEntry(uid + "/" + uid + ".cot");
                if (entry != null) {
                    InputStream is = null;
                    try {
                        is = _existing.getInputStream(entry);
                        _zos.putNextEntry(entry);
                        FileSystemUtils.copyStream(is, true, _zos, false);
                        _zos.closeEntry();
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to copy MP entry: " + entry, e);
                        _warnings.add("Failed to copy entry: " + uid);
                    } finally {
                        try {
                            if (is != null)
                                is.close();
                        } catch (Exception ignore) {
                        }
                    }
                    return;
                }
            }
            _warnings.add("Failed to find MapItem: " + uid);
            return;
        }

        Log.d(TAG, "Processing map item CoT: " + item);
        CotEvent event = CotEventFactory.createCotEvent(item);
        if (event == null || !event.isValid()) {
            _warnings.add("Failed to export item to CoT: " + item);
            return;
        }
        AddCoTContent(content, event);
    }

    private void AddCoTContent(MissionPackageContent content, CotEvent event) {

        String eventXML = event.toString();
        if (FileSystemUtils.isEmpty(eventXML)) {
            _warnings.add("Failed to serialize CoT Event: " + event.getUID());
            return;
        }

        // create new zip entry
        ZipEntry entry = new ZipEntry(content.getManifestUid());

        try {
            byte[] eventData = eventXML.getBytes(FileSystemUtils.UTF8_CHARSET);
            if (FileSystemUtils.isEmpty(eventData)) {
                _warnings.add("Failed to serialize CoT Event Data: "
                        + event.getUID());
                return;
            }

            // set ZIPEXTRA action sp receiver knows how to process this ZipEntry
            Log.d(TAG, "Adding: " + entry.getName() + " with size: "
                    + eventData.length);
            _zos.putNextEntry(entry);

            // stream data into zipstream
            // Note, here we dont use write buffering as we've already got the whole event in RAM...
            _zos.write(eventData, 0, eventData.length);

            // close zip entry
            _zos.closeEntry();
        } catch (IOException e) {
            Log.e(TAG, "Failed to add CoT Entry: " + event.getUID(), e);
            _warnings.add("Skipping, Zip Error for CoT Entry: "
                    + event.getUID());
        }
    }
}
