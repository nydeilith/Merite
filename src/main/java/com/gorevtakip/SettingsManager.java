package com.gorevtakip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

public final class SettingsManager {

    private static final String SETTINGS_FILE_NAME = "ayarlar.properties";
    private static final String KEY_LANGUAGE = "language";

    private SettingsManager() {
    }

    private static File getSettingsFile() {
        // Merite.exe ile aynı klasöre yazar/okur
        return new File(SETTINGS_FILE_NAME);
    }

    public static Locale loadLanguage() {
        File file = getSettingsFile();
        if (!file.exists()) {
            // Varsayılan TR
            return new Locale("tr", "TR");
        }

        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
        } catch (IOException e) {
            return new Locale("tr", "TR");
        }

        String code = props.getProperty(KEY_LANGUAGE, "tr");
        if ("en".equalsIgnoreCase(code)) {
            return Locale.ENGLISH;
        } else {
            return new Locale("tr", "TR");
        }
    }

    public static void saveLanguage(Locale locale) {
        Properties props = new Properties();
        if (Locale.ENGLISH.getLanguage().equals(locale.getLanguage())) {
            props.setProperty(KEY_LANGUAGE, "en");
        } else {
            props.setProperty(KEY_LANGUAGE, "tr");
        }

        File file = getSettingsFile();
        try (FileOutputStream out = new FileOutputStream(file)) {
            props.store(out, "Merite settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

