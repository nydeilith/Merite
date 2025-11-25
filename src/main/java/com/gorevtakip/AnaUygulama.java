package com.gorevtakip;

import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
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

    // ================== Dil & Tema ==================
    private enum Language { TR, EN }
    private Language currentLang = Language.TR;

    private boolean isDarkMode = false;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("LLLL yyyy", new Locale("tr","TR"));

    // ================== UI Referansları ==================
    private final StackPane contentArea = new StackPane();

    private ToggleButton btnHome;
    private ToggleButton btnManage;
    private ToggleButton btnReports;
    private ToggleButton btnGoals;
    private ToggleButton btnStore;
    private ToggleButton btnCalendar;
    private ToggleButton btnTimer;

    private Button btnLangTR;
    private Button btnLangEN;

    private Label lblBugunPuan, lblSeviye, lblCuzdan;

    private DatePicker tarihSecici;
    private ComboBox<Gorev> gorevSecici;
    private TextField miktarInput;
    private Label birimEtiketiRef;

    private ObservableList<TamamlananGorev> seciliGunGorevleri;
    private TableView<TamamlananGorev> gorevTablosu;

    // Kronometre
    private Timeline timer = null;
    private long chronoSeconds = 0;
    private boolean running = false;
    private Label timerLabelRef;
    private Button startStopRef, saveBtnRef;
    private ComboBox<Gorev> cbDakikaRef;
    private DatePicker dpRef;

    // Veri
    private List<TamamlananGorev> tumGorevler;
    private List<HarcananOdul> tumHarcamalar;

    private enum View { HOME, MANAGE, REPORTS, GOALS, STORE, CALENDAR, TIMER }
    private View currentView = View.HOME;

    // =============== Level Sistemi ===============
    private static class LevelInfo {
        final int minPoints;
        final String trName;
        final String enName;
        LevelInfo(int minPoints, String trName, String enName) {
            this.minPoints = minPoints;
            this.trName = trName;
            this.enName = enName;
        }
    }

    // Puan yükseldikçe seviye atlamak zorlaşsın
    private static final LevelInfo[] LEVELS = new LevelInfo[] {
            new LevelInfo(0,    "Başlangıç",    "Rookie"),
            new LevelInfo(200,  "Çaylak",       "Novice"),
            new LevelInfo(500,  "Isınma",       "Getting Warm"),
            new LevelInfo(900,  "İstikrarlı",   "Consistent"),
            new LevelInfo(1400, "Disiplinli",   "Disciplined"),
            new LevelInfo(2000, "Uzman",        "Expert"),
            new LevelInfo(3000, "Üstat",        "Master"),
            new LevelInfo(4500, "Siber Ninja",  "Cyber Ninja"),
            new LevelInfo(6500, "Efsane",       "Legend")
    };

    // ================== Başlatma ==================
    @Override
    public void start(Stage primaryStage) {
        tumGorevler = new ArrayList<>(VeriYoneticisi.verileriYukle());
        tumHarcamalar = new ArrayList<>(VeriYoneticisi.harcamalariYukle());
        showSplashScreen(primaryStage);
    }

    // Basit TR/EN helper
    private String trEn(String tr, String en) {
        return currentLang == Language.TR ? tr : en;
    }

    // Birim çevirisi – sadece ekranda
    private String localizeUnit(String unit) {
        if (unit == null) return "";
        String u = unit.trim().toLowerCase(Locale.ROOT);
        if (currentLang == Language.TR) return unit; // veri zaten TR
        switch (u) {
            case "dakika":
            case "dk":
                return "minute";
            case "saat":
                return "hour";
            case "sayfa":
                return "page";
            default:
                return unit;
        }
    }

    private int computeLevel(double totalPoints) {
        int level = 1;
        for (int i = 0; i < LEVELS.length; i++) {
            if (totalPoints >= LEVELS[i].minPoints) level = i + 1;
        }
        return level;
    }

    private String getLevelName(int level) {
        int idx = Math.max(0, Math.min(level - 1, LEVELS.length - 1));
        return currentLang == Language.TR ? LEVELS[idx].trName : LEVELS[idx].enName;
    }

    private String buildLevelTooltip() {
        StringBuilder sb = new StringBuilder();
        sb.append(trEn("Seviye eşikleri:\n", "Level thresholds:\n"));
        for (int i = 0; i < LEVELS.length; i++) {
            LevelInfo li = LEVELS[i];
            String nameTR = li.trName;
            String nameEN = li.enName;
            sb.append("Lv ").append(i + 1).append(" - ")
              .append(currentLang == Language.TR ? nameTR : nameEN)
              .append(" : ≥ ").append(li.minPoints)
              .append(trEn(" puan", " pts"))
              .append("\n");
        }
        return sb.toString();
    }

    // ================== Splash ==================
    private void showSplashScreen(Stage mainStage) {
        Stage splashStage = new Stage();
        splashStage.initStyle(StageStyle.UNDECORATED);

        VBox splashLayout = new VBox(20);
        splashLayout.setAlignment(Pos.CENTER);
        splashLayout.setStyle(
                "-fx-background-color: #0b1a33; -fx-padding: 30; " +
                "-fx-background-radius: 10; -fx-border-color: #d4af37; -fx-border-width: 2;"
        );

        try {
            if (getClass().getResource("/images/splash_banner.jpg") != null) {
                ImageView bannerView = new ImageView(
                        new Image(getClass().getResourceAsStream("/images/splash_banner.jpg")));
                bannerView.setFitWidth(450);
                bannerView.setPreserveRatio(true);
                splashLayout.getChildren().add(bannerView);
            }
        } catch (Exception ignored) {}

        Label loadingLabel = new Label("Merite Başlatılıyor...");
        loadingLabel.setStyle("-fx-text-fill: #d4af37; -fx-font-size: 16px; -fx-font-weight: bold;");
        splashLayout.getChildren().addAll(loadingLabel, new ProgressBar(-1.0));

        Scene splashScene = new Scene(splashLayout, 550, 400);
        splashScene.setFill(Color.TRANSPARENT);
        splashStage.setScene(splashScene);
        splashStage.centerOnScreen();
        splashStage.show();

        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(e -> {
            splashStage.close();
            showMainWindow(mainStage);
        });
        delay.play();
    }

    // ================== Ana Pencere ==================
    private void showMainWindow(Stage stage) {
        try {
            if (getClass().getResource("/images/app_icon.jpg") != null) {
                stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/app_icon.jpg")));
            }
        } catch (Exception ignored) {}

        stage.setTitle("Merite");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("content-background");

        root.setTop(buildHeader());
        root.setLeft(buildSideMenu());

        contentArea.getChildren().setAll(buildHomeView());
        root.setCenter(contentArea);

        Scene scene = new Scene(root, 1280, 800);
        if (getClass().getResource("/style.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        }

        stage.setOnCloseRequest(ev -> {
            SesYoneticisi.oynat(SesYoneticisi.Ses.CIKIS);
            if (running) {
                ev.consume();
                new Alert(Alert.AlertType.WARNING,
                        trEn("Kronometre çalışıyor!", "Timer is still running!")
                ).show();
            }
        });

        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    private HBox buildHeader() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TextField search = new TextField();
        search.setPromptText(trEn("Ara...", "Search..."));
        search.setPrefWidth(300);
        search.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                if (currentView != View.HOME) {
                    navTo(View.HOME, false);
                }
                filtreleVeGuncelleTablo(search.getText());
            }
        });

        HBox header = new HBox(15, spacer, search);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 30, 15, 30));
        header.setStyle("-fx-background-color: transparent;");
        return header;
    }

    // ================== Yan Menü ==================
    private VBox buildSideMenu() {
        // Logo + isim
        HBox brandBox = new HBox(15);
        brandBox.setAlignment(Pos.CENTER_LEFT);
        brandBox.setPadding(new Insets(20, 20, 40, 20));

        try {
            if (getClass().getResource("/images/app_icon.jpg") != null) {
                ImageView logo = new ImageView(
                        new Image(getClass().getResourceAsStream("/images/app_icon.jpg")));
                logo.setFitWidth(32);
                logo.setFitHeight(32);

                Label brandName = new Label("Merite");
                brandName.getStyleClass().add("brand-label");

                brandBox.getChildren().addAll(logo, brandName);
            }
        } catch (Exception ignored) {}

        ToggleGroup group = new ToggleGroup();

        btnHome     = makeMenuButton(trEn("Ana Ekran", "Home"), "🏠", group);
        btnManage   = makeMenuButton(trEn("Görevleri Yönet", "Manage Tasks"), "🛠️", group);
        btnReports  = makeMenuButton(trEn("Raporlar", "Reports"), "📈", group);
        btnGoals    = makeMenuButton(trEn("Hedefler", "Goals"), "🏆", group);
        btnStore    = makeMenuButton(trEn("Mağaza", "Store"), "🛍️", group);
        btnCalendar = makeMenuButton(trEn("Takvim", "Calendar"), "🗓️", group);
        btnTimer    = makeMenuButton(trEn("Kronometre", "Timer"), "⏱️", group);

        btnHome.setSelected(true);

        btnHome.setOnAction(e     -> navTo(View.HOME, true));
        btnManage.setOnAction(e   -> navTo(View.MANAGE, true));
        btnReports.setOnAction(e  -> navTo(View.REPORTS, true));
        btnGoals.setOnAction(e    -> navTo(View.GOALS, true));
        btnStore.setOnAction(e    -> navTo(View.STORE, true));
        btnCalendar.setOnAction(e -> navTo(View.CALENDAR, true));
        btnTimer.setOnAction(e    -> navTo(View.TIMER, true));

        // Dil butonları
        btnLangTR = new Button("TR");
        btnLangEN = new Button("EN");
        btnLangTR.setMaxWidth(Double.MAX_VALUE);
        btnLangEN.setMaxWidth(Double.MAX_VALUE);

        btnLangTR.setOnAction(e -> setLanguage(Language.TR));
        btnLangEN.setOnAction(e -> setLanguage(Language.EN));

        HBox langBox = new HBox(8, btnLangTR, btnLangEN);
        langBox.setAlignment(Pos.CENTER);
        langBox.setPadding(new Insets(10));

        // Tema switch
        Node themeSwitch = createThemeSwitch();
        HBox switchBox = new HBox(themeSwitch);
        switchBox.setAlignment(Pos.CENTER);
        switchBox.setPadding(new Insets(10));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox menu = new VBox(
                5,
                brandBox,
                btnHome,
                btnManage,
                btnReports,
                btnGoals,
                btnStore,
                new Separator(),
                btnCalendar,
                btnTimer,
                spacer,
                langBox,
                switchBox
        );
        menu.setPadding(new Insets(10));
        menu.getStyleClass().add("side-drawer");

        updateLanguageButtons();
        return menu;
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

    private void updateSideMenuTexts() {
        if (btnHome == null) return;
        btnHome.setGraphic(makeMenuGraphic("🏠", trEn("Ana Ekran", "Home")));
        btnManage.setGraphic(makeMenuGraphic("🛠️", trEn("Görevleri Yönet", "Manage Tasks")));
        btnReports.setGraphic(makeMenuGraphic("📈", trEn("Raporlar", "Reports")));
        btnGoals.setGraphic(makeMenuGraphic("🏆", trEn("Hedefler", "Goals")));
        btnStore.setGraphic(makeMenuGraphic("🛍️", trEn("Mağaza", "Store")));
        btnCalendar.setGraphic(makeMenuGraphic("🗓️", trEn("Takvim", "Calendar")));
        btnTimer.setGraphic(makeMenuGraphic("⏱️", trEn("Kronometre", "Timer")));
    }

    private HBox makeMenuGraphic(String icon, String text) {
        Label iconLbl = new Label(icon);
        Label textLbl = new Label(text);
        HBox box = new HBox(15, iconLbl, textLbl);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void setLanguage(Language lang) {
        if (currentLang == lang) return;
        currentLang = lang;

        // i18n bundle dilini güncelle
        if (currentLang == Language.EN) {
            Messages.setLocale(Locale.ENGLISH);
        } else {
            Messages.setLocale(new Locale("tr"));
        }

        updateLanguageButtons();
        updateSideMenuTexts();
        // Mevcut görünümü yeniden oluştur (bulunduğun sayfada kal)
        navTo(currentView, false);
    }
    private void updateLanguageButtons() {
        if (btnLangTR == null) return;
        btnLangTR.setDisable(currentLang == Language.TR);
        btnLangEN.setDisable(currentLang == Language.EN);
    }

    private Node createThemeSwitch() {
        Rectangle track = new Rectangle(40, 20, Color.valueOf("#4b5563"));
        track.setArcWidth(20);
        track.setArcHeight(20);
        Circle thumb = new Circle(8, Color.WHITE);
        thumb.setTranslateX(-10);

        StackPane switchPane = new StackPane(track, thumb);
        switchPane.setCursor(Cursor.HAND);

        switchPane.setOnMouseClicked(e -> {
            isDarkMode = !isDarkMode;
            TranslateTransition tt = new TranslateTransition(Duration.millis(160), thumb);
            if (isDarkMode) {
                tt.setToX(10);
                track.setFill(Color.valueOf("#3b82f6"));
            } else {
                tt.setToX(-10);
                track.setFill(Color.valueOf("#4b5563"));
            }
            tt.play();
            toggleTheme(switchPane.getScene());
        });

        return switchPane;
    }

    private void toggleTheme(Scene scene) {
        if (scene == null) return;
        scene.getStylesheets().clear();
        String css = isDarkMode ? "/dark.css" : "/style.css";
        if (getClass().getResource(css) != null) {
            scene.getStylesheets().add(getClass().getResource(css).toExternalForm());
        }
    }

    // ================== Navigation ==================
    private void navTo(View view, boolean playSound) {
        currentView = view;
        if (playSound) SesYoneticisi.oynat(SesYoneticisi.Ses.SAYFA);

        Node viewNode;
        switch (view) {
            case MANAGE:
                viewNode = GorevYoneticiPenceresi.buildView(this::gorevTanimlariDegisti);
                break;
            case REPORTS:
                viewNode = RaporlamaPenceresi.buildView();
                break;
            case GOALS:
                viewNode = HedeflerPenceresi.buildView(tumGorevler, null);
                break;
            case STORE:
                viewNode = OdulMagazasiPenceresi.buildView(this::dashboardIstatistikGuncelle);
                break;
            case CALENDAR:
                viewNode = buildCalendarView();
                break;
            case TIMER:
                viewNode = buildTimerView();
                break;
            case HOME:
            default:
                viewNode = buildHomeView();
        }

        contentArea.getChildren().setAll(viewNode);
        if (btnHome != null) {
            btnHome.setSelected(view == View.HOME);
            btnManage.setSelected(view == View.MANAGE);
            btnReports.setSelected(view == View.REPORTS);
            btnGoals.setSelected(view == View.GOALS);
            btnStore.setSelected(view == View.STORE);
            btnCalendar.setSelected(view == View.CALENDAR);
            btnTimer.setSelected(view == View.TIMER);
        }
        dashboardIstatistikGuncelle();
    }

    // ================== Home View ==================
    private Node buildHomeView() {
        VBox mainLayout = new VBox(25);
        mainLayout.setPadding(new Insets(30));

        Label pageTitle = new Label(trEn("Genel Bakış", "Overview"));
        pageTitle.getStyleClass().add("header-title");

        double toplamPuan = tumGorevler.stream()
                .mapToDouble(TamamlananGorev::getHesaplananPuan).sum();
        double harcanan = tumHarcamalar.stream()
                .mapToDouble(HarcananOdul::getHarcananPuan).sum();
        double bugunPuan = tumGorevler.stream()
                .filter(t -> t.getTarih().equals(LocalDate.now().toString()))
                .mapToDouble(TamamlananGorev::getHesaplananPuan).sum();
        int seviye = computeLevel(toplamPuan);

        HBox statsContainer = new HBox(20);

        Node cardBugun = createStatCard(
                trEn("Bugünkü Puan", "Today’s Points"),
                String.format("+%.0f", bugunPuan),
                trEn("Hedeflerine odaklan!", "Focus on your goals!"),
                "#3498db"
        );

        String levelTitle = trEn("Seviye ", "Level ") + seviye;
        String levelValue = getLevelName(seviye);
        String levelSubtitle = trEn("Toplam: ", "Total: ") + (int) toplamPuan;

        Node cardSeviye = createStatCard(
                levelTitle,
                levelValue,
                levelSubtitle,
                "#e67e22"
        );

        Node cardCuzdan = createStatCard(
                trEn("Cüzdan", "Wallet"),
                String.format("%.0f", toplamPuan - harcanan),
                trEn("Mağaza bakiyesi", "Store balance"),
                "#27ae60"
        );

        lblBugunPuan = (Label) ((VBox) cardBugun).getChildren().get(1);
        lblSeviye    = (Label) ((VBox) cardSeviye).getChildren().get(1);
        lblCuzdan    = (Label) ((VBox) cardCuzdan).getChildren().get(1);

        Tooltip.install(lblSeviye, new Tooltip(buildLevelTooltip()));

        statsContainer.getChildren().addAll(cardBugun, cardSeviye, cardCuzdan);

        // Hızlı ekleme paneli
        VBox addPanel = new VBox(10);
        addPanel.getStyleClass().add("card-pane");
        addPanel.setPadding(new Insets(20));

        Label lblAddHeader = new Label(trEn("Hızlı Görev Girişi", "Quick Task Entry"));
        lblAddHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        tarihSecici = new DatePicker(LocalDate.now());
        gorevSecici = new ComboBox<>();
        gorevSecici.setPrefWidth(220);
        guncelleGorevSecici();

        miktarInput = new TextField();
        miktarInput.setPromptText(trEn("Miktar", "Amount"));

        birimEtiketiRef = new Label("");
        gorevSecici.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                birimEtiketiRef.setText(localizeUnit(newV.getBirim()));
            } else {
                birimEtiketiRef.setText("");
            }
        });

        Button btnEkle = new Button(trEn("Tamamla", "Complete"));
        btnEkle.setOnAction(e -> gorevEkle(miktarInput.getText(), tarihSecici.getValue()));

        HBox formRow = new HBox(15, tarihSecici, gorevSecici, miktarInput, birimEtiketiRef, btnEkle);
        formRow.setAlignment(Pos.CENTER_LEFT);
        addPanel.getChildren().addAll(lblAddHeader, formRow);

        // Tablo
        seciliGunGorevleri = FXCollections.observableArrayList();
        gorevTablosu = new TableView<>(seciliGunGorevleri);
        gorevTablosu.setPlaceholder(new Label(trEn("Kayıt yok.", "No records.")));
        gorevTablosu.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        gorevTablosu.setPrefHeight(400);

        setupTableColumns();
        tabloyuGuncelle(null);
        tarihSecici.valueProperty().addListener((o, oldV, newV) -> tabloyuGuncelle(null));

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

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-opacity: 0.7; -fx-font-weight: bold;");
        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        Label lblSub = new Label(subtitle);
        lblSub.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-size: 12px;");

        card.getChildren().addAll(lblTitle, lblValue, lblSub);
        return card;
    }

    private void setupTableColumns() {
        TableColumn<TamamlananGorev, String> cAd = new TableColumn<>(trEn("Görev", "Task"));
        cAd.setCellValueFactory(new PropertyValueFactory<>("gorevAdi"));

        TableColumn<TamamlananGorev, Double> cMiktar = new TableColumn<>(trEn("Miktar", "Amount"));
        cMiktar.setCellValueFactory(new PropertyValueFactory<>("yapilanMiktar"));

        TableColumn<TamamlananGorev, String> cBirim = new TableColumn<>(trEn("Birim", "Unit"));
        cBirim.setCellValueFactory(new PropertyValueFactory<>("birim"));
        cBirim.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); }
                else       { setText(localizeUnit(item)); }
            }
        });

        TableColumn<TamamlananGorev, Double> cPuan = new TableColumn<>(trEn("Puan", "Points"));
        cPuan.setCellValueFactory(new PropertyValueFactory<>("hesaplananPuan"));

        TableColumn<TamamlananGorev, Void> cSil = new TableColumn<>("");
        cSil.setCellFactory(col -> new TableCell<>() {
            final Button btn = new Button(trEn("Sil", "Delete"));
            { btn.getStyleClass().add("delete-button"); }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
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

    // ================== Takvim ==================
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

        String[] dayNamesTR = {"Pzt","Sal","Çar","Per","Cum","Cmt","Paz"};
        String[] dayNamesEN = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
        String[] dayNames = currentLang == Language.TR ? dayNamesTR : dayNamesEN;

        for (int c = 0; c < 7; c++) {
            Label hdr = new Label(dayNames[c]);
            hdr.setStyle("-fx-font-weight: bold; -fx-opacity: 0.7;");
            GridPane.setHalignment(hdr, HPos.CENTER);
            monthGrid.add(hdr, c, 0);
        }

        VBox right = new VBox(10);
        Label weekTitle = new Label();
        weekTitle.setStyle("-fx-font-weight: bold;");
        Label weekTotalPts = new Label();
        Label weekTotalsByUnit = new Label();

        VBox summaryBox = new VBox(6, weekTitle, weekTotalPts, weekTotalsByUnit);
        summaryBox.setPadding(new Insets(10));
        summaryBox.getStyleClass().add("card-pane");

        TitledPane weeklyPane = new TitledPane(
                trEn("Haftalık Özet", "Weekly Summary"),
                summaryBox);
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
                        } catch (Exception e) { return false; }
                    }).collect(Collectors.toList());

            double toplam = haftalik.stream()
                    .mapToDouble(TamamlananGorev::getHesaplananPuan).sum();

            Map<String, Double> birimeGore = haftalik.stream().collect(Collectors.groupingBy(
                    TamamlananGorev::getBirim,
                    Collectors.summingDouble(TamamlananGorev::getYapilanMiktar)
            ));

            String ozet = birimeGore.isEmpty() ? "-" :
                    birimeGore.entrySet().stream()
                            .map(e -> String.format("%.0f %s", e.getValue(), localizeUnit(e.getKey())))
                            .collect(Collectors.joining(" | "));

            weekTitle.setText(start + " — " + end);
            weekTotalPts.setText(trEn("Toplam Puan: ", "Total Points: ") + String.format("%.2f", toplam));
            weekTotalsByUnit.setText(trEn("Miktarlar: ", "Amounts: ") + ozet);
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

        String bgColor = selected
                ? (isDarkMode ? "#3d5afe" : "#e3f2fd")
                : (isDarkMode ? "#2d2d2d" : "#ffffff");
        String borderColor = isDarkMode ? "#444" : "#e0e0e0";

        box.setStyle(
                "-fx-background-color: " + bgColor +
                "; -fx-border-color: " + borderColor +
                "; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand;"
        );
        if (selected && isDarkMode) dayLbl.setStyle("-fx-text-fill: white;");
        return box;
    }

    private VBox buildCalendarRightPanel(LocalDate[] selectedDateRef) {
        VBox root = new VBox(10);
        Label title = new Label(trEn("Seçilen Gün: ", "Selected Day: ") + selectedDateRef[0]);
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        ObservableList<TamamlananGorev> gunluk = FXCollections.observableArrayList();
        TableView<TamamlananGorev> table = new TableView<>(gunluk);
        table.setPrefHeight(400);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<TamamlananGorev, String> cAd = new TableColumn<>(trEn("Görev", "Task"));
        cAd.setCellValueFactory(new PropertyValueFactory<>("gorevAdi"));

        TableColumn<TamamlananGorev, Double> cM = new TableColumn<>(trEn("Miktar", "Amount"));
        cM.setCellValueFactory(new PropertyValueFactory<>("yapilanMiktar"));

        TableColumn<TamamlananGorev, Double> cP = new TableColumn<>(trEn("Puan", "Points"));
        cP.setCellValueFactory(new PropertyValueFactory<>("hesaplananPuan"));

        TableColumn<TamamlananGorev, Void> cSil = new TableColumn<>("");
        cSil.setPrefWidth(50);
        cSil.setCellFactory(col -> new TableCell<>() {
            final Button btn = new Button("X");
            {
                btn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 10px;");
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
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
                        }).collect(Collectors.toList())
        );
    }

    // ================== Kronometre ==================
    private Node buildTimerView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);

        Label title = new Label(trEn("Kronometre", "Timer"));
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        timerLabelRef = new Label();
        timerLabelRef.getStyleClass().add("timer-label");
        updateTimerLabel();

        GridPane form = new GridPane();
        form.setHgap(15);
        form.setVgap(15);
        form.setAlignment(Pos.CENTER);

        dpRef = new DatePicker(dpRef != null ? dpRef.getValue() : LocalDate.now());

        cbDakikaRef = new ComboBox<>();
        cbDakikaRef.setPromptText(trEn("Görev Seçiniz (dakika)", "Select Task (minute)"));
        cbDakikaRef.setPrefWidth(250);

        List<Gorev> dakikaGorevler = VeriYoneticisi.gorevTanimlariniYukle().stream()
                .filter(g -> g.getBirim() != null &&
                        g.getBirim().trim().toLowerCase(Locale.ROOT).contains("dakika"))
                .collect(Collectors.toList());
        cbDakikaRef.getItems().setAll(dakikaGorevler);
        if (cbDakikaRef.getValue() == null && !dakikaGorevler.isEmpty()) {
            cbDakikaRef.getSelectionModel().selectFirst();
        }

        form.add(new Label(trEn("Tarih:", "Date:")), 0, 0);
        form.add(dpRef, 1, 0);
        form.add(new Label(trEn("Görev:", "Task:")), 0, 1);
        form.add(cbDakikaRef, 1, 1);

        startStopRef = new Button(running ? trEn("Durdur", "Stop") : trEn("Başlat", "Start"));
        startStopRef.setPrefWidth(120);

        saveBtnRef = new Button(trEn("Kaydet", "Save"));
        saveBtnRef.setPrefWidth(120);
        saveBtnRef.setDisable(!running && chronoSeconds == 0);

        Button resetBtn = new Button(trEn("Sıfırla", "Reset"));
        resetBtn.setPrefWidth(120);
        resetBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");

        HBox buttons = new HBox(15, startStopRef, saveBtnRef, resetBtn);
        buttons.setAlignment(Pos.CENTER);

        ensureTimer();

        startStopRef.setOnAction(e -> {
            if (!running) {
                running = true;
                startStopRef.setText(trEn("Durdur", "Stop"));
                startStopRef.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                saveBtnRef.setDisable(true);
                timer.play();
            } else {
                running = false;
                startStopRef.setText(trEn("Başlat", "Start"));
                startStopRef.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
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
                new Alert(Alert.AlertType.WARNING,
                        trEn("Lütfen listeden bir görev seçin.", "Please select a task.")
                ).show();
                return;
            }
            long minutes = Math.round(chronoSeconds / 60.0);
            if (minutes <= 0) {
                new Alert(Alert.AlertType.WARNING,
                        trEn("Henüz 1 dakika bile olmadı!", "Less than 1 minute passed!")
                ).show();
                return;
            }

            double puan = minutes * g.getPuanPerBirim();
            TamamlananGorev t = new TamamlananGorev(
                    dpRef.getValue().toString(),
                    g.getAd(),
                    g.getBirim(),
                    minutes,
                    puan
            );
            tumGorevler.add(t);
            VeriYoneticisi.verileriKaydet(tumGorevler);

            SesYoneticisi.oynat(SesYoneticisi.Ses.ONAY);
            new Alert(Alert.AlertType.INFORMATION,
                    trEn(
                            "Kaydedildi: " + minutes + " dk " + g.getAd() + "\nKazandığın Puan: " + puan,
                            "Saved: " + minutes + " min " + g.getAd() + "\nPoints gained: " + puan
                    )
            ).show();

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

    // ================== Diğer Yardımcılar ==================
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
        int seviye = computeLevel(toplamPuan);

        lblBugunPuan.setText(String.format("+%.0f", bugunPuan));
        lblCuzdan.setText(String.format("%.0f", netBakiye));
        if (lblSeviye != null) {
            lblSeviye.setText(getLevelName(seviye));
            Tooltip.install(lblSeviye, new Tooltip(buildLevelTooltip()));
        }
    }

    private void guncelleGorevSecici() {
        if (gorevSecici != null) {
            gorevSecici.getItems().setAll(VeriYoneticisi.gorevTanimlariniYukle());
        }
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
            new Alert(Alert.AlertType.WARNING,
                    trEn("Miktar için geçerli bir sayı gir.", "Enter a valid number for amount.")
            ).show();
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
                                g.getGorevAdi().toLowerCase(Locale.ROOT)
                                        .contains(q.toLowerCase(Locale.ROOT)))
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

