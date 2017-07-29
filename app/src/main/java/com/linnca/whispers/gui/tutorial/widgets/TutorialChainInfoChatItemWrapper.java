package com.linnca.whispers.gui.tutorial.widgets;

public class TutorialChainInfoChatItemWrapper {
    private String message;
    private boolean user;

    public TutorialChainInfoChatItemWrapper(String message, boolean user) {
        this.message = message;
        this.user = user;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isUser() {
        return user;
    }

    public void setUser(boolean user) {
        this.user = user;
    }
}
