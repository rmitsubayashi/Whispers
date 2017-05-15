package data.datawrappers;

import java.io.Serializable;

public class ChainQueue implements Serializable{
    private String chainID;
    private String audioPath;
    //used as the ordering key of a queue
    private String dateTimeAdded;

    public ChainQueue(){}

    public ChainQueue(String chainID, String audioPath, String dateTimeAdded) {
        this.chainID = chainID;
        this.audioPath = audioPath;
        this.dateTimeAdded = dateTimeAdded;
    }

    public String getChainID() {
        return chainID;
    }

    public void setChainID(String chainID) {
        this.chainID = chainID;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public String getDateTimeAdded() {
        return dateTimeAdded;
    }

    public void setDateTimeAdded(String dateAdded) {
        this.dateTimeAdded = dateAdded;
    }
}
