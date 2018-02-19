package rs.readahead.washington.mobile.domain.entity;

import java.io.Serializable;


public class MediaRecipientList implements Serializable {

    private int id;
    private String title;

    public MediaRecipientList() {
    }

    public MediaRecipientList(int id, String title) {
        this.id = id;
        this.title = title;
    }

    public MediaRecipientList(String title) {
        this.title = title;
    }

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

    @Override
    public boolean equals(Object obj) {

        if (obj == this) {
            return true;
        }

        if (!(obj instanceof MediaRecipientList)) {
            return false;
        }

        MediaRecipientList mediaRecipientList = (MediaRecipientList) obj;

        return id == mediaRecipientList.id ;
    }
}
