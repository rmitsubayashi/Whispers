package com.linnca.whispers.gui.widgets;

public interface ActionAfterEndListener {
    void saveData(String audioFileName, String answer);
    void redirectUser();
}
