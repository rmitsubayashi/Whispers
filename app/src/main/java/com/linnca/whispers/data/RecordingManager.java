package com.linnca.whispers.data;

import android.content.Context;
import android.net.Uri;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;

public class RecordingManager {
    public interface OnSaveRecordingListener{
        void onSaveRecording();
    }

    public static void uploadFromInternalStorage(final Context context, String fileName, final OnSaveRecordingListener listener){
        StorageReference ref = FirebaseStorage.getInstance().getReference(
                FirebaseDBHeaders.STORAGE_RECORDINGS + "/" +
                        fileName
        );
        final File recordingFile = context.getFileStreamPath(fileName);
        Uri recordingUri = Uri.fromFile(recordingFile);
        UploadTask task = ref.putFile(recordingUri);
        task.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                listener.onSaveRecording();
            }
        });
    }

    public static void writeToInternalStorage(final Context context, byte[] recording, final String saveToFileName, final OnSaveRecordingListener listener){
        try {
            FileOutputStream outputStream = context.openFileOutput(saveToFileName, Context.MODE_PRIVATE);
            outputStream.write(recording);
            outputStream.close();

            if (listener != null){
                listener.onSaveRecording();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void saveRecordingToInternalStorage(final Context context, String storageFileName, final String saveToFileName, final OnSaveRecordingListener listener){
        StorageReference recordingRef = FirebaseStorage.getInstance().getReference(
                FirebaseDBHeaders.STORAGE_RECORDINGS + "/" +
                        storageFileName
        );
        final long ONE_MEGABYTE = 1024 * 1024;
        recordingRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                FileOutputStream outputStream;
                try {
                    outputStream = context.openFileOutput(saveToFileName, Context.MODE_PRIVATE);
                    outputStream.write(bytes);
                    outputStream.close();

                    if (listener != null){
                        listener.onSaveRecording();
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }

            }
        });
    }

    public static boolean removeRecordingFromInternalStorage(Context context, String fileName){
        if (fileName == null)
            return false;
        File fileToRemove = context.getFileStreamPath(fileName);
        return fileToRemove.delete();
    }

    public static String getInternalStorageFilePath(Context context, String fileName){
        return context.getFileStreamPath(fileName).getAbsolutePath();
    }

}
