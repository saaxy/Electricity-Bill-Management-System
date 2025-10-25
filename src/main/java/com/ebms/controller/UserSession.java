package com.ebms.controller;

public class UserSession {
    public enum Role { CONSUMER, ADMIN }

    private Role role;
    private Integer consumerId; // present when role == CONSUMER
    private String adminUsername; // present when role == ADMIN

    public UserSession(Role role) {
        this.role = role;
    }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Integer getConsumerId() { return consumerId; }
    public void setConsumerId(Integer consumerId) { this.consumerId = consumerId; }

    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }
}
