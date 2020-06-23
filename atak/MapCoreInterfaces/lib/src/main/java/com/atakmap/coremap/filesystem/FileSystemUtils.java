
package com.atakmap.coremap.filesystem;

import android.content.Context;
import android.os.Environment;

import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import com.atakmap.coremap.locale.LocaleUtil;
import java.util.Set;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Centralize the definition of root file system on the File System.
 */
public class FileSystemUtils {

    private static final String TAG = "FileSystemUtils";

    private static final String[] ROOT_FS_WHITELIST = {
            "/sdcard/", "/mnt/", "/data/data/", "/data/user/", "/storage/",
            "/maps/"
    }; // maps is for MCE hardware devices

    // Paths which point to internal storage and can be shortened to /sdcard
    private static final String[] INTERNAL_STORAGE_PATHS = new String[] {
            "/storage/emulated/0",
            "/storage/emulated/legacy"
    };

    /**
     * Max extension length to display in UI
     */
    private static final int MAX_EXT_LENGTH = 6;

    // List of all of the mount points containing a ROOT_DIRECTORY
    static String[] mountPoints = null;

    final public static String ATAK_ROOT_DIRECTORY = "atak";
    final public static String OLD_ATAK_ROOT_DIRECTORY = "com.atakmap.map";

    final static public int BUF_SIZE = 8192;
    final static private int LARGE_BUF_SIZE = 8192 * 8;
    final static public int CHARBUFFERSIZE = 1024;

    /**
     * Tools & plugins may store data in atak/tooldata
     */
    final public static String TOOL_DATA_DIRECTORY = "tools";
    final public static String SUPPORT_DIRECTORY = "support";
    final public static String CONFIG_DIRECTORY = "config";
    final public static String OVERLAYS_DIRECTORY = "overlays";
    final public static String DTED_DIRECTORY = "DTED";
    final public static String DATABASES_DIRECTORY = "Databases";

    /**
     * A common directory for exporting data the user may need to find on file system
     */
    final public static String EXPORT_DIRECTORY = "export";

    /**
     * Data place in this directory is deleted during shutdown
     */
    final public static String TMP_DIRECTORY = "tmp";

    /**
     * May be placed at root of each file system to have ATAK import all contents during startup
     */
    public static final String ATAKDATA = "atakdata";

    // Platform permissions XML file
    final public static String PLATFORM_XML = "/system/etc/permissions/platform.xml";
    final public static String PLATFORM_MEDIA_RW = "<group gid=\"media_rw\" />";
    private static final boolean systemRestartRequired = false;

    public static final String UTF8_CHARSET = "UTF-8";

    private static final File sdcard = Environment
            .getExternalStorageDirectory();

    private static boolean scanned = false;

    /**
     * Explicit initialization
     */
    public static void init() {
        getRoot();
    }

