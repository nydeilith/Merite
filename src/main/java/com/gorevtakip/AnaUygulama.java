package com.gorevtakip;

import javafx.animation.*;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

public class AnaUygulama extends Application {

    private boolean isDarkMode = false;
    private static final double MENU_WIDTH = 240;
    private static final Duration MENU_ANIM = Duration.millis(200); // Şu an kullanılmasa da dursun

    private final StackPane contentArea = new StackPane();
    private List<TamamlananGorev> tumGorevler;
    private List<HarcananOdul> tumHarcamalar;
    
    private ObservableList<TamamlananGorev> seciliGunGorevleri;
    private TableView<TamamlananGorev> gorevTablosu;
    private DatePicker tarihSecici;
    private ComboBox<Gorev> gorevSecici;
    private TextField miktarInput;
    
    private Label lblBugunPuan, lblSeviye, lblCuzdan;

    // Kronometre
    private Timeline timer = null;
    private long chronoSeconds = 0;
    private boolean running = false;
    private Label timerLabelRef;
    private Button startStopRef, saveBtnRef;
    private ComboBox<Gorev> cbDakikaRef;
    private DatePicker dpRef;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("LLLL yyyy", new Locale("tr","TR"));

    @Override
    public void start(Stage primaryStage) {
        tumGorevler = new ArrayList<>(VeriYoneticisi.verileriYukle());
        tumHarcamalar = new ArrayList<>(VeriYoneticisi.harcamalariYukle());
        showSplashScreen(primaryStage);
    }

    private void showSplashScreen(Stage mainStage) {
        Stage splashStage = new Stage();
        splashStage.initStyle(StageStyle.UNDECORATED);
        VBox splashLayout = new VBox(20);
        splashLayout.setAlignment(Pos.CENTER);
        splashLayout.setStyle("-fx-background-color: #0b1a33; -fx-padding: 30; -fx-background-radius: 10; -fx-border-color: #d4af37; -fx-border-width: 2;");

        try {
            if (getClass().getResource("/images/splash_banner.jpg") != null) {
                ImageView bannerView = new ImageView(new Image(getClass().getResourceAsStream("/images/splash_banner.jpg")));
                bannerView.setFitWidth(450); bannerView.setPreserveRatio(true);
                splashLayout.getChildren().add(bannerView);
            }
        } catch (Exception e) {}

        Label loadingLabel = new Label("Merite Başlatılıyor...");
        loadingLabel.setStyle("-fx-text-fill: #d4af37; -fx-font-size: 16px; -fx-font-weight: bold;");
        splashLayout.getChildren().addAll(loadingLabel, new ProgressBar(-1.0));

        Scene splashScene = new Scene(splashLayout, 550, 400);
        splashScene.setFill(Color.TRANSPARENT);
        splashStage.setScene(splashScene);
        splashStage.centerOnScreen();
        splashStage.show();

        PauseTransition delay = new PauseTransition(Duration.seconds(2.0));
        delay.setOnFinished(e -> { splashStage.close(); showMainWindow(mainStage); });
        delay.play();
    }

    private void showMainWindow(Stage stage) {
        try {
            if (getClass().getResource("/images/app_icon.jpg") != null) {
                stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/app_icon.jpg")));
            }
        } catch (Exception e) {}
        
        stage.setTitle("Merite");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("content-background");

        root.setTop(buildHeader());
        root.setLeft(buildSideMenu());

        contentArea.getChildren().setAll(buildHomeView());
        BorderPane.setMargin(contentArea, new Insets(0));
        root.setCenter(contentArea);

        Scene scene = new Scene(root, 1280, 800);
        if (getClass().getResource("/style.css") != null)
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        stage.setOnCloseRequest(ev -> {
            SesYoneticisi.oynat(SesYoneticisi.Ses.CIKIS);
            if (running) {
                ev.consume();
                new Alert(Alert.AlertType.WARNING, "Kronometre çalışıyor!").show();
            }
        });

        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMaximized(true);
        stage.show();
    }

