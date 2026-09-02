package com.buffturf.buffturf_backend.dto;

public class TurfOwnerDto {

    private Long userId;
    private String username;
    private String email;
    private String password;
    private String phoneNumber;
    private Long turfId;
    private String turfName;

    public TurfOwnerDto() {}

    public TurfOwnerDto(Long userId, String username, String email, String phoneNumber, Long turfId, String turfName) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.turfId = turfId;
        this.turfName = turfName;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Long getTurfId() { return turfId; }
    public void setTurfId(Long turfId) { this.turfId = turfId; }

    public String getTurfName() { return turfName; }
    public void setTurfName(String turfName) { this.turfName = turfName; }
}
