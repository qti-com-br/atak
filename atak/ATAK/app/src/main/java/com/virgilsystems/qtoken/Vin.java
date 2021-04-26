package com.virgilsystems.qtoken;

import android.os.Bundle;
import android.widget.TextView;
import android.os.Looper;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;
import java.io.IOException;
import java.io.File;
import android.net.Uri;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.content.Intent;
import java.net.URISyntaxException;
import java.io.FileWriter;
import android.os.Build;
import android.content.pm.PackageManager;
import android.Manifest;
import android.net.wifi.WifiManager;
import 	java.net.InetAddress;
import 	java.net.UnknownHostException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.atakmap.app.R;

import static android.content.Context.MODE_PRIVATE;


public class Vin {

//    MainBridgeCPP bridge;

    public static Context mContext;

    static int SHARE_SELECTED_FILE_CODE = 0001;
    static int SPREAD_SELECTED_FILE_CODE = 0002;

    private String rootFolder = "";

    TextView bootstrapIpTextView;
    EditText log;

    String logTxt = "";

    SharedPreferences vinSharedPref;
    Editor vinSharedPrefEditor;


//    protected void onCreate(Bundle savedInstanceState) {
//
//        // Main Context
//        mContext = null;//getApplicationContext();
//
//        // Shared Preferences
//        vinSharedPref = mContext.getSharedPreferences("VIN_Shared_Pref", MODE_PRIVATE);
//        vinSharedPrefEditor = vinSharedPref.edit();
//        String bootstrapIP = vinSharedPref.getString("bootstrap_ip", "");
//
//        // Check Folder Permission
//        if (!isStoragePermissionGranted()) {
//            Toast.makeText(mContext, "This app needs access to the files.",
//                    Toast.LENGTH_LONG).show();
//        }
//
//        // Get main layout
//        final ConstraintLayout mConstraintLayout = findViewById(R.id.constraintLayoutParent);
//
//        // Get Ip Address
//        final TextView node_ip = mConstraintLayout.findViewById(R.id.node_ip);
//        node_ip.setText("Node IP: " + getIpAddress(mContext));
//
//        // Log on Screen
//        log = findViewById(R.id.log);
//        log.setEnabled(false);
//
//        // TextView Bootstrap IP
//        bootstrapIpTextView = mConstraintLayout.findViewById(R.id.bootstrap_ip);
//        if(bootstrapIP.equals("")) {
//            bootstrapIpTextView.setText("Set Bootstrap IP ->");
//            log.append("\nPlease set Bootstrap IP");
//        } else {
//            bootstrapIpTextView.setText("Bootstrap IP: " + bootstrapIP);
//            log.append("\nThe Bootstrap IP is " + bootstrapIP);
//        }
//
//        // RUN
//        final Button btnRun = mConstraintLayout.findViewById(R.id.qrun);
//        btnRun.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//                // Get and Check Bootstrap IP
//                String bootstrapIP = vinSharedPref.getString("bootstrap_ip", "");
//                if(bootstrapIP.equals("")) {
//                    log.append("\nPlease set Bootstrap IP");
//                    return;
//                }
//
//                // Creating Android node
//                log.append("\nCreating Android Node...");
//
//                Thread th = new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Log.d("### QTOKEN", "MainActivity run");
//                        MainBridgeCPP bridge = new MainBridgeCPP();
//
//                        (MainActivity.this).runOnUiThread(new Runnable() {
//                            public void run() {
//                                try {
//                                    Thread.sleep(3000);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//                                log.append("\n" + logTxt);
//                            }
//                        });
//
//                        try {
//                            if(!rootFolder.equals("")) {
//                                logTxt = "Android Node is ready";
//                                bridge.run(mContext, bootstrapIP, rootFolder);
//                            } else {
//                                logTxt = "Please allow access to the files";
//                                isStoragePermissionGranted();
//                            }
//                            Log.d("### QTOKEN", "MainActivity run - " + logTxt);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                });
//                th.start();
//            }
//        });
//
//
//        // PUT
//        final Button btnPut = mConstraintLayout.findViewById(R.id.qput);
//        btnPut.setOnClickListener(new View.OnClickListener() {
//            //Sending put comand...
//            @Override
//            public void onClick(View view) {
//                new Thread( new Runnable() { @Override public void run() {
//                    Log.d("###QTOKEN", "PluginDropDownReceiver put");
//                    MainBridgeCPP bridge = new MainBridgeCPP();
//                    bridge.put();
//                } } ).start();
//                log.append("\nPut command sent");
//            }
//        });
//
//
//        // GET
//        final Button btnGet = mConstraintLayout.findViewById(R.id.qget);
//        btnGet.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                new Thread( new Runnable() { @Override public void run() {
//                    Log.d("###QTOKEN", "PluginDropDownReceiver get");
//                    MainBridgeCPP bridge = new MainBridgeCPP();
//                    bridge.get();
//                } } ).start();
//                log.append("\nGet command sent");
//            }
//        });
//
//
//        // SHARE
//        final Button btnShare = mConstraintLayout.findViewById(R.id.qshare);
//        btnShare.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent igf = new Intent()
//                        .setType("*/*")
//                        .setAction(Intent.ACTION_GET_CONTENT);
//
//                startActivityForResult(Intent.createChooser(igf, "Select a file"),
//                        SHARE_SELECTED_FILE_CODE);
//
//                log.append("\nShare command");
//                log.append("\nSelecting file...");
//            }
//        });
//
//
//        // SPREAD
//        final Button btnSpread = mConstraintLayout.findViewById(R.id.qspread);
//        btnSpread.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent igf = new Intent()
//                        .setType("*/*")
//                        .setAction(Intent.ACTION_GET_CONTENT);
//
//                startActivityForResult(Intent.createChooser(igf, "Select a file"),
//                        SPREAD_SELECTED_FILE_CODE);
//
//                log.append("\nSpread command executed");
//            }
//        });
//
//
//        // GATHER
//        final Button btnGather = mConstraintLayout.findViewById(R.id.qgather);
//        btnGather.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                new Thread( new Runnable() { @Override public void run() {
//                    Log.d("###QTOKEN", "PluginDropDownReceiver Gather");
//                    MainBridgeCPP bridge = new MainBridgeCPP();
//                    bridge.gather();
//                } } ).start();
//
//                log.append("\nGather command executed");
//            }
//        });
//
//
//        // Settings
//        final ImageButton settingsBtn = mConstraintLayout.findViewById(R.id.settings);
//        settingsBtn.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View view) {
//                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//                builder.setTitle("Bootstrap IP");
//
//                final EditText input = new EditText(mContext);
//                input.setInputType(InputType.TYPE_CLASS_TEXT);
//                input.setText(vinSharedPref.getString("bootstrap_ip", ""));
//                builder.setView(input);
//
//                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        final String ip = input.getText().toString();
//
//                        try {
//                            Log.d("###QTOKEN", "MainActivity settings");
//                            checkAndSaveIP(ip);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                });
//                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.cancel();
//                    }
//                });
//
//                // Loading at main Thread
//                Handler mainHandler = new Handler(Looper.getMainLooper());
//                Runnable myRunnable = new Runnable() {
//                    @Override
//                    public void run() {
//                        builder.show();
//                    }
//                };
//                mainHandler.post(myRunnable);
//
//            }
//        });
//    }

