package com.buffturf.buffturf_backend.service.impl;

import com.buffturf.buffturf_backend.dto.OwnerAnalyticsDto;
import com.buffturf.buffturf_backend.dto.SlotResponseDto;
import com.buffturf.buffturf_backend.exception.ApiException;
import com.buffturf.buffturf_backend.exception.ResourceNotFoundException;
import com.buffturf.buffturf_backend.model.*;
import com.buffturf.buffturf_backend.repository.*;
import com.buffturf.buffturf_backend.service.OwnerService;
import com.buffturf.buffturf_backend.service.SlotService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OwnerServiceImpl implements OwnerService {

    private final TurfRepository turfRepository;
    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final SlotService slotService;
    private final AuditLogRepository auditLogRepository;

    public OwnerServiceImpl(TurfRepository turfRepository,
                            SlotRepository slotRepository,
                            BookingRepository bookingRepository,
                            UserRepository userRepository,
                            SlotService slotService,
                            AuditLogRepository auditLogRepository) {
        this.turfRepository = turfRepository;
        this.slotRepository = slotRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.slotService = slotService;
        this.auditLogRepository = auditLogRepository;
    }

    private Turf getOwnerTurfEntity(String ownerEmail) {
        // Find turf assigned to this owner email or owner user
        Optional<Turf> turfOpt = turfRepository.findByOwnerEmail(ownerEmail);
        if (turfOpt.isPresent()) {
            return turfOpt.get();
        }

        // If user is Super Admin, allow access to the first turf as fallback
        User user = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + ownerEmail));
        if (user.getRole() == User.Role.ADMIN) {
            List<Turf> all = turfRepository.findAll();
            if (!all.isEmpty()) return all.get(0);
        }

        throw new ResourceNotFoundException("No turf currently assigned to owner account: " + ownerEmail);
    }

    @Override
    public Turf getMyTurf(String ownerEmail) {
        return getOwnerTurfEntity(ownerEmail);
    }

    @Override
    @Transactional
    public Turf updateMyTurf(String ownerEmail, Turf updated) {
        Turf turf = getOwnerTurfEntity(ownerEmail);
        StringBuilder changes = new StringBuilder();

        if (updated.getName() != null && !updated.getName().equals(turf.getName())) {
            changes.append(String.format("Name: '%s' -> '%s'; ", turf.getName(), updated.getName()));
            turf.setName(updated.getName());
        }
        if (updated.getPricePerHour() != null && !updated.getPricePerHour().equals(turf.getPricePerHour())) {
            changes.append(String.format("Price: ₹%.0f -> ₹%.0f; ", turf.getPricePerHour(), updated.getPricePerHour()));
            turf.setPricePerHour(updated.getPricePerHour());
        }
        if (updated.getLocation() != null) turf.setLocation(updated.getLocation());
        if (updated.getAddress() != null) turf.setAddress(updated.getAddress());
        if (updated.getDescription() != null) turf.setDescription(updated.getDescription());
        if (updated.getPhotoUrls() != null) turf.setPhotoUrls(updated.getPhotoUrls());
        if (updated.getOpenTime() != null) turf.setOpenTime(updated.getOpenTime());
        if (updated.getCloseTime() != null) turf.setCloseTime(updated.getCloseTime());

        Turf saved = turfRepository.save(turf);

        String detailMsg = changes.length() > 0 ? changes.toString() : "Updated profile details & operating hours";
        auditLogRepository.save(new AuditLog(saved.getId(), saved.getName(), ownerEmail, "TURF_PROFILE_UPDATED", detailMsg));

        return saved;
    }

    @Override
    public List<SlotResponseDto> getMyTurfSlots(String ownerEmail, LocalDate date) {
        Turf turf = getOwnerTurfEntity(ownerEmail);
        return slotService.getEnrichedSlotsByTurfAndDate(turf.getId(), date, ownerEmail);
    }

    @Override
    @Transactional
    public List<Slot> generateMyTurfSlots(String ownerEmail, LocalDate date) {
        Turf turf = getOwnerTurfEntity(ownerEmail);
        List<Slot> generated = slotService.generateSlots(turf.getId(), date);

        auditLogRepository.save(new AuditLog(
                turf.getId(),
                turf.getName(),
                ownerEmail,
                "SLOTS_GENERATED",
                String.format("Generated %d hourly slots for date: %s", generated.size(), date)
        ));

        return generated;
    }

    @Override
    @Transactional
    public void deleteMyTurfSlot(String ownerEmail, Long slotId) {
        Turf turf = getOwnerTurfEntity(ownerEmail);
        Slot slot = slotService.getSlotById(slotId);

        if (!slot.getTurf().getId().equals(turf.getId())) {
            throw new ApiException("You are not authorized to delete slots from another turf", HttpStatus.FORBIDDEN);
        }
        if (!slot.getIsAvailable()) {
            throw new ApiException("Cannot delete a slot that is already booked by a customer", HttpStatus.BAD_REQUEST);
        }

        slotService.deleteSlot(slotId);

        auditLogRepository.save(new AuditLog(
                turf.getId(),
                turf.getName(),
                ownerEmail,
                "SLOT_DELETED",
                String.format("Deleted slot #%d (Time: %s - %s on %s)", slotId, slot.getStartTime(), slot.getEndTime(), slot.getSlotDate())
        ));
    }

    @Override
    public List<Booking> getMyTurfBookings(String ownerEmail) {
        Turf turf = getOwnerTurfEntity(ownerEmail);
        return bookingRepository.findAll().stream()
                .filter(b -> b.getTurf() != null && b.getTurf().getId().equals(turf.getId()))
                .sorted((b1, b2) -> b2.getBookingDate().compareTo(b1.getBookingDate()))
                .collect(Collectors.toList());
    }

    @Override
    public OwnerAnalyticsDto getMyTurfAnalytics(String ownerEmail) {
        Turf turf = getOwnerTurfEntity(ownerEmail);
        List<Booking> venueBookings = getMyTurfBookings(ownerEmail);

        java.time.ZoneId istZone = java.time.ZoneId.of("Asia/Kolkata");
        LocalDate now = LocalDate.now(istZone);
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();

        double totalRevenue = 0.0;
        double monthlyRevenue = 0.0;
        double todayRevenue = 0.0;
        int confirmedCount = 0;

        Map<String, Integer> peakHours = new LinkedHashMap<>();
        peakHours.put("Morning (6 AM - 12 PM)", 0);
        peakHours.put("Afternoon (12 PM - 5 PM)", 0);
        peakHours.put("Evening Peak (5 PM - 11 PM)", 0);

        for (Booking b : venueBookings) {
            if (b.getStatus() == Booking.BookingStatus.CONFIRMED || b.getStatus() == Booking.BookingStatus.COMPLETED) {
                confirmedCount++;
                double bookingAmount = b.getAmountPaid() != null && b.getAmountPaid() > 0 ?
                        b.getAmountPaid() :
                        (turf.getPricePerHour() * (b.getSlots() != null ? b.getSlots().size() : 1));

                totalRevenue += bookingAmount;

                if (b.getBookingDate() != null) {
                    if (b.getBookingDate().getYear() == currentYear && b.getBookingDate().getMonthValue() == currentMonth) {
                        monthlyRevenue += bookingAmount;
                    }
                    if (b.getBookingDate().isEqual(now)) {
                        todayRevenue += bookingAmount;
                    }
                }

                // Peak hours breakdown calculation
                if (b.getSlots() != null) {
                    for (Slot s : b.getSlots()) {
                        if (s.getStartTime() != null) {
                            int hour = s.getStartTime().getHour();
                            if (hour >= 6 && hour < 12) {
                                peakHours.put("Morning (6 AM - 12 PM)", peakHours.get("Morning (6 AM - 12 PM)") + 1);
                            } else if (hour >= 12 && hour < 17) {
                                peakHours.put("Afternoon (12 PM - 5 PM)", peakHours.get("Afternoon (12 PM - 5 PM)") + 1);
                            } else {
                                peakHours.put("Evening Peak (5 PM - 11 PM)", peakHours.get("Evening Peak (5 PM - 11 PM)") + 1);
                            }
                        }
                    }
                }
            }
        }

        // Occupancy calculation for current month
        List<Slot> allSlots = slotRepository.findAll().stream()
                .filter(s -> s.getTurf() != null && s.getTurf().getId().equals(turf.getId()))
                .collect(Collectors.toList());

        int totalSlots = allSlots.size();
        long bookedSlots = allSlots.stream().filter(s -> !s.getIsAvailable()).count();
        double occupancyPercent = totalSlots > 0 ? ((double) bookedSlots / totalSlots) * 100.0 : 0.0;

        double platformFeePercent = 10.0;
        double platformFeeAmount = totalRevenue * (platformFeePercent / 100.0);
        double netPayoutAmount = totalRevenue - platformFeeAmount;

        OwnerAnalyticsDto dto = new OwnerAnalyticsDto();
        dto.setTurfId(turf.getId());
        dto.setTurfName(turf.getName());
        dto.setSportType(turf.getSportType());
        dto.setPricePerHour(turf.getPricePerHour());
        dto.setTotalRevenue(totalRevenue);
        dto.setMonthlyRevenue(monthlyRevenue);
        dto.setTodayRevenue(todayRevenue);
        dto.setConfirmedBookings(confirmedCount);
        dto.setTotalBookings(venueBookings.size());
        dto.setTotalSlotsGenerated(totalSlots);
        dto.setOccupancyRatePercent(Math.round(occupancyPercent * 10.0) / 10.0);
        dto.setPlatformFeePercent(platformFeePercent);
        dto.setPlatformFeeAmount(Math.round(platformFeeAmount * 100.0) / 100.0);
        dto.setNetPayoutAmount(Math.round(netPayoutAmount * 100.0) / 100.0);
        dto.setPeakHoursBreakdown(peakHours);

        return dto;
    }
}
