package data.datawrappers;

import java.io.Serializable;

//used for the user's list of chains.
//we don't need all the information on the chain
// like chat messages and audio paths

public class MinimalChain implements Serializable{
    private String chainID;
    //this is when this user linked to the chain
    private long linkedLinkNumber;
    private long nextLinkNumber;
    private String situation;
    private String dateTimeLinked;
    //don't need a notification when the user first creates this
    private boolean newNotification = false;

    public MinimalChain(){}

    public MinimalChain(String chainID, long linkedLinkNumber, long nextLinkNumber, String situation, String dateTimeLinked, boolean newNotification) {
        this.chainID = chainID;
        this.linkedLinkNumber = linkedLinkNumber;
        this.nextLinkNumber = nextLinkNumber;
        this.situation = situation;
        this.dateTimeLinked = dateTimeLinked;
        this.newNotification = newNotification;
    }

    public String getChainID() {
        return chainID;
    }

    public void setChainID(String chainID) {
        this.chainID = chainID;
    }

    public long getLinkedLinkNumber() {
        return linkedLinkNumber;
    }

    public void setLinkedLinkNumber(long linkedLinkNumber) {
        this.linkedLinkNumber = linkedLinkNumber;
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

    public boolean isNewNotification() {
        return newNotification;
    }

    public void setNewNotification(boolean newNotification) {
        this.newNotification = newNotification;
    }
}
