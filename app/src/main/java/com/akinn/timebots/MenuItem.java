package com.akinn.timebots;

/**
 * Created by ratta on 10/25/2017.
 */

public class MenuItem {

    String mId;
    String mText;
    int mIconResId;

    public MenuItem(String id, String text, int icon) {
        this.mId = id;
        this.mText = text;
        this.mIconResId = icon;
    }

    public String getText() {
        return this.mText;
    }

    public int getIconResourceId() {
        return this.mIconResId;
    }

    public String getId() { return this.mId; }
}
