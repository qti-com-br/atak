package com.virgilsystems.qtoken;


public class QToken {

    static {
        System.loadLibrary("gflags_nothreads");

        System.loadLibrary("crypto");
        System.loadLibrary("ssl");

        System.loadLibrary("config++");

        System.loadLibrary("qtoken");

        System.loadLibrary("vin-bridge");
    }

    public static native void run(String bootstrapIp, String rootFolder);

    public static native void put(String message);

    public static native void get();

    public static native void share(String filePath);

    public static native void spread(String filePath);

    public static native void gather();

}

