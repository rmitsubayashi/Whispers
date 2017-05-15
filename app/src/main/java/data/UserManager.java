package data;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UserManager {
    public static long CREDIT_REWARD_FOR_CORRECT_ANSWER = 25;
    public static long CREDIT_REWARD_FOR_TEACHING = 75;
    public static long CREDIT_NEEDED_FOR_LEARNING = 100;

    public static void addCredit(String userID, final long value){
        final DatabaseReference creditRef = FirebaseDatabase.getInstance().getReference(
                FirebaseDBHeaders.USER + "/" +
                userID + "/" +
                FirebaseDBHeaders.USER_ID_CREDITS
        );

        creditRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long creditValue = dataSnapshot.getValue(long.class);
                long newCreditValue = value + creditValue;
                creditRef.setValue(newCreditValue);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public static void removeCredit(String userID, final long value){
        final DatabaseReference creditRef = FirebaseDatabase.getInstance().getReference(
                FirebaseDBHeaders.USER + "/" +
                        userID + "/" +
                        FirebaseDBHeaders.USER_ID_CREDITS
        );

        creditRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long creditValue = dataSnapshot.getValue(long.class);
                //no negative values
                long newCreditValue = (creditValue - value > 0) ? creditValue - value : 0;
                creditRef.setValue(newCreditValue);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //this can be called when there is a new chat
    //or a progression in the chain.
    //there is a different 'new notification' indicator
    //for each minimum chain of the user
    public static void notifyUser(String... userIDs){
        for (String userID : userIDs) {
            DatabaseReference notificationRef = FirebaseDatabase.getInstance().getReference(
                    FirebaseDBHeaders.USER + "/" +
                            userID + "/" +
                            FirebaseDBHeaders.USER_ID_NEW_NOTIFICATION
            );
            //the value doesn't matter, as long as it's true
            notificationRef.setValue(true);
        }
    }


}