    /**
     * Cleans up the temporary directory
     */
    public static void cleanup() {
        try {
            //delete left over temp files, but leave tmp directory in place
            File tmp = FileSystemUtils.getItem(TMP_DIRECTORY);
            FileSystemUtils.deleteDirectory(tmp, false);
            if (!tmp.exists()) {
                Log.d(TAG, "creating tmp directory: " + tmp);
                if (!tmp.mkdirs())
                    Log.w(TAG, "Failed to mkdir: " + tmp);
            }

            //Remove any empty attachment directories
            File dir = FileSystemUtils.getItem("attachments");
            File[] files = dir.listFiles();
            if (files != null) {
                for (File i : files) {
                    if (i.isDirectory()) {
                        String[] subfiles = i.list();
                        if (subfiles == null || subfiles.length == 0) {
                            if (!i.delete()) {
                                Log.d(TAG, "could not clear empty directory: "
                                        + i);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {

        }
    }

    /**
     * Get the location where all ATAK information should reside. Each call returns a new File
     * object representing the root directory.
     */
    synchronized public static File getRoot() {
        if (!scanned) {
            String[] points = findMountPoints();
            for (String point : points) {
                Log.v(TAG, "directory discovered after initial scan: "
                        + point);
            }

            scanned = true;
        }

        return new File(sdcard, ATAK_ROOT_DIRECTORY);
    }

    /**
     * Used to obtain a resource on the file system within the ATAK base directory.
     * The return result is the internal memory for which the atak directory exists.
     *
     * @param path is the name of the directory or file using File.separatorChar
     *             to delineate between path elements.
     */
    public static File getItem(String path) {
        if (isEmpty(path))
            return getRoot();
        else
            return new File(getRoot().getPath() + File.separatorChar +
                    path.replaceAll("\\.\\.", ""));
    }

    /**
     * Used to obtain a resource on the file system within any ATAK directory.
     * The result is all possible combinations of the atak directory with
     * the path appended.
     *
     * @param path is the name of the directory or file using File.separatorChar
     *             to delineate between path elements.
     */
    public static File[] getItems(String path) {
        String[] mountPoints = findMountPoints();
        File[] retVal = new File[mountPoints.length];
        for (int i = 0; i < mountPoints.length; ++i) {
            retVal[i] = new File(mountPoints[i] + File.separatorChar + path);
        }
        return retVal;
    }

    /**
     * Get the location where all ATAK information should reside, on same SD card as specified file.
     */
    public static File getRoot(File file) {

        getRoot();
        String[] roots = findMountPoints();

        //Log.d(TAG, "Looking for root of: " + file.getAbsolutePath());

        // search all mount/roots
        File bestRoot = null;
        for (String rootPath : roots) {
            //Log.d(TAG, "Comparing root: " + rootPath);            
            // get ATAK root dir's parent e.g. /mnt/sdcard/external_sd
            File root = new File(rootPath);
            File rootParent = root.getParentFile();
            if (rootParent == null)
                continue;

            // walk up the chain until we find ATAK data dir's parent
            File currentFile = file;
            while (currentFile != null) {
                if (currentFile.getAbsolutePath().equals(
                        rootParent.getAbsolutePath())) {
                    // some devices mount SD cards as a child of internal SD (e.g. Note 1)
                    // so a 'file' could match both the external and internal SD card
                    // so we keep the longest path as a hack... e.g.
                    // /mnt/sdcard/external_sd/atak/...
                    File currentRoot = new File(currentFile,
                            ATAK_ROOT_DIRECTORY);
                    if (bestRoot == null) {
                        bestRoot = currentRoot;
                    } else if (currentRoot.getAbsolutePath().length() > bestRoot
                            .getAbsolutePath()
                            .length()) {
                        bestRoot = currentRoot;
                    }
                }

                currentFile = currentFile.getParentFile();
            }
        }

        //Log.d(TAG, "Best root: " + (bestRoot==null?"no match" : bestRoot.getAbsolutePath()));
        return bestRoot;
    }

    /**
     * Clears a cached list of mount points. (Do not call unless you know what you are doing).
     */
    public void clearCachedMountPoints() {
        mountPoints = null;
    }

    private static void legacyFindMountRootDirs(Set<File> roots) {
        File root;
        try {
            Process proc = new ProcessBuilder().command("mount")
                    .redirectErrorStream(true).start();
            proc.waitFor();
            InputStream is = proc.getInputStream();

            BufferedReader r = new BufferedReader(
                    new InputStreamReader(is, UTF8_CHARSET));

            String line;
            while ((line = r.readLine()) != null) {
                // XXX - filesystem types for device storage?
                if (line.contains("fat")) {
                    Log.d(TAG, "Found mnt line: " + line);
                    String[] lineParts = line.split(" ");
                    String path = lineParts[1];
                    if (path == null || "on".equalsIgnoreCase(path))
                        path = lineParts[2];

                    if (path != null) {
                        root = new File(sanitizeWithSpacesAndSlashes(path));
                        try {
                            root = root.getCanonicalFile();
                        } catch (IOException ignored) {
                        }
                        if (root.exists() && root.isDirectory()
                                && root.canWrite())
                            roots.add(root);
                    } else {
                        Log.d(TAG, "null path encountered");
                    }

                }
            }
            is.close();
            destroyProcess(proc);
        } catch (IOException e2) {
            Log.e(TAG, "error: ", e2);
        } catch (InterruptedException e) {
            Log.e(TAG, "error: ", e);
        }
    }

    private static void envFindMountRootDirs(Set<File> retval) {
        String[] storageDirs = new String[2];
        storageDirs[0] = System.getenv("EXTERNAL_STORAGE");
        storageDirs[1] = System.getenv("SECONDARY_STORAGE");

        String android_storage = System.getenv("ANDROID_STORAGE");
        if (android_storage != null) {
            File f = new File(android_storage);
            if (f.exists() && f.isDirectory() && f.canRead()) {
                File[] subdirs = f.listFiles();
                if (subdirs != null) {
                    for (File subdir : subdirs) {
                        if (subdir != null) {
                            File root = new File(subdir,
                                    ATAK_ROOT_DIRECTORY);
                            if (root.isDirectory() && root.canRead()) {
                                try {
                                    root = subdir.getCanonicalFile();
                                    retval.add(root);
                                    Log.d(TAG, "found atak directory under: "
                                            + root);
                                } catch (IOException ignored) {
                                }
                            }
                        }
                    }
                }
            }
        }

        String[] splits;
        File root;
        String[] children;
        for (String storageDir : storageDirs) {
            if (storageDir == null)
                continue;

            splits = storageDir.split("\\" + File.pathSeparator);
            for (String split : splits) {
                root = new File(split);
                try {
                    root = root.getCanonicalFile();
                } catch (IOException ignored) {
                }
                Log.d(TAG, "Found env path: " + root.getAbsolutePath());
                if (!root.isDirectory() || !root.canRead())
                    continue;
                children = root.list();
                if (children == null || children.length < 1)
                    continue;
                retval.add(root);
            }
        }
    }

    /**
     * Gather list of mounts that may contain ATAK data First time it is called, create data
     * directory if "ATAK" import folder exists Note: findMountPoints caches the result of the
     * initial run and does not perform any additional attempts to scan for new mount points. see:
     * http
     * ://stackoverflow.com/questions/8688382/runtime-exec-bug-hangs-without-providing-a-process-
     * object which describes a real issue we are seeing on the Note 1's running the persistent
     * systems rom. specifically:
     * https://github.com/android/platform_bionic/blob/jb-release/libc/bionic/cpuacct.c If a
     * non-cached version is desired, run clearCachedMountPoints() and then call findMountPoints.
     * (NOT RECOMMENDED)
     *
     * @return list of mount points (ATAK data dir included in path)
     */
    synchronized public static String[] findMountPoints() {
        if (mountPoints == null) {
            Set<String> points = new HashSet<>();

            File[] roots = getDeviceRoots();
            for (File f : roots) {
                String path = f.getAbsolutePath();
                FileSystemUtils.migrate(path);

                /**
                 * For code migration, this uses the historic variable naming convention.
                 */
                File arrowmakerPath = new File(path + File.separator
                        + ATAK_ROOT_DIRECTORY);
                if (arrowmakerPath.isDirectory()) {
                    Log.d(TAG, "Mount Point: "
                            + arrowmakerPath.getAbsolutePath());
                    points.add(arrowmakerPath.getAbsolutePath());

                    // Make sure all mount points are writable
                    if (!canWrite(arrowmakerPath)) {
                        Log.w(TAG,
                                "Mount point "
                                        + arrowmakerPath.getAbsolutePath()
                                        + " is not writable!");

                        // If the SD card isn't writable, it's most likely the API 19+ restriction
                        //if(!systemRestartRequired && System.getenv("SECONDARY_STORAGE").contains(path))
                        //    systemRestartRequired = makeSDCardWritable();
                    }
                } else {
                    // if "atakdata" directory exists, then build out com.atakmap.map/layers
                    // directory, data may be imported
                    File atakPath = new File(path + File.separator + ATAKDATA);
                    if (atakPath.isDirectory()) {
                        Log.d(TAG,
                                "Creating Mount Point: "
                                        + arrowmakerPath.getAbsolutePath());
                        File layersDir = new File(
                                arrowmakerPath.getAbsolutePath()
                                        + File.separator
                                        + "layers");
                        if (layersDir.mkdirs()) {
                            Log.d(TAG,
                                    "Mount Point: "
                                            + arrowmakerPath.getAbsolutePath());
                            points.add(arrowmakerPath.getAbsolutePath());
                        } else {
                            Log.w(TAG,
                                    "Failed to create Mount Point: "
                                            + arrowmakerPath.getAbsolutePath());
                        }
                    } else {
                        Log.d(TAG, "Ignoring non-directory: " + path);
                    }
                }
            }

            FileSystemUtils.migrate(Environment.getExternalStorageDirectory()
                    .getPath());
            points.add(new File(Environment.getExternalStorageDirectory()
                    .getPath()
                    + File.separatorChar + ATAK_ROOT_DIRECTORY).getPath());

            mountPoints = points.toArray(new String[points.size()]);
        }

        return mountPoints;
    }

    /**
     * Migration logic for switching the ATAK main directory. handles the first migration from the
     * old to the new for all mount points. does not overwrite cases where the OLD and NEW root
     * directories exist. Handle a card that has both directories on it.
     *
     * @param path is the path to be used that describes the mount point.
     */
    private static boolean migrate(String path) {
        /***
         * Check to see if the old directory exists, if it does, move it to the new directory name.
         * If the new directory already exist, do not move the old directory for now. XXX - logic
         * for this case still in progress.
         */
        File oldPath = new File(path + File.separator
                + FileSystemUtils.OLD_ATAK_ROOT_DIRECTORY);
        File newPath = new File(path + File.separator
                + FileSystemUtils.ATAK_ROOT_DIRECTORY);

        boolean success = false;
        if (oldPath.exists() && oldPath.isDirectory()) {
            Log.d(TAG, "successful migration of " + oldPath + " to " + newPath);
            success = oldPath.renameTo(newPath);
        } else {
            Log.e(TAG, "failed migration of " + oldPath + " to " + newPath);
        }

        return success;
    }

    /**
     * Loop all mount points, attempt to migrate old file or directory to new, on same mount
     *
     * @param oldFile the old mount point
     * @param newFile the new mount point
     */
    public static void migrate(String oldFile, String newFile) {

        final File[] mounts = FileSystemUtils.getDeviceRoots();

        for (File root : mounts) {
            File oldPath = FileSystemUtils.getItemOnSameRoot(root, oldFile);
            File newPath = FileSystemUtils.getItemOnSameRoot(root, newFile);

            File newParent = newPath.getParentFile();
            if (newParent != null && !newParent.exists()) {
                if (!newParent.mkdirs()) {
                    Log.w(TAG, "Failed to create migration dir: " + newParent);
                }
            }

            if (oldPath.exists() && !newPath.exists()) {
                Log.d(TAG, "Migrating " + oldPath.getAbsolutePath() + " to "
                        + newPath.getAbsolutePath());
                if (!oldPath.renameTo(newPath)) {
                    Log.w(TAG,
                            "Failed to migrate: " + newPath.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Used to obtain a resource on the file system within the ATAK base directory on the same SD
     * card.
     *
     * @param file file to use a reference for finding an ATAK root dir
     * @param path is the name of the directory or file
     */
    public static File getItemOnSameRoot(File file, String path) {
        // get "atak" on same SD
        File root = getRoot(file);
        if (root == null) {
            Log.w(TAG,
                    "Unable to find root, using default root for: "
                            + file.getAbsolutePath());
            return getItem(path);
        }

        // now get specified subpath
        if (isEmpty(path))
            return root;
        else
            return new File(root, path);
    }

    /**
     * Retrieves a date as a formatted string useful for using as part of a file name.
     *
     * @return a logging date/time string based on the current system time in yyyyMMdd_HHmm_ss
     * format.
     */
    public static String getLogDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm_ss",
                LocaleUtil.getCurrent());
        return sdf.format(CoordinatedTime.currentDate());
    }

    /**
     * Matching signature for IOUtils.
     */
    public static void copy(final InputStream in, final OutputStream out)
            throws IOException {
        copyStream(in, true, out, true);
    }

    public static String copyStreamToString(final File file)
            throws IOException {
        return copyStreamToString(new FileInputStream(file), true,
                FileSystemUtils.UTF8_CHARSET);
    }

    public static String copyStreamToString(final InputStream in,
            final boolean closeIn,
            final String charSet)
            throws IOException {
        return copyStreamToString(in, closeIn, charSet, new char[BUF_SIZE]);
    }

    public static String copyStreamToString(final InputStream in,
            final boolean closeIn,
            final String charSet,
            char[] buffer) throws IOException {

        if (in == null)
            throw new IOException("InputStream is null");

        if (buffer == null)
            buffer = new char[BUF_SIZE];

        BufferedReader reader = null;
        InputStreamReader isr = null;
        StringBuilder fileData = new StringBuilder(BUF_SIZE);

        try {
            reader = new BufferedReader(isr = new InputStreamReader(in,
                    charSet));
            int numRead;
            String readData;
            while ((numRead = reader.read(buffer)) != -1) {
                readData = String.valueOf(buffer, 0, numRead);
                fileData.append(readData);
            }
        } finally {
            if (closeIn) {
                try {
                    if (reader != null)
                        reader.close();
                } catch (IOException ignore) {
                }
                try {
                    if (isr != null)
                        isr.close();
                } catch (IOException ignore) {
                }
                try {
                    if (in != null)
                        in.close();
                } catch (IOException ignore) {
                }
            }
        }
        return fileData.toString();
    }

    /**
     * Reads the entirety of the file at filename into a String[]
     * where each item in the array is a separate line.
     * Will propagate any exceptions, or return a (possibly empty) array.
     *
     * @param filename the file to turn into a list of lines.
     * @return the list of lines.
     */
    public static List<String> readLines(final String filename)
            throws Exception {
        List<String> ret = new java.util.LinkedList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(filename), UTF8_CHARSET));
            String line = reader.readLine();
            while (line != null) {
                ret.add(line);
                line = reader.readLine();
            }
        } catch (Exception e) {
            //Log.w(TAG, "Error reading file " + filename, e);
            throw e;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
        }
        return ret;
    }

    public static byte[] read(InputStream inputStream, int size,
            boolean closeIn)
            throws IOException {
        try {
            final byte[] retval = new byte[size];

            int numRead;
            int off = 0;
            do {
                numRead = inputStream.read(retval, off, retval.length - off);
                if (numRead < 0)
                    break;
                off += numRead;
            } while (off < retval.length);

            return retval;
        } finally {
            if (closeIn && inputStream != null)
                inputStream.close();
        }
    }

    /**
     * Given an input stream, read the input stream until no more data is available and return the
     * byte[] that represents the read data.
     */
    public static byte[] read(final InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[LARGE_BUF_SIZE];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    public static void write(final OutputStream os, String s)
            throws IOException {
        try {
            os.write(s.getBytes(FileSystemUtils.UTF8_CHARSET));
        } finally {
            if (os != null)
                os.close();
        }
    }

    /**
     * Matching signature for IOUtils
     */
    public static byte[] toByteArray(final InputStream is) throws IOException {
        return read(is);
    }

    public static void deleteFile(File file) {
        if (file == null)
            return;

        String filepath = file.getAbsolutePath();
        if (SecureDelete.delete(file))
            Log.d(TAG, "Deleted file: " + filepath);
        else
            Log.w(TAG, "Failed to delete file: " + filepath);
    }

    /**
     * Clear files in the specified directory (not recursive)
     *
     * @param dir
     */
    public static void clearDirectory(File dir) {
        SecureDelete.deleteDirectory(dir, true, false);
    }

    /**
     * Deletes the specified path. If the path is a directory, the directory and
     * all of its contents are recursively deleted.
     *
     * @param path A path on the filesystem
     */
    public static void delete(String path) {
        delete(new File(sanitizeWithSpacesAndSlashes(path)));
    }

    /**
     * Deletes the specified path. If the path is a directory, the directory and
     * all of its contents are recursively deleted.
     *
     * @param path A path on the filesystem
     */
    public static void delete(File path) {
        if (path.isDirectory())
            deleteDirectory(path, false);
        else
            deleteFile(path);
    }

    /**
     * Recursively delete all files/directories in specified path
     *
     * @param dir
     * @param bContentsOnly true to delete all contents but leave the entire directory structure in place
     */
    public static void deleteDirectory(File dir, boolean bContentsOnly) {
        SecureDelete.deleteDirectory(dir, bContentsOnly);
    }

    /**
     * Recursively delete all files/directories in specified path
     *
     * @param dir
     * @param ignore   [optional] filter to ignore/skip, not place matching files in the zip
     */
    public static void deleteDirectory(File dir, FilenameFilter ignore) {
        SecureDelete.deleteDirectory(dir, ignore);
    }

    /**
     * Format file extension for UI
     *
     * @param f
     * @param toUpper
     * @param truncate
     * @return
     */
    public static String getExtension(File f, boolean toUpper,
            boolean truncate) {
        String extension = "";

        int i = f.getName().lastIndexOf('.');
        if (i > 0) {
            extension = f.getName().substring(i + 1);
        }

        if (toUpper)
            extension = extension.toUpperCase(LocaleUtil.getCurrent());

        if (truncate && extension.length() > MAX_EXT_LENGTH)
            extension = extension.substring(0, MAX_EXT_LENGTH);

        return extension;
    }

    /**
     * Returns true if the path actually points to a file
     * @param path the file path
     * @return true if the path is a file.
     */
    public static boolean isFile(final String path) {
        if (isEmpty(path))
            return false;

        try {
            return isFile(new File(validityScan(path)));
        } catch (IOException ioe) {
            return false;
        }
    }

    public static boolean isFile(final File file) {
        return file != null && file.exists();
    }

    public static boolean isEmpty(String s) {
        return (s == null || s.length() < 1);
    }

    public static boolean isEmpty(final byte[] b) {
        return (b == null || b.length < 1);
    }

    public static boolean isEmpty(final Object[] b) {
        return (b == null || b.length < 1);
    }

    public static boolean isEquals(final String lhs, final String rhs) {
        if (FileSystemUtils.isEmpty(lhs) && FileSystemUtils.isEmpty(rhs))
            return true;

        return FileSystemUtils.isEmpty(lhs) == FileSystemUtils.isEmpty(rhs)
                && lhs.equals(rhs);

    }

    public static boolean isEquals(Object lhs, Object rhs) {
        if (lhs == rhs)
            return true;
        if (lhs != null)
            return lhs.equals(rhs);
        return false;
    }

    public static boolean isEquals(List<?> lhs,
            List<?> rhs) {
        if (FileSystemUtils.isEmpty(lhs) && FileSystemUtils.isEmpty(rhs))
            return true;

        if (FileSystemUtils.isEmpty(lhs) != FileSystemUtils.isEmpty(rhs))
            return false;

        if (lhs.size() != rhs.size())
            return false;

        for (int i = 0; i < lhs.size(); i++) {
            if (lhs.get(i) == null && rhs.get(i) == null) {
                // both null
            } else if (lhs.get(i) == null && rhs.get(i) != null) {
                // rhs is not null but lhs is null, then not equals
                return false;
            } else if (!lhs.get(i).equals(rhs.get(i))) {
                return false;
            }
        }

        return true;
    }

    public static boolean isEmpty(final List<?> list) {
        return list == null || list.size() < 1;
    }

    /**
     * Get new filepath with 8 random characters on end of file name
     *
     * @param filePath
     * @return
     */
    public static String getRandomFilepath(String filePath) {
        String newDestName;
        String random = UUID.randomUUID().toString().substring(0, 8);

        // see if file has an extension
        int extStart = filePath.lastIndexOf(".");
        if (extStart < 0) {
            newDestName = filePath.concat("_").concat(random);
        } else {
            newDestName = filePath.substring(0, extStart).concat("_")
                    .concat(random);
            newDestName = newDestName.concat(filePath.substring(extStart));
        }

        return newDestName;
    }

    /**
     * Prepare for DB storage and use as a filename restrict characters during input i.e.
     * alphanumeric and various allowed filename characters.
     * <p>
     * This also removes any occurance of ..  which could potentially be used
     * for a filename manipulation attack.
     *
     * @param s the input filename
     * @return the sanitized filename
     */
    public static String sanitizeFilename(final String s) {
        return sanitizeWithSpaces(s);
    }

    /**
     * Prepare for DB storage and use restrict characters during input i.e.
     * alphanumeric and various allowed filename characters.
     * <p>
     * This also removes any occurance of ..  which could potentially be used
     * for a filename manipulation attack.
     *
     * @param s the original string
     * @return the string sanitized
     */
    public static String sanitizeWithSpaces(final String s) {
        if (FileSystemUtils.isEmpty(s))
            return s;

        final String retval = s
                .trim()
                .replaceAll("[^A-Za-z0-9-_. ',%$#:@+=&\u0600-\u06FF()\\]\\[]",
                        "")
                .replaceAll("\\.\\.", "");

        if (!retval.equals(s))
            Log.e(TAG, "soft warning, file name did not pass sanity check: "
                    + s,
                    new Exception());

        if (!whiteListCheck(retval)) {
            return "";
        }

        return retval;
    }

    /**
     * Prepare for DB storage, restrict characters during input i.e.
     * alphanumeric and '$' and '+' and '#' and '.' and '-' and '_' and ' ' and '/'.
     * This also removes any occurrence of ..  which could potentially be used
     * for a filename manipulation attack.
     *
     * @param s the string to sanitize.
     * @return the sanitized string without any .. and other characters not listed above.
     */
    public static String sanitizeWithSpacesAndSlashes(final String s) {
        if (FileSystemUtils.isEmpty(s))
            return s;

        final String retval = s
                .trim()
                .replaceAll("[^A-Za-z0-9-_./ $#:@+=&',\u0600-\u06FF()\\]\\[]",
                        "")
                .replaceAll("\\.\\.", "");

        if (!retval.equals(s))
            Log.e(TAG, "soft warning, file name did not pass sanity check: "
                    + s,
                    new Exception());

        if (!whiteListCheck(retval)) {
            return "";
        }

        return retval;
    }

    /**
     * Sanitize URL (replace spaces, brackets, etc.)
     *
     * @param url URL string
     * @return Sanitized url string
     */
    public static String sanitizeURL(String url) {
        if (FileSystemUtils.isEmpty(url))
            return url;

        return url.replace(" ", "%20")
                .replace("#", "%23")
                .replace("[", "%5B")
                .replace("]", "%5D");
    }

    /**
     * Sanitize URL (brackets and #)
     *
     * @param url URL string
     * @return Sanitized url string
     */
    public static String sanitizeURLKeepSpaces(String url) {
        if (FileSystemUtils.isEmpty(url))
            return url;

        return url.replace("#", "%23")
                .replace("[", "%5B")
                .replace("]", "%5D");
    }

    public static void ensureDataDirectory(String name,
            boolean externalMounts) {
        if (!externalMounts) {
            // just setup the internal flash
            try {
                File f = FileSystemUtils.getItem(name);
                if (!f.exists())
                    if (!f.mkdirs())
                        Log.w(TAG,
                                "Failed to create directories "
                                        + f.getAbsolutePath());
            } catch (Exception ex) { /* ignore */
            }
        } else {

            // setup data directory on all atak mounts
            for (String atakDirectory : FileSystemUtils.findMountPoints()) {
                if (FileSystemUtils.isFile(atakDirectory)) {
                    File dir = new File(atakDirectory, name);
                    if (!dir.exists()) {
                        if (!dir.mkdirs())
                            Log.w(TAG,
                                    "Failed to create directories "
                                            + dir.getAbsolutePath());
                    }
                }
            }
        }
    }

    /**
     * Returns true if the assetExists in the apk otherwise false.
     *
     * @param context  the context to use when checking to see if an asset exists.
     * @param fileName asset filename/path
     */
    public static boolean assetExists(final Context context,
            final String fileName) {
        InputStream in = null;
        try {
            in = context.getAssets().open(fileName);
        } catch (IOException ioe) {
            return false;
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException ignore) {
            }
        }
        return true;
    }

    /**
     * Returns an input stream that is pointing to the asset.
     *
     * @param context  the context to use when getting and asset.
     * @param fileName asset filename/path, null if it does not exist.
     */
    public static InputStream getInputStreamFromAsset(final Context context,
            final String fileName) {
        try {
            InputStream in = context.getAssets().open(fileName);
            return in;
        } catch (IOException ioe) {
            return null;
        }
    }

    /**
     * Copy a single asset from the assets directory to the file system.
     *
     * @param context
     * @param fileName   asset filename/path
     * @param outputPath output filename relative to ATAK root dir
     * @param forceCopy  true to overwrite existing outputPath
     * @return true if the file already exists, or if it was successfully copied
     */
    public static boolean copyFromAssetsToStorageFile(Context context,
            String fileName, String outputPath,
            boolean forceCopy) {
        File outputFile = new File(FileSystemUtils.getRoot(), outputPath);

        if (outputFile.exists() && !forceCopy) {
            Log.i(TAG, "the " + outputPath + " already exists.");
            return true;
        }

        InputStream in = null;
        OutputStream out = null;

        // attempt to make sure that the actual directory exists 
        // before attempting to write into it.
        if (!outputFile.getParentFile().exists()) {
            if (!outputFile.getParentFile().mkdirs())
                Log.d(TAG, "Parent directory could not be created: " +
                        outputFile.getParentFile().getAbsolutePath());
        }
        try {
            in = context.getAssets().open(fileName);
            out = new FileOutputStream(outputFile);
            FileSystemUtils.copyStream(in, out);
        } catch (IOException ioe) {
            Log.e(TAG, "could not copy " + fileName + " to " + outputPath, ioe);
            try {
                if (in != null)
                    in.close();
            } catch (IOException ignore) {
            }
            try {
                if (out != null)
                    out.close();
            } catch (IOException ignore) {
            }

            return false;
        }
        return true;
    }

    /**
     * Copy an entire assets directory to the file system.
     *
     * @returns true if the file already exists, or if it was successfully copied
     */
    public static boolean copyFromAssetsToStorageDir(final Context context,
            final String assetDir,
            final String outputPath,
            final boolean forceCopy) {

        try {
            String[] files = context.getAssets().list(assetDir);
            if (files != null) {
                for (String file : files) {
                    final String from = assetDir + "/" + file;
                    final String to = outputPath + "/" + file;
                    boolean retval = copyFromAssetsToStorageFile(context,
                            from,
                            to,
                            forceCopy);
                    if (!retval) {
                        Log.e(TAG, "could not extract the asset from: " + from
                                + " to: " + to);
                    } else {
                        Log.d(TAG, "extracted the asset from: " + from + "to: "
                                + to);
                    }
                }
            }
        } catch (IOException ignored) {
            Log.e(TAG, "could not find the asset directory: " + assetDir);

        }

        return true;
    }

    // List of all of the mount points containing a ROOT_DIRECTORY
    private static File[] rootDirectories = null;

    private static void destroyProcess(Process process) {
        try {
            if (process != null) {
                // use exitValue() to determine if process is still running.
                process.exitValue();
            }
        } catch (IllegalThreadStateException e) {
            // process is still running, kill it.
            process.destroy();
        }
    }

    /**
     * Clears a cached list of mount points. (Do not call unless you know what you are doing).
     */
    public synchronized static void clearCachedRoots() {
        rootDirectories = null;
    }

    /**
     * Returns the discovered root directories on the device. These directories should consist of
     * the accessible internal and external storages for the device.
     *
     * @return the discovered root directories on the device.
     */
    public synchronized static File[] getDeviceRoots() {
        if (rootDirectories == null) {
            Set<File> roots = new HashSet<>();

            legacyFindMountRootDirs(roots);
            envFindMountRootDirs(roots);
            // XXX - do we really want this or should it just be in the ATAK
            //       application
            getSpecialRoots(roots);

            File primaryStorage = Environment.getExternalStorageDirectory();
            try {
                primaryStorage = primaryStorage.getCanonicalFile();
            } catch (IOException ignored) {
            }
            if (!roots.contains(primaryStorage)) {
                // XXX - make sure we don't have both /storage/emulated/N and
                // /storage/emulated/legacy in the list
                if (primaryStorage.getAbsolutePath().matches(
                        "\\/storage\\/emulated\\/\\d+")) {
                    Iterator<File> iter = roots.iterator();
                    File root;
                    while (iter.hasNext()) {
                        root = iter.next();
                        if (root.getAbsolutePath().equals(
                                "/storage/emulated/legacy")) {
                            iter.remove();
                            break;
                        }
                    }
                }
                roots.add(primaryStorage);
            }

            if (!roots.contains(Environment.getExternalStorageDirectory()))
                roots.add(Environment.getExternalStorageDirectory());
            rootDirectories = roots.toArray(new File[roots.size()]);
        }

        return rootDirectories;
    }

    /**
     * To take care of the weird mounts, only return existing directories.
     */
    private static void getSpecialRoots(Set<File> paths) {
        // To take care of the weird SII issue. AS
        String[] specialRoots = {
                "/mnt/sdcard/external_sd/",
                "/sdcard/bstfolder/BstSharedFolder/",
                "/mnt/USB0/",
                "/mnt/USB1/",
                "/mnt/USB2/",
                "/maps/"
        };

        for (String specialRoot : specialRoots) {
            final File f = new File(specialRoot);
            if (f.exists())
                paths.add(f);
        }
    }

    /**
     * Remove any portion of the filePath including and after the last '.'
     *
     * @param filePath
     * @return
     */
    public static String stripExtension(String filePath) {
        if (filePath == null)
            return null;

        String name = filePath;
        int i = filePath.lastIndexOf('.');
        if (i > 0) {
            name = filePath.substring(0, i);
        }

        return name;
    }

    public static String stripTrailingSlash(String s) {
        if (s.endsWith("/"))
            return s.substring(0, s.length() - 1);

        return s;
    }

    /**
     * Attempt to rename file May have to byte copy across internal vs external SD boundary, as
     * renameTo does not support that on Android. In that case, attempt byte for byte copy
     *
     * @param src
     * @param dest
     */
    public static boolean renameTo(File src, File dest) {

        if (src == null || dest == null) {
            Log.w(TAG, "Unable to rename null file");
            return false;
        }

        if (!src.exists()) {
            Log.w(TAG,
                    "Unable to rename missing file: " + src.getAbsolutePath());
            return false;
        }

        if (FileSystemUtils.isEquals(src.getAbsolutePath(),
                dest.getAbsolutePath())) {
            Log.d(TAG,
                    "Source is same as destination: " + dest.getAbsolutePath());
            return true;
        }

        if (src.renameTo(dest)) {
            Log.d(TAG, "Successfully renamed: " + dest.getAbsolutePath());
            return true;
        } else
            return move(src, dest);
    }

    /**
     * Move file to temp in preparation for deletion. Used to avoid triggering FileObserver.
     *
     * @param f File to move
     * @return Temp file
     */
    public static File moveToTemp(Context c, File f, boolean useRoot) {
        String root = getItemOnSameRoot(f, "tmp").getAbsolutePath();
        String del = "delete_" + f.getName();
        File moved;
        if (useRoot)
            moved = new File(root, del);
        else
            moved = new File(c.getCacheDir(), del);

        //If the parent file does not exist
        if (!moved.getParentFile().exists()) {
            //Try making the directory
            if (!moved.getParentFile().mkdirs()) {
                //Log if .mkdirs() returns false
                Log.e(TAG, "Failed to make directory "
                        + moved.getParentFile().getAbsolutePath());
            }
        }
        if (f.renameTo(moved))
            f = moved;
        else {
            // Probably can't write to /tmp/ from SD card
            // Try current ATAK root instead
            if (!useRoot)
                return moveToTemp(c, f, true);
            Log.w(TAG, "Failed to move file to temp: "
                    + moved.getAbsolutePath());
        }
        return f;
    }

    public static File moveToTemp(Context c, File f) {
        return moveToTemp(c, f, true);
    }

    /**
     * Byte for byte copy, followed by delete of source file See FileSystemUtils.renameTo
     *
     * @param src  the source file
     * @param dest the destination file
     * @return true if the file copied successfully
     */
    private static boolean move(File src, File dest) {
        try {
            FileSystemUtils.copyFile(src, dest);
        } catch (IOException e) {
            Log.w(TAG, "Failed to copy: " + src.getAbsolutePath(), e);
            return false;
        }

        if (!dest.exists()) {
            Log.w(TAG, "Failed to copy: " + src.getAbsolutePath());
            return false;
        }

        // now delete original
        if (!SecureDelete.delete(src))
            Log.w(TAG, "Failed to delete: " + src.getAbsolutePath());

        return true;
    }

    public static void copyFile(final File src, final File dst)
            throws IOException {

        FileInputStream fis = null;
        FileOutputStream fos = null;

        try {
            fis = new FileInputStream(src);
            fos = new FileOutputStream(dst);
            copyStream(fis, fos);
        } finally {
            try {
                if (fis != null)
                    fis.close();
            } catch (IOException ignore) {
            }
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException ignore) {
            }
        }
    }

    public static void copyFile(File src, File dst, byte[] buf)
            throws IOException {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(src);
            fos = new FileOutputStream(dst);
            copyStream(fis, fos, buf);
        } finally {
            try {
                if (fis != null)
                    fis.close();
            } catch (IOException ignore) {
            }
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException ignore) {
            }
        }
    }

    public static void copyStream(final InputStream in, final OutputStream out)
            throws IOException {
        copyStream(in, true, out, true);
    }

    public static void copyStream(final InputStream in, final OutputStream out,
            final byte[] buf)
            throws IOException {
        copyStream(in, true, out, true, buf);
    }

    public static void copyStream(final InputStream in, final boolean closeIn,
            final OutputStream out,
            final boolean closeOut) throws IOException {
        copyStream(in, closeIn, out, closeOut, new byte[BUF_SIZE]);
    }

    public static void copyStream(final InputStream in, final boolean closeIn,
            final OutputStream out,
            final boolean closeOut, final byte[] buf) throws IOException {

        try {
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } finally {
            if (closeIn) {
                try {
                    in.close();
                } catch (Exception ignore) {
                }
            }
            if (closeOut) {
                try {
                    out.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    public static byte[] read(File f) throws IOException {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(f);
            final byte[] retval = new byte[(int) f.length()];

            int numRead;
            int off = 0;
            do {
                numRead = inputStream.read(retval, off, retval.length - off);
                if (numRead < 0)
                    break;
                off += numRead;
            } while (off < retval.length);

            return retval;
        } finally {
            if (inputStream != null)
                inputStream.close();
        }
    }

    /**
     * Checks to see if the specified directory is writable. This method will attempt to actually
     * write a file in the directory, overcoming the shortcomings of {@link java.io.File#canWrite()}
     * , which simply checks the file permissions.
     *
     * @param directory A directory
     * @return <code>true</code> if the directory is writable, <code>false</code> otherwise.
     */
    public static boolean canWrite(File directory) {
        if (!directory.exists())
            return false;
        File writeCheck = null;
        try {
            writeCheck = File.createTempFile(".writecheck", null, directory);

            return writeCheck.exists();
        } catch (IOException ignored) {
            return false;
        } finally {
            if (writeCheck != null) {
                if (!writeCheck.delete())
                    writeCheck.deleteOnExit();
            }
        }
    }

    public static File createTempDir(String prefix, String suffix, File dir)
            throws IOException {
        File retval = File.createTempFile(prefix, suffix, dir);
        if (!retval.delete())
            throw new IOException();
        if (!retval.mkdirs())
            throw new IOException();
        return retval;
    }

    /**
     * Attempt to make the SD card writable (root only)
     */
    public static boolean makeSDCardWritable() {
        Process cp;
        Process mount;

        // ATAK root directory
        String rootDir = Environment.getExternalStorageDirectory()
                .getPath() +
                File.separatorChar + ATAK_ROOT_DIRECTORY;

        // Attempt to copy the platform permissions XML for writing
        try {
            cp = Runtime.getRuntime().exec(
                    "su -c cp " + PLATFORM_XML + " " + rootDir);
            cp.waitFor();

            File platform = new File(rootDir + File.separatorChar
                    + "platform.xml");

            // Copy succeeded
            if (platform.exists() && cp.exitValue() == 0) {
                // Add the permission line to file
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        new FileInputStream(platform), UTF8_CHARSET));

                String line;
                StringBuilder permissions = new StringBuilder();
                StringBuilder platformContents = new StringBuilder();
                long fileLength = platform.length();
                boolean readingPermission = false;
                boolean fileModified = false;
                try {
                    while ((line = br.readLine()) != null) {
                        if (readingPermission)
                            permissions.append(line);

                        // Begin reading permission data
                        if (line.contains("WRITE_EXTERNAL_STORAGE")
                                && line.contains("<permission"))
                            readingPermission = true;
                        // Finish reading permission data
                        if (readingPermission
                                && line.contains("</permission>")) {
                            // If the media_rw permission isn't found
                            if (!permissions.toString().contains(
                                    PLATFORM_MEDIA_RW)) {
                                platformContents.append(PLATFORM_MEDIA_RW);
                                platformContents.append("\n");
                                fileModified = true;
                                Log.d(TAG,
                                        "External storage media_rw permission not found!");
                            } else
                                Log.d(TAG,
                                        "External storage media_rw permission already exists");
                            readingPermission = false;
                        }
                        platformContents.append(line);
                        platformContents.append("\n");
                    }
                } catch (java.io.IOException e) {
                    br.close();
                }
                br.close();

                // Write permission to file
                if (fileModified) {
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream((platform));
                        fos.write(platformContents.toString().getBytes(
                                UTF8_CHARSET));
                    } catch (UnsupportedEncodingException e) {
                        Log.w(TAG, "error writing the file", e);
                    } catch (IOException e) {
                        Log.w(TAG, "error writing the file", e);
                    } finally {
                        if (fos != null) {
                            fos.close();
                        }
                    }

                    // Make sure there was no data lost in the process
                    if (platform.length() > fileLength) {
                        try {
                            // Make system temporarily writable
                            mount = Runtime.getRuntime().exec(
                                    "su -c mount -o rw,remount /system");
                            mount.waitFor();

                            // Copy platform file back to system directory
                            cp = Runtime.getRuntime().exec(
                                    "su -c cp "
                                            + platform.getAbsolutePath() + " "
                                            + PLATFORM_XML);
                            cp.waitFor();

                            // Make system read-only again
                            mount = Runtime.getRuntime().exec(
                                    "su -c mount -o ro,remount /system");
                            mount.waitFor();

                            // Delete local copy
                            deleteFile(platform);

                            Log.d(TAG,
                                    "Successfully updated system permissions file!");
                            return true;
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                } else {
                    deleteFile(platform);
                    return true;
                }
            } else {
                Log.w(TAG, "Failed to copy platform.xml to " + rootDir);
            }
        } catch (Exception e) {
            Log.e(TAG, "catch of any number of other errors", e);
        }
        return false;
    }

    /**
     * Is a system restart required?
     * Only true when the system permissions file has been modified
     */
    public static boolean isSystemRestartRequired() {
        return systemRestartRequired;
    }

    @Deprecated
    public static void extract(File zip, File destDir, boolean overwrite)
            throws IOException {
        unzip(zip, destDir, overwrite);
    }

    /**
     * Extract the zip file to the destination directory
     *
     * @param zip       the zip file
     * @param destDir   the destination directory to unzip to
     * @param overwrite true to overwrite existing files
     */
    public static void unzip(File zip, File destDir, boolean overwrite)
            throws IOException {
        if (!FileSystemUtils.isFile(zip)) {
            throw new IOException("Cannot extract missing file: "
                    + zip.getAbsolutePath());
        }

        ZipInputStream zin = null;

        try {
            if (!destDir.isDirectory() || !destDir.exists()) {
                boolean r = destDir.mkdirs();
                if (!r)
                    Log.d(TAG, "could not create: " + destDir);
            }

            Log.d(TAG, "Extracting zip: " + zip.getAbsolutePath() + " into "
                    + destDir.getAbsolutePath());
            //reuse buffer for performance
            byte[] buffer = new byte[8192];

            // read in from zip
            zin = new ZipInputStream(new FileInputStream(zip));
            ZipEntry zinEntry;

            // iterate all zip entries
            while ((zinEntry = zin.getNextEntry()) != null) {
                if (zinEntry.isDirectory()) {
                    Log.d(TAG, "Skipping zip directory: " + zinEntry.getName());
                    continue;
                }

                File f = new File(destDir,
                        sanitizeWithSpacesAndSlashes(zinEntry.getName()));

                if (!f.getParentFile().exists()) {
                    if (!f.getParentFile().mkdirs())
                        Log.d(TAG, "could not create: " + f.getParentFile());
                }

                FileOutputStream fos = new FileOutputStream(f);

                // stream from in zip to out file

                try {
                    if (f.exists()) {
                        if (overwrite) {
                            Log.d(TAG,
                                    "Overwriting zip file: "
                                            + zinEntry.getName()
                                            + " to " + f.getAbsolutePath());
                            FileSystemUtils.copyStream(zin, false, fos, true,
                                    buffer);
                        }
                    } else {
                        Log.d(TAG, "Extracting zip file: " + zinEntry.getName()
                                + " to " + f.getAbsolutePath());
                        FileSystemUtils.copyStream(zin, false, fos, true,
                                buffer);
                    }
                } finally {
                    fos.close();
                }

                //see if created successfully
                if (!f.exists()) {
                    Log.w(TAG, "Failed to extract: " + f.getAbsolutePath());
                }
            } // end zin loop
        } finally {
            if (zin != null) {
                try {
                    zin.close();
                } catch (IOException e) {
                    Log.e(TAG,
                            "Failed to close zip file: "
                                    + zip.getAbsolutePath(),
                            e);
                }
            }
        }
    }

    /**
     * Compress the directory contents into a ZIP file
     *
     * @param dir      the directory to zip.
     * @param dest     the destination file.
     * @param compress True to compress the ZIP, false to store
     * @return the destination file if the zip is successful, otherwise null.
     * @throws IOException in case there are any io exceptions.
     */
    public static File zipDirectory(File dir, File dest, boolean compress)
            throws IOException {
        return zipDirectory(dir, dest, compress, null);
    }

    /**
     * Compress the directory contents into a ZIP file
     *
     * @param dir      the directory to zip.
     * @param dest     the destination file.
     * @param compress True to compress the ZIP, false to store
     * @param ignore   [optional] filter to ignore/skip, not place matching files in the zip
     * @return the destination file if the zip is successful, otherwise null.
     * @throws IOException in case there are any io exceptions.
     */
    public static File zipDirectory(File dir, File dest, boolean compress,
            FilenameFilter ignore)
            throws IOException {
        if (dest == null) {
            Log.w(TAG, "Cannot zip to missing file");
            return null;
        }

        if (!FileSystemUtils.isFile(dir)) {
            Log.w(TAG, "Cannot zip missing Directory file");
            return null;
        }

        if (!dir.isDirectory()) {
            Log.w(TAG,
                    "Cannot zip non Directory file: " + dir.getAbsolutePath());
            return null;
        }

        File[] files = dir.listFiles();
        if (isEmpty(files)) {
            Log.w(TAG, "Cannot zip empty Directory: " + dir.getAbsolutePath());
            return null;
        }

        ZipOutputStream zos = null;
        try {
            FileOutputStream fos = new FileOutputStream(dest);
            zos = new ZipOutputStream(new BufferedOutputStream(fos));

            // Don't compress the ZIP
            if (!compress)
                zos.setLevel(Deflater.NO_COMPRESSION);

            //loop and add all files
            for (File file : files) {
                if (file == null) {
                    Log.w(TAG, "Skipping invalid file");
                    continue;
                }

                if (file.isDirectory()) {
                    addDirectory(zos, file, null, ignore);
                } else {
                    if (ignore != null
                            && ignore.accept(dir, file.getAbsolutePath())) {
                        Log.d(TAG, "Skipping file: " + file);
                        continue;
                    }

                    addFile(zos, file);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to create Zip file", e);
            throw new IOException(e);
        } finally {
            if (zos != null) {
                try {
                    zos.close();
                } catch (Exception e) {
                    Log.w(TAG,
                            "Failed to close Zip: " + dest.getAbsolutePath());
                }
            }
        }

        //validate the required files
        if (FileSystemUtils.isFile(dest)) {
            Log.d(TAG, "Exported: " + dest.getAbsolutePath());
            return dest;
        } else {
            Log.w(TAG,
                    "Failed to export valid zip: "
                            + dest.getAbsolutePath());
            return null;
        }
    }

    public static File zipDirectory(File dir, File dest) throws IOException {
        return zipDirectory(dir, dest, true);
    }

    /**
     * Compress the file list into a ZIP file
     *
     * @param files The list of files to be compressed.
     * @param dest the destination zip file.
     * @param compress True to compress the ZIP, false to store
     * @return the destination zip file if successful otherwise null
     * @throws IOException io exception if there is an exception writing the file.
     */
    public static File zipDirectory(List<File> files, File dest,
            boolean compress) throws IOException {
        if (dest == null) {
            Log.w(TAG, "Cannot zip to missing file");
            return null;
        }

        if (FileSystemUtils.isEmpty(files)) {
            Log.w(TAG, "Cannot zip empty file list");
            return null;
        }

        List<String> filenames = new ArrayList<>();
        ZipOutputStream zos = null;
        try {
            FileOutputStream fos = new FileOutputStream(dest);
            zos = new ZipOutputStream(new BufferedOutputStream(fos));

            // Don't compress the ZIP
            if (!compress)
                zos.setLevel(Deflater.NO_COMPRESSION);

            //loop and add all files
            for (File file : files) {
                if (!FileSystemUtils.isFile(file) || file.isDirectory()) {
                    Log.w(TAG, "Skipping invalid file: "
                            + (file == null ? "" : file.getAbsolutePath()));
                    continue;
                }

                //zip may only contains a given filename once
                String filename = file.getName();
                if (filenames.contains(filename)) {
                    filename = getRandomFilepath(filename);
                }
                filenames.add(filename);
                addFile(zos, file, filename);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to create Zip file", e);
            throw new IOException(e);
        } finally {
            if (zos != null) {
                try {
                    zos.close();
                } catch (Exception e) {
                    Log.w(TAG,
                            "Failed to close Zip: " + dest.getAbsolutePath());
                }
            }
        }

        //validate the required files
        if (FileSystemUtils.isFile(dest)) {
            Log.d(TAG, "Exported: " + dest.getAbsolutePath());
            return dest;
        } else {
            Log.w(TAG,
                    "Failed to export valid zip: "
                            + dest.getAbsolutePath());
            return null;
        }
    }

    public static File zipDirectory(List<File> files, File dest)
            throws IOException {
        return zipDirectory(files, dest, true);
    }

    /**
     * Pull String/text from Zip Entry: MANIFEST/manifest.xml
     *
     * @param zip the zip file
     * @param zipEntry the entry
     * @return the string that describes the entry in the zip file.
     */
    public static String GetZipFileString(File zip, String zipEntry) {

        if (!FileSystemUtils.isFile(zip)) {
            Log.e(TAG, "Zip does not exist: " + zip.getAbsolutePath());
            return null;
        }

        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(zip);
            ZipEntry manifest = zipFile
                    .getEntry(zipEntry);
            if (manifest == null) {
                Log.d(TAG, "Zip does not contain: " + zipEntry);
                return null;
            }

            char[] charBuffer = new char[CHARBUFFERSIZE];
            return FileSystemUtils.copyStreamToString(
                    zipFile.getInputStream(manifest), true,
                    FileSystemUtils.UTF8_CHARSET, charBuffer);

        } catch (Exception e) {
            Log.e(TAG,
                    "Failed to get content listing for: "
                            + zip.getAbsolutePath(),
                    e);
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    Log.e(TAG,
                            "Failed to close zip file: "
                                    + zip.getAbsolutePath(),
                            e);
                }
            }
        }

        return null;
    }

    public static boolean ZipHasFile(File zip, String zipEntry) {

        if (!FileSystemUtils.isFile(zip)) {
            Log.e(TAG, "Zip does not exist: " + zip.getAbsolutePath());
            return false;
        }

        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(zip);
            ZipEntry manifest = zipFile
                    .getEntry(zipEntry);
            return manifest != null;

        } catch (Exception e) {
            Log.e(TAG,
                    "Failed to get file for: "
                            + zip.getAbsolutePath(),
                    e);
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    Log.e(TAG,
                            "Failed to close zip file: "
                                    + zip.getAbsolutePath(),
                            e);
                }
            }
        }

        return false;
    }

    /**
     * Check if a given file is a valid ZIP file
     * @param f File
     * @return True if the file exists and is a valid ZIP file
     */
    public static boolean isZip(File f) {
        if (!FileSystemUtils.isFile(f))
            return false;

        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(f);
            zipFile.entries();
            return true;
        } catch (Exception e) {
            // This method just checks if the file is a ZIP or not
            // No need to log exceptions if it's not a ZIP
            //Log.e(TAG, "Failed to get zip for: " + f, e);
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (Exception ignore) {
                }
            }
        }

        return false;
    }

