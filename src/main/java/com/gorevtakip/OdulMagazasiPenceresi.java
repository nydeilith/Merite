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

        // Verileri yükle
        List<TamamlananGorev> gorevler = VeriYoneticisi.verileriYukle();
        List<HarcananOdul> harcamalar = VeriYoneticisi.harcamalariYukle();
        List<Odul> oduller = new ArrayList<>(VeriYoneticisi.odulleriYukle());

        // Bakiye etiketi
        Label lblBakiye = new Label();
        lblBakiye.getStyleClass().add("store-balance-label");
        updateBalanceLabel(lblBakiye, gorevler, harcamalar);

        HBox topBox = new HBox(lblBakiye);
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new Insets(0, 0, 20, 0));
        root.setTop(topBox);

        // Ödül kartları grid'i
        TilePane grid = new TilePane();
        grid.setHgap(24);
        grid.setVgap(24);
        grid.setPrefColumns(3);
        grid.setPadding(new Insets(10, 0, 10, 0));

        Runnable refreshGrid = () -> {
            grid.getChildren().clear();
            for (Odul odul : oduller) {
                Node card = createOdulCard(
                        odul,
                        oduller,
                        gorevler,
                        harcamalar,
                        grid,
                        lblBakiye,
                        onBalanceChange
                );
                grid.getChildren().add(card);
            }
        };
        refreshGrid.run();

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: transparent;");
        root.setCenter(scroll);

        // Alt kısım: yeni ödül ekleme formu
        Label lblYeniOdul = new Label(Messages.get("store.add.newReward"));

        TextField tfAd = new TextField();
        tfAd.setPromptText(Messages.get("store.add.placeholderName"));
        tfAd.setPrefWidth(220);

        TextField tfFiyat = new TextField();
        tfFiyat.setPromptText(Messages.get("store.add.placeholderCost"));
        tfFiyat.setPrefWidth(120);

        Button btnEkle = new Button(Messages.get("store.add.button"));
        btnEkle.getStyleClass().add("primary-button");
        btnEkle.setDefaultButton(true);

        btnEkle.setOnAction(e -> {
            String ad = tfAd.getText().trim();
            String fy = tfFiyat.getText().trim();

            if (ad.isEmpty() || fy.isEmpty()) {
                new Alert(Alert.AlertType.ERROR,
                        Messages.get("manage.warn.emptyFields")).show();
                return;
            }

            try {
                double bedel = Double.parseDouble(fy);
                Odul yeni = new Odul(ad, bedel);
                oduller.add(yeni);
                VeriYoneticisi.odulleriKaydet(oduller);
                refreshGrid.run();
                tfAd.clear();
                tfFiyat.clear();
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.ERROR,
                        Messages.get("store.add.invalidNumber")).show();
            }
        });

        HBox form = new HBox(10, lblYeniOdul, tfAd, tfFiyat, btnEkle);
        form.setPadding(new Insets(15, 0, 0, 0));
        form.setAlignment(Pos.CENTER_LEFT);
        root.setBottom(form);

        return root;
    }

    // Bakiye hesaplama ve etiketi güncelleme
    private static void updateBalanceLabel(Label lblBakiye,
                                           List<TamamlananGorev> gorevler,
                                           List<HarcananOdul> harcamaList) {

        double toplamKazanilan = gorevler.stream()
                .mapToDouble(TamamlananGorev::getHesaplananPuan)
                .sum();

        double toplamHarcanan = harcamaList.stream()
                .mapToDouble(HarcananOdul::getHarcananPuan)
                .sum();

        double bakiye = toplamKazanilan - toplamHarcanan;
        lblBakiye.setText(Messages.format("store.balance", (int) bakiye));
    }

    private static double computeBalance(List<TamamlananGorev> gorevler,
                                         List<HarcananOdul> harcamaList) {

        double toplamKazanilan = gorevler.stream()
                .mapToDouble(TamamlananGorev::getHesaplananPuan)
                .sum();

        double toplamHarcanan = harcamaList.stream()
                .mapToDouble(HarcananOdul::getHarcananPuan)
                .sum();

        return toplamKazanilan - toplamHarcanan;
    }

    // Tek bir ödül kartını oluşturur
    private static Node createOdulCard(
            Odul odul,
            List<Odul> odulList,
            List<TamamlananGorev> gorevler,
            List<HarcananOdul> harcamaList,
            TilePane parent,
            Label lblBakiyeRef,
            Runnable onBalanceChange
    ) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(20));
        card.setPrefWidth(320);
        card.setMinHeight(200);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("store-card"); // Renk/arka plan CSS'den, dark.css ile uyumlu

        // Başlık
        Label lblAd = new Label(odul.getAd());
        lblAd.getStyleClass().add("store-title-label");
        lblAd.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        // Fiyat
        Label lblFiyat = new Label(
                Messages.format("store.price.format", (int) odul.getBedel())
        );
        lblFiyat.getStyleClass().add("store-price-label");
        lblFiyat.setStyle("-fx-font-size: 16px; -fx-font-weight: 600;");

        // Butonlar
        Button btnSatinAl = new Button(Messages.get("store.buy"));
        btnSatinAl.getStyleClass().add("buy-button");
        btnSatinAl.setMinWidth(90);
        btnSatinAl.setStyle("-fx-font-size: 14px; -fx-padding: 8 20;");

        Button btnSil = new Button(Messages.get("store.delete"));
        btnSil.getStyleClass().add("delete-button");
        btnSil.setMinWidth(90);
        btnSil.setStyle("-fx-font-size: 14px; -fx-padding: 8 20;");

        // Satın alma davranışı
        btnSatinAl.setOnAction(e -> {
            double currentBalance = computeBalance(gorevler, harcamaList);
            if (currentBalance >= odul.getBedel()) {
                HarcananOdul harcama = new HarcananOdul(
                        LocalDate.now().toString(),
                        odul.getAd(),
                        odul.getBedel()
                );
                harcamaList.add(harcama);
                VeriYoneticisi.harcamalariKaydet(harcamaList);

                updateBalanceLabel(lblBakiyeRef, gorevler, harcamaList);
                if (onBalanceChange != null) {
                    onBalanceChange.run(); // Ana ekrandaki cüzdan kartını da güncelle
                }

                Alert ok = new Alert(Alert.AlertType.INFORMATION,
                        Messages.get("store.buy.successMessage"));
                ok.setTitle(Messages.get("store.buy.successTitle"));
                ok.setHeaderText(null);
                ok.show();
            } else {
                Alert err = new Alert(Alert.AlertType.ERROR,
                        Messages.get("store.buy.notEnough"));
                err.setTitle(Messages.get("store.buy.error.balance"));
                err.setHeaderText(null);
                err.show();
            }
        });

        // Silme
        btnSil.setOnAction(e -> {
            odulList.remove(odul);
            VeriYoneticisi.odulleriKaydet(odulList);
            parent.getChildren().remove(card);
        });

        HBox actions = new HBox(12, btnSatinAl, btnSil);
        actions.setAlignment(Pos.CENTER);

        Separator sep = new Separator();
        sep.setOpacity(0.4);

        card.getChildren().addAll(lblAd, lblFiyat, sep, actions);
        return card;
    }
}