    /**
     * This function check the permission for storage folder
     * And request access to the folder in case it's not accessible
     * @return void
     */
    public  boolean isStoragePermissionGranted() {
        Log.v("### QToken","Checking Storage permission");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(mContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("### QToken","Permission is granted");
                return true;
            } else {

                Log.v("### QToken","Permission is revoked");
//                ActivityCompat.requestPermissions(this,
//                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
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
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
//            Log.v("### QToken","Permission: "+permissions[0]+ "was "+grantResults[0]);
//
//            createStructure();
//        }
//    }

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

            String f1 = "/sdcard";
            String f2 = "/data/data";
            String f3 = "/mnt";
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
                log.append("\nNo Root Folder available");
                log.append("\nCheck storage permission");
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


//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if(requestCode == SHARE_SELECTED_FILE_CODE && resultCode == RESULT_OK) {
//            Uri selectedfile = data.getData();
//
//            try {
//
//                FileHandler fileHandler = new FileHandler();
//                String filePath = fileHandler.getFilePath(mContext, selectedfile);
//
//                MainBridgeCPP bridge = new MainBridgeCPP();
//
//                Log.d("###QTOKEN", "PluginDropDownReceiver share");
//                bridge.share(filePath);
//
//            } catch(URISyntaxException e) {
//                e.printStackTrace();
//            }
//
//            log.append("\nFile shared");
//            //Toast.makeText(mContext, "File shared.", Toast.LENGTH_LONG).show();
//        }
//
//        if(requestCode == SPREAD_SELECTED_FILE_CODE && resultCode == RESULT_OK) {
//            Uri selectedfile2 = data.getData();
//
//            try {
//
//                FileHandler fileHandler = new FileHandler();
//                String filePath2 = fileHandler.getFilePath(mContext, selectedfile2);
//
//                new Thread( new Runnable() { @Override public void run() {
//
//                    Log.d("###QTOKEN", "MainActivity Spread");
//                    MainBridgeCPP bridge = new MainBridgeCPP();
//                    bridge.spread(filePath2);
//
//                } } ).start();
//
//                log.append("\nFile shared");
//                //Toast.makeText(mContext, "Spread command executed", Toast.LENGTH_LONG).show();
//
//            } catch(URISyntaxException e) {
//                e.printStackTrace();
//            }
//
//            //Toast.makeText(mContext, "File shared.", Toast.LENGTH_LONG).show();
//        }
//
//    }


    private void checkAndSaveIP(String bootstrapIP) {
        if(bootstrapIP.equals("")) {
            Toast.makeText(mContext, "Please configure the Bootstrap IP.",
                    Toast.LENGTH_SHORT).show();

        } else if(!Patterns.IP_ADDRESS.matcher(bootstrapIP).matches()) {
            Toast.makeText(mContext, "Invalid IP Address.",
                    Toast.LENGTH_SHORT).show();
        } else {
            // Save Bootstrap IP on Shared Preferences
            vinSharedPrefEditor.putString("bootstrap_ip", bootstrapIP);
            vinSharedPrefEditor.apply();

            // Set Bootstrap IP TextView
            bootstrapIpTextView.setText("Bootstrap IP: " + bootstrapIP);

            log.append("\nBootstrap IP format Ok");
        }

    }


//    public static String getIpAddress(Context context) {
//        WifiManager wifiManager = (WifiManager) context.getApplicationContext()
//                .getSystemService(WIFI_SERVICE);
//
//        String ipAddress = intToInetAddress(wifiManager.getDhcpInfo().ipAddress).toString();
//
//        ipAddress = ipAddress.substring(1);
//
//        return ipAddress;
//    }

    public static InetAddress intToInetAddress(int hostAddress) {
        byte[] addressBytes = { (byte)(0xff & hostAddress),
                (byte)(0xff & (hostAddress >> 8)),
                (byte)(0xff & (hostAddress >> 16)),
                (byte)(0xff & (hostAddress >> 24)) };

        try {
            return InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
            throw new AssertionError();
        }
    }


}