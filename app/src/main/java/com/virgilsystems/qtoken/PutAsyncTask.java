package com.virgilsystems.qtoken;

import android.os.AsyncTask;

public class PutAsyncTask extends AsyncTask<String, Integer, String> {

    @Override
    protected String doInBackground(String... params) {
        VINBridgeCPP.waiting = true;
        String key = params[0];
        String message = params[1];
        //Log.d("### VIN","ChatAsyncTask: GetAsyncTask");
        QToken.put(key, message);
        return "";
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        VINBridgeCPP.waiting = false;
    }

}
