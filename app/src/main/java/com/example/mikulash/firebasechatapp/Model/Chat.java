package com.example.mikulash.firebasechatapp.Model;

import java.util.Date;

public class Chat {

    private String from;
    private String to;
    private String message;
    private Date dateTime;

    public Chat(String from, String to, String message, Date dateTime) {
        this.from = from;
        this.to = to;
        this.message = message;
        this.dateTime = dateTime;
    }

    public Chat() {
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getMessage() {
        return message;
    }

    public Date getDateTime() {
        return dateTime;
    }

    public void setDateTime(Date dateTime) {
        this.dateTime = dateTime;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
