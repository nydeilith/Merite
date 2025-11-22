package com.gorevtakip;

import javafx.application.Application;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;

public class Launcher {

    private static FileChannel lockChannel;
    private static FileLock appLock;

    public static void main(String[] args) {
        if (!acquireAppLock()) {
            // İkinci kopya çalıştırılmak istendi → sessizce çık
            System.err.println("Uygulama zaten çalışıyor (tek kopya kuralı).");
            return;
        }

        // Çıkarken kilidi bırak
        Runtime.getRuntime().addShutdownHook(new Thread(Launcher::releaseAppLock));

        Application.launch(AnaUygulama.class, args);
    }

    /** Kullanıcı ev dizininde .gorevtakip/app.lock dosyası üstünden exclusive kilit alır. */
    private static boolean acquireAppLock() {
        try {
            Path dir = Paths.get(System.getProperty("user.home"), ".gorevtakip");
            Files.createDirectories(dir);
            Path lockPath = dir.resolve("app.lock");
            lockChannel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            appLock = lockChannel.tryLock(); // başka bir süreçte kilit varsa null döner
            return appLock != null;
        } catch (Throwable t) {
            // Bazı nadir ortamlarda kilit atılamazsa, kullanıcıyı engellemeyelim
            System.err.println("Uyarı: Tek kopya kilidi alınamadı, yine de başlatılıyor. Sebep: " + t.getMessage());
            return true;
        }
    }

    private static void releaseAppLock() {
        try { if (appLock != null && appLock.isValid()) appLock.release(); } catch (IOException ignore) {}
        try { if (lockChannel != null && lockChannel.isOpen()) lockChannel.close(); } catch (IOException ignore) {}
    }
}

