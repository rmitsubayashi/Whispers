package com.linnca.whispers.data.datawrappers;

import java.io.Serializable;
import java.util.List;

import com.linnca.whispers.data.ChainManager;

//used for the user's list of chains.
//we don't need all the information on the chain
// like chat messages and audio paths

public class MinimalChain implements Serializable{
    private String id;
    private String chainID;
    //this is when this user linked to the chain
    private List<Long> linkedLinkNumbers;
    private long nextLinkNumber;
    private String situation;
    private String dateTimeLinked;
    //don't need a notification when the user first creates this
    private boolean newNotification = false;
    //shouldn't be hidden at first
    private long visibility = ChainManager.MINIMUM_CHAIN_VISIBILITY_VISIBLE;

    public MinimalChain(){}

    public MinimalChain(String id, String chainID, List<Long> linkedLinkNumbers, long nextLinkNumber, String situation, String dateTimeLinked) {
        this.id = id;
        this.chainID = chainID;
        this.linkedLinkNumbers = linkedLinkNumbers;
        this.nextLinkNumber = nextLinkNumber;
        this.situation = situation;
        this.dateTimeLinked = dateTimeLinked;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChainID() {
        return chainID;
    }

    public void setChainID(String chainID) {
        this.chainID = chainID;
    }

    public List<Long> getLinkedLinkNumbers() {
        return linkedLinkNumbers;
    }

    public void setLinkedLinkNumbers(List<Long> linkedLinkNumbers) {
        this.linkedLinkNumbers = linkedLinkNumbers;
    }

    public long getNextLinkNumber() {
        return nextLinkNumber;
    }

    public void setNextLinkNumber(long nextLinkNumber) {
        this.nextLinkNumber = nextLinkNumber;
    }

    public String getSituation() {
        return situation;
    }

    public void setSituation(String situation) {
        this.situation = situation;
    }

    public String getDateTimeLinked() {
        return dateTimeLinked;
    }

    public void setDateTimeLinked(String dateTimeLinked) {
        this.dateTimeLinked = dateTimeLinked;
    }

    public boolean getNewNotification() {
        return newNotification;
    }

    public void setNewNotification(boolean newNotification) {
        this.newNotification = newNotification;
    }

    public long getVisibility() {
        return visibility;
    }

    public void setVisibility(long visibility) {
        this.visibility = visibility;
    }
}
