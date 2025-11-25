package com.gorevtakip;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class VeriYoneticisi {
    
    private static final String VERI_KLASORU = System.getProperty("user.home") + "/.gorevtakip";
    
    private static final String TAMAMLANAN_GOREV_DOSYASI = VERI_KLASORU + "/gorev_verileri.json";
    private static final String TANIMLI_GOREV_DOSYASI = VERI_KLASORU + "/tanimli_gorevler.json";
    private static final String HEDEF_DOSYASI = VERI_KLASORU + "/hedefler.json";
    private static final String ODUL_DOSYASI = VERI_KLASORU + "/oduller.json"; // YENİ
    private static final String HARCAMA_DOSYASI = VERI_KLASORU + "/harcamalar.json"; // YENİ
    
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    static {
        try {
            Path path = Paths.get(VERI_KLASORU);
            if (!Files.exists(path)) Files.createDirectories(path);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // --- Tamamlanan Görevler ---
    public static void verileriKaydet(List<TamamlananGorev> list) { dosyaYaz(TAMAMLANAN_GOREV_DOSYASI, list); }
    public static List<TamamlananGorev> verileriYukle() { return dosyaOku(TAMAMLANAN_GOREV_DOSYASI, new TypeToken<ArrayList<TamamlananGorev>>(){}.getType()); }

    // --- Tanımlı Görevler ---
    public static void gorevTanimlariniKaydet(List<Gorev> list) { dosyaYaz(TANIMLI_GOREV_DOSYASI, list); }
    public static List<Gorev> gorevTanimlariniYukle() {
        List<Gorev> list = dosyaOku(TANIMLI_GOREV_DOSYASI, new TypeToken<ArrayList<Gorev>>(){}.getType());
        if (list.isEmpty()) return varsayilanGorevleriOlustur();
        return list;
    }
    
    // --- Hedefler ---
    public static void hedefleriKaydet(List<Hedef> list) { dosyaYaz(HEDEF_DOSYASI, list); }
    public static List<Hedef> hedefleriYukle() { return dosyaOku(HEDEF_DOSYASI, new TypeToken<ArrayList<Hedef>>(){}.getType()); }

    // --- YENİ: Ödüller ---
    public static void odulleriKaydet(List<Odul> list) { dosyaYaz(ODUL_DOSYASI, list); }
    public static List<Odul> odulleriYukle() { return dosyaOku(ODUL_DOSYASI, new TypeToken<ArrayList<Odul>>(){}.getType()); }

    // --- YENİ: Harcamalar ---
    public static void harcamalariKaydet(List<HarcananOdul> list) { dosyaYaz(HARCAMA_DOSYASI, list); }
    public static List<HarcananOdul> harcamalariYukle() { return dosyaOku(HARCAMA_DOSYASI, new TypeToken<ArrayList<HarcananOdul>>(){}.getType()); }

    // --- Yardımcılar ---
    private static <T> void dosyaYaz(String path, List<T> data) {
        try (FileWriter writer = new FileWriter(path)) { gson.toJson(data, writer); } 
        catch (IOException e) { e.printStackTrace(); }
    }
    
    private static <T> List<T> dosyaOku(String path, Type type) {
        try (FileReader reader = new FileReader(path)) {
            List<T> list = gson.fromJson(reader, type);
            return list != null ? list : new ArrayList<>();
        } catch (IOException e) { return new ArrayList<>(); }
    }

    private static List<Gorev> varsayilanGorevleriOlustur() {
        List<Gorev> l = new ArrayList<>();
        l.add(new Gorev("Kitap Okuma", "sayfa", 1.5));
        l.add(new Gorev("Ders Çalışma", "dakika", 0.5));
        gorevTanimlariniKaydet(l);
        return l;
    }
}
