package com.virgilsystems.qtoken;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import com.atakmap.android.chat.ChatManagerMapComponent;
import com.atakmap.app.R;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;

public class GetAsyncTask extends AsyncTask<String, Integer, String> {

    ChatManagerMapComponent chat = new ChatManagerMapComponent();

    @Override
    protected String doInBackground(String... params) {
        VINBridgeCPP.waiting = true;

        String chatKey = params[0];
        //Log.d("### VIN","ChatAsyncTask: GetAsyncTask");

        return QToken.get(chatKey);
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        VINBridgeCPP.waiting = false;

//        try {
//            //result= URLDecoder.decode(result, "utf-8");
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }

//        result = "Bundle[{receiveTime=1620901822693, conversationId=ANDROID-ed9cc914abd42c7e, " +
//                "messageId=f65d9e77-5669-407c-9205-ae274af1520e, protocol=CoT, conversationName=PIGGY, " +
//                "id=4, type=CHAT3, senderUid=ANDROID-ed9cc914abd42c7e, message=at VDO, senderCallsign=PIGGY}]";

        result = result.replace("Bundle[{", "");
        result = result.replace("}]", "");

        Log.d("### VIN", "GetAsyncTask: onPostExecute 1 | " + result );

        String[] parts = result.split(", "); // escape \\|

        if(parts.length < 1) {
            Log.d("### VIN", "GetAsyncTask: onPostExecute 2 | Wrong array size");
            return;
        }

        Log.d("### VIN", "GetAsyncTask: onPostExecute 3 | " + Arrays.toString(parts));

        //  0   conversationId      String
        //  1   messageId           String
        //  2   destinations        String Array
        //  3   parent              String
        //  4   protocol            String
        //  5   conversationName    String
        //  6   id                  Long
        //  7   uid                 String
        //  8   type                String
        //  9   senderUid           String
        // 10   paths               Bundle
        // 11   groupId             Long
        // 12   deviceType          String
        // 13   message             String
        // 14   sentTime            Long
        // 15   senderCallsign      String

//        Long receiveTime        = Long.parseLong(parts[0].split("=")[1], 10);
//        String conversationId   = parts[1].split("=")[1];
//        String messageId        = parts[2].split("=")[1];
//        String protocol         = parts[3].split("=")[1];
//        String conversationName = parts[4].split("=")[1];
//        Long id                 = Long.parseLong(parts[5].split("=")[1],10);
//        String type             = parts[6].split("=")[1];
//        String senderUid        = parts[7].split("=")[1];
//        String message          = parts[8].split("=")[1];
//        String senderCallsign   = parts[9].split("=")[1];

        String conversationId   = parts[0].split("=")[1];
        String messageId        = parts[1].split("=")[1];
        String destinationsStr  = parts[2].split("=")[1]
                .replace("[","").replace("]","");
        Log.d("### VIN", "GetAsyncTask: onPostExecute 3.1 | " + destinationsStr);
        String[] destinations = {destinationsStr};
//        try {
//            JSONArray jsonArray = new JSONArray(destinationsStr);
//            String[] strArr = new String[jsonArray.length()];
//            for (int i = 0; i < jsonArray.length(); i++) {
//                strArr[i] = jsonArray.getString(i);
//            }
//            destinations = strArr;
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
        String parent           = parts[3].split("=")[1];
        String protocol         = parts[4].split("=")[1];
        String conversationName = parts[5].split("=")[1];
        Long id                 = Long.parseLong(parts[6].split("=")[1],10);
        String uid              = parts[7].split("=")[1];
        String type             = parts[8].split("=")[1];
        String senderUid        = parts[9].split("=")[1];
        Bundle paths = new Bundle(); //           = parts[10].split("=")[1] not necessary for now
        Long groupId            = Long.parseLong(parts[11].split("=")[1],10);
        String deviceType       = parts[12].split("=")[1];
        String message          = parts[13].split("=")[1];
        Long sentTime           = Long.parseLong(parts[14].split("=")[1], 10);
        String senderCallsign   = parts[15].split("=")[1];

        Bundle bundle = new Bundle();



        if(conversationId.equals("All Chat Rooms")) {
//            bundle.putString("conversationId", conversationId);
//            bundle.putString("messageId", messageId);
//            bundle.putStringArray("destinations", destinations);
//            bundle.putString("parent", parent);
//            bundle.putString("protocol", protocol);
//            bundle.putString("conversationName", conversationName);
//            bundle.putLong("id", id);
//            bundle.putString("uid", uid);
//            bundle.putString("type", type);
//            bundle.putString("senderUid", senderUid);
//            bundle.putBundle("paths", paths);
//            bundle.putLong("groupId", groupId);
//            bundle.putString("deviceType", deviceType);
//            bundle.putString("message", message);
//            bundle.putLong("sentTime", sentTime);
//            bundle.putString("senderCallsign", senderCallsign);

            bundle.putLong("receiveTime", sentTime + 1);
            bundle.putString("conversationId", conversationId);
            bundle.putString("messageId", messageId);
            bundle.putString("protocol", protocol);
            bundle.putString("conversationName", conversationName);
            bundle.putLong("id", id);
            bundle.putString("type", "CHAT3");
            bundle.putString("senderUid", senderUid);
            bundle.putString("message", message);
            bundle.putString("senderCallsign", senderCallsign);

        } else {
            bundle.putLong("receiveTime", sentTime + 1);
            bundle.putString("conversationId", uid);
            bundle.putString("messageId", messageId);
            bundle.putString("protocol", protocol);
            bundle.putString("conversationName", senderCallsign);
            bundle.putLong("id", id);
            bundle.putString("type", "CHAT3");
            bundle.putString("senderUid", senderUid);
            bundle.putString("message", message);
            bundle.putString("senderCallsign", senderCallsign);
        }

        Log.d("### VIN", "GetAsyncTask: onPostExecute 4 | " + bundle);

        Log.d("### VIN", "GetAsyncTask: onPostExecute 5 | " + VINBridgeCPP.lastChatMessageId + "|" + messageId);

        if(!VINBridgeCPP.lastChatMessageId.equals(messageId) &&
            !senderCallsign.equals(ChatManagerMapComponent.mysenderCallsign)) {

            Log.d("### VIN", "GetAsyncTask: onPostExecute 6 | " + message);

            VINBridgeCPP.lastChatMessageId = messageId;

            //Log.d("### VIN", "ChatAsyncTask: onPostExecute 4 | " + message);

            chat.addMessageToConversation(bundle);
            chat.vinNotifyUser(bundle);

//            Intent intent = new Intent(PLUGIN_SEND_MESSAGE_ALL_CHAT_ROOMS);
//            intent.putExtra(PLUGIN_SEND_MESSAGE_EXTRA, result);
//            AtakBroadcast.getInstance().sendBroadcast(intent);
        }

    }
}

