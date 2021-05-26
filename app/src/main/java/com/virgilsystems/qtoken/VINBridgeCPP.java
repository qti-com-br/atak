/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.virgilsystems.qtoken;


import android.util.Log;

import com.atakmap.app.ATAKActivity;

import static android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE;

public class VINBridgeCPP {

    public static boolean waiting = false;

    public void run(String bootstrapIp, String nodePort, String receiptPort, String rootFolder) {
        Log.d("### VIN","VINBridgeCPP: run");

        Thread run = new Thread( new Runnable() { @Override public void run() {
            QToken.run(bootstrapIp, nodePort, receiptPort, rootFolder);
        } } );
        run.setPriority(Thread.MAX_PRIORITY);
        run.start();
    }

    public void put(String key, String message) {
        Thread put = new Thread( new Runnable() { @Override public void run() {
            QToken.put(key, message);
        } } );
        put.setPriority(Thread.MAX_PRIORITY);
        put.start();
    }

    public void get(String key) {
            if(!waiting)
                new GetAsyncTask().execute(key);
    }

    public void share(byte[] cot, String receiverIP, String receiverReceiptPort) {
        new Thread( new Runnable() { @Override public void run() {
            QToken.share(cot, receiverIP, receiverReceiptPort);
        } } ).start();
    }

    public void spread(String filePath) {
        new Thread( new Runnable() { @Override public void run() {
            QToken.spread(filePath);
        } } ).start();
    }

    public void gather() {
        new Thread( new Runnable() { @Override public void run() {
            QToken.gather();
        } } ).start();
    }
}