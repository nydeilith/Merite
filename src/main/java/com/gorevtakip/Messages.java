package com.gorevtakip;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class Messages {

    // Varsayılan TR
    private static Locale currentLocale = new Locale("tr");
    private static ResourceBundle bundle = loadBundle();

    private static ResourceBundle loadBundle() {
        return ResourceBundle.getBundle("i18n.messages", currentLocale);
    }

    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    public static void setLocale(Locale locale) {
        if (locale == null) {
            return;
        }
        if (!locale.equals(currentLocale)) {
            currentLocale = locale;
            bundle = loadBundle();
        }
    }

    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            // Key bulunamazsa ekranda !key! görünsün
            return "!" + key + "!";
        }
    }

    public static String format(String key, Object... args) {
        return MessageFormat.format(get(key), args);
    }
}

