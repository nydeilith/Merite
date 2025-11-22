package com.gorevtakip;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Eskiden Stage açıyordu; artık Pane döndürür (tek sahnede görüntülenir).
 * JSON kalıcılığı: VeriYoneticisi.gorevTanimlariniYukle/Kaydet. 
 */
public class GorevYoneticiPenceresi {

    /** buildView: yan menüden çağrılır. onChanged.run() -> AnaUygulama combobox’ı yeniler. */
    public static Pane buildView(Runnable onChanged) {
        List<Gorev> tanimli = VeriYoneticisi.gorevTanimlariniYukle();
        ObservableList<Gorev> source = FXCollections.observableArrayList(tanimli);

        TableView<Gorev> table = new TableView<>(source);
        table.setPrefHeight(480);

        TableColumn<Gorev, String> cAd = new TableColumn<>("Görev Adı");
        cAd.setCellValueFactory(new PropertyValueFactory<>("ad"));      // 
        cAd.setPrefWidth(240);

        TableColumn<Gorev, String> cBirim = new TableColumn<>("Birim");
        cBirim.setCellValueFactory(new PropertyValueFactory<>("birim")); // 
        cBirim.setPrefWidth(120);

        TableColumn<Gorev, Double> cPuan = new TableColumn<>("Birim Başına Puan");
        cPuan.setCellValueFactory(new PropertyValueFactory<>("puanPerBirim")); // 
        cPuan.setPrefWidth(160);

        TableColumn<Gorev, Void> cSil = new TableColumn<>("İşlem");
        cSil.setCellFactory(col -> new TableCell<>() {
            final Button btn = new Button("Sil");
            { btn.getStyleClass().add("delete-button"); }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                btn.setOnAction(e -> {
                    Gorev g = getTableView().getItems().get(getIndex());
                    source.remove(g);
                    VeriYoneticisi.gorevTanimlariniKaydet(source); // JSON’a yaz
                    if (onChanged != null) onChanged.run();
                });
                setGraphic(btn);
            }
        });

        table.getColumns().setAll(cAd, cBirim, cPuan, cSil);

        TextField tfAd = new TextField(); tfAd.setPromptText("Görev adı");
        TextField tfBirim = new TextField(); tfBirim.setPromptText("Birim (dakika/sayfa)");
        TextField tfPuan = new TextField(); tfPuan.setPromptText("Birim başına puan (örn: 0.5)");

        Button btnEkle = new Button("Ekle");
        btnEkle.setId("ekle-butonu");
        btnEkle.setOnAction(e -> {
            String ad = val(tfAd), birim = val(tfBirim), puanS = val(tfPuan);
            if (ad.isBlank() || birim.isBlank() || puanS.isBlank()) { warn("Tüm alanlar zorunludur."); return; }
            double puan;
            try { puan = Double.parseDouble(puanS); }
            catch (NumberFormatException ex) { warn("Puan sayısal olmalı."); return; }
            source.add(new Gorev(ad, birim, puan));
            VeriYoneticisi.gorevTanimlariniKaydet(source);
            if (onChanged != null) onChanged.run();
            tfAd.clear(); tfBirim.clear(); tfPuan.clear();
        });

        Button btnGuncelle = new Button("Seçileni Güncelle");
        btnGuncelle.setOnAction(e -> {
            Gorev g = table.getSelectionModel().getSelectedItem();
            if (g == null) { info("Önce tabloda bir görev seçin."); return; }
            String ad = val(tfAd), birim = val(tfBirim), puanS = val(tfPuan);
            if (!ad.isBlank()) g.setAd(ad);
            if (!birim.isBlank()) g.setBirim(birim);
            if (!puanS.isBlank()) {
                try { g.setPuanPerBirim(Double.parseDouble(puanS)); }
                catch (NumberFormatException ex) { warn("Puan sayısal olmalı."); return; }
            }
            table.refresh();
            VeriYoneticisi.gorevTanimlariniKaydet(source);
            if (onChanged != null) onChanged.run();
            tfAd.clear(); tfBirim.clear(); tfPuan.clear();
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                tfAd.setText(n.getAd());
                tfBirim.setText(n.getBirim());
                tfPuan.setText(String.valueOf(n.getPuanPerBirim()));
            }
        });

        HBox buttons = new HBox(8, btnEkle, btnGuncelle);
        buttons.setAlignment(Pos.CENTER_LEFT);

        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(8); form.setPadding(new Insets(10));
        form.add(new Label("Ad:"), 0, 0); form.add(tfAd, 1, 0);
        form.add(new Label("Birim:"), 0, 1); form.add(tfBirim, 1, 1);
        form.add(new Label("Puan:"), 0, 2); form.add(tfPuan, 1, 2);
        form.add(buttons, 1, 3);

        VBox root = new VBox(12,
                new Label("Tanımlı Görevler (ekle / güncelle / sil)"),
                table,
                new Separator(),
                form
        );
        root.setPadding(new Insets(10));
        return root;
    }

    private static String val(TextField tf) { return tf.getText() == null ? "" : tf.getText().trim(); }
    private static void warn(String m) { new Alert(Alert.AlertType.WARNING, m, ButtonType.OK).showAndWait(); }
    private static void info(String m) { new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait(); }
}

