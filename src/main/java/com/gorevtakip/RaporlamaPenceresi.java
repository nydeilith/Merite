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

public class RaporlamaPenceresi {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("MM-dd");

    public static VBox buildView() {

        VBox root = new VBox(12);
        root.setPadding(new Insets(12));

        // Başlık
        Label title = new Label(
                trEn("Raporlar", "Reports")
        );
        title.getStyleClass().add("label");

        // Verileri al
        List<TamamlananGorev> tum = VeriYoneticisi.verileriYukle();

        // Toplam Puan
        double toplamPuan = tum.stream()
                .mapToDouble(TamamlananGorev::getHesaplananPuan)
                .sum();

        Label toplamPuanLbl = new Label(
                String.format(trEn("Toplam Puan: %.2f",
                        "Total Points: %.2f"), toplamPuan)
        );

        // Birimlere göre toplam miktar
        Map<String, Double> birimeGore = tum.stream().collect(
                Collectors.groupingBy(TamamlananGorev::getBirim,
                        Collectors.summingDouble(TamamlananGorev::getYapilanMiktar))
        );

        String miktarOzet;
        if (birimeGore.isEmpty()) {
            miktarOzet = trEn("Toplam Miktar Özeti: -",
                    "Total Amount Summary: -");
        } else {
            String joined = birimeGore.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> String.format("%.0f %s", e.getValue(), e.getKey()))
                    .collect(Collectors.joining(" | "));

            miktarOzet = trEn("Toplam Miktar Özeti: ",
                    "Total Amount Summary: ") + joined;
        }

        Label miktarOzetLbl = new Label(miktarOzet);

        // Pie chart
        PieChart pieChart = buildPieChart(tum);

        // Line chart
        LineChart<String, Number> lineChart = buildDailyChart(tum);

        root.getChildren().addAll(title, toplamPuanLbl, miktarOzetLbl, pieChart, lineChart);
        return root;
    }

    // ---------------- PIE CHART ----------------
    private static PieChart buildPieChart(List<TamamlananGorev> tum) {

        Map<String, Double> puanDagilimi = tum.stream().collect(
                Collectors.groupingBy(TamamlananGorev::getGorevAdi,
                        Collectors.summingDouble(TamamlananGorev::getHesaplananPuan))
        );

        ObservableList<PieChart.Data> pie = FXCollections.observableArrayList();

        puanDagilimi.forEach((ad, p) -> {
            if (p > 0) pie.add(new PieChart.Data(ad, p));
        });

        if (pie.isEmpty()) {
            pie.addAll(
                    new PieChart.Data(trEn("Ders Çalışma", "Study"), 10),
                    new PieChart.Data(trEn("Spor Yapma", "Workout"), 6),
                    new PieChart.Data(trEn("Kuran Okuma", "Quran Reading"), 8),
                    new PieChart.Data(trEn("Kitap Okuma", "Book Reading"), 5)
            );
        }

        PieChart chart = new PieChart(pie);
        chart.setLegendVisible(true);
        chart.setTitle(
                trEn("Görevlere Göre Puan Dağılımı",
                        "Points Distribution by Task")
        );

        return chart;
    }

    // ---------------- DAILY LINE CHART ----------------
    private static LineChart<String, Number> buildDailyChart(List<TamamlananGorev> tum) {

        Map<LocalDate, Double> gunlukToplam = new HashMap<>();

        for (TamamlananGorev g : tum) {
            try {
                LocalDate d = LocalDate.parse(g.getTarih(), ISO);
                gunlukToplam.merge(d, g.getHesaplananPuan(), Double::sum);
            } catch (Exception ignored) {}
        }

        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(29);

        List<LocalDate> days = start.datesUntil(today.plusDays(1)).collect(Collectors.toList());

        // X axis
        CategoryAxis xAxis = new CategoryAxis();
        List<String> labels = days.stream().map(d -> LABEL_FMT.format(d)).collect(Collectors.toList());
        xAxis.setCategories(FXCollections.observableArrayList(labels));
        xAxis.setLabel(trEn("Gün", "Day"));

        // Y axis
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(trEn("Günlük Toplam Puan", "Daily Total Points"));

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);

        chart.setTitle(trEn(
                "Günlük Toplam Puan (Son 30 Gün)",
                "Daily Total Points (Last 30 Days)"
        ));

        chart.setLegendVisible(false);
        chart.setCreateSymbols(true);

        XYChart.Series<String, Number> s = new XYChart.Series<>();

        for (LocalDate d : days) {
            double toplam = gunlukToplam.getOrDefault(d, 0.0);
            s.getData().add(new XYChart.Data<>(LABEL_FMT.format(d), toplam));
        }

        chart.getData().add(s);
        return chart;
    }

    // ---------- Dil helper ----------

    private static boolean isEnglish() {
        return "Home".equals(Messages.get("menu.home"));
    }

    private static String trEn(String tr, String en) {
        return isEnglish() ? en : tr;
    }
}

