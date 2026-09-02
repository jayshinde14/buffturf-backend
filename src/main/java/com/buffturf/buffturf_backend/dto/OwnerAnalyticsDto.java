package com.buffturf.buffturf_backend.dto;

import java.util.Map;

public class OwnerAnalyticsDto {

    private Long turfId;
    private String turfName;
    private String sportType;
    private Double pricePerHour;
    private Double totalRevenue;
    private Double monthlyRevenue; // MRR
    private Double todayRevenue;
    private int confirmedBookings;
    private int totalBookings;
    private int totalSlotsGenerated;
    private double occupancyRatePercent;
    private double platformFeePercent;
    private double platformFeeAmount;
    private double netPayoutAmount;
    private Map<String, Integer> peakHoursBreakdown;

    public OwnerAnalyticsDto() {}

    public Long getTurfId() { return turfId; }
    public void setTurfId(Long turfId) { this.turfId = turfId; }

    public String getTurfName() { return turfName; }
    public void setTurfName(String turfName) { this.turfName = turfName; }

    public String getSportType() { return sportType; }
    public void setSportType(String sportType) { this.sportType = sportType; }

    public Double getPricePerHour() { return pricePerHour; }
    public void setPricePerHour(Double pricePerHour) { this.pricePerHour = pricePerHour; }

    public Double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }

    public Double getMonthlyRevenue() { return monthlyRevenue; }
    public void setMonthlyRevenue(Double monthlyRevenue) { this.monthlyRevenue = monthlyRevenue; }

    public Double getTodayRevenue() { return todayRevenue; }
    public void setTodayRevenue(Double todayRevenue) { this.todayRevenue = todayRevenue; }

    public int getConfirmedBookings() { return confirmedBookings; }
    public void setConfirmedBookings(int confirmedBookings) { this.confirmedBookings = confirmedBookings; }

    public int getTotalBookings() { return totalBookings; }
    public void setTotalBookings(int totalBookings) { this.totalBookings = totalBookings; }

    public int getTotalSlotsGenerated() { return totalSlotsGenerated; }
    public void setTotalSlotsGenerated(int totalSlotsGenerated) { this.totalSlotsGenerated = totalSlotsGenerated; }

    public double getOccupancyRatePercent() { return occupancyRatePercent; }
    public void setOccupancyRatePercent(double occupancyRatePercent) { this.occupancyRatePercent = occupancyRatePercent; }

    public double getPlatformFeePercent() { return platformFeePercent; }
    public void setPlatformFeePercent(double platformFeePercent) { this.platformFeePercent = platformFeePercent; }

    public double getPlatformFeeAmount() { return platformFeeAmount; }
    public void setPlatformFeeAmount(double platformFeeAmount) { this.platformFeeAmount = platformFeeAmount; }

    public double getNetPayoutAmount() { return netPayoutAmount; }
    public void setNetPayoutAmount(double netPayoutAmount) { this.netPayoutAmount = netPayoutAmount; }

    public Map<String, Integer> getPeakHoursBreakdown() { return peakHoursBreakdown; }
    public void setPeakHoursBreakdown(Map<String, Integer> peakHoursBreakdown) { this.peakHoursBreakdown = peakHoursBreakdown; }
}
