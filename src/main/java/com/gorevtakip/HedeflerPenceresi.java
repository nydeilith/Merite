package com.gorevtakip;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class HedeflerPenceresi {

    /** Ana görünüm: kart ızgarası + ekleme formu */
    public static Pane buildView(List<TamamlananGorev> tumGorevler, Runnable onAnyChange) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Üst: Ekleme formu
        HBox form = new HBox(10);
        form.setAlignment(Pos.CENTER_LEFT);
        form.setPadding(new Insets(6));

        TextField tfAd = new TextField(); tfAd.setPromptText("Başlık (örn: Haftalık Matematik)");
        ComboBox<Gorev> cbGorev = new ComboBox<>();
        cbGorev.getItems().setAll(VeriYoneticisi.gorevTanimlariniYukle());
        if (!cbGorev.getItems().isEmpty()) cbGorev.getSelectionModel().selectFirst();

        ComboBox<Hedef.Donem> cbDonem = new ComboBox<>();
        cbDonem.getItems().addAll(Hedef.Donem.HAFTALIK, Hedef.Donem.AYLIK);
        cbDonem.getSelectionModel().select(Hedef.Donem.HAFTALIK);

        TextField tfHedefPuan = new TextField(); tfHedefPuan.setPromptText("Hedef Puan (örn: 300)");
        Button btnEkle = new Button("Hedef Ekle"); btnEkle.setId("ekle-butonu");

        form.getChildren().addAll(new Label("Başlık:"), tfAd,
                new Label("Görev:"), cbGorev,
                new Label("Dönem:"), cbDonem,
                new Label("Hedef Puan:"), tfHedefPuan,
                btnEkle);

        // Orta: kart ızgarası
        TilePane grid = new TilePane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPrefColumns(2);
        grid.setPadding(new Insets(6));

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        root.setTop(form);
        root.setCenter(scroll);

        // Veri
        List<Hedef> hedefler = new ArrayList<>(VeriYoneticisi.hedefleriYukle());
        Consumer<List<Hedef>> refresh = hs -> {
            grid.getChildren().setAll(hs.stream().map(h -> buildCard(h, tumGorevler, hs, onAnyChange)).collect(Collectors.toList()));
        };
        refresh.accept(hedefler);

        btnEkle.setOnAction(e -> {
            Gorev g = cbGorev.getValue();
            if (g == null) { uyar("Önce bir görev tanımı seç."); return; }
            double hedefPuan;
            try { hedefPuan = Double.parseDouble(tfHedefPuan.getText().trim()); }
            catch (Exception ex) { uyar("Hedef puanı sayı olmalı."); return; }
            String baslik = tfAd.getText().isBlank() ? (cbDonem.getValue()== Hedef.Donem.HAFTALIK ? "Haftalık " : "Aylık ")+g.getAd() : tfAd.getText().trim();

            hedefler.add(new Hedef(baslik, g.getAd(), hedefPuan, cbDonem.getValue()));
            VeriYoneticisi.hedefleriKaydet(hedefler);
            refresh.accept(hedefler);
            if (onAnyChange != null) onAnyChange.run();
            tfAd.clear(); tfHedefPuan.clear();
        });

        return root;
    }

    /** Tek bir hedef kartı (animasyonlu progress) */
    private static Node buildCard(Hedef h, List<TamamlananGorev> tum, List<Hedef> hedefler, Runnable onAnyChange) {
        // 1. Kart Kutusu (VBox) Oluşturma
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setMinWidth(360);
        card.setPrefWidth(360);
        card.getStyleClass().add("hedef-card"); // Standart stil

        // 2. Tarih ve Puan Hesaplamaları
        LocalDate now = LocalDate.now();
        LocalDate start, end;
        
        // Haftalık mı Aylık mı? Başlangıç/Bitiş tarihlerini bul
        if (h.getDonem() == Hedef.Donem.HAFTALIK) {
            var wf = WeekFields.of(Locale.forLanguageTag("tr-TR"));
            start = now.with(wf.dayOfWeek(), 1); // Pazartesi
            end = now.with(wf.dayOfWeek(), 7);   // Pazar
        } else {
            start = YearMonth.from(now).atDay(1);
            end = YearMonth.from(now).atEndOfMonth();
        }

        // Bu tarih aralığındaki ve bu görevdeki puanları topla
        double toplamPuan = tum.stream()
                .filter(t -> t.getGorevAdi().equalsIgnoreCase(h.getGorevAdi()))
                .filter(t -> {
                    try {
                        LocalDate d = LocalDate.parse(t.getTarih());
                        return !d.isBefore(start) && !d.isAfter(end);
                    } catch (Exception e) { return false; }
                })
                .mapToDouble(TamamlananGorev::getHesaplananPuan)
                .sum();

        // İlerleme oranını hesapla (0.0 ile 1.0 arası)
        double oran = h.getHedefPuan() <= 0 ? 0 : Math.min(1.0, toplamPuan / h.getHedefPuan());

        // 3. Başlık
        Label title = new Label(h.getAd());
        title.getStyleClass().add("label");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // === [YENİ] KUTLAMA MANTIĞI BAŞLANGICI ===
        if (oran >= 1.0) {
            // Kartın rengini altın sarısı yap
            card.setStyle("-fx-background-color: #fff9c4; -fx-border-color: #f1c40f; -fx-border-width: 2; -fx-background-radius: 10; -fx-border-radius: 10;");
            
            // Tebrik yazısı
            Label congrats = new Label("🎉 TEBRİKLER! HEDEF TAMAMLANDI 🎉");
            congrats.setStyle("-fx-text-fill: #d35400; -fx-font-weight: bold; -fx-font-size: 13px;");
            congrats.setAlignment(Pos.CENTER);
            congrats.setMaxWidth(Double.MAX_VALUE);
            
            // Kutlama butonu
            Button btnKutla = new Button("🏆 Zaferi Kutla");
            btnKutla.setStyle("-fx-background-color: #f1c40f; -fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-cursor: hand;");
            btnKutla.setMaxWidth(Double.MAX_VALUE);
            
            // Butona basınca ne olacak?
            btnKutla.setOnAction(e -> {
                SesYoneticisi.oynat(SesYoneticisi.Ses.BASARIM); // Müzik çal
                new Alert(Alert.AlertType.INFORMATION, "Harika bir iş çıkardın! Bu disiplinle her şeyi başarırsın.").show();
            });
            
            // Yazıyı ve butonu karta ekle
            card.getChildren().add(congrats);
            card.getChildren().add(btnKutla);
        }
        // === [YENİ] KUTLAMA MANTIĞI BİTİŞİ ===

        // 4. Alt Başlık (Görev Adı)
        Label sub = new Label("Görev: " + h.getGorevAdi());
        sub.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

        // 5. İlerleme Çubuğu (Progress Bar)
        ProgressBar pb = new ProgressBar(0);
        pb.setPrefWidth(320);
        pb.getStyleClass().add("goals-progress");
        
        // Eğer tamamlandıysa bar yeşil olsun
        if (oran >= 1.0) pb.setStyle("-fx-accent: #27ae60;");

        // Barın dolma animasyonu
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(800),
                new KeyValue(pb.progressProperty(), oran, Interpolator.EASE_BOTH)));
        tl.play();

        // 6. Özet Yazısı (Örn: 150 / 300 puan)
        Label summary = new Label(String.format("%.0f / %.0f puan", toplamPuan, h.getHedefPuan()));

        // 7. Silme Butonu (Sağ altta)
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button del = new Button("Sil");
        del.getStyleClass().add("delete-button");
        del.setOnAction(e -> {
            hedefler.removeIf(x -> Objects.equals(x.getId(), h.getId()));
            VeriYoneticisi.hedefleriKaydet(hedefler);
            // Kartı ekrandan kaldır
            if (card.getParent() instanceof Pane) {
                ((Pane)card.getParent()).getChildren().remove(card);
            }
            // Gerekirse ana ekranı yenile
            if (onAnyChange != null) onAnyChange.run();
        });
        actions.getChildren().add(del);

        // 8. Tüm parçaları karta ekle
        card.getChildren().addAll(title, sub, pb, summary, actions);
        
        return card;
    }

    private static void uyar(String m) {
        new Alert(Alert.AlertType.WARNING, m, ButtonType.OK).showAndWait();
    }
}
