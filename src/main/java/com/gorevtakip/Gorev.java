package com.gorevtakip;

public class Gorev {
    private String ad;
    private String birim;
    private double puanPerBirim;

    public Gorev(String ad, String birim, double puanPerBirim) {
        this.ad = ad;
        this.birim = birim;
        this.puanPerBirim = puanPerBirim;
    }

    // Mevcut Getter'lar
    public String getAd() { return ad; }
    public String getBirim() { return birim; }
    public double getPuanPerBirim() { return puanPerBirim; }

    // YENİ: Görevleri düzenlemek için Setter'lar
    public void setAd(String ad) { this.ad = ad; }
    public void setBirim(String birim) { this.birim = birim; }
    public void setPuanPerBirim(double puanPerBirim) { this.puanPerBirim = puanPerBirim; }

    @Override
    public String toString() {
        return this.ad;
    }
}
