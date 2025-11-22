package com.gorevtakip;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OdulMagazasiPenceresi {

    public static Node buildView(Runnable onBalanceChange) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));

        // Verileri Çek
        List<TamamlananGorev> gorevler = VeriYoneticisi.verileriYukle();
        List<HarcananOdul> harcamalar = VeriYoneticisi.harcamalariYukle();
        List<Odul> oduller = new ArrayList<>(VeriYoneticisi.odulleriYukle());

        // Bakiye Hesapla
        double toplamKazanilan = gorevler.stream()
                .mapToDouble(TamamlananGorev::getHesaplananPuan)
                .sum();
        double toplamHarcanan = harcamalar.stream()
                .mapToDouble(HarcananOdul::getHarcananPuan)
                .sum();
        double bakiye = toplamKazanilan - toplamHarcanan;

        // Üst: Cüzdan Bilgisi
        Label lblBakiye = new Label(String.format("Mevcut Bakiye: %.1f Puan", bakiye));
        lblBakiye.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #22c55e;");
        HBox header = new HBox(lblBakiye);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 20, 0));
        root.setTop(header);

        // Orta: Ödül Kartları (Grid)
        TilePane grid = new TilePane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPrefColumns(3);

        Runnable refreshGrid = () -> {
            grid.getChildren().clear();
            for (Odul odul : oduller) {
                grid.getChildren().add(
                        createOdulCard(odul, oduller, harcamalar, grid, lblBakiye, onBalanceChange)
                );
            }
        };
        refreshGrid.run();

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        root.setCenter(scroll);

        // Alt: Yeni Ödül Ekleme Formu
        HBox form = new HBox(10);
        form.setPadding(new Insets(15, 0, 0, 0));
        form.setAlignment(Pos.CENTER_LEFT);
        // Açık gri yerine koyu border, dark mode'a uyumlu
        form.setStyle("-fx-border-color: #1f2937; -fx-border-width: 1 0 0 0; -fx-padding: 15;");

        TextField tfAd = new TextField();
        tfAd.setPromptText("Ödül Adı (örn: 1 Bölüm Dizi)");
        TextField tfFiyat = new TextField();
        tfFiyat.setPromptText("Bedel (Puan)");

        Button btnEkle = new Button("Mağazaya Ekle");
        // Genel button stilin kalsın diye sadece biraz vurgulu mavi
        btnEkle.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white;");

        btnEkle.setOnAction(e -> {
            String ad = tfAd.getText().trim();
            String fy = tfFiyat.getText().trim();
            if (ad.isEmpty() || fy.isEmpty()) return;

            try {
                double bedel = Double.parseDouble(fy);
                oduller.add(new Odul(ad, bedel));
                VeriYoneticisi.odulleriKaydet(oduller);
                refreshGrid.run();
                tfAd.clear();
                tfFiyat.clear();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Lütfen geçerli bir puan girin.").show();
            }
        });

        form.getChildren().addAll(new Label("Yeni Ödül:"), tfAd, tfFiyat, btnEkle);
        root.setBottom(form);

        return root;
    }

    private static VBox createOdulCard(
            Odul odul,
            List<Odul> odulList,
            List<HarcananOdul> harcamaList,
            TilePane parent,
            Label lblBakiyeRef,
            Runnable onBalanceChange
    ) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setPrefWidth(200);
        card.setAlignment(Pos.CENTER);
        // Dark mode uyumlu kart için CSS sınıfı
        card.getStyleClass().add("store-card");

        Label lblAd = new Label(odul.getAd());
        lblAd.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label lblFiyat = new Label((int) odul.getBedel() + " Puan");
        lblFiyat.getStyleClass().add("store-price-label");

        Button btnSatinAl = new Button("Satın Al");
        btnSatinAl.getStyleClass().add("buy-button");

        btnSatinAl.setOnAction(e -> {
            // Anlık Bakiye Kontrolü
            double kazanilan = VeriYoneticisi.verileriYukle().stream()
                    .mapToDouble(TamamlananGorev::getHesaplananPuan)
                    .sum();
            double harcanan = harcamaList.stream()
                    .mapToDouble(HarcananOdul::getHarcananPuan)
                    .sum();
            double mevcut = kazanilan - harcanan;

            if (mevcut >= odul.getBedel()) {
                // Satın alma işlemi
                harcamaList.add(new HarcananOdul(
                        LocalDate.now().toString(),
                        odul.getAd(),
                        odul.getBedel())
                );
                VeriYoneticisi.harcamalariKaydet(harcamaList);

                SesYoneticisi.oynat(SesYoneticisi.Ses.KASA);

                double yeniBakiye = mevcut - odul.getBedel();
                lblBakiyeRef.setText(String.format("Mevcut Bakiye: %.1f Puan", yeniBakiye));

                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                        "Keyfini çıkar! Ödül alındı.");
                alert.setHeaderText("Satın Alma Başarılı");
                alert.show();

                if (onBalanceChange != null) onBalanceChange.run();
            } else {
                new Alert(Alert.AlertType.ERROR,
                        "Yetersiz Bakiye! Biraz daha çalışman lazım.").show();
            }
        });

        Button btnSil = new Button("Sil");
        // Global delete-button stilini kullan
        btnSil.getStyleClass().add("delete-button");
        btnSil.setOnAction(e -> {
            odulList.remove(odul);
            VeriYoneticisi.odulleriKaydet(odulList);
            parent.getChildren().remove(card);
        });

        HBox actions = new HBox(10, btnSatinAl, btnSil);
        actions.setAlignment(Pos.CENTER);

        card.getChildren().addAll(lblAd, lblFiyat, new Separator(), actions);
        return card;
    }
}

