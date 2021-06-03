package com.virgilsystems.qtoken;


import android.os.Bundle;
import android.util.Log;

import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.cot.event.CotEvent;

import java.util.Vector;

public class QToken {

    static {
        System.loadLibrary("gflags_nothreads");

        System.loadLibrary("crypto");
        System.loadLibrary("ssl");

        System.loadLibrary("config++");

        System.loadLibrary("qtoken");

        System.loadLibrary("vin-bridge");
    }

    public static native int run(String bootstrapIp, String nodePort,
                                  String receiptPort, String rootFolder);

    public static native void put(String key, String message);

    public static native String get(String key);

    public static native void spread(String filePath);

    public static native void gather();

    public static native void share(byte[] cot, String receiverIP, String receiverReceiptPort);

    public static void shareHandler(String cot) {
        Log.d("### VIN", "QToken.shareHandler 1 | " + cot);

        new Thread( new Runnable() { @Override public void run() {
            Log.d("### VIN", "QToken.shareHandler 2 " + cot);

            CommsMapComponent instance = CommsMapComponent.getInstance();

            CotEvent cotEvent = CotEvent.parse(cot);

            Log.d("### VIN", "QToken.shareHandler 3 | " + cotEvent);

            String endpoint = cotEvent.getDetail().getChild(1).getAttribute("endpoint");

            Log.d("### VIN", "QToken.shareHandler 4 | " + cotEvent + " | " + endpoint);

            instance.cotMessageReceived(cot, endpoint);
        } } ).start();
    }

}

