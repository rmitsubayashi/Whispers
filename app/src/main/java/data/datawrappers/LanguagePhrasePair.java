package data.datawrappers;

public class LanguagePhrasePair {
    private String languageCode;
    private String phrase;

    public LanguagePhrasePair(){}

    public LanguagePhrasePair(String languageCode, String phrase) {
        this.languageCode = languageCode;
        this.phrase = phrase;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getPhrase() {
        return phrase;
    }

    public void setPhrase(String phrase) {
        this.phrase = phrase;
    }
}
