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

    public static Pane buildView(List<TamamlananGorev> tumGorevler, Runnable onAnyChange) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        /* --- Üst: Form --- */
        HBox form = new HBox(10);
        form.setAlignment(Pos.CENTER_LEFT);
        form.setPadding(new Insets(6));

        TextField tfAd = new TextField();
        tfAd.setPromptText(Messages.get("goals.form.titleLabel"));

        ComboBox<Gorev> cbGorev = new ComboBox<>();
        cbGorev.getItems().setAll(VeriYoneticisi.gorevTanimlariniYukle());
        if (!cbGorev.getItems().isEmpty())
            cbGorev.getSelectionModel().selectFirst();

        ComboBox<Hedef.Donem> cbDonem = new ComboBox<>();
        cbDonem.getItems().addAll(Hedef.Donem.HAFTALIK, Hedef.Donem.AYLIK);
        cbDonem.getSelectionModel().select(Hedef.Donem.HAFTALIK);

        TextField tfHedefPuan = new TextField();
        tfHedefPuan.setPromptText(Messages.get("goals.form.targetLabel"));

        Button btnEkle = new Button(Messages.get("goals.form.addButton"));
        btnEkle.setId("ekle-butonu");

        form.getChildren().addAll(
                new Label(Messages.get("goals.form.titleLabel")),
                tfAd,
                new Label(Messages.get("goals.form.taskLabel")),
                cbGorev,
                new Label(Messages.get("goals.form.periodLabel")),
                cbDonem,
                new Label(Messages.get("goals.form.targetLabel")),
                tfHedefPuan,
                btnEkle
        );

        /* --- Orta: Kart Izgarası --- */
        TilePane grid = new TilePane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPrefColumns(2);
        grid.setPadding(new Insets(6));

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);

        root.setTop(form);
        root.setCenter(scroll);

        /* --- Veri --- */
        List<Hedef> hedefler = new ArrayList<>(VeriYoneticisi.hedefleriYukle());

        Consumer<List<Hedef>> refresh = hs -> grid.getChildren().setAll(
                hs.stream()
                        .map(h -> buildCard(h, tumGorevler, hs, onAnyChange))
                        .collect(Collectors.toList())
        );
        refresh.accept(hedefler);

        btnEkle.setOnAction(e -> {
            Gorev g = cbGorev.getValue();
            if (g == null) {
                warn(Messages.get("goals.warn.selectTask"));
                return;
            }

            double hedefPuan;
            try {
                hedefPuan = Double.parseDouble(tfHedefPuan.getText().trim());
            } catch (Exception ex) {
                warn(Messages.get("goals.warn.targetNaN"));
                return;
            }

            String baslik;
            if (tfAd.getText().isBlank()) {
                // Boşsa otomatik başlık
                String prefix = (cbDonem.getValue() == Hedef.Donem.HAFTALIK)
                        ? Messages.get("goals.form.period.weekly")
                        : Messages.get("goals.form.period.monthly");
                baslik = prefix + " " + g.getAd();
            } else {
                baslik = tfAd.getText().trim();
            }

            hedefler.add(new Hedef(baslik, g.getAd(), hedefPuan, cbDonem.getValue()));
            VeriYoneticisi.hedefleriKaydet(hedefler);
            refresh.accept(hedefler);

            if (onAnyChange != null) onAnyChange.run();

            tfAd.clear();
            tfHedefPuan.clear();
        });

        return root;
    }

    private static Node buildCard(Hedef h, List<TamamlananGorev> tum, List<Hedef> hedefler, Runnable onAnyChange) {

        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setMinWidth(360);
        card.setPrefWidth(360);
        card.getStyleClass().add("hedef-card");

        /* --- Tarih Aralığı --- */
        LocalDate now = LocalDate.now();
        LocalDate start, end;

        if (h.getDonem() == Hedef.Donem.HAFTALIK) {
            var wf = WeekFields.of(Locale.forLanguageTag("tr-TR"));
            start = now.with(wf.dayOfWeek(), 1);
            end = now.with(wf.dayOfWeek(), 7);
        } else {
            start = YearMonth.from(now).atDay(1);
            end = YearMonth.from(now).atEndOfMonth();
        }

        /* --- Bu hedefin puanlarını hesapla --- */
        double toplamPuan = tum.stream()
                .filter(t -> t.getGorevAdi().equalsIgnoreCase(h.getGorevAdi()))
                .filter(t -> {
                    try {
                        LocalDate d = LocalDate.parse(t.getTarih());
                        return !d.isBefore(start) && !d.isAfter(end);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .mapToDouble(TamamlananGorev::getHesaplananPuan)
                .sum();

        double oran = h.getHedefPuan() <= 0 ? 0
                : Math.min(1.0, toplamPuan / h.getHedefPuan());

        /* --- Başlık --- */
        Label title = new Label(h.getAd());
        title.getStyleClass().add("label");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        /* === Tamamlanma Kutlaması === */
        if (oran >= 1.0) {
            card.setStyle("-fx-background-color: #fff9c4; -fx-border-color: #f1c40f; " +
                    "-fx-border-width: 2; -fx-background-radius: 10; -fx-border-radius: 10;");

            Label congrats = new Label(Messages.get("goals.card.completedBanner"));
            congrats.setStyle("-fx-text-fill: #d35400; -fx-font-weight: bold; -fx-font-size: 13px;");
            congrats.setAlignment(Pos.CENTER);
            congrats.setMaxWidth(Double.MAX_VALUE);

            Button btnCelebrate = new Button(Messages.get("goals.card.completedButton"));
            btnCelebrate.setStyle("-fx-background-color: #f1c40f; -fx-text-fill: #2c3e50; " +
                    "-fx-font-weight: bold; -fx-cursor: hand;");
            btnCelebrate.setMaxWidth(Double.MAX_VALUE);

            btnCelebrate.setOnAction(e -> {
                SesYoneticisi.oynat(SesYoneticisi.Ses.BASARIM);
                new Alert(Alert.AlertType.INFORMATION,
                        Messages.get("goals.card.completedMessage")).show();
            });

            card.getChildren().addAll(congrats, btnCelebrate);
        }

        /* --- Görev Alt Başlığı --- */
        Label sub = new Label(Messages.get("goals.card.goalPrefix") + " " + h.getGorevAdi());
        sub.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

        /* --- Progress Bar --- */
        ProgressBar pb = new ProgressBar(0);
        pb.setPrefWidth(320);
        pb.getStyleClass().add("goals-progress");

        if (oran >= 1.0)
            pb.setStyle("-fx-accent: #27ae60;");

        Timeline tl = new Timeline(new KeyFrame(Duration.millis(800),
                new KeyValue(pb.progressProperty(), oran, Interpolator.EASE_BOTH)));
        tl.play();

        /* --- Özet --- */
        String summaryText;
        if (Messages.get("lang.tr").equalsIgnoreCase("TR")) {
            summaryText = String.format("%.0f / %.0f puan", toplamPuan, h.getHedefPuan());
        } else {
            summaryText = String.format("%.0f / %.0f points", toplamPuan, h.getHedefPuan());
        }
        Label summary = new Label(summaryText);

        /* --- Sil Butonu --- */
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button del = new Button(Messages.get("goals.card.deleteButton"));
        del.getStyleClass().add("delete-button");

        del.setOnAction(e -> {
            hedefler.removeIf(x -> Objects.equals(x.getId(), h.getId()));
            VeriYoneticisi.hedefleriKaydet(hedefler);

            if (card.getParent() instanceof Pane)
                ((Pane) card.getParent()).getChildren().remove(card);

            if (onAnyChange != null)
                onAnyChange.run();
        });

        actions.getChildren().add(del);

        /* --- Kartı Tamamla --- */
        card.getChildren().addAll(title, sub, pb, summary, actions);
        return card;
    }

    private static void warn(String m) {
        new Alert(Alert.AlertType.WARNING, m, ButtonType.OK).showAndWait();
    }
}