    private HBox buildHeader() {
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        
        TextField search = new TextField(); 
        search.setPromptText("Ara..."); 
        search.setPrefWidth(300);
        search.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                contentArea.getChildren().setAll(buildHomeView());
                filtreleVeGuncelleTablo(search.getText());
            }
        });

        HBox header = new HBox(15, spacer, search);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 30, 15, 30));
        header.setStyle("-fx-background-color: transparent;");
        return header;
    }

    /* === YAN MENÜ === */
    private VBox buildSideMenu() {
        // LOGO
        HBox brandBox = new HBox(15);
        brandBox.setAlignment(Pos.CENTER_LEFT);
        brandBox.setPadding(new Insets(20, 20, 40, 20));
        
        try {
            if (getClass().getResource("/images/app_icon.jpg") != null) {
                ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/images/app_icon.jpg")));
                logo.setFitWidth(32); logo.setFitHeight(32);
                
                Label brandName = new Label("Merite");
                brandName.getStyleClass().add("brand-label");
                
                brandBox.getChildren().addAll(logo, brandName);
            }
        } catch (Exception e) {}

        ToggleGroup group = new ToggleGroup();
        ToggleButton btnHome = makeMenuButton("Ana Ekran", "🏠", group); btnHome.setSelected(true);
        ToggleButton btnManage = makeMenuButton("Görevleri Yönet", "🛠️", group);
        ToggleButton btnReports = makeMenuButton("Raporlar", "📈", group);
        ToggleButton btnGoals = makeMenuButton("Hedefler", "🏆", group);
        ToggleButton btnStore = makeMenuButton("Mağaza", "🛍️", group);
        ToggleButton btnCalendar = makeMenuButton("Takvim", "🗓️", group);
        ToggleButton btnTimer = makeMenuButton("Kronometre", "⏱️", group);

        btnHome.setOnAction(e -> navTo(buildHomeView()));
        btnManage.setOnAction(e -> navTo(GorevYoneticiPenceresi.buildView(this::gorevTanimlariDegisti)));
        btnReports.setOnAction(e -> navTo(RaporlamaPenceresi.buildView()));
        btnStore.setOnAction(e -> navTo(OdulMagazasiPenceresi.buildView(this::dashboardIstatistikGuncelle)));
        btnGoals.setOnAction(e -> navTo(HedeflerPenceresi.buildView(tumGorevler, null)));
        btnCalendar.setOnAction(e -> navTo(buildCalendarView()));
        btnTimer.setOnAction(e -> navTo(buildTimerView()));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        HBox switchBox = new HBox(createThemeSwitch());
        switchBox.setAlignment(Pos.CENTER);
        switchBox.setPadding(new Insets(20));

        VBox menu = new VBox(5, brandBox, btnHome, btnManage, btnReports,
                              btnGoals, btnStore, new Separator(),
                              btnCalendar, btnTimer, spacer, switchBox);
        menu.setPadding(new Insets(10));
        menu.getStyleClass().add("side-drawer");
        
        return menu;
    }

    private void navTo(Node view) {
        SesYoneticisi.oynat(SesYoneticisi.Ses.SAYFA);
        contentArea.getChildren().setAll(view);
        dashboardIstatistikGuncelle();
    }

    private ToggleButton makeMenuButton(String text, String icon, ToggleGroup group) {
        Label iconLbl = new Label(icon);
        Label textLbl = new Label(text);
        
        HBox box = new HBox(15, iconLbl, textLbl); 
        box.setAlignment(Pos.CENTER_LEFT);
        
        ToggleButton b = new ToggleButton();
        b.setGraphic(box);
        b.setToggleGroup(group);
        b.setMaxWidth(Double.MAX_VALUE);
        b.getStyleClass().add("menu-item");
        
        return b;
    }

    /* === HOME VIEW === */
    private Node buildHomeView() {
        VBox mainLayout = new VBox(25);
        mainLayout.setPadding(new Insets(30));

        Label pageTitle = new Label("Genel Bakış");
        pageTitle.getStyleClass().add("header-title");

        HBox statsContainer = new HBox(20);
        double toplamPuan = tumGorevler.stream().mapToDouble(TamamlananGorev::getHesaplananPuan).sum();
        double harcanan = tumHarcamalar.stream().mapToDouble(HarcananOdul::getHarcananPuan).sum();
        double bugunPuan = tumGorevler.stream()
                .filter(t -> t.getTarih().equals(LocalDate.now().toString()))
                .mapToDouble(TamamlananGorev::getHesaplananPuan).sum();
        int seviye = (int) Math.sqrt(toplamPuan / 5.0); if (seviye < 1) seviye = 1;

        Node cardBugun = createStatCard("Bugünkü Puan", String.format("+%.0f", bugunPuan), "Hedeflere odaklan!", "#3498db");
        Node cardSeviye = createStatCard("Seviye " + seviye, getUnvan(seviye), "Toplam: " + (int)toplamPuan, "#e67e22");
        Node cardCuzdan = createStatCard("Cüzdan", String.format("%.0f Puan", toplamPuan - harcanan), "Mağaza Bakiyesi", "#27ae60");

        lblBugunPuan = (Label) ((VBox)cardBugun).getChildren().get(1);
        lblSeviye = (Label) ((VBox)cardSeviye).getChildren().get(1);
        lblCuzdan = (Label) ((VBox)cardCuzdan).getChildren().get(1);
        statsContainer.getChildren().addAll(cardBugun, cardSeviye, cardCuzdan);

        VBox addPanel = new VBox(10);
        addPanel.getStyleClass().add("card-pane");
        addPanel.setPadding(new Insets(20));
        
        Label lblAddHeader = new Label("Hızlı Görev Girişi");
        lblAddHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        tarihSecici = new DatePicker(LocalDate.now());
        gorevSecici = new ComboBox<>(); gorevSecici.setPrefWidth(200); guncelleGorevSecici();
        miktarInput = new TextField(); miktarInput.setPromptText("Miktar");
        Label birimEtiketi = new Label("birim");
        gorevSecici.valueProperty().addListener((obs, o, n) -> { if (n != null) birimEtiketi.setText(n.getBirim()); });
        Button btnEkle = new Button("Tamamla"); 
        btnEkle.setOnAction(e -> gorevEkle(miktarInput.getText(), tarihSecici.getValue()));

        HBox formRow = new HBox(15, tarihSecici, gorevSecici, miktarInput, birimEtiketi, btnEkle);
        formRow.setAlignment(Pos.CENTER_LEFT);
        addPanel.getChildren().addAll(lblAddHeader, formRow);

        seciliGunGorevleri = FXCollections.observableArrayList();
        gorevTablosu = new TableView<>(seciliGunGorevleri);
        gorevTablosu.setPlaceholder(new Label("Kayıt yok."));
        gorevTablosu.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        gorevTablosu.setPrefHeight(400);
        
        setupTableColumns();
        tabloyuGuncelle(null);
        tarihSecici.valueProperty().addListener((o,old,newVal) -> tabloyuGuncelle(null));

        mainLayout.getChildren().addAll(pageTitle, statsContainer, addPanel, gorevTablosu);
        
        ScrollPane scroll = new ScrollPane(mainLayout);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        return scroll;
    }

    private Node createStatCard(String title, String value, String subtitle, String accentColor) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(20));
        card.setMinWidth(250);
        card.getStyleClass().add("card-pane");
        card.setStyle("-fx-border-color: " + accentColor + "; -fx-border-width: 0 0 0 4;");

        Label lblTitle = new Label(title); lblTitle.setStyle("-fx-opacity: 0.7; -fx-font-weight: bold;");
        Label lblValue = new Label(value); lblValue.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        Label lblSub = new Label(subtitle); lblSub.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-size: 12px;");
        
        card.getChildren().addAll(lblTitle, lblValue, lblSub);
        return card;
    }

    /* === THEME SWITCH === */
    private Node createThemeSwitch() {
        Rectangle track = new Rectangle(40, 20, Color.valueOf("#4b5563")); 
        track.setArcWidth(20); track.setArcHeight(20);
        Circle thumb = new Circle(8, Color.WHITE);
        thumb.setTranslateX(-10);
        
        StackPane switchPane = new StackPane(track, thumb);
        switchPane.setCursor(javafx.scene.Cursor.HAND);
        
        switchPane.setOnMouseClicked(e -> {
            isDarkMode = !isDarkMode;
            TranslateTransition tt = new TranslateTransition(Duration.millis(200), thumb);
            if (isDarkMode) {
                tt.setToX(10); track.setFill(Color.valueOf("#3b82f6"));
            } else {
                tt.setToX(-10); track.setFill(Color.valueOf("#4b5563"));
            }
            tt.play();
            toggleTheme(switchPane.getScene());
        });
        return switchPane;
    }

    private void toggleTheme(Scene scene) {
        scene.getStylesheets().clear();
        if (isDarkMode) {
            if (getClass().getResource("/dark.css") != null)
                scene.getStylesheets().add(getClass().getResource("/dark.css").toExternalForm());
        } else {
            if (getClass().getResource("/style.css") != null)
                scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        }
    }

    // --- Tablo kolonları ---
    private void setupTableColumns() {
        TableColumn<TamamlananGorev, String> cAd = new TableColumn<>("Görev");
        cAd.setCellValueFactory(new PropertyValueFactory<>("gorevAdi"));

        TableColumn<TamamlananGorev, Double> cMiktar = new TableColumn<>("Miktar");
        cMiktar.setCellValueFactory(new PropertyValueFactory<>("yapilanMiktar"));

        TableColumn<TamamlananGorev, String> cBirim = new TableColumn<>("Birim");
        cBirim.setCellValueFactory(new PropertyValueFactory<>("birim"));

        TableColumn<TamamlananGorev, Double> cPuan = new TableColumn<>("Puan");
        cPuan.setCellValueFactory(new PropertyValueFactory<>("hesaplananPuan"));

        TableColumn<TamamlananGorev, Void> cSil = new TableColumn<>("");
        cSil.setCellFactory(col -> new TableCell<>() {
            final Button btn = new Button("Sil");
            { btn.getStyleClass().add("delete-button"); }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); }
                else {
                    btn.setOnAction(e -> {
                        TamamlananGorev g = getTableView().getItems().get(getIndex());
                        tumGorevler.remove(g);
                        VeriYoneticisi.verileriKaydet(tumGorevler);
                        tabloyuGuncelle(null);
                        dashboardIstatistikGuncelle();
                    });
                    setGraphic(btn);
                }
            }
        });
        gorevTablosu.getColumns().setAll(cAd, cMiktar, cBirim, cPuan, cSil);
    }

    /* ================= TAKVİM (FULL) ================= */
    private Node buildCalendarView() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        final YearMonth[] currentYM = { YearMonth.now() };
        final LocalDate[] selectedDate = { LocalDate.now() };

        VBox left = new VBox(8);
        left.setPrefWidth(420);

        Label monthLabel = new Label(MONTH_FMT.format(LocalDate.now()));
        monthLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Button prev = new Button("◀");
        Button next = new Button("▶");

        HBox monthBar = new HBox(15, prev, monthLabel, next);
        monthBar.setAlignment(Pos.CENTER_LEFT);

        GridPane monthGrid = new GridPane();
        monthGrid.setHgap(6);
        monthGrid.setVgap(6);

        String[] dayNames = {"Pzt","Sal","Çar","Per","Cum","Cmt","Paz"};
        for (int c = 0; c < 7; c++) {
            Label hdr = new Label(dayNames[c]);
            hdr.setStyle("-fx-font-weight: bold; -fx-opacity: 0.7;");
            GridPane.setHalignment(hdr, HPos.CENTER);
            monthGrid.add(hdr, c, 0);
        }

        VBox right = new VBox(10);
        Label weekTitle = new Label(); weekTitle.setStyle("-fx-font-weight: bold;");
        Label weekTotalPts = new Label();
        Label weekTotalsByUnit = new Label();
        
        VBox summaryBox = new VBox(6, weekTitle, weekTotalPts, weekTotalsByUnit);
        summaryBox.setPadding(new Insets(10));
        summaryBox.getStyleClass().add("card-pane");


        TitledPane weeklyPane = new TitledPane("Haftalık Özet", summaryBox);
        weeklyPane.setExpanded(true);
        weeklyPane.setCollapsible(false);

        VBox dayPanel = buildCalendarRightPanel(selectedDate);

        Runnable rebuildWeekSummary = () -> {
            LocalDate sel = selectedDate[0];
            WeekFields wf = WeekFields.of(new Locale("tr","TR"));
            LocalDate start = sel.with(wf.dayOfWeek(), 1);
            LocalDate end   = sel.with(wf.dayOfWeek(), 7);

            List<TamamlananGorev> haftalik = tumGorevler.stream()
                    .filter(t -> {
                        try {
                            LocalDate dt = LocalDate.parse(t.getTarih(), ISO);
                            return !dt.isBefore(start) && !dt.isAfter(end);
                        } catch(Exception e){return false;}
                    })
                    .collect(Collectors.toList());

            double toplam = haftalik.stream()
                    .mapToDouble(TamamlananGorev::getHesaplananPuan).sum();

            Map<String, Double> birimeGore = haftalik.stream().collect(Collectors.groupingBy(
                    TamamlananGorev::getBirim,
                    Collectors.summingDouble(TamamlananGorev::getYapilanMiktar)
            ));

            String ozet = birimeGore.isEmpty() ? "-" :
                    birimeGore.entrySet().stream()
                            .map(e -> String.format("%.0f %s", e.getValue(), e.getKey()))
                            .collect(Collectors.joining(" | "));

            weekTitle.setText(start + " — " + end);
            weekTotalPts.setText(String.format("Toplam Puan: %.2f", toplam));
            weekTotalsByUnit.setText("Miktarlar: " + ozet);
        };

        final Runnable[] rebuildMonth = new Runnable[1];
        rebuildMonth[0] = () -> {
            monthGrid.getChildren().removeIf(n -> {
                Integer row = GridPane.getRowIndex(n);
                return row != null && row > 0;
            });

            monthLabel.setText(MONTH_FMT.format(currentYM[0].atDay(1)));
            LocalDate first = currentYM[0].atDay(1);
            int firstDow = first.getDayOfWeek().getValue();
            int daysInMonth = currentYM[0].lengthOfMonth();

            Map<LocalDate, Double> gunlukPuan = tumGorevler.stream()
                    .collect(Collectors.groupingBy(
                            g -> LocalDate.parse(g.getTarih(), ISO),
                            Collectors.summingDouble(TamamlananGorev::getHesaplananPuan)
                    ));

            int day = 1;
            for (int r = 1; r <= 6; r++) {
                for (int c = 0; c < 7; c++) {
                    if (r == 1 && c < firstDow - 1) {
                        monthGrid.add(new Label(""), c, r);
                    } else if (day <= daysInMonth) {
                        LocalDate d = currentYM[0].atDay(day);
                        double puan = gunlukPuan.getOrDefault(d, 0.0);

                        VBox cell = buildCalendarCell(d, puan, selectedDate[0].equals(d));
                        
                        cell.setOnMouseClicked(ev -> {
                            selectedDate[0] = d;
                            SesYoneticisi.oynat(SesYoneticisi.Ses.SAYFA);
                            rebuildMonth[0].run();
                            right.getChildren().setAll(weeklyPane, buildCalendarRightPanel(selectedDate));
                            rebuildWeekSummary.run();
                        });

                        monthGrid.add(cell, c, r);
                        day++;
                    }
                }
            }
        };

        prev.setOnAction(e -> { currentYM[0] = currentYM[0].minusMonths(1); rebuildMonth[0].run(); });
        next.setOnAction(e -> { currentYM[0] = currentYM[0].plusMonths(1); rebuildMonth[0].run(); });

        left.getChildren().addAll(monthBar, monthGrid);
        right.getChildren().addAll(weeklyPane, dayPanel);
        right.setPadding(new Insets(0,0,0,15));
        HBox.setHgrow(right, Priority.ALWAYS);

        rebuildMonth[0].run();
        rebuildWeekSummary.run();

        HBox main = new HBox(10, left, new Separator(javafx.geometry.Orientation.VERTICAL), right);
        root.setCenter(main);
        
        return root;
    }

    private VBox buildCalendarCell(LocalDate date, double gunPuani, boolean selected) {
        Label dayLbl = new Label(String.valueOf(date.getDayOfMonth()));
        Label ptsLbl = new Label(gunPuani > 0 ? String.format("▲ %.0f", gunPuani) : "");
        ptsLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #27ae60;");

        VBox box = new VBox(2, dayLbl, ptsLbl);
        box.setPadding(new Insets(6));
        box.setAlignment(Pos.TOP_LEFT);
        box.setPrefSize(52, 52);

        String bgColor = selected ? (isDarkMode ? "#3d5afe" : "#e3f2fd") : (isDarkMode ? "#2d2d2d" : "#ffffff");
        String borderColor = isDarkMode ? "#444" : "#e0e0e0";
        
        box.setStyle("-fx-background-color: " + bgColor + "; -fx-border-color: " + borderColor
                + "; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand;");
        
        if (selected && isDarkMode) dayLbl.setStyle("-fx-text-fill: white;");
        return box;
    }

    private VBox buildCalendarRightPanel(LocalDate[] selectedDateRef) {
        VBox root = new VBox(10);
        Label title = new Label("Seçilen Gün: " + selectedDateRef[0]);
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        ObservableList<TamamlananGorev> gunluk = FXCollections.observableArrayList();
        TableView<TamamlananGorev> table = new TableView<>(gunluk);
        table.setPrefHeight(400);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<TamamlananGorev, String> cAd = new TableColumn<>("Görev");
        cAd.setCellValueFactory(new PropertyValueFactory<>("gorevAdi"));

        TableColumn<TamamlananGorev, Double> cM = new TableColumn<>("Miktar");
        cM.setCellValueFactory(new PropertyValueFactory<>("yapilanMiktar"));

        TableColumn<TamamlananGorev, Double> cP = new TableColumn<>("Puan");
        cP.setCellValueFactory(new PropertyValueFactory<>("hesaplananPuan"));

        TableColumn<TamamlananGorev, Void> cSil = new TableColumn<>("");
        cSil.setPrefWidth(50);
        cSil.setCellFactory(col -> new TableCell<>() {
            final Button btn = new Button("X");
            { btn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 10px;"); }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                btn.setOnAction(e -> {
                    TamamlananGorev g = getTableView().getItems().get(getIndex());
                    tumGorevler.remove(g);
                    VeriYoneticisi.verileriKaydet(tumGorevler);
                    guneGoreYukleTakvim(selectedDateRef[0], gunluk);
                    dashboardIstatistikGuncelle();
                });
                setGraphic(btn);
            }
        });

        table.getColumns().setAll(cAd, cM, cP, cSil);
        guneGoreYukleTakvim(selectedDateRef[0], gunluk);
        
        root.getChildren().addAll(title, table);
        return root;
    }

    private void guneGoreYukleTakvim(LocalDate tarih, ObservableList<TamamlananGorev> hedefListe) {
        if (hedefListe == null) return;
        hedefListe.clear();

        hedefListe.addAll(
                tumGorevler.stream()
                        .filter(t -> {
                            String s = t.getTarih();
                            if (s == null) return false;
                            try {
                                LocalDate dt = LocalDate.parse(s, ISO);
                                return dt.equals(tarih);
                            } catch (Exception ex) {
                                return s.equals(tarih.toString());
                            }
                        })
                        .collect(Collectors.toList())
        );
    }

    /* ================= KRONOMETRE (FULL) ================= */
    private Node buildTimerView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);

        Label title = new Label("Kronometre");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        timerLabelRef = new Label();
        timerLabelRef.getStyleClass().add("timer-label");

        updateTimerLabel();

        GridPane form = new GridPane();
        form.setHgap(15); form.setVgap(15);
        form.setAlignment(Pos.CENTER);

        dpRef = new DatePicker(dpRef != null ? dpRef.getValue() : LocalDate.now());
        
        cbDakikaRef = new ComboBox<>();
        cbDakikaRef.setPromptText("Görev Seçiniz (dakika)");
        cbDakikaRef.setPrefWidth(250);
        
        List<Gorev> dakikaGorevler = VeriYoneticisi.gorevTanimlariniYukle().stream()
                .filter(g -> g.getBirim() != null &&
                        g.getBirim().trim().toLowerCase(Locale.ROOT).contains("dakika"))
                .collect(Collectors.toList());
        cbDakikaRef.getItems().setAll(dakikaGorevler);
        if (cbDakikaRef.getValue() == null && !dakikaGorevler.isEmpty())
            cbDakikaRef.getSelectionModel().selectFirst();

        form.add(new Label("Tarih:"), 0, 0); form.add(dpRef, 1, 0);
        form.add(new Label("Görev:"), 0, 1); form.add(cbDakikaRef, 1, 1);

        startStopRef = new Button(running ? "Durdur" : "Başlat");
        startStopRef.setPrefWidth(120);
        startStopRef.setStyle("-fx-font-size: 14px; -fx-padding: 10;");

        saveBtnRef = new Button("Kaydet");
        saveBtnRef.setPrefWidth(120);
        saveBtnRef.setStyle("-fx-font-size: 14px; -fx-padding: 10;");
        saveBtnRef.setDisable(!running && chronoSeconds == 0);

        Button resetBtn = new Button("Sıfırla");
        resetBtn.setPrefWidth(120);
        resetBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10; -fx-background-color: #e74c3c; -fx-text-fill: white;");

        HBox buttons = new HBox(15, startStopRef, saveBtnRef, resetBtn);
        buttons.setAlignment(Pos.CENTER);

        ensureTimer();

        startStopRef.setOnAction(e -> {
            if (!running) {
                running = true;
                startStopRef.setText("Durdur");
                startStopRef.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10;");
                saveBtnRef.setDisable(true);
                timer.play();
            } else {
                running = false;
                startStopRef.setText("Başlat");
                startStopRef.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10;");
                timer.stop();
                saveBtnRef.setDisable(chronoSeconds == 0);
            }
        });

        resetBtn.setOnAction(e -> {
            chronoSeconds = 0;
            updateTimerLabel();
            if (!running) saveBtnRef.setDisable(true);
        });

        saveBtnRef.setOnAction(e -> {
            Gorev g = cbDakikaRef.getValue();
            if (g == null) {
                new Alert(Alert.AlertType.WARNING, "Lütfen listeden bir görev seçin.").show();
                return;
            }
            long minutes = Math.round(chronoSeconds / 60.0);
            if (minutes <= 0) {
                new Alert(Alert.AlertType.WARNING, "Henüz 1 dakika bile olmadı!").show();
                return;
            }
            
            double puan = minutes * g.getPuanPerBirim();
            
            TamamlananGorev t = new TamamlananGorev(
                    dpRef.getValue().toString(), g.getAd(), g.getBirim(), minutes, puan);
            
            tumGorevler.add(t);
            VeriYoneticisi.verileriKaydet(tumGorevler);
            
            SesYoneticisi.oynat(SesYoneticisi.Ses.ONAY);
            new Alert(Alert.AlertType.INFORMATION, 
                    "Kaydedildi: " + minutes + " dk " + g.getAd() + "\nKazandığın Puan: " + puan).show();
            
            chronoSeconds = 0;
            updateTimerLabel();
            saveBtnRef.setDisable(true);
            dashboardIstatistikGuncelle();
        });

        root.getChildren().addAll(title, timerLabelRef, form, buttons);
        return root;
    }

    private void ensureTimer() {
        if (timer == null) {
            timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                chronoSeconds++;
                updateTimerLabel();
            }));
            timer.setCycleCount(Timeline.INDEFINITE);
        }
    }

    private void updateTimerLabel() {
        if (timerLabelRef != null) {
            long h = chronoSeconds / 3600;
            long m = (chronoSeconds % 3600) / 60;
            long s = chronoSeconds % 60;
            timerLabelRef.setText(String.format("%02d:%02d:%02d", h, m, s));
        }
    }

    /* === DASHBOARD GÜNCELLEME === */
    private void dashboardIstatistikGuncelle() { 
        if (lblBugunPuan == null) return;
        tumHarcamalar = VeriYoneticisi.harcamalariYukle();

        double toplamPuan = tumGorevler.stream()
                .mapToDouble(TamamlananGorev::getHesaplananPuan).sum();
        double toplamHarcanan = tumHarcamalar.stream()
                .mapToDouble(HarcananOdul::getHarcananPuan).sum();
        double netBakiye = toplamPuan - toplamHarcanan;
        double bugunPuan = tumGorevler.stream()
                .filter(t -> t.getTarih().equals(LocalDate.now().toString()))
                .mapToDouble(TamamlananGorev::getHesaplananPuan).sum();
        int seviye = (int) Math.sqrt(toplamPuan / 5.0); 
        if (seviye < 1) seviye = 1;

        lblBugunPuan.setText(String.format("+%.0f", bugunPuan));
        lblCuzdan.setText(String.format("%.0f Puan", netBakiye));
        if (lblSeviye != null)
            lblSeviye.setText(getUnvan(seviye));
    }
    
    private String getUnvan(int lvl) {
        if (lvl < 5)  return "Çaylak";
        if (lvl < 10) return "Hevesli";
        if (lvl < 20) return "İstikrarlı";
        if (lvl < 40) return "Uzman";
        if (lvl < 70) return "Üstat";
        return "Siber Ninja";
    }

    private void guncelleGorevSecici() {
        if (gorevSecici != null)
            gorevSecici.getItems().setAll(VeriYoneticisi.gorevTanimlariniYukle());
    }

    private void gorevEkle(String m, LocalDate d) {
        Gorev secili = gorevSecici.getValue();
        if (secili == null || m == null || m.trim().isEmpty()) return;

        try {
            double miktar = Double.parseDouble(m.trim());
            double puan = miktar * secili.getPuanPerBirim();

            TamamlananGorev yeni = new TamamlananGorev(
                    d.toString(),
                    secili.getAd(),
                    secili.getBirim(),
                    miktar,
                    puan
            );
            tumGorevler.add(yeni);
            VeriYoneticisi.verileriKaydet(tumGorevler);
            SesYoneticisi.oynat(SesYoneticisi.Ses.ONAY);
            tabloyuGuncelle(null);
            dashboardIstatistikGuncelle();
            if (miktarInput != null) miktarInput.clear();
        } catch (NumberFormatException ex) {
            new Alert(Alert.AlertType.WARNING, "Miktar için geçerli bir sayı gir.").show();
        }
    }

    private void tabloyuGuncelle(String q) { 
        if (seciliGunGorevleri == null || tarihSecici == null) return;
        seciliGunGorevleri.clear();
        String gun = tarihSecici.getValue().toString();
        seciliGunGorevleri.addAll(
                tumGorevler.stream()
                        .filter(g -> g.getTarih().equals(gun))
                        .filter(g -> q == null ||
                                g.getGorevAdi().toLowerCase().contains(q.toLowerCase()))
                        .collect(Collectors.toList())
        );
    }

    private void filtreleVeGuncelleTablo(String q) { 
        if (gorevTablosu != null) tabloyuGuncelle(q); 
    }

    private void gorevTanimlariDegisti() { 
        guncelleGorevSecici(); 
    }

    public static void main(String[] args) { 
        launch(args); 
    }
}

