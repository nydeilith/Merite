package com.gorevtakip;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tek sahnede çalışan rapor paneli.
 * - Toplam puan
 * - Birimlere göre toplam miktar özeti
 * - Göreve göre puan dağılımı (Pie)
 * - Günlük toplam puan (son 30 gün) LineChart
 */
public class RaporlamaPenceresi {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("MM-dd");

    public static VBox buildView() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(12));

        Label title = new Label("Raporlar");
        title.getStyleClass().add("label");

        // Veriyi yükle
        List<TamamlananGorev> tum = VeriYoneticisi.verileriYukle();

        // ---- Toplam puan ----
        double toplamPuan = tum.stream().mapToDouble(TamamlananGorev::getHesaplananPuan).sum();

        // ---- Birimlere göre toplam miktar (dk/sayfa vs.) ----
        Map<String, Double> birimeGore = tum.stream().collect(
                Collectors.groupingBy(TamamlananGorev::getBirim,
                        Collectors.summingDouble(TamamlananGorev::getYapilanMiktar))
        );

        String ozet = birimeGore.isEmpty()
                ? "Toplam Miktar Özeti: -"
                : "Toplam Miktar Özeti: " + birimeGore.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> String.format("%.0f %s", e.getValue(), e.getKey()))
                .collect(Collectors.joining(" | "));

        Label toplamPuanLbl = new Label(String.format("Toplam Puan: %.2f", toplamPuan));
        Label miktarOzetLbl = new Label(ozet);

        // ---- Göreve göre toplam puan (Pie) ----
        PieChart pieChart = buildPuanPieChart(tum);

        // ---- Günlük toplam puan (son 30 gün - LineChart) ----
        LineChart<String, Number> dailyLine = buildDailyTotalChart(tum);

        root.getChildren().addAll(title, toplamPuanLbl, miktarOzetLbl, pieChart, dailyLine);
        return root;
    }

    /* ================= Helpers ================ */

    private static PieChart buildPuanPieChart(List<TamamlananGorev> tum) {
        Map<String, Double> puanDagilimi = tum.stream().collect(
                Collectors.groupingBy(TamamlananGorev::getGorevAdi,
                        Collectors.summingDouble(TamamlananGorev::getHesaplananPuan))
        );

        ObservableList<PieChart.Data> pie = FXCollections.observableArrayList();
        puanDagilimi.forEach((ad, p) -> {
            if (p > 0) pie.add(new PieChart.Data(ad, p));
        });

        if (pie.isEmpty()) {
            // İlk kullanımda boşsa örnek göster
            pie.addAll(
                    new PieChart.Data("Ders Çalışma", 10),
                    new PieChart.Data("Spor Yapma", 6),
                    new PieChart.Data("Kuran Okuma", 8),
                    new PieChart.Data("Kitap Okuma", 5)
            );
        }

        PieChart chart = new PieChart(pie);
        chart.setLegendVisible(true);
        chart.setTitle("Görevlere Göre Puan Dağılımı");
        return chart;
    }

    private static LineChart<String, Number> buildDailyTotalChart(List<TamamlananGorev> tum) {
        // Tarih -> günlük toplam puan
        Map<LocalDate, Double> gunlukToplam = new HashMap<>();
        for (TamamlananGorev g : tum) {
            try {
                LocalDate d = LocalDate.parse(g.getTarih(), ISO);
                gunlukToplam.merge(d, g.getHesaplananPuan(), Double::sum);
            } catch (Exception ignored) {
                // Tarih formatı beklenen gibi değilse atla
            }
        }

        // Son 30 günü hazırlayalım (veri yoksa da eksen üzerinde gösterelim)
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(29);
        List<LocalDate> days = start.datesUntil(today.plusDays(1)).collect(Collectors.toList());

        // Kategori ekseni (MM-dd etiketleri)
        CategoryAxis xAxis = new CategoryAxis();
        List<String> labels = days.stream().map(d -> LABEL_FMT.format(d)).collect(Collectors.toList());
        xAxis.setCategories(FXCollections.observableArrayList(labels));
        xAxis.setLabel("Gün");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Günlük Toplam Puan");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Günlük Toplam Puan (Son 30 Gün)");
        chart.setLegendVisible(false);
        chart.setCreateSymbols(true);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (LocalDate d : days) {
            double toplam = gunlukToplam.getOrDefault(d, 0.0);
            series.getData().add(new XYChart.Data<>(LABEL_FMT.format(d), toplam));
        }
        chart.getData().add(series);
        return chart;
    }
}

