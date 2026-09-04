package com.buffturf.buffturf_backend.dto;

public class UserLookupDto {

    private Long id;
    private String username;
    private String email;
    private String phoneNumber;
    private String role;
    private boolean isRegistered;

    public UserLookupDto() {}

    public UserLookupDto(Long id, String username, String email, String phoneNumber, String role, boolean isRegistered) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.isRegistered = isRegistered;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isRegistered() { return isRegistered; }
    public void setRegistered(boolean registered) { isRegistered = registered; }
}
