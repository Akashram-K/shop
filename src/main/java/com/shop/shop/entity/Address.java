package com.shop.shop.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    private String recipientName;
    private String phone;

    @Column(nullable = false)
    private String street;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String pincode;

    private String landmark;

    private boolean isDefault = false;

    public Address() {
    }

    public Address(User user, String recipientName, String phone, String street, String city, String state, String pincode, String landmark, boolean isDefault) {
        this.user = user;
        this.recipientName = recipientName;
        this.phone = phone;
        this.street = street;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.landmark = landmark;
        this.isDefault = isDefault;
    }

    public String getFormattedAddress() {
        StringBuilder sb = new StringBuilder();
        if (recipientName != null && !recipientName.isBlank()) {
            sb.append(recipientName).append(", ");
        }
        if (phone != null && !phone.isBlank()) {
            sb.append("Ph: ").append(phone).append(", ");
        }
        sb.append(street).append(", ");
        if (landmark != null && !landmark.isBlank()) {
            sb.append("Near ").append(landmark).append(", ");
        }
        sb.append(city).append(", ").append(state).append(" - ").append(pincode);
        return sb.toString();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public String getLandmark() {
        return landmark;
    }

    public void setLandmark(String landmark) {
        this.landmark = landmark;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }
}
