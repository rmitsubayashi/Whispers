package com.linnca.whispers.data.datawrappers;

import java.io.Serializable;

public class ChainQueue implements Serializable{
    private String chainID;
    private String audioPath;
    //we don't want to remove anything from the queue until it's finished
    //so indicate whether this item is in the hands of the user or in the queue
    private long inQueue;
    public final static long IN_QUEUE = 0;
    public final static long WITH_USER = 1;

    public ChainQueue(){}

    public ChainQueue(String chainID, String audioPath, long inQueue) {
        this.chainID = chainID;
        this.audioPath = audioPath;
        this.inQueue = inQueue;
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

    public long getInQueue() {
        return inQueue;
    }

    public void setInQueue(long inQueue) {
        this.inQueue = inQueue;
    }
}
