package com.virgilsystems.qtoken;

import android.os.AsyncTask;
import android.util.Log;
import com.atakmap.android.chat.ChatManagerMapComponent;


public class RunAsyncTask extends AsyncTask<String, Integer, Boolean> {

    public static boolean waiting = false;

    public static boolean success = false;

    @Override
    protected Boolean doInBackground(String... params) {
        waiting = true;

        String bootstrapIp = params[0];
        String nodePort = params[1];
        String receiptPort = params[2];
        String rootFolder = params[3];

        Log.d("### VIN","RunAsyncTask: doInBackground");

        int result = QToken.run(bootstrapIp, nodePort, receiptPort, rootFolder);
        return result == 0;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        waiting = false;

        Log.d("### VIN","RunAsyncTask: onPostExecute 1 " + result);

        success = result;
    }
}