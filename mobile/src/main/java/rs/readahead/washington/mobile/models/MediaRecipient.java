package rs.readahead.washington.mobile.models;

import java.io.Serializable;


public class MediaRecipient implements Serializable {
    private long id;
    private String title;
    private String mail;

    public MediaRecipient(String title, String mail) {
        this.mail = mail;
        this.title = title;
    }

    public MediaRecipient() {
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
