package com.linnca.whispers.data.datawrappers;

import org.joda.time.DateTime;

public class ChatMessage {
    private String message;
    private String userID;
    private String dateTime;
    public static String USER_ID_ADMIN = "admin";

    public ChatMessage(){}

    public ChatMessage(String message, String userID) {
        this.message = message;
        this.userID = userID;
        this.dateTime = DateTime.now().toString();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }
}
