package com.linnca.whispers.data.datawrappers;

public class UserAddedToChain {
    private String userID;
    //private String username;
    private String dateTimeAdded;
    //this is also referenced on FireBaseDBHeaders
    private String chatID;
    private long notificationType;
    private long visibility;

    public UserAddedToChain(){}

    public UserAddedToChain(String userID, String dateTimeAdded, String chatID, long notificationType, long visibility) {
        this.userID = userID;
        this.dateTimeAdded = dateTimeAdded;
        this.chatID = chatID;
        this.notificationType = notificationType;
        this.visibility = visibility;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getDateTimeAdded() {
        return dateTimeAdded;
    }

    public void setDateTimeAdded(String dateTimeAdded) {
        this.dateTimeAdded = dateTimeAdded;
    }

    public String getChatID() {
        return chatID;
    }

    public void setChatID(String chatID) {
        this.chatID = chatID;
    }

    public long getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(long notificationType) {
        this.notificationType = notificationType;
    }

    public long getVisibility(){ return visibility; }

    public void setVisibility(long visibility){
        this.visibility = visibility;
    }
}
