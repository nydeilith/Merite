package com.gorevtakip;

import java.time.LocalDate;
import java.util.UUID;

public class Hedef {
    public enum Donem { HAFTALIK, AYLIK }

    private String id;
    private String ad;          // Kart başlığı (örn: “Matematik – Haftalık 300 puan”)
    private String gorevAdi;    // Hangi görev adı toplanacak (TamamlananGorev.gorevAdi ile eşleşir)
    private double hedefPuan;   // Bu dönem hedeflenen puan
    private Donem donem;        // HAFTALIK / AYLIK
    private String olusturma;   // ISO tarih (sürüm/takip için)

    public Hedef() { }

    public Hedef(String ad, String gorevAdi, double hedefPuan, Donem donem) {
        this.id = UUID.randomUUID().toString();
        this.ad = ad;
        this.gorevAdi = gorevAdi;
        this.hedefPuan = hedefPuan;
        this.donem = donem;
        this.olusturma = LocalDate.now().toString();
    }

    public String getId() { return id; }
    public String getAd() { return ad; }
    public String getGorevAdi() { return gorevAdi; }
    public double getHedefPuan() { return hedefPuan; }
    public Donem getDonem() { return donem; }
    public String getOlusturma() { return olusturma; }

    public void setId(String id) { this.id = id; }
    public void setAd(String ad) { this.ad = ad; }
    public void setGorevAdi(String gorevAdi) { this.gorevAdi = gorevAdi; }
    public void setHedefPuan(double hedefPuan) { this.hedefPuan = hedefPuan; }
    public void setDonem(Donem donem) { this.donem = donem; }
    public void setOlusturma(String olusturma) { this.olusturma = olusturma; }
}
