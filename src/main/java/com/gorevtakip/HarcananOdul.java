package com.gorevtakip;

public class HarcananOdul {
    private String tarih; // ISO format
    private String odulAdi;
    private double harcananPuan;

    public HarcananOdul(String tarih, String odulAdi, double harcananPuan) {
        this.tarih = tarih;
        this.odulAdi = odulAdi;
        this.harcananPuan = harcananPuan;
    }

    public String getTarih() { return tarih; }
    public String getOdulAdi() { return odulAdi; }
    public double getHarcananPuan() { return harcananPuan; }
}
