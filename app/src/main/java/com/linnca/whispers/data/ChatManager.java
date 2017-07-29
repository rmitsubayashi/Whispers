package com.linnca.whispers.data;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onesignal.OneSignal;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

import com.linnca.whispers.data.datawrappers.ChatMessage;

public class ChatManager {
    public static final int NOTIFICATION_IDENTIFIER_NEW_MESSAGE = 0;
    public static final int NOTIFICATION_IDENTIFIER_CHAIN_UPDATE = 1;

    //we need a context to grab the string from the resources file so just store them here
    private static final String enNewMessage = "You have a new message!";
    private static final String jaNewMessage = "新しいメッセージです！";
    private static final String enChainUpdate = "Your chain has been updated!";
    private static final String jaChainUpdate = "チェインが更新されました";

    public static void sendChatMessage(final String chainID, final ChatMessage message){
        saveChatMessage(chainID, message);

        final String userFrom = message.getUserID();
        DatabaseReference chainUserRef = FirebaseDatabase.getInstance().getReference(
                FirebaseDBHeaders.CHAINS + "/" +
                chainID + "/" +
                FirebaseDBHeaders.CHAINS_ID_USERS
        );

        chainUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Set<String> pushIDs = new HashSet<>();
                Set<String> notifyIDs = new HashSet<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    String userID = snapshot.child(FirebaseDBHeaders.CHAINS_ID_USERS_USER_ID)
                            .getValue(String.class);
                    String chatID = snapshot.child(FirebaseDBHeaders.CHAINS_ID_USERS_CHAT_ID)
                            .getValue(String.class);
                    long visibility = snapshot.child(FirebaseDBHeaders.CHAINS_ID_USERS_VISIBILITY)
                            .getValue(long.class);
                    long notificationType = snapshot.child(FirebaseDBHeaders.CHAINS_ID_USERS_NOTIFICATION_TYPE)
                            .getValue(long.class);
                    //we don't want to notify the same user that sent the message
                    if (!userID.equals(userFrom)) {
                        if (visibility == ChainManager.MINIMUM_CHAIN_VISIBILITY_VISIBLE ||
                                visibility == ChainManager.MINIMUM_CHAIN_VISIBILITY_HIDDEN) {
                            notifyIDs.add(userID);
                            ChainManager.updateMinimalChain(chainID, userID, ChainManager.MINIMUM_CHAIN_UPDATE_NO_NEW_LINK);

                            if (notificationType == ChainManager.NOTIFICATION_TYPE_ALL ||
                                    notificationType == ChainManager.NOTIFICATION_TYPE_CHAT_ONLY){
                                pushIDs.add(chatID);
                            }
                        }
                    }
                }

                String[] pushIDsArray = pushIDs.toArray(new String[pushIDs.size()]);
                sendNotification(NOTIFICATION_IDENTIFIER_NEW_MESSAGE, message.getMessage(), pushIDsArray );
                String[] notifyIDsArray = notifyIDs.toArray(new String[notifyIDs.size()]);
                UserManager.notifyUserBottomNavigation(notifyIDsArray);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private static void saveChatMessage(String chainID, ChatMessage chatMessage) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference(
                FirebaseDBHeaders.CHAINS + "/" +
                        chainID + "/" +
                        FirebaseDBHeaders.CHAINS_ID_CHAT_MESSAGES
        );

        ref.push().setValue(chatMessage);
    }

    static void sendNotification(int notificationIdentifier, String message, String... chatUserIDs){
        if (chatUserIDs.length == 0)
            return;
        String chatUserIDsString = "";
        for (String chatUserID : chatUserIDs){
            chatUserIDsString += "'" + chatUserID + "',";
        }
        //remove last comma
        chatUserIDsString = chatUserIDsString.substring(0, chatUserIDsString.length()-1);

        String enNotification;
        String jaNotification;

        switch (notificationIdentifier){
            case NOTIFICATION_IDENTIFIER_CHAIN_UPDATE:
                enNotification = enChainUpdate;
                jaNotification = jaChainUpdate;
                break;
            case NOTIFICATION_IDENTIFIER_NEW_MESSAGE:
                enNotification = enNewMessage;
                jaNotification = jaNewMessage;
                break;
            default:
                return;
        }

        try {
            OneSignal.postNotification(
                    new JSONObject("{'contents': {'en':'"+enNotification+"', 'ja':'"+jaNotification+"'}, 'include_player_ids': [" + chatUserIDsString + "]}"), null
            );
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
