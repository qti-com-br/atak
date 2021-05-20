package com.virgilsystems.qtoken;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import com.atakmap.android.chat.ChatManagerMapComponent;
import com.atakmap.android.cot.importer.CotImporterManager;
import com.atakmap.app.ATAKActivity;
import com.atakmap.app.R;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

import static com.atakmap.app.ATAKActivity.vinSharedPref;
import static android.content.Context.MODE_PRIVATE;

public class GetAsyncTask extends AsyncTask<String, Integer, String> {

    ChatManagerMapComponent chat = new ChatManagerMapComponent();

    SharedPreferences.Editor vinSharedPrefEditor = vinSharedPref.edit();

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

        // Cot or Shape
        if(result.contains("<?xml")) {
            CotImporterManager cotImporterManager = CotImporterManager.getInstance();
            CotEvent cotEvent = CotEvent.parse(result);

            List<CotDetail> cotDetails = cotEvent.getDetail().getChildrenByName("shape");

            Log.d("### VIN", "GetAsyncTask: onPostExecute shape 1 | "+ cotDetails.toString());

            // Cot
            if(cotDetails.isEmpty()) {
                // Is Cot
            }
            // Shape
            else {
                Log.d("##### VIN", "GetAsyncTask: onPostExecute shape 2 | "+ cotEvent.getUID());

                if(!ATAKActivity.vinShapes.contains(cotEvent.getUID())) {
                    ATAKActivity.vinShapes.add(cotEvent.getUID());
                    cotImporterManager.processImportDataVIN(cotEvent, new Bundle(), true);
                }
            }


        }
        // Chat
        else {
            result = result.replace("Bundle[{", "");
            result = result.replace("}]", "");

            //Log.d("### VIN", "GetAsyncTask: onPostExecute 1 | " + result );

            String[] parts = result.split(", "); // escape \\|

            if(parts.length < 10) {
                Log.d("### VIN", "GetAsyncTask: onPostExecute Chat | Wrong array size");
                return;
            }

            processChat(parts);
        }
    }


    private void processChat(String[] parts) {

        //Log.d("### VIN", "GetAsyncTask: onPostExecute 3 | " + Arrays.toString(parts));

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
        //Log.d("### VIN", "GetAsyncTask: onPostExecute 3.1 | " + destinationsStr);
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

        //Log.d("### VIN", "GetAsyncTask: onPostExecute 4 | " + bundle);

        //Log.d("### VIN", "GetAsyncTask: onPostExecute 5 | " + VINBridgeCPP.lastChatMessageId + "|" + messageId);

        if(!senderCallsign.equals(ChatManagerMapComponent.mysenderCallsign)) {

            Log.d("### VIN", "GetAsyncTask: onPostExecute 6 | " + message);

            //Log.d("### VIN", "ChatAsyncTask: onPostExecute 4 | " + message);

            chat.addMessageToConversation(bundle);
            chat.vinNotifyUser(bundle);

//            Intent intent = new Intent(PLUGIN_SEND_MESSAGE_ALL_CHAT_ROOMS);
//            intent.putExtra(PLUGIN_SEND_MESSAGE_EXTRA, result);
//            AtakBroadcast.getInstance().sendBroadcast(intent);
        }

    }
}