    /**
     * Add a file to a ZIP archive stream
     *
     * @param zos ZIP output stream
     * @param file File to compress
     * @param filename File name that will show up in the ZIP
     */
    public static void addFile(ZipOutputStream zos, File file,
            String filename) {
        try {
            // create new zip entry
            ZipEntry entry = new ZipEntry(filename);
            zos.putNextEntry(entry);

            // stream file into zipstream

            FileInputStream fis = null;

            try {
                if (!file.isDirectory()) {
                    fis = new FileInputStream(file);
                    FileSystemUtils.copyStream(fis, true, zos, false);
                }
            } finally {
                try {
                    if (fis != null)
                        fis.close();
                } catch (Exception ignored) {
                }

                // close current file & corresponding zip entry
                zos.closeEntry();

            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to add File: " + file.getAbsolutePath(), e);
        }
    }

    public static void addFile(ZipOutputStream zos, File file) {
        addFile(zos, file, file.getName());
    }

    /**
     * Add a directory to a ZIP archive stream
     *
     * @param zos ZIP output stream
     * @param dir Directory
     * @param parentPath The parent directory path (null if root)
     */
    public static void addDirectory(ZipOutputStream zos, File dir,
            String parentPath) {
        addDirectory(zos, dir, parentPath, null);
    }

    public static void addDirectory(ZipOutputStream zos, File dir,
            String parentPath, FilenameFilter ignore) {
        String prefix = dir.getName() + File.separator;
        if (!FileSystemUtils.isEmpty(parentPath))
            prefix = parentPath + prefix;
        File[] files = dir.listFiles();
        if (FileSystemUtils.isEmpty(files)) {
            // Create an empty directory entry
            addFile(zos, dir, prefix);
            return;
        }

        // Add files
        for (File file : files) {
            if (file == null) {
                Log.w(TAG, "Skipping invalid file");
                continue;
            }

            if (file.isDirectory())
                addDirectory(zos, file, prefix, ignore);
            else {
                if (ignore != null
                        && ignore.accept(dir, file.getAbsolutePath())) {
                    Log.d(TAG, "Skipping file: " + file);
                    continue;
                }

                addFile(zos, file, prefix + file.getName());
            }
        }
    }

    public static class FileTreeData {
        public long size = 0;
        public long lastModified = -1L;
        public long numFiles = 0;
    }

    public static void getFileData(File derivedFrom, FileTreeData result) {
        getFileData(derivedFrom, result, Long.MAX_VALUE);
    }

    /**
     * Given a File, produce a slightly prettier printout of that file so that it
     * does not show /storage/emulated/0.
     * @param f the file to pretty print.
     * @return the pretty printed string.
     */
    public static String prettyPrint(File f) {
        if (f == null)
            return null;

        String path = f.getAbsolutePath();
        // do not show the user /storage/emulated/0
        for (String prefix : INTERNAL_STORAGE_PATHS) {
            if (path.equals(prefix))
                return "/sdcard";
            // Tack on separator so we don't accidentally
            // replace part of a directory name
            prefix += File.separator;
            if (path.startsWith(prefix))
                return path.replaceAll(prefix, "/sdcard/");
        }
        return path;
    }

    /**
     *
     * @param derivedFrom
     * @param result
     * @param limit
     * @return
     */
    public static boolean getFileData(File derivedFrom, FileTreeData result,
            long limit) {
        if (derivedFrom.isFile()) {
            result.size += derivedFrom.length();

            long t = derivedFrom.lastModified();
            if (t > result.lastModified) {
                result.lastModified = t;
            }

            result.numFiles++;
        } else {
            String[] children = derivedFrom.list();
            if (children != null) {
                for (String aChildren : children) {
                    if (!getFileData(new File(derivedFrom, aChildren),
                            result, limit))
                        break;
                }
            }
        }
        return (result.numFiles <= limit);
    }

    public static long getFileSize(File derivedFrom) {
        if (derivedFrom.isFile())
            return derivedFrom.length();
        File[] children = derivedFrom.listFiles();
        if (children == null) {
            Log.w(TAG, "No children for file " + derivedFrom.getAbsolutePath());
            return -1;
        }
        long retval = 0;

        for (File aChildren : children)
            retval += getFileSize(aChildren);
        return retval;
    }

    public static long getLastModified(File derivedFrom) {
        if (derivedFrom.isFile())
            return derivedFrom.lastModified();
        long retval = -1L;
        long t;
        File[] children = derivedFrom.listFiles();
        if (children != null) {
            for (File aChildren : children) {
                t = getLastModified(aChildren);
                if (t > retval)
                    retval = t;
            }
        }
        return retval;
    }

    /**
     * The number of files in a directory tree.
     * @param derivedFrom the file or directory.   if a file is described then the return is 1 and
     * if a directory is provided, the directory is recursed counting all of the files.
     * @return the number of files in a directory tree.
     */
    public static int getNumberOfFiles(File derivedFrom) {
        if (derivedFrom.isFile())
            return 1;
        int retval = 0;
        File[] children = derivedFrom.listFiles();
        if (children != null) {
            for (File aChildren : children)
                retval += getNumberOfFiles(aChildren);
        }
        return retval;
    }

    /**
     * Fortify Validity check for String filenames so they cannot 
     * contain path manipulation characters.
     */
    public static String validityScan(final String fileName)
            throws FileNameInvalid {
        return validityScan(fileName, null);
    }

    /**
     * Fortify Validity check for String filenames so they cannot 
     * contain path manipulation characters.
     * 
     * If the following string is expected to have a specific 
     * extension, if no extension is specified then the file is 
     * still checked for sanity.
     * The extension list that is passed in contains extensions
     * that are not dotted.
     */
    public static String validityScan(final String fileName,
            final String[] exts)
            throws FileNameInvalid {

        //Log.d(TAG, "validity scan: " + fileName);
        if (fileName == null || fileName.isEmpty())
            throw new FileNameInvalid("empty name");

        if (exts != null && exts.length > 0) {
            boolean valid = false;
            for (String ext : exts) {
                if (fileName.toLowerCase(LocaleUtil.getCurrent()).endsWith(
                        "." + ext.toLowerCase(LocaleUtil.getCurrent()))) {
                    valid = true;
                }
            }
            if (!valid)
                throw new FileNameInvalid("invalid extension " + fileName);
        }

        if (fileName.contains("..") || fileName.indexOf('\0') >= 0)
            throw new FileNameInvalid("illegal .. or null character in: "
                    + fileName);

        if (!whiteListCheck(fileName)) {
            throw new FileNameInvalid(
                    "invalid root path, failed whitelist check: "
                            + fileName);
        }

        return fileName;

    }

    /** 
     * Check a filename to see if it passes the white list check.   True if it passes, false if it 
     * does not.
     */
    private static boolean whiteListCheck(final String fileName) {
        boolean pass = false;

        if (fileName != null) {
            if (!fileName.startsWith("/"))
                return true;

            for (String r : ROOT_FS_WHITELIST) {
                if (fileName.startsWith(r)) {
                    pass = true;
                }
            }
            if (!pass) {
                Log.e(TAG,
                        "hard warning, file name did not pass whitelist check: "
                                + fileName,
                        new Exception());
                return false;
            }
        }

        return true;
    }

    /**
     * Given a directory, basename and a set of extensions, return the first file 
     * that exists in a directory and matches.
     * @param directory the directory to look in.
     * @param base the name of the file without the extension.
     * @param exts list of extensions.
     */
    public static File findFile(File directory, String base, String[] exts) {
        if (exts != null && base != null) {
            for (String ext : exts) {
                File f = new File(directory, base + ext);
                if (f.exists())
                    return f;
            }
        }
        return null;
    }

    public static class FileNameInvalid extends IOException {
        public FileNameInvalid(final String reason) {
            super(reason);
            Log.d(TAG, "validity scan failed: " + reason);
        }
    }
}
