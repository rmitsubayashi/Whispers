package com.linnca.whispers.gui.widgets;

public class SituationListItemWrapper {
    private String title;
    private String situationID;

    public SituationListItemWrapper(String title, String situationID) {
        this.title = title;
        this.situationID = situationID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSituationID() {
        return situationID;
    }

    public void setSituationID(String situationID) {
        this.situationID = situationID;
    }

}
