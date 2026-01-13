package com.agriminds.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "crop_charts")
public class CropChart {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String farmerId;
    private String farmerName;
    private String cropName;
    private String season;
    private String cultivationStartDate;
    private String cultivationEndDate;
    private String fertilizersUsed;
    private double seedCost;
    private double fertilizerCost;
    private double laborCost;
    private double otherCosts;
    private double totalCost;
    private double totalYield; // in kg or quintals
    private double sellPrice; // per unit
    private double totalRevenue;
    private double profit;
    private boolean isShared;
    private boolean hasEverBeenShared;
    private long createdAt;
    private long updatedAt;

    public CropChart() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isShared = false;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFarmerId() {
        return farmerId;
    }

    public void setFarmerId(String farmerId) {
        this.farmerId = farmerId;
    }

    public String getFarmerName() {
        return farmerName;
    }

    public void setFarmerName(String farmerName) {
        this.farmerName = farmerName;
    }

    public String getCropName() {
        return cropName;
    }

    public void setCropName(String cropName) {
        this.cropName = cropName;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getCultivationStartDate() {
        return cultivationStartDate;
    }

    public void setCultivationStartDate(String cultivationStartDate) {
        this.cultivationStartDate = cultivationStartDate;
    }

    public String getCultivationEndDate() {
        return cultivationEndDate;
    }

    public void setCultivationEndDate(String cultivationEndDate) {
        this.cultivationEndDate = cultivationEndDate;
    }

    public String getFertilizersUsed() {
        return fertilizersUsed;
    }

    public void setFertilizersUsed(String fertilizersUsed) {
        this.fertilizersUsed = fertilizersUsed;
    }

    public double getSeedCost() {
        return seedCost;
    }

    public void setSeedCost(double seedCost) {
        this.seedCost = seedCost;
        calculateTotalCost();
    }

    public double getFertilizerCost() {
        return fertilizerCost;
    }

    public void setFertilizerCost(double fertilizerCost) {
        this.fertilizerCost = fertilizerCost;
        calculateTotalCost();
    }

    public double getLaborCost() {
        return laborCost;
    }

    public void setLaborCost(double laborCost) {
        this.laborCost = laborCost;
        calculateTotalCost();
    }

    public double getOtherCosts() {
        return otherCosts;
    }

    public void setOtherCosts(double otherCosts) {
        this.otherCosts = otherCosts;
        calculateTotalCost();
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public double getTotalYield() {
        return totalYield;
    }

    public void setTotalYield(double totalYield) {
        this.totalYield = totalYield;
        calculateProfit();
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
        calculateTotalRevenue();
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public double getProfit() {
        return profit;
    }

    public void setProfit(double profit) {
        this.profit = profit;
    }

    public boolean isShared() {
        return isShared;
    }

    public void setShared(boolean shared) {
        isShared = shared;
    }

    public boolean isHasEverBeenShared() {
        return hasEverBeenShared;
    }

    public void setHasEverBeenShared(boolean hasEverBeenShared) {
        this.hasEverBeenShared = hasEverBeenShared;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper methods
    private void calculateTotalCost() {
        this.totalCost = seedCost + fertilizerCost + laborCost + otherCosts;
        calculateProfit();
    }

    private void calculateTotalRevenue() {
        this.totalRevenue = totalYield * sellPrice;
        calculateProfit();
    }

    private void calculateProfit() {
        this.profit = totalRevenue - totalCost;
    }

    public void recalculate() {
        calculateTotalCost();
        calculateTotalRevenue();
        this.updatedAt = System.currentTimeMillis();
    }
}
