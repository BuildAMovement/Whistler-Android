package rs.readahead.washington.mobile.domain.entity;

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

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof MediaRecipient)) {
            return false;
        }

        MediaRecipient mediaRecipient = (MediaRecipient) obj;

        return id == mediaRecipient.id ;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id>>>32));
    }
}
