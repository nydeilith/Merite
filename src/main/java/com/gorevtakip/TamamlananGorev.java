package com.gorevtakip;

import java.time.LocalDate; // YENİ: Bu satırı ekliyoruz

public class TamamlananGorev {
    private String tarih; // "2025-10-11" formatında
    private String gorevAdi;
    private String birim;
    private double yapilanMiktar;
    private double hesaplananPuan;

    public TamamlananGorev(String tarih, String gorevAdi, String birim, double yapilanMiktar, double hesaplananPuan) {
        this.tarih = tarih;
        this.gorevAdi = gorevAdi;
        this.birim = birim;
        this.yapilanMiktar = yapilanMiktar;
        this.hesaplananPuan = hesaplananPuan;
    }

    
    public String getTarih() { return tarih; }
    public String getGorevAdi() { return gorevAdi; }
    public double getYapilanMiktar() { return yapilanMiktar; }
    public double getHesaplananPuan() { return hesaplananPuan; }
    public String getBirim() { return birim; }

    // YENİ: Raporlama için String tarihi gerçek LocalDate nesnesine çeviren yardımcı metod
    public LocalDate getTarihAsDate() {
        return LocalDate.parse(this.tarih);
    }
}

