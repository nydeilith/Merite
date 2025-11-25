package com.gorevtakip;

import java.util.UUID;

public class Odul {
    private String id;
    private String ad;
    private double bedel; // Puan maliyeti

    public Odul(String ad, double bedel) {
        this.id = UUID.randomUUID().toString();
        this.ad = ad;
        this.bedel = bedel;
    }

    public String getId() { return id; }
    public String getAd() { return ad; }
    public double getBedel() { return bedel; }
    
    public void setAd(String ad) { this.ad = ad; }
    public void setBedel(double bedel) { this.bedel = bedel; }
}
