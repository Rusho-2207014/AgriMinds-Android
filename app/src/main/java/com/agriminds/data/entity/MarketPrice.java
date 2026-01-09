package com.agriminds.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "market_prices")
public class MarketPrice {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String cropName;
    public String marketName;
    public String district;
    public double wholesalePrice;
    public double retailPrice;
    public String date; // ISO-8601 format preferred
}
