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

public class VINBridgeCPP {

    public static String lastMessage = "";

    public void run(String bootstrapIp, String nodePort, String receiptPort, String rootFolder) {
        new Thread( new Runnable() { @Override public void run() {
            QToken.run(bootstrapIp, nodePort, receiptPort, rootFolder);
        } } ).start();
    }

    public void put(String message) {
        new Thread( new Runnable() { @Override public void run() {
//            ChatAsyncTask.waiting = true;
            QToken.put(message);
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            ChatAsyncTask.waiting = false;
        } } ).start();
    }

    public void get(String key) {
        Log.d("### VIN","VINBridgeCPP: get");
        if(!ChatAsyncTask.waiting)
            new ChatAsyncTask().execute("chat");
    }

    public void share(String filePath) {
        new Thread( new Runnable() { @Override public void run() {
            QToken.share(filePath);
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




