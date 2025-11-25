package com.gorevtakip;

import javafx.scene.media.AudioClip;
import java.net.URL;

public class SesYoneticisi {

    // Ses türlerini tanımlayalım
    public enum Ses {
        SAYFA("sayfa.mp3"),
        KASA("kasa.mp3"),
        ONAY("onay.mp3"),
        CIKIS("cikis.mp3"),
        BASARIM("basarim.mp3");

        private final String dosyaAdi;
        Ses(String dosyaAdi) { this.dosyaAdi = dosyaAdi; }
    }

    // Sesi oynatan metod
    public static void oynat(Ses ses) {
        try {
            // resources/sounds klasöründen dosyayı bul
            URL resource = SesYoneticisi.class.getResource("/sounds/" + ses.dosyaAdi);
            if (resource != null) {
                AudioClip clip = new AudioClip(resource.toExternalForm());
                clip.play();
            } else {
                System.out.println("Ses dosyası bulunamadı: " + ses.dosyaAdi);
            }
        } catch (Exception e) {
            System.err.println("Ses oynatma hatası: " + e.getMessage());
        }
    }
}
