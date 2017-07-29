package com.linnca.whispers.data;

import android.content.Context;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import com.linnca.whispers.data.datawrappers.ChainLink;

public class OfflineModeManager {
    public static final String NO_RECORDINGS = "no recordings";
    private static final String recordingFileHeader = "offlineRecording";
    private static final String answerFileHeader = "answer";
    private static final int maxRecordingCount = 1;
    private static final int maxRecordingIndex = maxRecordingCount-1;

    public static String getNextRecordingFileName(Context context){
        for (int i=maxRecordingIndex; i>=0; i--){
            String fileName = getRecordingFileName(i);
            if (fileExists(context, fileName)){
                return fileName;
            }
        }
        
        return NO_RECORDINGS;
    }

    public static boolean removeNextRecordingFile(Context context){
        for (int i=maxRecordingIndex; i>=0; i--){
            String fileName = getRecordingFileName(i);
            if (fileExists(context, fileName)){
                return RecordingManager.removeRecordingFromInternalStorage(context, fileName);
            }
        }

        return false;
    }

    public static boolean removeNextAnswerFile(Context context){
        for (int i=maxRecordingIndex; i>=0; i--){
            String fileName = getAnswerFileName(i);
            if (fileExists(context, fileName)){
                return new File(fileName).delete();
            }
        }

        return false;
    }
    
    public static String getNextAnswer(Context context){
        for (int i=maxRecordingIndex; i>=0; i--){
            String fileName = getAnswerFileName(i);
            if (fileExists(context, fileName)){
                try{
                    FileInputStream fileInputStream = context.openFileInput(fileName);
                    InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
                    StringBuilder builder = new StringBuilder();
                    BufferedReader reader = new BufferedReader(inputStreamReader);
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }

                    return builder.toString();
                }catch(Exception e){
                    e.printStackTrace();
                }

            }
        }

        return null;
    }

    public static void repopulateOfflineRecordings(final Context context, String languageCode){
        int startingIndex = -1;
        for (int i=0; i<maxRecordingCount; i++){
            String fileToSearch = getRecordingFileName(i);
            if (!fileExists(context, fileToSearch)) {
                startingIndex = i;
            }
        }

        //no need to repopulate recordings
        if (startingIndex == -1) {
            return;
        }

        final int toFill = maxRecordingCount - startingIndex;
        getRecordings(context, toFill, languageCode);

    }

    //grab the first recording from a correct chain and add it.
    //we only add the first because the first is the only guaranteed-to-be-correct recording.
    public static void uploadOfflineRecording(final String chainID, final String answer, final String languageCode){
        DatabaseReference chainRef = FirebaseDatabase.getInstance().getReference(
                FirebaseDBHeaders.CHAINS + "/" +
                chainID + "/" +
                FirebaseDBHeaders.CHAINS_ID_LINKS
        );
        chainRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String recordingFileName = "";
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    ChainLink chainLink = snapshot.getValue(ChainLink.class);
                    recordingFileName = chainLink.getAudioFileName();
                    break;
                }

                DatabaseReference offlineRecordingsRef = FirebaseDatabase.getInstance().getReference(
                        FirebaseDBHeaders.OFFLINE_RECORDINGS + "/" +
                                languageCode
                );
                String key = offlineRecordingsRef.push().getKey();
                DatabaseReference offlineRecordingRef = offlineRecordingsRef.child(key);
                String randomSequence = getRandomSequence(new SecureRandom());
                Map newRecordingData = new HashMap();
                //unchecked cast not handled in FireBase blog so no need?
                newRecordingData.put(FirebaseDBHeaders.OFFLINE_RECORDINGS_LANGUAGE_RECORDING_FILE_NAME, recordingFileName);
                newRecordingData.put(FirebaseDBHeaders.OFFLINE_RECORDINGS_LANGUAGE_RECORDING_ANSWER, answer);
                newRecordingData.put(FirebaseDBHeaders.OFFLINE_RECORDINGS_LANGUAGE_RECORDING_RANDOM_SEQUENCE, randomSequence);
                offlineRecordingRef.updateChildren(newRecordingData);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private static boolean fileExists(Context context, String fileName){
            File file = context.getFileStreamPath(fileName);
            return file.exists();
    }

    private static void getRecordings(final Context context, final int toFill, final String languageCode){
        DatabaseReference offlineRecordingsRef = FirebaseDatabase.getInstance().getReference(
                FirebaseDBHeaders.OFFLINE_RECORDINGS + "/" +
                languageCode
        );

        SecureRandom secureRandom = new SecureRandom();
        String randomSequence = getRandomSequence(secureRandom);

        Query offlineRecordingsQuery = offlineRecordingsRef
                .orderByChild(FirebaseDBHeaders.OFFLINE_RECORDINGS_LANGUAGE_RECORDING_RANDOM_SEQUENCE)
                .startAt(randomSequence).limitToFirst(toFill);
        offlineRecordingsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //if the random sequence doesn't get enough recordings, try again
                long fillCount = dataSnapshot.getChildrenCount();
                if (fillCount < toFill){
                    getRecordings(context, toFill, languageCode);
                    return;
                }

                int recordingFileIndex = maxRecordingCount - toFill;
                for (DataSnapshot recordingSnapshot : dataSnapshot.getChildren()){
                    String fileName = recordingSnapshot.child(
                            FirebaseDBHeaders.OFFLINE_RECORDINGS_LANGUAGE_RECORDING_FILE_NAME
                    ).getValue(String.class);
                    String recordingFileName = getRecordingFileName(recordingFileIndex);
                    RecordingManager.saveRecordingToInternalStorage(context, fileName, recordingFileName, null);
                    String answer = recordingSnapshot.child(
                            FirebaseDBHeaders.OFFLINE_RECORDINGS_LANGUAGE_RECORDING_ANSWER
                    ).getValue(String.class);
                    String answerFileName = getAnswerFileName(recordingFileIndex);
                    storeAnswerFile(context, answerFileName, answer);
                    recordingFileIndex++;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //since SecureRandom is expensive to initialize, only initialize once
    private static String getRandomSequence(SecureRandom secureRandom){
        return new BigInteger(130, secureRandom).toString(32);
    }

    private static String getRecordingFileName(int index){
        return recordingFileHeader + index;
    }

    private static String getAnswerFileName(int index){
        return answerFileHeader + index + ".txt";
    }

    private static void storeAnswerFile(Context context, String fileName, String answer) {
        try {
            FileOutputStream outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            outputStreamWriter.write(answer);
            outputStreamWriter.flush();
            outputStreamWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
