package com.virgilsystems.qtoken;


import android.os.Bundle;
import android.util.Log;

import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.cot.event.CotEvent;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

        new Thread( new Runnable() { @Override public void run() {

            String allAsString = new String(bytes, StandardCharsets.UTF_8);

            String[] stringParts = allAsString.split("<VIN>");

            byte[] arr1 = stringParts[0].getBytes(); // transferFilename
            byte[] arr2 = stringParts[1].getBytes(); // transferName
            byte[] arr3 = stringParts[2].getBytes(); // file.getPath()
            byte[] arr4 = stringParts[3].getBytes(); // fileToBytes()


//            byte[] arr1 = getPartOfByteArray(1, bytes); // transferFilename
//            byte[] arr2 = getPartOfByteArray(2, bytes); // transferName
//            byte[] arr3 = getPartOfByteArray(3, bytes); // file.getPath()
//            byte[] arr4 = getPartOfByteArray(4, bytes); // fileToBytes()

            String path = new String(arr3, StandardCharsets.UTF_8);

            String transfer = "/storage/emulated/0/atak/tools/datapackage/transfer/";

            Log.d("### VIN", "QToken.shareHandler 1 | " + arr4.length + " | " + path);

            try {
                String[] arrPath = path.split("/");

//                for (int x=1; x<arrPath.length-1; x++) {
//                    Log.d("### VIN", "QToken.shareHandler 2 | " + arrPath[x]);
//                    File file = new File(arrPath[x]);
//                    if (!file.exists()) {
//                        file.createNewFile();
//                    }
//                }

                String hashPath = transfer + arrPath[arrPath.length-2];

                Log.d("### VIN", "QToken.shareHandler 2 | " + arrPath[arrPath.length-2]);
                File hash = new File(hashPath);
                if (!hash.exists()) {
                    hash.mkdirs();
                }

                // byte[] to file
                Log.d("### VIN", "QToken.shareHandler 3 | " + arrPath[arrPath.length-1]);
//                File file = new File(hashPath + "/" + arrPath[arrPath.length-1]);


//                File file = new File(hashPath + "/", arrPath[arrPath.length-1]);

//                Path path2 = Paths.get("C:\\myfile.pdf");
//                Files.write(path2, bytes);

                ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(arr4));
//                ZipEntry entry = null;
//                while ((entry = zipInputStream.getNextEntry()) != null) {
//
//                    String entryName = entry.getName();
//
//                    FileOutputStream out = new FileOutputStream(entryName);
//
//                    byte[] byteBuff = new byte[arr4.length];
//                    int bytesRead = 0;
//                    while ((bytesRead = zipInputStream.read(byteBuff)) != -1)
//                    {
//                        out.write(byteBuff, 0, bytesRead);
//                    }
//
//                    out.close();
//                    zipInputStream.closeEntry();
//                }
//                zipInputStream.close();

                ZipEntry zipEntry = zipInputStream.getNextEntry();

                byte[] buffer = new byte[arr4.length];
                int readLength;

                while(zipEntry != null){
                    File newFile = new File(hashPath + "/", arrPath[arrPath.length-1]);

                    if (!zipEntry.isDirectory()) {
                        FileOutputStream fos = new FileOutputStream(newFile);
                        while ((readLength = zipInputStream.read(buffer)) > 0) {
                            fos.write(buffer, 0, readLength);
                        }
                        fos.close();
                    } else {
                        newFile.mkdirs();
                    }

                    Log.i("zip file path = ", newFile.getPath());
                    zipInputStream.closeEntry();
                    zipEntry = zipInputStream.getNextEntry();
                }

                zipInputStream.closeEntry();
                zipInputStream.close();


//                if (!file.exists()) {
//                    file.createNewFile();
//                }
//                FileOutputStream fos = new FileOutputStream(file);
//                fos.write(zipStream);
//                fos.close();

            } catch (Exception e) {
                Log.e("### VIN", "QToken.shareHandler 4 " + e.getMessage());
            }

        } } ).start();

//        new Thread( new Runnable() { @Override public void run() {
//            Log.d("### VIN", "QToken.shareHandler 2 " + cot);
//
//            CommsMapComponent instance = CommsMapComponent.getInstance();
//
//            CotEvent cotEvent = CotEvent.parse(cot);
//
//            Log.d("### VIN", "QToken.shareHandler 3 | " + cotEvent);
//
//            String endpoint = cotEvent.getDetail().getChild(1).getAttribute("endpoint");
//
//            Log.d("### VIN", "QToken.shareHandler 4 | " + cotEvent + " | " + endpoint);
//
//            instance.cotMessageReceived(cot, endpoint);
//        } } ).start();
    }

    private static byte[] getPartOfByteArray(int whichOne, byte[] arr) {
        int indexStart = -1;
        int indexEnd = 0;
        int arrCount = 1;
        for(int x=0; x<arr.length; x++) {
            if(arr[x] == '|') {
                arrCount++;
            } else if(whichOne == arrCount && indexStart == -1) {
                indexStart = x;
            } else if(whichOne < arrCount) {
                indexEnd = x -1;
                break;
            }
        }
        return Arrays.copyOfRange(arr, indexStart, indexEnd);
    }

}

