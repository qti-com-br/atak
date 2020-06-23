
package com.atakmap.android.chat;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.contact.ContactPresenceDropdown;
import android.os.Bundle;

import com.atakmap.android.contact.Contact;
import com.atakmap.android.contact.Contacts;
import com.atakmap.android.contact.GroupContact;
import com.atakmap.android.contact.IndividualContact;
import com.atakmap.android.contact.TadilJContact;
import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.app.R;
import com.atakmap.comms.CotServiceRemote;
import com.atakmap.comms.CotServiceRemote.ConnectionListener;
import com.atakmap.comms.CotServiceRemote.CotEventListener;
import com.atakmap.comms.NetConnectString;
import com.atakmap.comms.NetworkUtils;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class GeoChatService implements
        CotEventListener,
        ConnectionListener {

    private static final Object lock = new Object();

    public static final String TAG = "GeoChatService";

    public static String DEFAULT_CHATROOM_NAME;

    static final String HISTORY_UPDATE = "com.atakmap.android.chat.HISTORY_UPDATE";

    private static String storedStreamingContactEndpoint = null;

    // Connectivity to CotService
    private CotServiceRemote cotRemote;

    // DB
    private final ChatDatabase chatDb;

    private static GeoChatService _instance;

    private final MapView _mapView;
    private final Context _context;
    private final SharedPreferences _prefs;

    private GeoChatService() {
        _mapView = MapView.getMapView();
        _context = _mapView.getContext();
        DEFAULT_CHATROOM_NAME = _context.getString(R.string.all_chat_rooms);
        chatDb = ChatDatabase.getInstance(_context);

        _prefs = PreferenceManager.getDefaultSharedPreferences(_context);
        cotRemote = new CotServiceRemote();
        cotRemote.setCotEventListener(this);
        cotRemote.connect(this);
    }

    public synchronized static GeoChatService getInstance() {
        if (_instance == null)
            _instance = new GeoChatService();
        return _instance;
    }

    @Override
    public void onCotEvent(CotEvent cotEvent, Bundle bundle) {
        if (isGeoChat(cotEvent)) {
            //Log.d(TAG, "CoT Event with Bundle: " + bundle);

            try {

                ChatMessageParser parser = new ChatMessageParser(_mapView);
                parser.parseCotEvent(cotEvent);
                bundle.putAll(parser.getBundle());
                String senderUid = bundle.getString("senderUid");

                if (bundle.getBoolean("tadilj")) {
                    // Ignore TADIL-J messages that weren't sent to us
                    if (senderUid == null)
                        return;
                    // Otherwise create one-way contact
                    String convName = bundle.getString("conversationName");
                    String convId = bundle.getString("conversationId");
                    Contact tc = Contacts.getInstance()
                            .getContactByUuid(convId);
                    Contact group = Contacts.getInstance()
                            .getContactByUuid("TadilJGroup");
                    if (tc == null && group instanceof GroupContact) {
                        tc = new TadilJContact(convName, convId);
                        tc.getExtras().putBoolean("editable", false);
                        Contacts.getInstance().addContact((GroupContact) group,
                                tc);
                    }
                }

                if (senderUid == null
                        || senderUid.isEmpty()
                        || Contacts.getInstance()
                                .getContactByUuid(senderUid) == null) {
                    //Sender isn't publishing their SA message
                    String senderString = bundle.getString("from");
                    String senderIp = null;
                    /*int senderPort = -1;*/
                    if (senderString != null && !senderString.isEmpty()) {
                        String[] parts = senderString.split(":");
                        senderIp = parts[0];
                        /*try {
                            senderPort = Integer.parseInt(parts[1]);
                        } catch (Exception e) {
                            Log.w(TAG, "Couldn't parse port!");
                        }*/
                    }

                    else
                        Log.e(TAG,
                                "Recieved message from unknown sender.  Possibly through the TAK server");
                    CotDetail detail = cotEvent.getDetail();
                    if (senderUid == null) {
                        for (int i = 0; i < detail.childCount(); i++) {
                            CotDetail child = detail.getChild(i);
                            if (child.getElementName().equals("link")) { //Find the link element
                                if (child.getAttribute("relation")
                                        .equals("p-p")) {
                                    senderUid = child.getAttribute("uid"); //And grab the UID
                                    break;
                                }
                            }
                        }
                    }
                    if (senderIp != null && senderUid != null
                            && !senderUid.equals(MapView.getDeviceUid())) {
                        String senderCallsign = bundle
                                .getString("senderCallsign");
                        String senderName = bundle
                                .getString("senderName");

                        if (FileSystemUtils.isEmpty(senderCallsign)
                                && senderName != null) {
                            senderCallsign = senderName;
                        }
                        if (FileSystemUtils.isEmpty(senderCallsign)) {
                            senderCallsign = senderUid;
                        }

                        if (senderUid.isEmpty()) {
                            senderUid = bundle.getString("senderCallsign");
                        }

                        IndividualContact sender;

                        if (senderIp.startsWith("224")) {
                            int port = 17012;
                            sender = new IndividualContact(
                                    senderCallsign,
                                    senderUid,
                                    new NetConnectString("udp", senderIp,
                                            port));
                        } else {
                            int port = 4242;
                            sender = new IndividualContact(
                                    senderCallsign,
                                    senderUid,
                                    new NetConnectString("tcp", senderIp,
                                            port));
                        }
                        Contacts.getInstance().addContact(sender);
                        //Make sure the chats go to the right place
                        bundle.putString("senderUid", senderUid);
                        String convId = bundle.getString("conversationId");
                        if (convId == null || convId
                                .equals(MapView.getDeviceUid()))
                            bundle.putString("conversationId", senderUid);
                    }
                }

                //Log.d(TAG, "Persist Chat message: " + chatMessageBundle);
                chatDb.addChat(bundle);
                sendToUiLayer(bundle);

                //Log.d(TAG, "bundle contents\n" + bundle);

            } catch (InvalidChatMessageException e) {
                Log.e(TAG, "Couldn't derive chat event from CoT: " + cotEvent,
                        e);
            }
        }
    }

    private void sendToUiLayer(Bundle chatMessageBundle) {
        //Log.d(TAG, "Sending Chat message to UI layer: " + chatMessageBundle);
        Intent gotNewChat = new Intent();
        gotNewChat.setAction("com.atakmap.android.chat.NEW_CHAT_MESSAGE");
        gotNewChat.putExtra("id", chatMessageBundle.getLong("id"));
        gotNewChat.putExtra("groupId", chatMessageBundle.getLong("groupId"));
        gotNewChat.putExtra("conversationId",
                chatMessageBundle.getString("conversationId"));
        AtakBroadcast.getInstance().sendBroadcast(gotNewChat);

        // Refresh chat drop-down (if it's open)
        AtakBroadcast.getInstance().sendBroadcast(new Intent(
                ContactPresenceDropdown.REFRESH_LIST));
    }

    /**
     * @return CotEvent from the info in chatMessage Bundle
     * @param chatMessage conversationId -> String (reference this to send messages) messageId ->
     *            String (uuid for this message) protocol -> String (e.g., xmpp, geochat, etc.) type
     *            -> String (relevant info about the message type) conversationName -> String
     *            (display name for conversation) receiveTime -> Long (time message was received)
     *            senderName -> String (display name for sender) message -> String (text sent as
     *            message) callsign -> String (this device's callsign) deviceType -> String (this
     *            device's CoT type) uid -> String (this device's CoT uid)
     */
    private CotEvent bundleToCot(Bundle chatMessage) {
        //Log.d(TAG, "Converting Bundle to CoT Event.");

        CotEvent cotEvent;

        String from = chatMessage.getString("senderUid");
        if (from == null)
            from = "Android";
        String fromCallsign = chatMessage.getString("senderCallsign");
        if (fromCallsign == null)
            fromCallsign = from;
        String type = chatMessage.getString("deviceType");
        if (type == null)
            type = "a-f";
        String uid = chatMessage.getString("uid");
        if (uid == null)
            uid = "Android";

        String connectionSettings = NetworkUtils.getIP() + ":4242:tcp:" + from;
        String room = chatMessage.getString("conversationName");
        String id = chatMessage.getString("conversationId");
        if (room == null)
            room = DEFAULT_CHATROOM_NAME;
        if (id == null)
            id = UUID.randomUUID().toString();
        String message = chatMessage.getString("message");
        if (message == null)
            message = "";
        String messageId = chatMessage.getString("messageId");
        if (messageId == null)
            messageId = UUID.randomUUID().toString();

        cotEvent = new CotEvent();

        cotEvent.setType("b-t-f");
        cotEvent.setUID("GeoChat." + from + "." + room + "." + messageId);

        CoordinatedTime time = new CoordinatedTime();
        cotEvent.setTime(time);
        cotEvent.setStart(time);
        cotEvent.setStale(time.addDays(1));

        cotEvent.setVersion("2.0");
        cotEvent.setHow("h-g-i-g-o");
        if (_prefs.getBoolean("dispatchLocationCotExternal", true)) {
            if (_prefs.getBoolean("dispatchLocationHidden", false)) {
                cotEvent.setPoint(CotPoint.ZERO);
            } else {
                cotEvent.setPoint(new CotPoint(_mapView.getSelfMarker()
                        .getPoint()));
            }

        } else {
            cotEvent.setPoint(CotPoint.ZERO);
        }

        CotDetail detail = new CotDetail("detail");
        cotEvent.setDetail(detail);

        CotDetail __chat = new CotDetail("__chat");
        __chat.setAttribute("id", id);

        String[] dests = chatMessage.getStringArray("destinations");
        if (dests == null) {
            dests = new String[] {};
            chatMessage.putStringArray("destinations", dests);
        }

        // Legacy chat group info - read from hierarchy node when possible
        CotDetail __chatContact = new CotDetail("chatgrp");
        __chatContact.setAttribute("id", id);
        int i = 0;
        __chatContact.setAttribute("uid" + i++,
                _mapView.getSelfMarker().getUID());
        for (String dest : dests)
            __chatContact.setAttribute("uid" + i++, dest);

        __chat.setAttribute("senderCallsign", fromCallsign);
        __chat.setAttribute("chatroom", room);
        __chat.setAttribute("parent", chatMessage.getString("parent", null));
        __chat.setAttribute("groupOwner", String.valueOf(
                chatMessage.getBoolean("groupOwner", false)));
        String deleteUID = chatMessage.getString("deleteChild", null);
        if (deleteUID != null)
            __chat.setAttribute("deleteChild", deleteUID);
        boolean tadilJ = chatMessage.getBoolean("tadilj", false);
        if (tadilJ)
            __chat.setAttribute("tadilj", "true");
        __chat.addChild(__chatContact);

        Bundle pathsBundle = chatMessage.getBundle("paths");
        if (pathsBundle != null && !pathsBundle.isEmpty()) {
            CotDetail hierarchy = new CotDetail("hierarchy");
            hierarchy.addChild(parseHierarchy(null, pathsBundle));
            __chat.addChild(hierarchy);
        }

        detail.addChild(__chat);

        CotDetail link = new CotDetail("link");
        link.setAttribute("uid", uid);
        link.setAttribute("type", type);
        link.setAttribute("relation", "p-p");
        detail.addChild(link);

        CotDetail remarks = new CotDetail("remarks");
        remarks.setAttribute("source", "BAO.F.ATAK." + from);
        for (String dest : dests) {
            if (!dest.equals(MapView.getDeviceUid())) {
                if (dest.equals(id))
                    remarks.setAttribute("to", dest);
                else {
                    remarks.removeAttribute("to");
                    break;
                }
            }
        }
        remarks.setAttribute("time", time.toString());
        remarks.setInnerText(message);
        detail.addChild(remarks);

        CotDetail __serverdestination = new CotDetail("__serverdestination");
        __serverdestination.setAttribute("destinations", connectionSettings);
        detail.addChild(__serverdestination);

        //Log.d(TAG, "chat: " + cotEvent);

        return cotEvent;
    }

    /**
     * @return true if the CoT event is a GeoChat message
     */
    private static boolean isGeoChat(CotEvent cotEvent) {
        return cotEvent != null
                && cotEvent.isValid()
                && cotEvent.getDetail() != null
                // (KYLE) non-ATAK devices don't necessarily specify this... looking at cot type
                // instead (Aug 1, 2014)
                // && cotEvent.getDetail().getFirstChildByName(0, "__chat") != null)
                && cotEvent.getType().startsWith("b-t-f");
    }

    /**
     * only allowed to be called by the MapComponent.  Marking package private.
     */
    void dispose() {
        //Log.d(TAG, "onDestroy()");
        setStreamingContactEndpoint(null);
        cotRemote.disconnect();

        if (chatDb != null) //should never happen, but let's be safe
            chatDb.close();
    }

    @Override
    public void onCotServiceConnected(Bundle fullServiceState) {
        Log.d(TAG, "Connected to CotService.");

        setStreamingContactEndpoint(storedStreamingContactEndpoint);
    }

    @Override
    public void onCotServiceDisconnected() {
        //Log.d(TAG, "Disconnected to CotService.");
    }

    private void setStreamingContactEndpoint(String endpoint) {
        synchronized (lock) {
            if (cotRemote != null) {
                if (storedStreamingContactEndpoint != null) {
                    cotRemote.removeInput(storedStreamingContactEndpoint);
                    cotRemote.removeOutput(storedStreamingContactEndpoint);
                }
                if (endpoint != null) {
                    Bundle geoChatMulticastInputChannel = new Bundle();
                    geoChatMulticastInputChannel.putString("description",
                            "Chat input");
                    Bundle geoChatMulticastOutputChannel = new Bundle();
                    geoChatMulticastOutputChannel.putString("description",
                            "Chat output");
                    cotRemote.addInput(endpoint,
                            geoChatMulticastInputChannel);
                    // The chat service manages this output - do not persist it
                    geoChatMulticastOutputChannel.putBoolean("noPersist", true);
                    geoChatMulticastOutputChannel.putBoolean("isChat", true);
                    cotRemote
                            .addOutput(endpoint, geoChatMulticastOutputChannel);
                }
            }
            storedStreamingContactEndpoint = endpoint;
        }
    }

    /** 
     * Given a cot message bundle and a destination, send the message out to
     * to the appropriate destination.
     * @param cotMessage, the chat message in a bundle form.
     * @param destination individual contact destination to send the chat message to.
     */
    public void sendMessage(Bundle cotMessage, IndividualContact destination) {
        if (cotMessage != null && destination != null) {
            CotEvent cotEvent = bundleToCot(cotMessage);
            if (cotEvent != null) {
                // Special case out all chat destination
                if (destination.getExtras().getBoolean("fakeGroup", false)) {
                    Log.d(TAG, "Broadcasting message: " + cotMessage);
                    CotMapComponent.getExternalDispatcher()
                            .dispatchToBroadcast(cotEvent);

                } else if (destination instanceof TadilJContact) {
                    //Log.d(TAG, "Sending TADIL-J message: " + cotMessage
                    //        + " to " + destination.getUID() + " callsign "
                    //        + destination.getName());
                    CotMapComponent.getExternalDispatcher()
                            .dispatchToContact(cotEvent, null);
                } else {
                    //Log.d(TAG, "Sending message: " + cotMessage + " to "
                    //        + destination.getUID() + " callsign "
                    //        + destination.getName());
                    CotMapComponent.getExternalDispatcher()
                            .dispatchToContact(
                                    cotEvent, destination);
                }
            } else {
                Log.w(TAG, "Could not create CotEvent from Bundle: "
                        + cotMessage);
            }
        } else {
            Log.w(TAG,
                    "CoT Dispatcher, Destination, or Message is NULL.  Cannot send GeoChat message: "
                            + cotMessage + " to " + destination);
        }
    }

    public Bundle getMessage(long rowIdOfMessage, long rowIdOfGroup) {

        Log.d(TAG, "rowIdOfMessage: " + rowIdOfMessage + " rowIdOfGroup: "
                + rowIdOfMessage);
        Bundle bundle = new Bundle();
        if (rowIdOfMessage != -1) {
            final Bundle b = chatDb.getMessage(rowIdOfMessage,
                    ChatDatabase.TABLE_CHAT);
            if (b != null)
                bundle.putAll(b);

            String convId = bundle.getString("conversationId");
            if (convId != null && (convId
                    .equals(_context.getString(R.string.all_chat_rooms))
                    || convId.equals(_context.getString(R.string.all_streaming))
                    || convId.equals(ChatManagerMapComponent.getTeamName())
                    || convId.equals(ChatManagerMapComponent.getTeamName())))
                bundle.putString("conversationName", convId);
            else {
                Contact sender = Contacts.getInstance().getContactByUuid(
                        bundle.getString("senderUid"));
                if (sender != null)
                    bundle.putString("conversationName", sender.getName());
            }
        }
        if (rowIdOfGroup != -1) {
            final Bundle b = chatDb.getMessage(rowIdOfGroup,
                    ChatDatabase.TABLE_GROUPS);
            if (b != null)
                bundle.putAll(b);
        }
        return bundle;
    }

    List<Bundle> getHistory(String conversationName) {
        return chatDb.getHistory(conversationName);
    }

    List<String> getPersistedConversationIds() {
        return chatDb.getPersistedConversationIds();
    }

    List<String> getGroupInfo(String groupName) {
        return chatDb.getGroupInfo(groupName);
    }

    public List<String> persistMessage(Bundle cotMessage) {
        ArrayList<String> ret = new ArrayList<>();

        //Log.d(TAG, "Persist Chat message: " + chatMessageBundle);
        List<Long> ids = chatDb.addChat(cotMessage);
        for (Long id : ids) {
            ret.add(Long.toString(id));
        }

        return ret;
    }

    void clearMessageDB() {
        chatDb.clearAll();
    }

    private void clearOlderThan(long time) {
        chatDb.clearOlderThan(time);
    }

    public void exportHistory(String filename) {
        chatDb.exportHistory(filename);
    }

    void setAllChatEndpoint(String endpoint) {
        setStreamingContactEndpoint(endpoint);
    }

    private static CotDetail parseHierarchy(Contact contact, Bundle hier) {
        Contacts cts = Contacts.getInstance();
        if (contact == null)
            contact = cts.getContactByUuid(Contacts.USER_GROUPS);
        CotDetail cotHier = new CotDetail(
                contact instanceof GroupContact ? "group" : "contact");
        cotHier.setAttribute("uid", contact.getUID());
        cotHier.setAttribute("name", contact.getName());
        if (hier == null)
            return cotHier;
        for (String key : hier.keySet()) {
            Object child = hier.get(key);
            if (!(child instanceof Bundle))
                continue;
            Bundle cBundle = (Bundle) child;
            String cUID = cBundle.getString("uid");
            Contact c = cts.getContactByUuid(cUID);
            if (c == null) {
                if (cUID != null && cUID.equals(MapView.getDeviceUid()))
                    c = CotMapComponent.getInstance().getSelfContact(false);
                else
                    continue;
            }
            CotDetail childHier = parseHierarchy(c, cBundle);
            if (c != contact)
                cotHier.addChild(childHier);
            else
                cotHier = childHier;
        }
        return cotHier;
    }
}
