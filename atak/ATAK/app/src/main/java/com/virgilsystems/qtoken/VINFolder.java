package com.virgilsystems.qtoken;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class VINFolder extends AppCompatActivity {

    Context mContext;

    public static String rootFolder = "";


    public VINFolder(Context context) {
        mContext = context;
    }


    /**
     * This function check the permission for storage folder
     * And request access to the folder in case it's not accessible
     * @return void
     */
    public boolean isStoragePermissionGranted() {
        Log.v("### QToken","Checking Storage permission");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(mContext,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {

                Log.v("### QToken","Permission is granted");

                createStructure();

                return true;

            } else {
                Log.v("### QToken","Permission is revoked");

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v("### QToken","Permission is granted");
            return true;
        }
    }


    /**
     * This function will popup a request for the storage folder
     * @param requestCode UID for the file
     * @param permissions Type of permission
     * @param grantResults Result of the request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            Log.v("### QToken","Permission: "+permissions[0]+ "was "+grantResults[0]);

            createStructure();
        }
    }

    /**
     * This function call others function to create the VIN folder structure and files
     */
    void createStructure() {
        createVinFolders();

        createConfigFile();
    }

    /**
     * This function create the folder structure
     */
    void createVinFolders() {

        rootFolder = "";

        try {

            String f1 = "/storage/emulated/0/atak";
            String f2 = "/data/data";
            String f3 = Environment.getExternalStorageDirectory().getPath(); //"/sdcard";
            String f4 = "/storage";

            File f1Folder = new File(f1);
            File f2Folder = new File(f2);
            File f3Folder = new File(f3);
            File f4Folder = new File(f4);

            if(f1Folder.exists() && f1Folder.canWrite()) {
                rootFolder = f1;

            } else if (f2Folder.exists() && f2Folder.canWrite()) {
                rootFolder = f2;

            } else if (f3Folder.exists() && f3Folder.canWrite()) {
                rootFolder = f3;

            } else if (f4Folder.exists() && f4Folder.canWrite()) {
                rootFolder = f4;

            } else {
                Log.v("### QToken","\nNo Root Folder available");
                Log.v("### QToken","\nCheck storage permission");
                isStoragePermissionGranted();
            }

            File rootFolderFile = new File(rootFolder + "/VIN");
            if(rootFolder.equals("")) {
                return;
            }

            if(!rootFolderFile.exists()) {
                rootFolderFile.mkdirs();
            }

            File vinFolderKeys = new File(rootFolder + "/VIN/keys");
            if(!vinFolderKeys.exists()) {
                vinFolderKeys.mkdirs();
            }

            File vinFolderOutputs = new File(rootFolder + "/VIN/outputs");
            if(!vinFolderOutputs.exists()) {
                vinFolderOutputs.mkdirs();
            }

            File vinFolderReceipts = new File(rootFolder + "/VIN/receipts");
            if(!vinFolderReceipts.exists()) {
                vinFolderReceipts.mkdirs();
            }

            File vinFolderLogs = new File(rootFolder + "/VIN/logs");
            if(!vinFolderLogs.exists()) {
                vinFolderLogs.mkdirs();
            }

            File vinFolderReceived = new File(rootFolder + "/VIN/receipts/received");
            if(!vinFolderReceived.exists()) {
                vinFolderReceived.mkdirs();
            }

            File vinFolderSent = new File(rootFolder + "/VIN/receipts/sent");
            if(!vinFolderSent.exists()) {
                vinFolderSent.mkdirs();
            }
        } catch(Exception e){
            e.printStackTrace();
        }

        try {
            Thread.sleep(1000);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * This function create the config file "defaults.cfg" in the VIN root folder
     */
    public void createConfigFile() {
        try {

            String sFileName = "defaults.cfg";

            String cfgBody = "vin_version = \"0.1.0\";\n" +
                    "default_bootstrap_address = \"0.0.0.0:8000\";\n" +
                    "default_lvm_port = \"60001\"\n" +
                    "chunk_size = \"50000\"\n" +
                    "\n" +
                    "file_system:\n" +
                    "{\n" +
                    "    base_dir = \"" + rootFolder + "/VIN\";\n" +
                    "    configs_dir = \"" + rootFolder + "/VIN\";\n" +
                    "    receipts:\n" +
                    "    {\n" +
                    "        receipts_base_dir = \"" + rootFolder + "/VIN/receipts/\";\n" +
                    "        receipts_received_dir = \"" + rootFolder + "/VIN/receipts/received/\";\n" +
                    "        receipts_sent_dir = \"" + rootFolder + "/VIN/receipts/sent/\";\n" +
                    "\n" +
                    "    }\n" +
                    "    keys:\n" +
                    "    {\n" +
                    "        keys_dir = \"" + rootFolder + "/VIN/keys/\";\n" +
                    "        public_key_name = \"" + rootFolder + "/VIN/keys/self.pub\";\n" +
                    "        private_key_name = \"" + rootFolder + "/VIN/keys/self.priv\";\n" +
                    "    }\n" +
                    "    general:\n" +
                    "    {\n" +
                    "        output_dir = \"" + rootFolder + "/VIN/outputs/\";\n" +
                    "        logs_dir = \"" + rootFolder + "/VIN/logs/\"\n" +
                    "        rebuilt_fd = \"" + rootFolder + "/VIN/outputs/rebuilt\";\n" +
                    "    }\n" +
                    "};";

            File cfg = new File(rootFolder + "/VIN", sFileName);

            cfg.createNewFile();
            FileWriter writer = new FileWriter(cfg);
            writer.append(cfgBody);
            writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
