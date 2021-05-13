package com.virgilsystems.qtoken;

import android.os.AsyncTask;

public class PutAsyncTask extends AsyncTask<String, Integer, String> {

    @Override
    protected String doInBackground(String... params) {
        VINBridgeCPP.waiting = true;
        String message = params[0];
        //Log.d("### VIN","ChatAsyncTask: GetAsyncTask");
        QToken.put(message);
        return "";
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        VINBridgeCPP.waiting = false;
    }

}
