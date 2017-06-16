package rs.readahead.washington.mobile.presentation.entity;

import java.io.Serializable;


public class MediaRecipientSelection implements Serializable {
    private int id;
    private String title;
    private boolean checked;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
