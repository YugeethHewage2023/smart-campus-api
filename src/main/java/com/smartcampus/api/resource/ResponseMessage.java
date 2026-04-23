package com.smartcampus.api.resource;

public class ResponseMessage {
    private String message;

    public ResponseMessage() {
    }

    public ResponseMessage(String message) {
        this.message = message;
    }

    public static ResponseMessage of(String message) {
        return new ResponseMessage(message);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
