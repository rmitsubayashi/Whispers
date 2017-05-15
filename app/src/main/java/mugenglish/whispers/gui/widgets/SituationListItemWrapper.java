package mugenglish.whispers.gui.widgets;

public class SituationListItemWrapper {
    private String title;
    private String situationID;
    private String image;

    public SituationListItemWrapper(String title, String situationID, String image) {
        this.title = title;
        this.situationID = situationID;
        this.image = image;
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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
