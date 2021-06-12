package com.virgilsystems.qtoken;


import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.missionpackage.MissionPackageMapComponent;
import com.atakmap.android.missionpackage.MissionPackageReceiver;
import com.atakmap.android.missionpackage.api.MissionPackageApi;
import com.atakmap.android.missionpackage.file.MissionPackageConfiguration;
import com.atakmap.android.missionpackage.file.MissionPackageManifest;
import com.atakmap.app.ATAKActivity;
import com.atakmap.commoncommo.MissionPackageTransferException;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.cot.event.CotEvent;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class QToken {

    static {
        System.loadLibrary("gflags_nothreads");

        System.loadLibrary("crypto");
        System.loadLibrary("ssl");

        System.loadLibrary("config++");

        System.loadLibrary("qtoken");

        System.loadLibrary("qtoken");
    }

    public static native int run(String bootstrapIp, String nodePort,
                                  String receiptPort, String rootFolder);

    public static native void put(String key, String message);

    public static native String get(String key);

    public static native void spread(String filePath);

    public static native void gather();

    public static native void share(byte[] cot, String receiverIP, String receiverReceiptPort);

    public static void shareHandler(byte[] bytes) {

        CommsMapComponent comms = CommsMapComponent.getInstance();

        Log.d("### VIN", "QToken.shareHandler type: " + bytes[0]);

        int type = (int)bytes[0];

        switch(bytes[0]) {

            case VINShareType.BYTE_COT: // CoT

                Log.d("### VIN", "QToken.shareHandler 1.1 ");

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        byte[] cotBytes = Arrays.copyOfRange(bytes,
                                3, bytes.length);

                        String cot = new String(cotBytes, StandardCharsets.UTF_8);

                        Log.d("### VIN", "QToken.shareHandler 2 " + cot);

                        CotEvent cotEvent = CotEvent.parse(cot);

                        Log.d("### VIN", "QToken.shareHandler 3 | " + cotEvent);

                        String endpoint = cotEvent.getDetail().getChild(1).getAttribute("endpoint");

                        Log.d("### VIN", "QToken.shareHandler 4 | " + cotEvent + " | " + endpoint);

                        comms.cotMessageReceived(cot, endpoint);
                    }
                }).start();

                break;

            case VINShareType.BYTE_PKG: // Data Package

                Log.d("### VIN", "QToken.shareHandler 1.2 ");

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        Log.d("### VIN", "QToken.shareHandler 1 | " + bytes.length);

                        try {

                            // Get Path
                            int pathByteLength = bytes[1];
                            byte[] pathBytes = Arrays.copyOfRange(bytes, 2, pathByteLength);
                            String pathString = new String(pathBytes, StandardCharsets.UTF_8);

                            // Get file bytes
                            byte[] zipFileBytes = Arrays.copyOfRange(bytes,
                                    pathByteLength, bytes.length);
                            Log.d("### VIN", "QToken.shareHandler 23 | " + pathByteLength + " | " + pathString);
                            String[] arrPath = pathString.split("/");
                            String hashPath = arrPath[arrPath.length - 2];
                            String fileName = arrPath[arrPath.length - 1];

                            Log.d("### VIN", "QToken.shareHandler 2 | " +
                                    hashPath + " | " + fileName);

                            String transfer = "/storage/emulated/0/atak/tools/datapackage/";

                            File hash = new File(transfer);
                            if (!hash.exists()) {
                                hash.mkdir();
                            }

                            // Create ZIP file
                            File zipFile = new File(transfer, fileName);

                            zipFile.createNewFile(); // if file already exists will do nothing
                            FileOutputStream fos = new FileOutputStream(zipFile, false);
                            fos.write(zipFileBytes);
                            fos.close();

//                            // Process it on ATAK
//                            MissionPackageReceiver mpr =
//                                    new MissionPackageReceiver(MapView.getMapView(),
//                                            MissionPackageMapComponent.getInstance());

//                            MissionPackageManifest manifest = new MissionPackageManifest();
//                            manifest.addFile(zipFile, hashPath);
//                            MissionPackageConfiguration.ImportInstructions inst =
//                                    manifest.getConfiguration().getImportInstructions();
//                            manifest.getConfiguration()

//                            MissionPackageManifest manifest = extras.getParcelable(
//                                    MissionPackageApi.INTENT_EXTRA_MISSIONPACKAGEMANIFEST);

//                            // Run in the UI Thread
//                            new Handler(Looper.getMainLooper()).post(new Runnable() {
//                                @Override
//                                public void run() {
////                                    try {
//                                        mpr.initiateReceive(fileName, fileName.split(".")[0],
//                                                hashPath, zipFileBytes.length, "PIGGY");
//
////                                        comms.mpio.missionPackageReceiveInit(fileName, fileName.split(".")[0],
////                                                hashPath, zipFileBytes.length, "PIGGY");
////                                    } catch (MissionPackageTransferException e) {
////                                        e.printStackTrace();
////                                    }
//                                }
//                            });

                        } catch (Exception e) {
                            Log.e("### VIN", "QToken.shareHandler 4 " + e.getMessage());
                        }

                    }
                }).start();

                break;
        }


    }

}

