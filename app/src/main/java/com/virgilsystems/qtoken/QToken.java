package com.virgilsystems.qtoken;


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

    public static native void share(String filePath);

    public static native void spread(String filePath);

    public static native void gather();

}

