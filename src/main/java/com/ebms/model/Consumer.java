// src/main/java/com/ebms/model/Consumer.java
package com.ebms.model;

public class Consumer {
    private int id;
    private String name;
    private String address;
    private String email;
    private String meterNumber;
    private String phone;
    private java.time.LocalDate birthdate; // optional, defaults to 1999-01-01
    private String passcode; // derived from birthdate as DDMMYY

    // Constructors
    public Consumer() {}
    public Consumer(String name, String address, String email, String meterNumber) {
        this.name = name;
        this.address = address;
        this.email = email;
        this.meterNumber = meterNumber;
    }
    public Consumer(String name, String address, String email, String meterNumber, java.time.LocalDate birthdate, String passcode) {
        this(name, address, email, meterNumber);
        this.birthdate = birthdate;
        this.passcode = passcode;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMeterNumber() { return meterNumber; }
    public void setMeterNumber(String meterNumber) { this.meterNumber = meterNumber; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public java.time.LocalDate getBirthdate() { return birthdate; }
    public void setBirthdate(java.time.LocalDate birthdate) { this.birthdate = birthdate; }
    public String getPasscode() { return passcode; }
    public void setPasscode(String passcode) { this.passcode = passcode; }
}