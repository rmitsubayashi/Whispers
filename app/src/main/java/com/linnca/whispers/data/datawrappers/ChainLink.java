package com.linnca.whispers.data.datawrappers;

public class ChainLink {
    private String userID;
    private long linkNumber;
    //optional
    private String audioFileName;
    //optional
    private String answer;

    public ChainLink(){}

    public ChainLink(String userID, long linkNumber, String audioFileName, String answer) {
        this.userID = userID;
        this.linkNumber = linkNumber;
        this.audioFileName = audioFileName;
        this.answer = answer;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public long getLinkNumber() {
        return linkNumber;
    }

    public void setLinkNumber(long linkNumber) {
        this.linkNumber = linkNumber;
    }

    public String getAudioFileName() {
        return audioFileName;
    }

    public void setAudioFileName(String audioFileName) {
        this.audioFileName = audioFileName;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
