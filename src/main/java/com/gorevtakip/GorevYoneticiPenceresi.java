package com.gorevtakip;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

public class GorevYoneticiPenceresi {

    public static Pane buildView(Runnable onChanged) {
        // Verileri yükle
        List<Gorev> tanimli = VeriYoneticisi.gorevTanimlariniYukle();
        ObservableList<Gorev> data = FXCollections.observableArrayList(tanimli);

        // Ana container
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));

        // Başlık
        Label lblTitle = new Label(Messages.get("manage.title"));
        lblTitle.getStyleClass().add("page-title");

        // Form alanları (önce oluştur, aşağıda kullanacağız)
        TextField tfAd = new TextField();
        tfAd.setPromptText(Messages.get("manage.input.name"));

        TextField tfBirim = new TextField();
        tfBirim.setPromptText(Messages.get("manage.input.unit"));

        TextField tfPuan = new TextField();
        tfPuan.setPromptText(Messages.get("manage.input.points"));

        // Tablo
        TableView<Gorev> table = new TableView<>(data);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label(Messages.get("table.empty")));

        // Kolonlar
        TableColumn<Gorev, String> colAd = new TableColumn<>(Messages.get("manage.col.name"));
        colAd.setCellValueFactory(new PropertyValueFactory<>("ad"));

        TableColumn<Gorev, String> colBirim = new TableColumn<>(Messages.get("manage.col.unit"));
        colBirim.setCellValueFactory(new PropertyValueFactory<>("birim"));

        TableColumn<Gorev, Double> colPuan = new TableColumn<>(Messages.get("manage.col.pointsPerUnit"));
        colPuan.setCellValueFactory(new PropertyValueFactory<>("puanPerBirim"));

        TableColumn<Gorev, Void> colAction = new TableColumn<>(Messages.get("manage.col.action"));
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button(Messages.get("manage.delete"));

            {
                btn.getStyleClass().add("danger-button");
                btn.setOnAction(e -> {
                    Gorev g = getTableView().getItems().get(getIndex());
                    getTableView().getItems().remove(g);
                    persist(data);
                    if (onChanged != null) {
                        onChanged.run();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        table.getColumns().addAll(colAd, colBirim, colPuan, colAction);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Tablo seçimi -> formu doldur
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                tfAd.setText(newV.getAd());
                tfBirim.setText(newV.getBirim());
                tfPuan.setText(String.valueOf(newV.getPuanPerBirim()));
            }
        });

        // Label + field grid
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(8);
        formGrid.setPadding(new Insets(15, 0, 0, 0));

        Label lblAd = new Label(Messages.get("manage.label.name"));
        Label lblBirim = new Label(Messages.get("manage.label.unit"));
        Label lblPuan = new Label(Messages.get("manage.label.points"));

        formGrid.add(lblAd, 0, 0);
        formGrid.add(tfAd, 1, 0);
        formGrid.add(lblBirim, 0, 1);
        formGrid.add(tfBirim, 1, 1);
        formGrid.add(lblPuan, 0, 2);
        formGrid.add(tfPuan, 1, 2);

        // Butonlar
        Button btnEkle = new Button(Messages.get("manage.add"));
        btnEkle.getStyleClass().add("primary-button");

        Button btnGuncelle = new Button(Messages.get("manage.update"));
        btnGuncelle.getStyleClass().add("secondary-button");

        HBox buttonBox = new HBox(10, btnEkle, btnGuncelle);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        formGrid.add(buttonBox, 1, 3);

        // Ekle butonu
        btnEkle.setOnAction(e -> {
            String ad = val(tfAd);
            String birim = val(tfBirim);
            String puanStr = val(tfPuan);

            if (ad.isEmpty() || birim.isEmpty() || puanStr.isEmpty()) {
                warn(Messages.get("manage.warn.emptyFields"));
                return;
            }

            double puan;
            try {
                puan = Double.parseDouble(puanStr.replace(",", "."));
            } catch (NumberFormatException ex) {
                warn(Messages.get("manage.warn.numericPoints"));
                return;
            }

            Gorev g = new Gorev(ad, birim, puan);
            data.add(g);
            persist(data);
            if (onChanged != null) {
                onChanged.run();
            }

            tfAd.clear();
            tfBirim.clear();
            tfPuan.clear();
        });

        // Güncelle butonu
        btnGuncelle.setOnAction(e -> {
            Gorev secili = table.getSelectionModel().getSelectedItem();
            if (secili == null) {
                info(Messages.get("manage.info.selectItem"));
                return;
            }

            String ad = val(tfAd);
            String birim = val(tfBirim);
            String puanStr = val(tfPuan);

            if (ad.isEmpty() || birim.isEmpty() || puanStr.isEmpty()) {
                warn(Messages.get("manage.warn.emptyFields"));
                return;
            }

            double puan;
            try {
                puan = Double.parseDouble(puanStr.replace(",", "."));
            } catch (NumberFormatException ex) {
                warn(Messages.get("manage.warn.numericPoints"));
                return;
            }

            secili.setAd(ad);
            secili.setBirim(birim);
            secili.setPuanPerBirim(puan);
            table.refresh();
            persist(data);
            if (onChanged != null) {
                onChanged.run();
            }
        });

        // Orta alan: başlık + tablo (tablo yüksekliği her zaman tam ekran)
        VBox centerBox = new VBox(10, lblTitle, table);
        centerBox.setFillWidth(true);
        VBox.setVgrow(table, Priority.ALWAYS);

        root.setCenter(centerBox);
        root.setBottom(formGrid);

        return root;
    }

    private static void persist(ObservableList<Gorev> data) {
        VeriYoneticisi.gorevTanimlariniKaydet(new ArrayList<>(data));
    }

    private static String val(TextField tf) {
        return tf.getText() == null ? "" : tf.getText().trim();
    }

    private static void warn(String m) {
        new Alert(Alert.AlertType.WARNING, m, ButtonType.OK).showAndWait();
    }

    private static void info(String m) {
        new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait();
    }
}

