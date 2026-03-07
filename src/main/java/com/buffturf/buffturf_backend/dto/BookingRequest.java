package com.buffturf.buffturf_backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public class BookingRequest {

    @NotNull(message = "Turf ID is required")
    private Long turfId;

    @NotNull(message = "Slot ID is required")
    private Long slotId;

    @NotNull(message = "Booking date is required")
    private LocalDate bookingDate;

    @Size(max = 10, message = "Maximum 10 players allowed")
    private List<PlayerRequest> players;

    public BookingRequest() {}

    public Long getTurfId() { return turfId; }
    public void setTurfId(Long turfId) { this.turfId = turfId; }

    public Long getSlotId() { return slotId; }
    public void setSlotId(Long slotId) { this.slotId = slotId; }

    public LocalDate getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDate bookingDate) { this.bookingDate = bookingDate; }

    public List<PlayerRequest> getPlayers() { return players; }
    public void setPlayers(List<PlayerRequest> players) { this.players = players; }

    public static class PlayerRequest {

        private String name;
        private Integer age;
        private String gender;
        private String contact;
        private String governmentId;

        public PlayerRequest() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }

        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }

        public String getContact() { return contact; }
        public void setContact(String contact) { this.contact = contact; }

        public String getGovernmentId() { return governmentId; }
        public void setGovernmentId(String governmentId) { this.governmentId = governmentId; }
    }
}