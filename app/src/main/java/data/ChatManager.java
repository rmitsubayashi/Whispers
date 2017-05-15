package data;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onesignal.OneSignal;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import data.datawrappers.ChatMessage;

public class ChatManager {
    public static void sendChatMessage(String chainID, final ChatMessage message){
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
                Set<String> chatIDs = new HashSet<>();
                Set<String> userIDs = new HashSet<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Log.d("ChatManager", snapshot.toString());
                    String userID = snapshot.child(FirebaseDBHeaders.CHAINS_ID_USERS_USER_ID)
                            .getValue(String.class);
                    Log.d("ChatManager", "User ID:" + userID);
                    //we don't want to notify the same user that sent the message
                    if (!userID.equals(userFrom)) {
                        String chatID = snapshot.child(FirebaseDBHeaders.CHAINS_ID_USERS_CHAT_ID)
                                .getValue(String.class);
                        Log.d("ChatManager", "ChatID: " + chatID);
                        chatIDs.add(chatID);
                        userIDs.add(userID);
                    }
                }

                //the user might send a message to himself
                //so make sure the size is greater than 0
                if (chatIDs.size() > 0) {
                    String[] chatIDsArray = chatIDs.toArray(new String[chatIDs.size()]);
                    Log.d("ChatManager", Arrays.toString(chatIDsArray));
                    sendNotification(message.getMessage(), chatIDsArray );
                    String[] userIDsArray = userIDs.toArray(new String[userIDs.size()]);
                    UserManager.notifyUser(userIDsArray);
                }

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

    //user id = chat user ids
    static void sendNotification(String notification, String... userIDs){
        String userIDsString = "";
        for (String userID : userIDs){
            userIDsString += "'" + userID + "',";
        }
        //remove last comma
        userIDsString = userIDsString.substring(0, userIDsString.length()-1);

        try {
            OneSignal.postNotification(
                    new JSONObject("{'contents': {'en':'"+notification+"'}, 'include_player_ids': [" + userIDsString + "]}"), null
            );
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
