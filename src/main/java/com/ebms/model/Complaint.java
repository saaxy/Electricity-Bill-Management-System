package com.ebms.model;

public class Complaint {
    private int id;
    private String description;
    private String status; // e.g., OPEN, IN_PROGRESS, RESOLVED, CLOSED

    public Complaint() {}

    public Complaint(String description) {
        this.description = description;
        this.status = "OPEN";
    }

    public Complaint(String description, String status) {
        this.description = description;
        this.status = status;
    }

    public Complaint(int id, String description, String status) {
        this.id = id;
        this.description = description;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
