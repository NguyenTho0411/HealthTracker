package hcmute.edu.vn.fitnesstracker.model;

public class Media {
    private String type, url, date;

    public Media(String type, String url, String date) {
        this.type = type;
        this.url = url;
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public String getDate() {
        return date;
    }
}