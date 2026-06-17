package ro.licenta.analiza;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.control.*;
import javafx.scene.control.ColorPicker;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import javafx.util.Duration;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.awt.Desktop;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import javafx.scene.text.FontPosture;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.CacheHint;
import javafx.geometry.Point2D;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.ScaleTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Cursor;
import javafx.scene.layout.FlowPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;

public class DashboardApp extends Application {

    private boolean isSidebarExpanded = true;
    private boolean isSidebarPinned = true; // Default pinned
    private VBox sidebar;
    private Label brandLabel;
    private Label brandSubtitle;
    private ImageView brandIconView;
    private Button toggleButton;

    private VBox ecranGestiuneDateCache;

    private TableView<FisierIncarcat> tabelFisierePersistent = new TableView<>();

    private Integer selectedDashboardYear = null; // Stare persistentă pentru anul selectat în dashboard
    private Integer selectedAnalysisYear1 = null; // Stare persistentă pentru anul 1 în analiză
    private Integer selectedAnalysisYear2 = null; // Stare persistentă pentru anul 2 în analiză

    private ReteaNeuronala rn;
    private ReteaNeuronala loadedModel;
    private File loadedModelSourceFile;
    private ReteaNeuronala trainedModel;
    private CheckBox chkUseTrainedModel;
    private XYChart.Series<Number, Number> serieEroare = new XYChart.Series<>();
    private XYChart.Series<Number, Number> serieTinta = new XYChart.Series<>();
    private XYChart.Series<Number, Number> seriePreviziune = new XYChart.Series<>();
    private Label lblInfoAntrenament = new Label("Niciun antrenament în curs");
    private NumberAxis xAxis; // Axă promovată
    private NumberAxis yAxisEroare; // Axă promovată pentru acces din thread-ul de antrenament
    private NumberAxis xAxisValid; // Axă promovată
    private NumberAxis yAxisValid; // Axă promovată
    private ScrollPane contentArea = new ScrollPane();

    private List<File> fisiereDateSelectate = new ArrayList<>(); // Reține fișierele încărcate de utilizator
    private Label lblStatus = new Label("0 fișiere încărcate"); // Etichetă pentru starea fișierelor

    // Extrage primul fisier Excel din lista (ignora fisierele .ser sau altele)
    private File getFisierDateExcel() {
        if (fisiereDateSelectate == null || fisiereDateSelectate.isEmpty())
            return null;
        for (File f : fisiereDateSelectate) {
            String nume = f.getName().toLowerCase();
            if (nume.endsWith(".xlsx") || nume.endsWith(".xls")) {
                return f;
            }
        }
        return fisiereDateSelectate.get(0); // Folosim primul fisier in caz ca nu are extensie xlsx
    }

    private Label lblRecomandareLocal; // Etichetă pentru recomandările AI

    // Elemente Sidebar
    private List<Button> sidebarButtons = new ArrayList<>();

    // Theme Settings
    private Label lblDarkModeToggle;
    private ToggleButton toggleDarkMode;
    private boolean isDarkMode = false;

    // Variabile cache pentru persistența taburilor
    private VBox ecranSimulatorCache;

    private VBox ecranAnalizaCache;
    private VBox ecranPredictiiCache;

    private ScrollPane ecranRapoarteCache;
    private TabPane ecranTehnicCache;
    private int lastTehnicTabIndex = 0; // Memorează ultimul subtab selectat în Gestiune & Antrenare
    private ScrollPane ecranDashboardCache;
    private HBox dashboardKpiRow;
    private VBox dashboardTrendSection;
    private Integer dashboardCachedYear;

    // Heatmap Colors (User Customizable)
    private Color heatmapLow = Color.WHITE;
    private Color heatmapHigh = Color.NAVY;

    // Elemente Simulator

    private TextField txtIteratii = new TextField("10000");
    private String savedIteratiiValue = "10000"; // Tracks the last accepted value
    private boolean suppressIterationWarning = false;
    private Button btnStartAntrenare;
    private Button btnStopAntrenare;
    private volatile boolean opresteAntrenarea = false;
    private boolean folosesteLeakyReLU = true; // Setare UI: true = LeakyReLU Optimizat, false = Sigmoid Clasic
    private boolean folosesteCustomSettings = false; // false = Implicit, true = Customizat de utilizator
    private double customInitialLR = 0.1; // Rata de învățare inițială (custom)
    private double customMomentum = 0.9; // Rata de inerție (custom)
    private int customNeuroniAscunsi = 12; // Numărul de neuroni ascunși (custom)

    // --- User Session Fields ---
    private User currentUser = null; // null = Guest
    private List<User> registeredUsers = new ArrayList<>();
    private final String USERS_FILE = "users.dat";
    private VBox bottomUserContainer; // Pentru actualizare dinamică

    // --- Tutorial Interactiv ---
    private StackPane centerStack; // Conține contentArea + overlay tutorial
    private Pane tutorialOverlay; // Overlay semitransparent
    private VBox tutorialCard; // Caseta cu text și butoane
    private int tutorialStep = 0; // Pasul curent (0-6)
    private static final int TUTORIAL_TOTAL_STEPS = 7;
    private javafx.scene.layout.HBox tutorialConfigBox;
    private Button tutorialBtnIncarca;

    // Target-uri pentru Tutorial Pasul 3 (Simulator)
    private VBox tutorialCardScenariu;
    private Button tutorialBtnCalculeaza;
    private VBox tutorialCardRezultate;
    private VBox tutorialGraficComparativ;

    // Target-uri pentru Tutorial Pasul 4 (Predictii)
    private VBox tutorialCalcPredictie;
    private VBox tutorialCalcGoal;

    // Target-uri pentru Tutorial Pasul 5 (Dashboard)
    private ComboBox<Integer> tutorialComboAn;

    // Target-uri pentru Tutorial Pasul 6 (Export)
    private javafx.scene.layout.FlowPane tutorialExportContainer;
    private VBox tutorialFormExport;

    // --- Campuri pentru Export CSV Predictii ---
    private ComboBox<String> predLunaCombo;
    private ComboBox<String> predSezonCombo;
    private TextField predBugetField;
    private TextField predPretField;
    private Label predRezultatLabel;
    private TextField predTargetField;
    private Label predOptBugetLabel;
    private Label predOptPretLabel;
    private Label predOptSalesLabel;

    // --- Campuri pentru Export CSV Simulator Scenarii ---
    private Slider simBugetSlider;
    private Slider simPretSlider;
    private TextField simBugetTextField;
    private TextField simPretTextField;
    private Label simVanzariActualLabel;
    private Label simVanzariScenariuLabel;
    private Label simDiferentaLabel;

    @Override
    public void start(Stage stage) {
        // Load Preferences
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(DashboardApp.class);
        String cLow = prefs.get("heatmapLow", "WHITE");
        String cHigh = prefs.get("heatmapHigh", "NAVY");
        isDarkMode = prefs.getBoolean("darkMode", false);
        try {
            heatmapLow = Color.valueOf(cLow);
            heatmapHigh = Color.valueOf(cHigh);
        } catch (Exception e) {
            heatmapLow = Color.WHITE;
            heatmapHigh = Color.NAVY;
        }

        stage.setTitle("NeuroCast (beta version)");

        // Setăm iconița ferestrei
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/brain-circuit.png")));
        } catch (Exception e) {
            System.err.println("Nu s-a putut încărca iconița: " + e.getMessage());
        }

        BorderPane mainLayout = new BorderPane();
        mainLayout.setLeft(creeazaSidebar());

        // Wrappăm contentArea într-un StackPane pentru a putea suprapune overlay-ul
        // tutorial
        centerStack = new StackPane();
        centerStack.getChildren().add(contentArea);
        mainLayout.setCenter(centerStack);

        // Stil global pentru butoane active
        contentArea.setStyle("-fx-background-color: #f4f7f6;");
        contentArea.setFitToWidth(true); // Ensure content stretches to fill width

        afiseazaDashboard(); // Pornire pe Dashboard General

        Scene scene = new Scene(mainLayout, 1200, 900);
        try {
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Nu s-a putut incarca style.css: " + e.getMessage());
        }
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();

        // Aplică tema salvată după afișarea scenei
        if (isDarkMode) {
            contentArea.getStyleClass().add("dark-theme");
            if (toggleDarkMode != null) {
                toggleDarkMode.setSelected(true);
                toggleDarkMode.setText("ON");
                toggleDarkMode.setStyle(
                        "-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15; -fx-cursor: hand; -fx-padding: 3 10; -fx-font-size: 11px;");
            }
            aplicaTemaRecursiv(contentArea, true);
        }

        // incarcaIstoric();
        incarcaIstoric();
        incarcaUtilizatori();
        // actualizeazaSidebarBottom(); // Nu mai e nevoie, folosim butoane standard

        // Verificam daca tutorialul a fost deja vazut
        boolean tutorialVazut = prefs.getBoolean("tutorialVazut", false);
        if (!tutorialVazut) {
            PauseTransition startDelay = new PauseTransition(Duration.millis(800));
            startDelay.setOnFinished(ev -> pornesteTutorial());
            startDelay.play();
        }
    }

    private void salveazaIstoric() {
        try (java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter("history.dat"))) {
            for (File f : fisiereDateSelectate) {
                writer.write(f.getAbsolutePath());
                writer.newLine();
            }
        } catch (Exception e) {
            System.err.println("Eroare la salvarea istoricului: " + e.getMessage());
        }
    }

    private void incarcaIstoric() {
        File historyFile = new File("history.dat");
        if (!historyFile.exists())
            return;

        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(historyFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                File f = new File(line);
                if (f.exists() && !fisiereDateSelectate.contains(f)) {
                    // Încărcare automată a modelului dacă este găsit în istoric
                    if (f.getName().endsWith(".ser")) {
                        try {
                            rn = ReteaNeuronala.incarcaModel(f.getAbsolutePath());
                            loadedModel = rn; // Initialize loadedModel
                            loadedModelSourceFile = f; // Track source file
                            System.out.println("Model încărcat automat din istoric: " + f.getName());
                            // Putem afișa și o notificare scurtă sau în consolă
                        } catch (Exception e) {
                            System.err.println("Nu s-a putut încărca modelul " + f.getName() + ": " + e.getMessage());
                        }
                    }

                    fisiereDateSelectate.add(f);
                    tabelFisierePersistent.getItems().add(new FisierIncarcat(
                            f.getName(),
                            new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date(f.lastModified())),
                            f));
                }
            }
            actualizeazaStatusFisiere();
            salveazaIstoric();
            // Invalidate caches after loading history
            ecranAnalizaCache = null;
            // ecranPredictiiCache = null; // Persist calculator state
            ecranSimulatorCache = null;
            invalidateDashboardCache();

        } catch (Exception e) {
            System.err.println("Eroare la încărcarea istoricului: " + e.getMessage());
        }

    }

    private VBox creeazaSidebar() {
        sidebar = new VBox(0);
        sidebar.setPadding(new Insets(15, 0, 20, 0)); // Padding controlled inside elements for full width click
        sidebar.setPrefWidth(280); // Increased width for 32px icons + text
        sidebar.setStyle("-fx-background-color: #1e293b;");

        // --- Branding Section ---
        HBox brandingBox = new HBox(15);
        brandingBox.setAlignment(Pos.CENTER_LEFT);
        brandingBox.setPadding(new Insets(5, 15, 20, 20)); // Align with buttons (20px left padding)
        brandingBox.setMinHeight(60); // Prevent vertical jump when title is hidden

        // Icon Branding
        // User requested: "artificial-intelligence"
        // Icon Branding
        // User requested: "artificial-intelligence" - ORIGINAL SIZE
        brandIconView = incarcaIcon("artificial-intelligence.png", 0);

        // Title & Subtitle Container
        VBox titleBox = new VBox(2);
        brandLabel = new Label("NEUROCAST");
        brandLabel.setFont(Font.font("Inter", FontWeight.BOLD, 17));
        brandLabel.setTextFill(Color.WHITE);

        brandSubtitle = new Label("AI-Driven Decision Support for Market Excellence");
        brandSubtitle.setFont(Font.font("Inter", FontWeight.NORMAL, 10));
        brandSubtitle.setTextFill(Color.web("#94a3b8"));
        brandSubtitle.setWrapText(true);
        brandSubtitle.setMaxWidth(180);

        titleBox.getChildren().addAll(brandLabel, brandSubtitle);

        if (brandIconView != null) {
            brandingBox.getChildren().add(brandIconView);
        }
        brandingBox.getChildren().add(titleBox);

        sidebar.getChildren().add(brandingBox);

        // --- Menu Button ---
        toggleButton = new Button("Meniu 📌"); // Default Pinned
        toggleButton.setMaxWidth(Double.MAX_VALUE);
        toggleButton.setAlignment(Pos.CENTER_LEFT);

        // User requested menu icon provided: "menu-burger.png"
        ImageView menuIcon = incarcaIcon("menu-burger.png", 0);
        if (menuIcon != null) {
            toggleButton.setGraphic(menuIcon);
            toggleButton.setGraphicTextGap(20);
        }

        // Styling - Padding handled by code, not CSS to avoid conflict
        toggleButton.setPadding(new Insets(10, 20, 10, 20));
        toggleButton.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 0; -fx-alignment: CENTER_LEFT; -fx-border-color: transparent; -fx-border-width: 0 0 0 5;");

        toggleButton.setOnAction(e -> toggleSidebarPin());

        // Auto-Expand Logic
        sidebar.setOnMouseEntered(e -> {
            if (!isSidebarPinned) {
                setSidebarExpanded(true);
            }
        });

        sidebar.setOnMouseExited(e -> {
            if (!isSidebarPinned) {
                setSidebarExpanded(false);
            }
        });

        sidebar.getChildren().add(toggleButton);

        // Add spacing
        Region spacer = new Region();
        spacer.setPrefHeight(20);
        sidebar.getChildren().add(spacer);

        sidebarButtons.clear();

        // Updated filenames based on checking directory
        sidebar.getChildren().addAll(
                creeazaButonNavigabil("Dashboard General", "home (1).png", this::afiseazaDashboard),
                creeazaButonNavigabil("Analiza Vânzărilor", "chart-simple.png", this::afiseazaAnaliza),
                creeazaButonNavigabil("Predicții Inteligente", "crystal-ball.png", this::afiseazaPredictii),
                creeazaButonNavigabil("Simulator Scenarii", "magic-wand.png", this::afiseazaEcranSimulator),
                creeazaButonNavigabil("Gestiune & Antrenare", "database-management.png", this::afiseazaEcranTehnic),
                creeazaButonNavigabil("Export", "file-export.png", this::afiseazaExport));

        // Push everything up
        Region bottomSpacer = new Region();
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);
        sidebar.getChildren().add(bottomSpacer);

        // --- Bottom Menu (User & Help) ---
        // Aliniat cu taburile de sus
        sidebar.getChildren().add(new Separator());

        // Dark Mode Toggle

        HBox darkModeBox = new HBox(10);
        darkModeBox.setAlignment(Pos.CENTER_LEFT);
        darkModeBox.setPadding(new Insets(10, 20, 10, 20));
        lblDarkModeToggle = new Label("Dark Mode 🌙");
        lblDarkModeToggle.setFont(Font.font("Inter", FontWeight.NORMAL, 14));
        lblDarkModeToggle.setTextFill(Color.WHITE);
        lblDarkModeToggle.setVisible(isSidebarExpanded);
        lblDarkModeToggle.setManaged(isSidebarExpanded);

        toggleDarkMode = new ToggleButton("OFF");
        toggleDarkMode.setStyle(
                "-fx-background-color: #475569; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15; -fx-cursor: hand; -fx-padding: 3 10; -fx-font-size: 11px;");

        toggleDarkMode.setOnAction(e -> {
            if (toggleDarkMode.isSelected()) {
                toggleDarkMode.setText("ON");
                toggleDarkMode.setStyle(
                        "-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15; -fx-cursor: hand; -fx-padding: 3 10; -fx-font-size: 11px;");
                setDarkMode(true);
            } else {
                toggleDarkMode.setText("OFF");
                toggleDarkMode.setStyle(
                        "-fx-background-color: #475569; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15; -fx-cursor: hand; -fx-padding: 3 10; -fx-font-size: 11px;");
                setDarkMode(false);
            }
        });

        if (isDarkMode) {
            toggleDarkMode.setSelected(true);
            toggleDarkMode.setText("ON");
            toggleDarkMode.setStyle(
                    "-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15; -fx-cursor: hand; -fx-padding: 3 10; -fx-font-size: 11px;");
        }

        Region dmSpacer = new Region();
        HBox.setHgrow(dmSpacer, Priority.ALWAYS);

        darkModeBox.getChildren().addAll(lblDarkModeToggle, dmSpacer, toggleDarkMode);
        sidebar.getChildren().add(darkModeBox);

        // 1. User Info Tab
        Button btnUserInfo = creeazaButonNavigabil("User Info", "icons_bottom_menu/user_icon.png",
                this::afiseazaEcranUser);
        sidebar.getChildren().add(btnUserInfo);

        // 2. Despre Tab
        Button btnDespre = creeazaButonNavigabil("Despre", "icons_bottom_menu/interrogation.png",
                this::afiseazaEcranHelp);
        sidebar.getChildren().add(btnDespre);

        return sidebar;
    }

    private Button creeazaButonNavigabil(String text, String iconName, Runnable action) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);

        // Standardized to 24 to match the original feel of the top icons
        ImageView icon = incarcaIcon(iconName, 24);
        if (icon != null) {
            btn.setGraphic(icon);
            btn.setGraphicTextGap(20); // 20px gap
        }

        btn.setPadding(new Insets(12, 20, 12, 20));
        btn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 0; -fx-border-color: transparent; -fx-border-width: 0 0 0 5;");

        sidebarButtons.add(btn);

        btn.setOnAction(e -> {
            sidebarButtons.forEach(b -> b.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 0; -fx-border-color: transparent; -fx-border-width: 0 0 0 5;"));
            btn.setStyle(
                    "-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 0; -fx-border-color: #3498db; -fx-border-width: 0 0 0 5;");
            action.run();
        });

        // Hover Effects (Visual + Scale)
        btn.setOnMouseEntered(e -> {
            if (!btn.getStyle().contains("-fx-border-color: #3498db")) { // If not active
                btn.setStyle(
                        btn.getStyle().replace("-fx-background-color: transparent;", "-fx-background-color: #2c3e50;"));
            }
            ScaleTransition st = new ScaleTransition(Duration.millis(200), btn);
            st.setToX(1.05);
            st.setToY(1.05);
            st.play();
        });

        btn.setOnMouseExited(e -> {
            if (!btn.getStyle().contains("-fx-border-color: #3498db")) { // If not active
                btn.setStyle(
                        btn.getStyle().replace("-fx-background-color: #2c3e50;", "-fx-background-color: transparent;"));
            }
            ScaleTransition st = new ScaleTransition(Duration.millis(200), btn);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        return btn;
    }

    private boolean isAnimating = false; // Prevent animation spam

    private ImageView incarcaIcon(String numeFisier, double size) {
        try {
            String path;
            // Allow flexibility if the name already contains a directory structure
            if (numeFisier.startsWith("/") || numeFisier.contains("/")) {
                path = numeFisier.startsWith("/") ? numeFisier : "/" + numeFisier;
            } else {
                path = "/icons/" + numeFisier;
            }

            java.net.URL resource = getClass().getResource(path);

            // Fallback for development specific paths if needed
            if (resource == null) {
                // Try looking in src/main/resources directly
                File f = new File("src/main/resources" + path);
                if (f.exists()) {
                    resource = f.toURI().toURL();
                } else {
                    // Fallback to old behavior for "icons/" specifically
                    f = new File("src/main/resources/icons/" + numeFisier);
                    if (f.exists()) {
                        resource = f.toURI().toURL();
                    }
                }
            }

            if (resource != null) {
                Image img = new Image(resource.toExternalForm());
                ImageView view = new ImageView(img);

                // Only resize if size > 0, otherwise keep original
                if (size > 0) {
                    view.setFitWidth(size);
                    view.setFitHeight(size);
                }
                view.setPreserveRatio(true);

                // High quality rendering settings
                view.setSmooth(true);
                view.setCache(true);
                view.setCacheHint(CacheHint.QUALITY);

                return view;
            }
        } catch (Exception e) {
            // System.err.println("Nu s-a putut încărca iconița: " + numeFisier);
        }
        return null; // Return null to allow caller to handle fallback
    }

    // ... (rest of code) ...

    private void generareRaportPDF() {
        if (selectedReportType == null)
            return;

        File fisierDate = getFisierDateExcel();
        if (fisierDate == null) {
            new Alert(Alert.AlertType.WARNING, "Încărcați mai întâi datele din Gestiune Fișiere.").show();
            return;
        }

        Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
        loadingAlert.setTitle("Generare PDF");
        loadingAlert.setHeaderText("Se generează PDF-ul structurat...");
        loadingAlert.setContentText("Generare conținut, tabele și grafice vectorizate...");
        loadingAlert.show();

        javafx.application.Platform.runLater(() -> {
            try {
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String fileName = "Export_" + selectedReportType + "_" + timestamp + ".pdf";

                String reportTitleStr = "Raport";
                Node reportView = null;
                switch (selectedReportType) {
                    case "DASHBOARD":
                        reportTitleStr = "Dashboard General";
                        reportView = createDashboardView(false, true);
                        break;
                    case "SALES_ANALYSIS":
                        reportTitleStr = "Analiză Vânzări";
                        reportView = afiseazaAnaliza(true);
                        break;
                    case "PREDICTIONS":
                        reportTitleStr = "Predicții Inteligente";
                        if (ecranPredictiiCache == null)
                            afiseazaPredictii();
                        reportView = ecranPredictiiCache;
                        break;
                    case "WHAT_IF":
                        reportTitleStr = "Simulator Scenarii (What-If)";
                        if (ecranSimulatorCache == null)
                            afiseazaEcranSimulator();
                        reportView = ecranSimulatorCache;
                        break;
                    case "MANAGEMENT":
                        reportTitleStr = "Gestiune & Antrenare Model";
                        reportView = creeazaRaportAntrenare(true);
                        break;
                    case "EXECUTIVE":
                        reportTitleStr = "Raport Executiv";
                        reportView = createExecutiveView();
                        break;
                }

                if (reportView == null) {
                    loadingAlert.close();
                    new Alert(Alert.AlertType.WARNING, "Nu aveți date sau modele pentru generare.").show();
                    return;
                }

                // Generăm PDF cu iText
                PdfWriter writer = new PdfWriter(fileName);
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf);

                final String finalTitle = reportTitleStr;

                pdf.addEventHandler(com.itextpdf.kernel.events.PdfDocumentEvent.END_PAGE,
                        new com.itextpdf.kernel.events.IEventHandler() {
                            @Override
                            public void handleEvent(com.itextpdf.kernel.events.Event event) {
                                com.itextpdf.kernel.events.PdfDocumentEvent docEvent = (com.itextpdf.kernel.events.PdfDocumentEvent) event;
                                PdfDocument pdfDoc = docEvent.getDocument();
                                com.itextpdf.kernel.pdf.PdfPage page = docEvent.getPage();
                                int pageNumber = pdfDoc.getPageNumber(page);
                                com.itextpdf.kernel.pdf.canvas.PdfCanvas canvas = new com.itextpdf.kernel.pdf.canvas.PdfCanvas(
                                        page.newContentStreamBefore(), page.getResources(), pdfDoc);
                                new com.itextpdf.layout.Canvas(canvas, page.getPageSize())
                                        .showTextAligned("Pagina " + pageNumber + " - " + finalTitle,
                                                page.getPageSize().getWidth() / 2, 20,
                                                com.itextpdf.layout.properties.TextAlignment.CENTER,
                                                com.itextpdf.layout.properties.VerticalAlignment.MIDDLE, 0);
                            }
                        });

                document.add(new Paragraph("NEUROCAST")
                        .setFontSize(26).setBold().setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(14, 51, 134)));
                document.add(new Paragraph("AI-Driven Decision Support for Market Excellence")
                        .setFontSize(12).setItalic().setMarginBottom(15));

                document.add(new Paragraph(reportTitleStr)
                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                        .setFontSize(20).setBold().setMarginBottom(5));
                document.add(new Paragraph("Generat la: " + new Date().toString())
                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                        .setFontSize(10).setItalic().setMarginBottom(30));

                document.add(new Paragraph("1. Performanță Generală")
                        .setFontSize(16).setBold().setMarginBottom(10));

                int currentYear = (selectedDashboardYear != null) ? selectedDashboardYear
                        : java.time.Year.now().getValue();
                java.util.Map<String, Object> stats = calculeazaStatisticiGenerale(currentYear);
                if (stats != null && stats.get("total") != null) {
                    com.itextpdf.layout.element.Table kpiTable = new com.itextpdf.layout.element.Table(
                            com.itextpdf.layout.properties.UnitValue.createPercentArray(new float[] { 1, 1, 1 }))
                            .useAllAvailableWidth().setMarginBottom(20);

                    kpiTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Vânzări Totale"))
                            .setBold().setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY));
                    kpiTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Medie Lunară")).setBold()
                            .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY));
                    kpiTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Tranzacții/Clienți"))
                            .setBold().setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY));

                    kpiTable.addCell(new com.itextpdf.layout.element.Cell().add(
                            new Paragraph(String.format("%.0f \u20AC", ((Number) stats.get("total")).doubleValue()))));
                    kpiTable.addCell(new com.itextpdf.layout.element.Cell().add(
                            new Paragraph(String.format("%.0f \u20AC", ((Number) stats.get("medie")).doubleValue()))));
                    kpiTable.addCell(new com.itextpdf.layout.element.Cell().add(
                            new Paragraph(stats.get("cantitate") != null ? stats.get("cantitate").toString() : "-")));

                    document.add(kpiTable);
                }

                document.add(new Paragraph("2. Date Integrate (Extras primile rânduri)")
                        .setFontSize(16).setBold().setMarginBottom(10));

                com.itextpdf.layout.element.Table dataTable = new com.itextpdf.layout.element.Table(
                        com.itextpdf.layout.properties.UnitValue.createPercentArray(new float[] { 1, 1, 2, 2, 2 }))
                        .useAllAvailableWidth().setMarginBottom(20);

                // Repeating Table Header
                String[] headers = { "An", "Lună", "Buget Marketing", "Preț Med.", "Total Vânzări" };
                for (String h : headers) {
                    dataTable.addHeaderCell(new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph(h).setBold().setFontSize(10))
                            .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY)
                            .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
                }

                try (FileInputStream fis = new FileInputStream(fisierDate);
                        org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(
                                fis)) {
                    org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
                    int limit = Math.min(sheet.getLastRowNum(), 60);

                    int rowsAdded = 0;
                    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                        org.apache.poi.ss.usermodel.Row r = sheet.getRow(i);
                        if (r != null) {
                            int rowYear = getAnValue(sheet, i);

                            // Filtrare după an pentru Dashboard
                            if (selectedReportType.equals("DASHBOARD") && selectedDashboardYear != null) {
                                if (rowYear != selectedDashboardYear) {
                                    continue;
                                }
                            }

                            // Filtrare după anii selectați pentru Analiza Vânzărilor
                            if (selectedReportType.equals("SALES_ANALYSIS")) {
                                boolean isYearMatched = false;
                                if (selectedAnalysisYear1 != null && rowYear == selectedAnalysisYear1)
                                    isYearMatched = true;
                                if (selectedAnalysisYear2 != null && rowYear == selectedAnalysisYear2)
                                    isYearMatched = true;

                                if (!isYearMatched)
                                    continue;
                            }

                            String y = String.valueOf(rowYear);
                            String m = r.getCell(1) != null ? String.valueOf((int) r.getCell(1).getNumericCellValue())
                                    : "-";
                            String b = r.getCell(3) != null
                                    ? String.format("%.2f \u20AC", r.getCell(3).getNumericCellValue())
                                    : "-";
                            String p = r.getCell(4) != null
                                    ? String.format("%.2f \u20AC", r.getCell(4).getNumericCellValue())
                                    : "-";
                            String v = r.getCell(5) != null
                                    ? String.format("%.2f \u20AC", r.getCell(5).getNumericCellValue())
                                    : "-";
                            dataTable.addCell(
                                    new com.itextpdf.layout.element.Cell().add(new Paragraph(y).setFontSize(10))
                                            .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
                            dataTable.addCell(
                                    new com.itextpdf.layout.element.Cell().add(new Paragraph(m).setFontSize(10))
                                            .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
                            dataTable.addCell(
                                    new com.itextpdf.layout.element.Cell().add(new Paragraph(b).setFontSize(10)));
                            dataTable.addCell(
                                    new com.itextpdf.layout.element.Cell().add(new Paragraph(p).setFontSize(10)));
                            dataTable.addCell(
                                    new com.itextpdf.layout.element.Cell().add(new Paragraph(v).setFontSize(10)));

                            rowsAdded++;
                            if (rowsAdded >= 60)
                                break; // safety limit
                        }
                    }
                }
                document.add(dataTable);

                // --- 3. Indicatori de Performanta (KPI) SAU Elemente Textuale ---
                List<java.util.Map<String, String>> kpiCards = new java.util.ArrayList<>();
                findKpiCards(reportView, kpiCards);

                if (!kpiCards.isEmpty()
                        && (selectedReportType.equals("DASHBOARD") || selectedReportType.equals("SALES_ANALYSIS"))) {
                    document.add(new Paragraph("Indicatori de Performanță / Tablou Bord")
                            .setFontSize(14).setBold().setMarginBottom(10).setMarginTop(20));

                    com.itextpdf.layout.element.Table kTable = new com.itextpdf.layout.element.Table(
                            com.itextpdf.layout.properties.UnitValue.createPercentArray(new float[] { 2, 2, 3 }))
                            .useAllAvailableWidth().setMarginBottom(20);

                    kTable.addHeaderCell(new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph("Indicator").setBold().setFontSize(10))
                            .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY));
                    kTable.addHeaderCell(new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph("Valoare").setBold().setFontSize(10))
                            .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY));
                    kTable.addHeaderCell(new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph("Detalii").setBold().setFontSize(10))
                            .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY));

                    for (java.util.Map<String, String> map : kpiCards) {
                        kTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(map.get("title"))));
                        kTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(map.get("val"))));

                        String det = "";
                        if (map.containsKey("detalii"))
                            det = map.get("detalii");
                        if (map.containsKey("diff"))
                            det += map.get("diff") + " ";
                        if (map.containsKey("luna"))
                            det += "(" + map.get("luna") + ")";

                        kTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(det)));
                    }
                    document.add(kTable);
                }

                if (selectedReportType.equals("WHAT_IF") || selectedReportType.equals("PREDICTIONS")) {
                    document.add(new Paragraph("Date Model / Simulator Scenarii")
                            .setFontSize(14).setBold().setMarginBottom(10).setMarginTop(20));
                    extrageTextPentruRapoarte(reportView, document);
                }

                // Extract forecast data table for Predictions
                if (selectedReportType.equals("PREDICTIONS")) {
                    String forecastData = findForecastData(reportView);
                    if (forecastData != null && !forecastData.isEmpty()) {
                        document.add(new Paragraph("Prognoză Vânzări (Următoarele 6 Luni)")
                                .setFontSize(14).setBold().setMarginBottom(10).setMarginTop(20));

                        com.itextpdf.layout.element.Table fTable = new com.itextpdf.layout.element.Table(
                                com.itextpdf.layout.properties.UnitValue.createPercentArray(new float[] { 2, 3 }))
                                .useAllAvailableWidth().setMarginBottom(20);

                        fTable.addHeaderCell(new com.itextpdf.layout.element.Cell()
                                .add(new Paragraph("Perioadă").setBold().setFontSize(10))
                                .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY));
                        fTable.addHeaderCell(new com.itextpdf.layout.element.Cell()
                                .add(new Paragraph("Vânzări Estimate (€)").setBold().setFontSize(10))
                                .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY));

                        String[] entries = forecastData.split(";");
                        for (String entry : entries) {
                            if (entry.contains("|")) {
                                String[] parts = entry.split("\\|");
                                fTable.addCell(new com.itextpdf.layout.element.Cell()
                                        .add(new Paragraph(parts[0]).setFontSize(10)));
                                fTable.addCell(new com.itextpdf.layout.element.Cell()
                                        .add(new Paragraph(parts.length > 1 ? parts[1] + " €" : "-").setFontSize(10)));
                            }
                        }
                        document.add(fTable);
                    }
                }
                // Folosim o Scenă invizibilă temporară pentru a forța redarea JavaFX și CSS-ul
                javafx.stage.Stage snapshotStage = new javafx.stage.Stage();
                snapshotStage.initStyle(javafx.stage.StageStyle.UTILITY);
                snapshotStage.setOpacity(0);

                StackPane rootPane = new StackPane();
                rootPane.setStyle("-fx-background-color: white;");
                rootPane.getChildren().add(reportView);
                Scene scene = new Scene(rootPane, 1280, 2000);
                try {
                    scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
                } catch (Exception ex) {
                }
                snapshotStage.setScene(scene);
                snapshotStage.setX(-10000);
                snapshotStage.show();

                // Așteptăm 1.2 secunde pentru a permite animatiilor și layout-ului să se
                // definească complet pe ecran invizibil
                javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                        new javafx.animation.KeyFrame(javafx.util.Duration.millis(1200), e -> {
                            try {
                                List<Node> chartsFound = new ArrayList<>();
                                extractCharts(rootPane, chartsFound);

                                if (chartsFound.isEmpty()) {
                                    document.add(
                                            new Paragraph("Nu au fost identificate grafice vizuale pentru acest modul.")
                                                    .setItalic());
                                } else {
                                    for (Node chart : chartsFound) {
                                        boolean isScatter = chart instanceof javafx.scene.chart.ScatterChart;
                                        boolean isLineChart = chart instanceof javafx.scene.chart.LineChart;
                                        boolean isBarChart = chart instanceof javafx.scene.chart.BarChart;
                                        boolean needsOwnPage = isScatter || isLineChart || isBarChart;

                                        // Backup original state
                                        double oldMinW = ((javafx.scene.layout.Region) chart).getMinWidth();
                                        double oldMinH = ((javafx.scene.layout.Region) chart).getMinHeight();
                                        double oldPrefW = ((javafx.scene.layout.Region) chart).getPrefWidth();
                                        double oldPrefH = ((javafx.scene.layout.Region) chart).getPrefHeight();
                                        double oldMaxW = ((javafx.scene.layout.Region) chart).getMaxWidth();
                                        double oldMaxH = ((javafx.scene.layout.Region) chart).getMaxHeight();
                                        String oldStyle = chart.getStyle();

                                        double oldParentMinW = -1;
                                        double oldParentPrefW = -1;
                                        javafx.scene.layout.Region parentRegion = null;
                                        if (chart.getParent() instanceof javafx.scene.layout.Region) {
                                            parentRegion = (javafx.scene.layout.Region) chart.getParent();
                                            oldParentMinW = parentRegion.getMinWidth();
                                            oldParentPrefW = parentRegion.getPrefWidth();
                                        }

                                        double oldCatGap = -1;
                                        if (isBarChart) {
                                            oldCatGap = ((javafx.scene.chart.BarChart<?, ?>) chart).getCategoryGap();
                                            ((javafx.scene.chart.BarChart<?, ?>) chart).setCategoryGap(400); // Facem
                                                                                                             // barele
                                                                                                             // mai
                                                                                                             // subțiri
                                                                                                             // pentru
                                                                                                             // export
                                        }
                                        javafx.scene.text.Font oldFontX = null;
                                        javafx.scene.text.Font oldFontY = null;
                                        boolean oldAnimated = false;
                                        if (chart instanceof javafx.scene.chart.Chart) {
                                            javafx.scene.chart.Chart chartObj = (javafx.scene.chart.Chart) chart;
                                            oldAnimated = chartObj.getAnimated();
                                            chartObj.setAnimated(false);
                                            if (chart instanceof javafx.scene.chart.XYChart) {
                                                javafx.scene.chart.XYChart<?, ?> xyc = (javafx.scene.chart.XYChart<?, ?>) chart;
                                                oldFontX = xyc.getXAxis().getTickLabelFont();
                                                oldFontY = xyc.getYAxis().getTickLabelFont();
                                            }
                                        }

                                        javafx.geometry.Insets oldPadding = ((javafx.scene.layout.Region) chart)
                                                .getPadding();

                                        // Put each chart on its own page with a title
                                        if (needsOwnPage) {
                                            document.add(new AreaBreak());

                                            // Extract title from chart or parent
                                            String chartTitle = "";
                                            if (chart instanceof javafx.scene.chart.Chart) {
                                                chartTitle = ((javafx.scene.chart.Chart) chart).getTitle();
                                            }
                                            if (chartTitle == null || chartTitle.isEmpty()) {
                                                if (isScatter)
                                                    chartTitle = "Eficiență Marketing (Buget vs Vânzări)";
                                                else if (isLineChart) {
                                                    if (selectedReportType.equals("PREDICTIONS"))
                                                        chartTitle = "Prognoză Vânzări (Următoarele 6 Luni)";
                                                    else
                                                        chartTitle = "Evoluția Vânzărilor";
                                                } else if (isBarChart)
                                                    chartTitle = "Actual vs Scenariu";
                                                else
                                                    chartTitle = "Grafic";
                                            }

                                            // Remove romanian diacritics for safe PDF Font encoding
                                            String safeTitle = chartTitle.replace("ă", "a").replace("Ă", "A")
                                                    .replace("â", "a").replace("Â", "A")
                                                    .replace("î", "i").replace("Î", "I")
                                                    .replace("ș", "s").replace("Ș", "S")
                                                    .replace("ț", "t").replace("Ț", "T")
                                                    .replace("ţ", "t").replace("Ţ", "T")
                                                    .replace("ş", "s").replace("Ş", "S");

                                            document.add(new Paragraph(safeTitle)
                                                    .setFontSize(16).setBold().setMarginBottom(15));

                                            // Apply styling BEFORE CSS formatting
                                            if (chart instanceof javafx.scene.chart.Chart) {
                                                chart.setStyle("-fx-font-size: 28px;");
                                                ((javafx.scene.layout.Region) chart)
                                                        .setPadding(new javafx.geometry.Insets(20, 20, 60, 20));
                                                if (chart instanceof javafx.scene.chart.XYChart) {
                                                    javafx.scene.chart.XYChart<?, ?> xyc = (javafx.scene.chart.XYChart<?, ?>) chart;
                                                    xyc.getXAxis().setTickLabelFont(javafx.scene.text.Font.font(28));
                                                    xyc.getYAxis().setTickLabelFont(javafx.scene.text.Font.font(28));
                                                    // Force layout update on axes
                                                    xyc.getXAxis().requestLayout();
                                                    xyc.getYAxis().requestLayout();
                                                }
                                            }

                                            // Resize chart to fill page width
                                            ((javafx.scene.layout.Region) chart).setMinSize(1400, 900);
                                            ((javafx.scene.layout.Region) chart).setPrefSize(1400, 900);
                                            ((javafx.scene.layout.Region) chart).setMaxSize(1400, 900);

                                            if (parentRegion != null) {
                                                parentRegion.setMinWidth(1500);
                                                parentRegion.setPrefWidth(1500);
                                                parentRegion.applyCss();
                                                parentRegion.layout();
                                            }

                                            ((javafx.scene.layout.Region) chart).applyCss();
                                            ((javafx.scene.layout.Region) chart).layout();
                                        }

                                        javafx.scene.SnapshotParameters snapParams = new javafx.scene.SnapshotParameters();
                                        snapParams.setTransform(new javafx.scene.transform.Scale(2.5, 2.5)); // High DPI
                                        snapParams.setFill(javafx.scene.paint.Color.WHITE);

                                        javafx.scene.image.WritableImage snap = chart.snapshot(snapParams, null);
                                        java.io.ByteArrayOutputStream byteOutput = new java.io.ByteArrayOutputStream();
                                        javax.imageio.ImageIO.write(
                                                javafx.embed.swing.SwingFXUtils.fromFXImage(snap, null), "png",
                                                byteOutput);
                                        com.itextpdf.layout.element.Image pdfImage = new com.itextpdf.layout.element.Image(
                                                com.itextpdf.io.image.ImageDataFactory
                                                        .create(byteOutput.toByteArray()));

                                        pdfImage.setAutoScale(true);
                                        document.add(pdfImage.setMarginBottom(30));

                                        // Add data table for BarChart scenarios
                                        if (isBarChart) {
                                            document.add(new Paragraph("Date Simulator Scenarii:")
                                                    .setFontSize(14).setBold().setMarginBottom(10));

                                            com.itextpdf.layout.element.Table barChartTable = new com.itextpdf.layout.element.Table(
                                                    new float[] { 1, 1 });
                                            barChartTable.setWidth(
                                                    com.itextpdf.layout.properties.UnitValue.createPercentValue(100));

                                            barChartTable.addHeaderCell(new com.itextpdf.layout.element.Cell()
                                                    .add(new Paragraph("Tip Vanzari").setBold())
                                                    .setBackgroundColor(
                                                            com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY));
                                            barChartTable.addHeaderCell(new com.itextpdf.layout.element.Cell()
                                                    .add(new Paragraph("Valoare (EUR)").setBold())
                                                    .setBackgroundColor(
                                                            com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY));

                                            @SuppressWarnings("unchecked")
                                            javafx.scene.chart.BarChart<String, Number> bc = (javafx.scene.chart.BarChart<String, Number>) chart;
                                            for (javafx.scene.chart.XYChart.Series<String, Number> series : bc
                                                    .getData()) {
                                                for (javafx.scene.chart.XYChart.Data<String, Number> data : series
                                                        .getData()) {
                                                    barChartTable.addCell(new com.itextpdf.layout.element.Cell()
                                                            .add(new Paragraph(data.getXValue())));
                                                    barChartTable.addCell(new com.itextpdf.layout.element.Cell()
                                                            .add(new Paragraph(data.getYValue().toString() + " EUR")));
                                                }
                                            }
                                            document.add(barChartTable.setMarginBottom(20));
                                        }

                                        // Restore original configuration
                                        if (chart instanceof javafx.scene.chart.Chart) {
                                            ((javafx.scene.chart.Chart) chart).setAnimated(oldAnimated);
                                        }
                                        if (needsOwnPage) {
                                            ((javafx.scene.layout.Region) chart).setMinSize(oldMinW, oldMinH);
                                            ((javafx.scene.layout.Region) chart).setPrefSize(oldPrefW, oldPrefH);
                                            ((javafx.scene.layout.Region) chart).setMaxSize(oldMaxW, oldMaxH);
                                            chart.setStyle(oldStyle != null ? oldStyle : "");
                                            ((javafx.scene.layout.Region) chart).setPadding(oldPadding);

                                            if (isBarChart && oldCatGap != -1) {
                                                ((javafx.scene.chart.BarChart<?, ?>) chart).setCategoryGap(oldCatGap);
                                            }
                                            if (parentRegion != null) {
                                                parentRegion.setMinWidth(oldParentMinW);
                                                parentRegion.setPrefWidth(oldParentPrefW);
                                            }
                                            if (chart instanceof javafx.scene.chart.XYChart) {
                                                javafx.scene.chart.XYChart<?, ?> xyc = (javafx.scene.chart.XYChart<?, ?>) chart;
                                                if (oldFontX != null)
                                                    xyc.getXAxis().setTickLabelFont(oldFontX);
                                                if (oldFontY != null)
                                                    xyc.getYAxis().setTickLabelFont(oldFontY);
                                            }
                                        }
                                    }
                                }

                                document.close();
                                snapshotStage.close();
                                loadingAlert.close();
                                new Alert(Alert.AlertType.INFORMATION,
                                        "Raport profesional structurat completat și salvat: " + fileName).show();
                                try {
                                    Desktop.getDesktop().open(new File(fileName));
                                } catch (Exception ex) {
                                }

                            } catch (Exception err) {
                                try {
                                    document.close();
                                } catch (Exception ignore) {
                                }
                                snapshotStage.close();
                                loadingAlert.close();
                                err.printStackTrace();
                                new Alert(Alert.AlertType.ERROR,
                                        "Eroare la generarea iText Grafice: " + err.getMessage()).show();
                            }
                        }));
                timeline.play();

            } catch (Exception e) {
                loadingAlert.close();
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Eroare la inițierea PDF-ului: " + e.getMessage()).show();
            }
        });
    }

    private void generareRaportCSV() {
        if (selectedReportType == null)
            return;

        File fisierDate = getFisierDateExcel();

        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        String fileName = "Export_" + selectedReportType + "_" + timestamp + ".csv";

        try (java.io.PrintWriter pw = new java.io.PrintWriter(
                new java.io.OutputStreamWriter(new java.io.FileOutputStream(fileName),
                        java.nio.charset.StandardCharsets.UTF_8))) {

            // UTF-8 BOM for Excel compatibility
            pw.print('\uFEFF');

            switch (selectedReportType) {

                case "DASHBOARD": {
                    pw.println("=== DASHBOARD GENERAL ===");
                    pw.println("Generat la:," + new java.util.Date());
                    pw.println();
                    int yr = (selectedDashboardYear != null) ? selectedDashboardYear : java.time.Year.now().getValue();
                    java.util.Map<String, Object> stats = calculeazaStatisticiGenerale(yr);
                    if (stats != null) {
                        pw.println("KPI,Valoare");
                        pw.println("An selectat," + yr);
                        if (stats.get("total") != null)
                            pw.println("Vanzari Totale (EUR),"
                                    + String.format("%.2f", ((Number) stats.get("total")).doubleValue()));
                        if (stats.get("medie") != null)
                            pw.println("Medie Lunara (EUR),"
                                    + String.format("%.2f", ((Number) stats.get("medie")).doubleValue()));
                        if (stats.get("cantitate") != null)
                            pw.println("Tranzactii/Clienti," + stats.get("cantitate"));
                    }
                    pw.println();
                    pw.println("Date Istorice:");
                    pw.println("An,Luna,Sezon,Buget Marketing (EUR),Pret Mediu (EUR),Vanzari (EUR),Clienti");
                    if (fisierDate != null) {
                        try (java.io.FileInputStream fis = new java.io.FileInputStream(fisierDate);
                                org.apache.poi.ss.usermodel.Workbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook(
                                        fis)) {
                            org.apache.poi.ss.usermodel.Sheet sh = wb.getSheetAt(0);
                            for (int i = 1; i <= sh.getLastRowNum(); i++) {
                                org.apache.poi.ss.usermodel.Row r = sh.getRow(i);
                                if (r == null)
                                    continue;
                                int rowYear = getAnValue(sh, i);
                                if (selectedDashboardYear != null && rowYear != selectedDashboardYear)
                                    continue;
                                String sezon = r.getCell(2) != null ? r.getCell(2).toString() : "-";
                                pw.printf("%d,%d,%s,%.2f,%.2f,%.2f,%d%n", rowYear,
                                        r.getCell(1) != null ? (int) r.getCell(1).getNumericCellValue() : 0, sezon,
                                        r.getCell(3) != null ? r.getCell(3).getNumericCellValue() : 0,
                                        r.getCell(4) != null ? r.getCell(4).getNumericCellValue() : 0,
                                        r.getCell(5) != null ? r.getCell(5).getNumericCellValue() : 0,
                                        r.getCell(6) != null ? (int) r.getCell(6).getNumericCellValue() : 0);
                            }
                        }
                    }
                    break;
                }

                case "SALES_ANALYSIS": {
                    pw.println("=== ANALIZA VANZARILOR ===");
                    pw.println("Generat la:," + new java.util.Date());
                    pw.println("An 1 selectat:," + (selectedAnalysisYear1 != null ? selectedAnalysisYear1 : "Toti"));
                    pw.println(
                            "An 2 selectat:," + (selectedAnalysisYear2 != null ? selectedAnalysisYear2 : "Niciunul"));
                    pw.println();
                    pw.println("An,Luna,Sezon,Buget Marketing (EUR),Pret Mediu (EUR),Vanzari (EUR),Clienti");
                    if (fisierDate != null) {
                        try (java.io.FileInputStream fis = new java.io.FileInputStream(fisierDate);
                                org.apache.poi.ss.usermodel.Workbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook(
                                        fis)) {
                            org.apache.poi.ss.usermodel.Sheet sh = wb.getSheetAt(0);
                            for (int i = 1; i <= sh.getLastRowNum(); i++) {
                                org.apache.poi.ss.usermodel.Row r = sh.getRow(i);
                                if (r == null)
                                    continue;
                                int rowYear = getAnValue(sh, i);
                                boolean match = (selectedAnalysisYear1 != null && rowYear == selectedAnalysisYear1)
                                        || (selectedAnalysisYear2 != null && rowYear == selectedAnalysisYear2)
                                        || (selectedAnalysisYear1 == null && selectedAnalysisYear2 == null);
                                if (!match)
                                    continue;
                                String sezon = r.getCell(2) != null ? r.getCell(2).toString() : "-";
                                pw.printf("%d,%d,%s,%.2f,%.2f,%.2f,%d%n", rowYear,
                                        r.getCell(1) != null ? (int) r.getCell(1).getNumericCellValue() : 0, sezon,
                                        r.getCell(3) != null ? r.getCell(3).getNumericCellValue() : 0,
                                        r.getCell(4) != null ? r.getCell(4).getNumericCellValue() : 0,
                                        r.getCell(5) != null ? r.getCell(5).getNumericCellValue() : 0,
                                        r.getCell(6) != null ? (int) r.getCell(6).getNumericCellValue() : 0);
                            }
                        }
                    }
                    break;
                }

                case "PREDICTIONS": {
                    pw.println("=== PREDICTII INTELIGENTE ===");
                    pw.println("Generat la:," + new java.util.Date());
                    pw.println();

                    // --- 1. Calculator Standard (Calculatorul 1) ---
                    pw.println("1. CALCULATOR PREDICTII (Standard)");
                    pw.println("Parametru,Valoare");
                    if (predLunaCombo != null)
                        pw.println("Luna selectata," + predLunaCombo.getValue());
                    if (predSezonCombo != null)
                        pw.println("Sezon selectat," + predSezonCombo.getValue());
                    if (predBugetField != null)
                        pw.println("Buget Marketing (EUR)," + predBugetField.getText());
                    if (predPretField != null)
                        pw.println("Pret Mediu (EUR)," + predPretField.getText());
                    if (predRezultatLabel != null)
                        pw.println(
                                "Vanzari Previzionate (EUR)," + predRezultatLabel.getText().replace(" €", "").trim());
                    pw.println();

                    // --- 2. Calculator Optimizer (Calculatorul 2) ---
                    pw.println("2. CALCULATOR OPTIMIZER (Tinta Vanzari)");
                    pw.println("Parametru,Valoare");
                    if (predTargetField != null)
                        pw.println("Tinta Vanzari (EUR)," + predTargetField.getText());
                    if (predOptBugetLabel != null && predOptBugetLabel.isVisible())
                        pw.println("Buget Recomandat (EUR)," + predOptBugetLabel.getText()
                                .replace("Buget Recomandat: ", "").replace(" €", "").trim());
                    if (predOptPretLabel != null && predOptPretLabel.isVisible())
                        pw.println("Pret Mediu Recomandat (EUR),"
                                + predOptPretLabel.getText().replace("Pret Mediu: ", "").replace(" €", "").trim());
                    if (predOptSalesLabel != null)
                        pw.println("Vanzari Estimate (EUR)," + predOptSalesLabel.getText().replace(" €", "").trim());
                    pw.println();

                    // --- 3. Grafic Prognoza Vanzari (Urmatoarele 6 Luni) ---
                    pw.println("3. PROGNOZA VANZARI (Urmatoarele 6 Luni)");
                    pw.println("Perioada (Luna/An),Vanzari Estimate (EUR)");
                    if (rn != null && fisierDate != null) {
                        try (java.io.FileInputStream fis2 = new java.io.FileInputStream(fisierDate);
                                org.apache.poi.ss.usermodel.Workbook wb2 = new org.apache.poi.xssf.usermodel.XSSFWorkbook(
                                        fis2)) {
                            org.apache.poi.ss.usermodel.Sheet sh2 = wb2.getSheetAt(0);
                            int lastRowIdx = sh2.getLastRowNum();
                            org.apache.poi.ss.usermodel.Row lastRow = null;
                            for (int i = lastRowIdx; i >= 1; i--) {
                                org.apache.poi.ss.usermodel.Row r = sh2.getRow(i);
                                if (r != null && getCellSafely(r, 1) != null &&
                                        getCellSafely(r, 1)
                                                .getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                                    lastRow = r;
                                    break;
                                }
                            }
                            if (lastRow != null) {
                                int currentMonth = (int) getCellSafely(lastRow, 1).getNumericCellValue();
                                int currentYear = getAnValue(sh2, lastRow.getRowNum());
                                double avgBudget = getCellSafely(lastRow, 3) != null
                                        ? getCellSafely(lastRow, 3).getNumericCellValue()
                                        : 5000;
                                double avgPrice = getCellSafely(lastRow, 4) != null
                                        ? getCellSafely(lastRow, 4).getNumericCellValue()
                                        : 50;
                                String[] monthNames = { "", "Ianuarie", "Februarie", "Martie", "Aprilie", "Mai",
                                        "Iunie", "Iulie", "August", "Septembrie", "Octombrie", "Noiembrie",
                                        "Decembrie" };
                                for (int i = 1; i <= 6; i++) {
                                    int nextM = currentMonth + i;
                                    int nextY = currentYear;
                                    if (nextM > 12) {
                                        nextM -= 12;
                                        nextY++;
                                    }
                                    int sezon = (nextM == 12 || nextM <= 2) ? 1
                                            : (nextM <= 5) ? 2 : (nextM <= 8) ? 3 : 4;
                                    double[] input = {
                                            (double) nextM / rn.factorLuna,
                                            (double) sezon / rn.factorSezon,
                                            avgBudget / rn.factorBuget,
                                            avgPrice / rn.factorPret
                                    };
                                    double[] out = rn.prezice(input);
                                    double val = out[0] * rn.factorVanzari;
                                    pw.printf("%s %d,%.2f%n", monthNames[nextM], nextY, val);
                                }
                            }
                        }
                    } else if (rn == null) {
                        pw.println("Nota,Modelul AI nu este antrenat - nu exista prognoza.");
                    }
                    break;
                }

                case "WHAT_IF": {
                    pw.println("=== SIMULATOR SCENARII ===");
                    pw.println("Generat la:," + new java.util.Date());
                    pw.println();

                    // --- Cardul de Scenariu (cardul oranj - parametrii introdusi) ---
                    pw.println("PARAMETRII SCENARIU (Cardul Oranj):");
                    pw.println("Parametru,Valoare");
                    if (simBugetTextField != null)
                        pw.println("Buget Marketing Scenariu (EUR)," + simBugetTextField.getText());
                    if (simPretTextField != null)
                        pw.println("Pret Mediu Scenariu (EUR)," + simPretTextField.getText());
                    pw.println();

                    // --- Rezultatele Comparatiei ---
                    pw.println("REZULTATE COMPARATIE:");
                    pw.println("Indicator,Valoare");
                    if (simVanzariActualLabel != null)
                        pw.println("Vanzari Actuale (EUR)," + simVanzariActualLabel.getText().replace(" €", "").trim());
                    if (simVanzariScenariuLabel != null)
                        pw.println(
                                "Vanzari Scenariu (EUR)," + simVanzariScenariuLabel.getText().replace(" €", "").trim());
                    if (simDiferentaLabel != null)
                        pw.println("Diferenta (EUR)," + simDiferentaLabel.getText().replace(" €", "").trim());
                    pw.println();

                    // --- Interpretarea AI ---
                    pw.println("INTERPRETARE REZULTATE (AI):");
                    pw.println("Recomandare");
                    if (lblRecomandareLocal != null && lblRecomandareLocal.getText() != null) {
                        String rec = lblRecomandareLocal.getText().replace(",", ";").replace("\n", " | ");
                        pw.println("\"" + rec + "\"");
                    }
                    break;
                }

                case "MANAGEMENT": {
                    pw.println("=== GESTIUNE & ANTRENARE MODEL ===");
                    pw.println("Generat la:," + new java.util.Date());
                    pw.println();
                    pw.println("Configurare Retea Neuronala:");
                    pw.println("Parametru,Valoare");
                    pw.println("Iteratii configurate," + txtIteratii.getText());
                    pw.println("Functie activare," + (folosesteLeakyReLU ? "LeakyReLU Optimizat" : "Sigmoid Clasic"));
                    pw.println("Setari custom," + (folosesteCustomSettings ? "DA" : "NU (Implicit)"));
                    if (folosesteCustomSettings) {
                        pw.println("Rata invatare initiala," + customInitialLR);
                        pw.println("Momentum," + customMomentum);
                        pw.println("Neuroni ascunsi," + customNeuroniAscunsi);
                    }
                    pw.println();
                    pw.println("Status antrenament," + lblInfoAntrenament.getText().replace(",", ";"));
                    pw.println();
                    pw.println("Fisiere Incarcate:");
                    pw.println("Nume Fisier,Cale,Dimensiune (bytes)");
                    for (File f : fisiereDateSelectate) {
                        pw.printf("%s,%s,%d%n", f.getName(), f.getAbsolutePath(), f.length());
                    }
                    if (!serieEroare.getData().isEmpty()) {
                        pw.println();
                        pw.println("Evolutie Eroare Antrenament:");
                        pw.println("Iteratie,Eroare (MSE)");
                        for (XYChart.Data<Number, Number> d : serieEroare.getData()) {
                            pw.printf("%s,%.6f%n", d.getXValue(), d.getYValue().doubleValue());
                        }
                    }
                    break;
                }

                case "EXECUTIVE": {
                    pw.println("=== RAPORT EXECUTIV COMPLET ===");
                    pw.println("Generat la:," + new java.util.Date());
                    pw.println("Aplicatie,NeuroCast - AI-Driven Decision Support");
                    pw.println();
                    int yr2 = (selectedDashboardYear != null) ? selectedDashboardYear : java.time.Year.now().getValue();
                    java.util.Map<String, Object> stats2 = calculeazaStatisticiGenerale(yr2);
                    if (stats2 != null) {
                        pw.println("KPI PRINCIPAL,Valoare");
                        pw.println("An de referinta," + yr2);
                        if (stats2.get("total") != null)
                            pw.println("Vanzari Totale (EUR),"
                                    + String.format("%.2f", ((Number) stats2.get("total")).doubleValue()));
                        if (stats2.get("medie") != null)
                            pw.println("Medie Lunara (EUR),"
                                    + String.format("%.2f", ((Number) stats2.get("medie")).doubleValue()));
                        if (stats2.get("cantitate") != null)
                            pw.println("Tranzactii/Clienti," + stats2.get("cantitate"));
                    }
                    pw.println();
                    pw.println("DATE ISTORICE COMPLETE:");
                    pw.println("An,Luna,Sezon,Buget Marketing (EUR),Pret Mediu (EUR),Vanzari (EUR),Clienti");
                    if (fisierDate != null) {
                        try (java.io.FileInputStream fis = new java.io.FileInputStream(fisierDate);
                                org.apache.poi.ss.usermodel.Workbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook(
                                        fis)) {
                            org.apache.poi.ss.usermodel.Sheet sh = wb.getSheetAt(0);
                            for (int i = 1; i <= sh.getLastRowNum(); i++) {
                                org.apache.poi.ss.usermodel.Row r = sh.getRow(i);
                                if (r == null)
                                    continue;
                                int rowYear = getAnValue(sh, i);
                                String sezon = r.getCell(2) != null ? r.getCell(2).toString() : "-";
                                pw.printf("%d,%d,%s,%.2f,%.2f,%.2f,%d%n", rowYear,
                                        r.getCell(1) != null ? (int) r.getCell(1).getNumericCellValue() : 0, sezon,
                                        r.getCell(3) != null ? r.getCell(3).getNumericCellValue() : 0,
                                        r.getCell(4) != null ? r.getCell(4).getNumericCellValue() : 0,
                                        r.getCell(5) != null ? r.getCell(5).getNumericCellValue() : 0,
                                        r.getCell(6) != null ? (int) r.getCell(6).getNumericCellValue() : 0);
                            }
                        }
                    }
                    pw.println();
                    if (!seriePreviziune.getData().isEmpty()) {
                        pw.println("PROGNOZA MODEL (Validare):");
                        pw.println("Index,Valoare Prezisa (EUR),Valoare Reala (EUR)");
                        java.util.List<XYChart.Data<Number, Number>> prev = seriePreviziune.getData();
                        java.util.List<XYChart.Data<Number, Number>> tint = serieTinta.getData();
                        int maxI = Math.max(prev.size(), tint.size());
                        for (int i = 0; i < maxI; i++) {
                            String pVal = i < prev.size() ? String.format("%.2f", prev.get(i).getYValue().doubleValue())
                                    : "-";
                            String tVal = i < tint.size() ? String.format("%.2f", tint.get(i).getYValue().doubleValue())
                                    : "-";
                            pw.printf("%d,%s,%s%n", i + 1, pVal, tVal);
                        }
                    }
                    pw.println();
                    pw.println("Configurare Retea Neuronala:");
                    pw.println("Iteratii," + txtIteratii.getText());
                    pw.println("Functie activare," + (folosesteLeakyReLU ? "LeakyReLU Optimizat" : "Sigmoid Clasic"));
                    pw.println("Status antrenament," + lblInfoAntrenament.getText().replace(",", ";"));
                    break;
                }
            }

            pw.flush();
            new Alert(Alert.AlertType.INFORMATION, "Fisier CSV salvat: " + fileName).show();
            try {
                Desktop.getDesktop().open(new File(fileName));
            } catch (Exception ex) {
                /* ignore */ }

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Eroare la generarea CSV: " + e.getMessage()).show();
        }
    }

    /** Extrage recursiv textele din Labels si Texts dintr-un nod JavaFX */
    private void extractTextLinesFromNode(javafx.scene.Node node, java.util.List<String> lines) {
        if (node instanceof javafx.scene.control.Label) {
            String t = ((javafx.scene.control.Label) node).getText();
            if (t != null && !t.isBlank())
                lines.add(t);
        } else if (node instanceof javafx.scene.text.Text) {
            String t = ((javafx.scene.text.Text) node).getText();
            if (t != null && !t.isBlank())
                lines.add(t);
        } else if (node instanceof javafx.scene.Parent) {
            for (javafx.scene.Node child : ((javafx.scene.Parent) node).getChildrenUnmodifiable()) {
                extractTextLinesFromNode(child, lines);
            }
        }
    }

    private void findKpiCards(Node root, List<java.util.Map<String, String>> kpiCards) {
        if (root == null)
            return;
        if (root.getProperties().containsKey("kpi_type")) {
            java.util.Map<String, String> map = new java.util.HashMap<>();
            map.put("type", (String) root.getProperties().get("kpi_type"));
            map.put("title", (String) root.getProperties().get("kpi_title"));
            map.put("val", (String) root.getProperties().get("kpi_val"));
            if (root.getProperties().containsKey("kpi_detalii"))
                map.put("detalii", (String) root.getProperties().get("kpi_detalii"));
            if (root.getProperties().containsKey("kpi_diff"))
                map.put("diff", (String) root.getProperties().get("kpi_diff"));
            if (root.getProperties().containsKey("kpi_luna"))
                map.put("luna", (String) root.getProperties().get("kpi_luna"));
            kpiCards.add(map);
        } else if (root instanceof javafx.scene.Parent) {
            for (Node child : ((javafx.scene.Parent) root).getChildrenUnmodifiable()) {
                findKpiCards(child, kpiCards);
            }
        }
    }

    private void extrageTextPentruRapoarte(Node root, Document document) {
        if (root == null || !root.isVisible() || !root.isManaged())
            return;
        if (root instanceof javafx.scene.chart.Chart || "heatmapBox".equals(root.getId()))
            return;

        if (root instanceof Label) {
            Label lbl = (Label) root;
            if (lbl.getText() != null && !lbl.getText().trim().isEmpty() && !lbl.getText().trim().equals("-")) {
                Paragraph p = new Paragraph(lbl.getText().trim());
                if (lbl.getFont() != null) {
                    if (lbl.getFont().getStyle().contains("Bold")) {
                        p.setBold();
                    }
                    if (lbl.getFont().getSize() >= 16) {
                        p.setFontSize((float) (lbl.getFont().getSize() * 0.7));
                        p.setMarginTop(10).setMarginBottom(5);
                    } else {
                        p.setFontSize(10f);
                    }
                }
                document.add(p);
            }
        } else if (root instanceof TextField) {
            TextField tf = (TextField) root;
            document.add(new Paragraph("> Input Data: " + tf.getText()).setItalic().setFontSize(10f).setMarginLeft(15));
        } else if (root instanceof ComboBox) {
            Object val = ((ComboBox<?>) root).getValue();
            if (val != null) {
                document.add(
                        new Paragraph("> Selecție: " + val.toString()).setItalic().setFontSize(10f).setMarginLeft(15));
            }
        } else if (root instanceof TableView) {
            TableView<?> tv = (TableView<?>) root;
            if (tv.getItems() != null && !tv.getItems().isEmpty() && !tv.getColumns().isEmpty()) {
                int colCount = tv.getColumns().size();
                com.itextpdf.layout.element.Table pdfTable = new com.itextpdf.layout.element.Table(
                        com.itextpdf.layout.properties.UnitValue.createPercentArray(colCount))
                        .useAllAvailableWidth().setMarginBottom(15).setMarginTop(5);

                // Headers
                for (javafx.scene.control.TableColumn<?, ?> col : tv.getColumns()) {
                    pdfTable.addHeaderCell(new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph(col.getText()).setBold().setFontSize(9))
                            .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY));
                }

                // Rows
                for (Object item : tv.getItems()) {
                    for (javafx.scene.control.TableColumn col : tv.getColumns()) {
                        Object cellResult = col.getCellValueFactory()
                                .call(new javafx.scene.control.TableColumn.CellDataFeatures(tv, col, item));
                        String cellText = "-";
                        if (cellResult instanceof javafx.beans.value.ObservableValue) {
                            Object val2 = ((javafx.beans.value.ObservableValue<?>) cellResult).getValue();
                            if (val2 != null)
                                cellText = val2.toString();
                        }
                        pdfTable.addCell(new com.itextpdf.layout.element.Cell()
                                .add(new Paragraph(cellText).setFontSize(9)));
                    }
                }
                document.add(pdfTable);
            }
        } else if (root instanceof javafx.scene.Parent) {
            for (Node child : ((javafx.scene.Parent) root).getChildrenUnmodifiable()) {
                extrageTextPentruRapoarte(child, document);
            }
        }
    }

    private void extractCharts(Node root, List<Node> chartsFound) {
        if (root == null)
            return;
        if (root instanceof javafx.scene.chart.Chart || "heatmapBox".equals(root.getId())) {
            chartsFound.add(root);
        } else if (root instanceof javafx.scene.Parent) {
            for (Node child : ((javafx.scene.Parent) root).getChildrenUnmodifiable()) {
                extractCharts(child, chartsFound);
            }
        }
    }

    private String findForecastData(Node root) {
        if (root == null)
            return null;
        if (root.getProperties().containsKey("forecast_values")) {
            return (String) root.getProperties().get("forecast_values");
        }
        if (root instanceof javafx.scene.Parent) {
            for (Node child : ((javafx.scene.Parent) root).getChildrenUnmodifiable()) {
                String val = findForecastData(child);
                if (val != null)
                    return val;
            }
        }
        return null;
    }

    private void generareRaportExecutiv() {
        // This method is now deprecated, its logic is moved to createExecutiveView()
        // and the call path is through generareRaportPDF()
        // The original content of this method is now in createExecutiveView()
    }

    // New helper to create the Executive Report View
    private VBox createExecutiveView() {
        if (fisiereDateSelectate.isEmpty() || rn == null) {
            return null;
        }

        // 1. Data Preparation (Calculation)
        double vanzariRealeLunaTrecuta = 0;
        double vanzariTotalePrevizionate3Luni = 0;
        String tendintaText = "incertă";
        double procentTendinta = 0;

        // Record Sales Calculation
        double maxSales = 0;
        String maxSalesDate = "-";
        int recordYear = 0;

        try (FileInputStream fis = new FileInputStream(getFisierDateExcel());
                Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRowIdx = sheet.getLastRowNum();
            Row lastRow = null;

            // Iterate all rows for Record Sales
            String[] months = { "", "Ian", "Feb", "Mar", "Apr", "Mai", "Iun", "Iul", "Aug", "Sep", "Oct", "Noi",
                    "Dec" };
            for (int i = 1; i <= lastRowIdx; i++) {
                Row r = sheet.getRow(i);
                if (r != null) {
                    org.apache.poi.ss.usermodel.Cell cV = getCellSafely(r, 5); // Sales
                    org.apache.poi.ss.usermodel.Cell cM = getCellSafely(r, 1); // Month

                    if (cV != null && cV.getCellType() == CellType.NUMERIC) {
                        double val = cV.getNumericCellValue();
                        if (val > maxSales) {
                            maxSales = val;
                            int m = (int) (cM != null ? cM.getNumericCellValue() : 0);
                            int y = getAnValue(sheet, i);
                            recordYear = y;
                            maxSalesDate = (m >= 1 && m <= 12 ? months[m] : "") + " " + y;
                        }
                    }
                }
            }

            for (int i = lastRowIdx; i >= 0; i--) {
                Row r = sheet.getRow(i);
                if (r != null && getCellSafely(r, 1) != null && getCellSafely(r, 1).getCellType() == CellType.NUMERIC) {
                    lastRow = r;
                    break;
                }
            }
            // ... rest of conclusion logic ...

            if (lastRow != null) {
                org.apache.poi.ss.usermodel.Cell cellVanzari = getCellSafely(lastRow, 5);
                vanzariRealeLunaTrecuta = (cellVanzari != null && cellVanzari.getCellType() == CellType.NUMERIC) 
                        ? cellVanzari.getNumericCellValue() : 0.0;
                
                org.apache.poi.ss.usermodel.Cell cellLuna = getCellSafely(lastRow, 1);
                int currentMonth = (cellLuna != null && cellLuna.getCellType() == CellType.NUMERIC) 
                        ? (int) cellLuna.getNumericCellValue() : 1;

                double avgBudget = (getCellSafely(lastRow, 3) != null) ? getCellSafely(lastRow, 3).getNumericCellValue()
                        : 5000;
                double avgPrice = (getCellSafely(lastRow, 4) != null) ? getCellSafely(lastRow, 4).getNumericCellValue()
                        : 50;

                for (int i = 1; i <= 3; i++) {
                    int nextM = currentMonth + i;
                    if (nextM > 12)
                        nextM -= 12;

                    int sezon = 1;
                    if (nextM >= 3 && nextM <= 5)
                        sezon = 2;
                    else if (nextM >= 6 && nextM <= 8)
                        sezon = 3;
                    else if (nextM >= 9 && nextM <= 11)
                        sezon = 4;

                    double[] input = {
                            (double) nextM / rn.factorLuna,
                            (double) sezon / rn.factorSezon,
                            avgBudget / rn.factorBuget,
                            avgPrice / rn.factorPret
                    };
                    double[] out = rn.prezice(input);
                    vanzariTotalePrevizionate3Luni += (out[0] * rn.factorVanzari);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        double mediePrevizionata = vanzariTotalePrevizionate3Luni / 3.0;
        double diff = mediePrevizionata - vanzariRealeLunaTrecuta;
        if (vanzariRealeLunaTrecuta > 0) {
            procentTendinta = (diff / vanzariRealeLunaTrecuta) * 100;
        }

        if (procentTendinta > 1)
            tendintaText = "creștere";
        else if (procentTendinta < -1)
            tendintaText = "scădere";
        else
            tendintaText = "stagnare";

        String concluzieFinala = String.format(
                "Modelul NeuroCast indică o tendință de %s a vânzărilor de %.1f%% pentru trimestrul următor (fata de ultima lună). "
                        +
                        "Se estimează un volum mediu lunar de %.0f EUR, în condițiile menținerii bugetului actual.",
                tendintaText, Math.abs(procentTendinta), mediePrevizionata);

        // 2. Build UI Layout with forced high resolution width
        int printWidth = 2480;
        VBox printView = new VBox(25);
        printView.setPadding(new Insets(40));
        printView.setStyle("-fx-background-color: white;");
        printView.setPrefWidth(printWidth);
        printView.setMaxWidth(printWidth);

        // Header
        Label lblTitluPrint = new Label("Analiza Vanzarilor & Prognoza (Executiv)");
        lblTitluPrint.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36)); // Larger font for 2480px width
        lblTitluPrint.setAlignment(Pos.CENTER);
        lblTitluPrint.setMaxWidth(Double.MAX_VALUE);

        // KPIs
        int anRef = (selectedDashboardYear != null) ? selectedDashboardYear : java.time.Year.now().getValue();
        java.util.Map<String, Object> stats = calculeazaStatisticiGenerale(anRef);

        VBox kpiTotal = creeazaCardKPIExport("Vânzări Totale (" + anRef + ")",
                String.format("%.0f €", stats.get("total")),
                "Total Anual", "#3b82f6");
        VBox kpiAvg = creeazaCardKPIExport("Vânzări Medii", String.format("%.0f €", stats.get("medie")), "Medie Lunară",
                "#10b981");
        // Record Sales KPI
        VBox kpiRecord = creeazaCardKPIExport("Vânzări Record", String.format("%.0f €", maxSales),
                maxSalesDate + " (Global)",
                "#ef4444");

        // Style KPIs for print (border, clean look)
        // Style KPIs for print (border, clean look)
        // creeazaCardKPIExport already sets style, but we can override if needed.
        // Actually, let's trust the export card style.

        // Make KPIs grow to fill width
        HBox.setHgrow(kpiTotal, Priority.ALWAYS);
        HBox.setHgrow(kpiAvg, Priority.ALWAYS);
        HBox.setHgrow(kpiRecord, Priority.ALWAYS);

        // Use HBox instead of GridPane for easier full width
        HBox kpiRow = new HBox(40); // Larger gap
        kpiRow.setAlignment(Pos.CENTER);
        kpiRow.getChildren().addAll(kpiTotal, kpiAvg, kpiRecord);
        kpiTotal.prefWidthProperty().bind(printView.widthProperty().divide(3));
        kpiAvg.prefWidthProperty().bind(printView.widthProperty().divide(3));
        kpiRecord.prefWidthProperty().bind(printView.widthProperty().divide(3));

        // Grafice
        VBox trendSection = creeazaSectiuneTrend(anRef, true);
        trendSection.setMaxWidth(Double.MAX_VALUE);
        trendSection.setPrefWidth(printWidth);

        // Increase chart font size for print - HANDLED INSIDE METHOD NOW
        // if (trendSection.getChildren().size() > 1 &&
        // trendSection.getChildren().get(1) instanceof LineChart) {
        // trendSection.getChildren().get(1).setStyle("-fx-font-size: 16px;");
        // }

        VBox forecastSection = creeazaSectiunePrognoza(true);
        forecastSection.setMaxWidth(Double.MAX_VALUE);
        forecastSection.setPrefWidth(printWidth);
        // if (forecastSection.getChildren().size() > 1 &&
        // forecastSection.getChildren().get(1) instanceof LineChart) {
        // forecastSection.getChildren().get(1).setStyle("-fx-font-size: 16px;");
        // }

        // Conclusion at the Bottom
        VBox conclusionBox = new VBox(15);
        conclusionBox.setPadding(new Insets(30));
        conclusionBox.setStyle(
                "-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1; -fx-border-width: 2; -fx-background-radius: 10;");

        Label lblConcTitle = new Label("CONCLUZIE MANAGERIALĂ");
        lblConcTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        lblConcTitle.setTextFill(Color.web("#1e293b"));

        Label lblConcText = new Label(concluzieFinala);
        lblConcText.setFont(Font.font("Segoe UI", FontPosture.ITALIC, 20));
        lblConcText.setWrapText(true);
        lblConcText.setTextFill(Color.web("#334155"));

        conclusionBox.getChildren().addAll(lblConcTitle, lblConcText);

        printView.getChildren().addAll(lblTitluPrint, kpiRow, trendSection, forecastSection, conclusionBox);

        return printView;
    }

    private void toggleSidebarPin() {
        isSidebarPinned = !isSidebarPinned;
        if (isSidebarPinned) {
            toggleButton.setText("Meniu 📌");
            setSidebarExpanded(true);
        } else {
            toggleButton.setText("Meniu");
            // Force collapse immediately when unpinned, even if mouse is over it
            setSidebarExpanded(false);
        }
    }

    private void setSidebarExpanded(boolean expand) {
        if (isAnimating || isSidebarExpanded == expand)
            return;
        isAnimating = true;

        isSidebarExpanded = expand;

        double endWidth = isSidebarExpanded ? 280 : 80;

        // Animation
        Timeline timeline = new Timeline();

        if (isSidebarExpanded) {
            // EXPANDING
            // Create animation for width
            KeyFrame kf = new KeyFrame(Duration.millis(250),
                    new KeyValue(sidebar.prefWidthProperty(), endWidth, javafx.animation.Interpolator.EASE_BOTH));
            timeline.getKeyFrames().add(kf);

            timeline.setOnFinished(e -> {
                // Show Text Elements AFTER expansion to avoid clipping/wrapping artifacts
                if (brandLabel != null && brandLabel.getParent() != null) {
                    brandLabel.getParent().setVisible(true);
                    brandLabel.getParent().setManaged(true);
                }

                if (lblDarkModeToggle != null) {
                    lblDarkModeToggle.setVisible(true);
                    lblDarkModeToggle.setManaged(true);
                }

                toggleButton.setContentDisplay(ContentDisplay.LEFT);
                toggleButton.setAlignment(Pos.CENTER_LEFT);
                toggleButton.setPadding(new Insets(10, 20, 10, 20));

                for (Button btn : sidebarButtons) {
                    btn.setContentDisplay(ContentDisplay.LEFT);
                    btn.setAlignment(Pos.CENTER_LEFT);
                    btn.setPadding(new Insets(12, 20, 12, 20));
                }
                isAnimating = false;
            });

        } else {
            // COLLAPSING
            // Hide Text Elements IMMEDIATELY to allow specific width collapse
            if (brandLabel != null && brandLabel.getParent() != null) {
                brandLabel.getParent().setVisible(false);
                brandLabel.getParent().setManaged(false);
            }

            if (lblDarkModeToggle != null) {
                lblDarkModeToggle.setVisible(false);
                lblDarkModeToggle.setManaged(false);
            }

            // Icon Only for Toggle Button - KEEP ALIGNMENT CENTER_LEFT to prevent jump
            toggleButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            toggleButton.setAlignment(Pos.CENTER_LEFT);
            toggleButton.setPadding(new Insets(10, 20, 10, 20)); // Keep 20px padding

            for (Button btn : sidebarButtons) {
                btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                btn.setAlignment(Pos.CENTER_LEFT); // Keep CENTER_LEFT
                btn.setPadding(new Insets(12, 20, 12, 20)); // Keep 20px padding
            }

            // Animate Width
            KeyFrame kf = new KeyFrame(Duration.millis(250),
                    new KeyValue(sidebar.prefWidthProperty(), endWidth, javafx.animation.Interpolator.EASE_BOTH));
            timeline.getKeyFrames().add(kf);

            timeline.setOnFinished(e -> isAnimating = false);
        }

        timeline.play();
    }

    // --- SCENARIU WHAT-IF (Simulare Decizii Manageriale) ---
    private void afiseazaEcranSimulator() {
        if (ecranSimulatorCache == null) {
            VBox view = new VBox(25);
            view.setPadding(new Insets(40));

            Label title = new Label("Simulator Scenarii");
            title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));

            Label subtitle = new Label("Ce se întâmplă DACĂ modific o decizie managerială?");
            subtitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
            subtitle.setTextFill(Color.GRAY);

            // Valori de bază (baseline) - pentru butonul de calcul
            TextField txtBugetActual = new TextField("5000");
            TextField txtPretActual = new TextField("50");
            ComboBox<String> comboLunaActual = new ComboBox<>();
            comboLunaActual.getItems().setAll("Ianuarie", "Februarie", "Martie", "Aprilie", "Mai", "Iunie", "Iulie",
                    "August", "Septembrie", "Octombrie", "Noiembrie", "Decembrie");
            comboLunaActual.getSelectionModel().select(0);
            ComboBox<String> comboSezonActual = new ComboBox<>();
            comboSezonActual.getItems().setAll("Iarnă (1)", "Primăvară (2)", "Vară (3)", "Toamnă (4)");
            comboSezonActual.getSelectionModel().select(0);

            // Date actuale din fișier (card implicit)
            VBox dateActualeBox = new VBox(15);
            dateActualeBox.setPadding(new Insets(15));
            dateActualeBox.setStyle(
                    "-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-width: 1; -fx-background-radius: 10; -fx-border-radius: 10;");

            Label dateActualeTitle = new Label("Date Actuale (la moment)");
            dateActualeTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));

            GridPane dateActualeForm = new GridPane();
            dateActualeForm.setHgap(15);
            dateActualeForm.setVgap(10);

            Label lblBugetActual = new Label("Buget Marketing: -");
            Label lblPretActual = new Label("Preț Mediu: -");
            Label lblLunaActual = new Label("Lună: -");
            Label lblSezonActual = new Label("Sezon: -");
            Label lblAnActual = new Label("An: -");
            Label lblVanzariActualDate = new Label("Vânzări: -");
            Label lblClientiActual = new Label("Nr. Clienți: -");

            dateActualeForm.add(lblBugetActual, 0, 0);
            dateActualeForm.add(lblPretActual, 1, 0);
            dateActualeForm.add(lblLunaActual, 0, 1);
            dateActualeForm.add(lblSezonActual, 1, 1);
            dateActualeForm.add(lblAnActual, 0, 2);
            dateActualeForm.add(lblVanzariActualDate, 1, 2);
            dateActualeForm.add(lblClientiActual, 0, 3);

            dateActualeForm.setVisible(false);
            dateActualeForm.setManaged(false);

            TableView<DateActualeItem> tabelDate = new TableView<>();
            tabelDate.setFixedCellSize(28); // Setează o dimensiune fixă a rândului
            tabelDate.setPrefHeight(58); // Fix pentru header + un rând
            tabelDate.setMinHeight(58);
            tabelDate.setMaxHeight(58);
            tabelDate.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: -1;");
            tabelDate.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            // Consumăm evenimentele de scroll pentru a dezactiva complet zona de scrolling
            tabelDate.addEventFilter(javafx.scene.input.ScrollEvent.ANY, javafx.event.Event::consume);

            TableColumn<DateActualeItem, String> colAn = new TableColumn<>("An");
            colAn.setCellValueFactory(data -> data.getValue().anProperty());

            TableColumn<DateActualeItem, String> colLuna = new TableColumn<>("Lună");
            colLuna.setCellValueFactory(data -> data.getValue().lunaProperty());

            TableColumn<DateActualeItem, String> colSezon = new TableColumn<>("Sezon");
            colSezon.setCellValueFactory(data -> data.getValue().sezonProperty());

            TableColumn<DateActualeItem, String> colBuget = new TableColumn<>("Buget");
            colBuget.setCellValueFactory(data -> data.getValue().bugetProperty());
            colBuget.getProperties().put("highlight-column", Boolean.TRUE);
            colBuget.setStyle("-fx-background-color: #fffbeb; -fx-font-weight: bold; -fx-text-fill: #d97706;");

            TableColumn<DateActualeItem, String> colPret = new TableColumn<>("Preț Med.");
            colPret.setCellValueFactory(data -> data.getValue().pretProperty());
            colPret.getProperties().put("highlight-column", Boolean.TRUE);
            colPret.setStyle("-fx-background-color: #fffbeb; -fx-font-weight: bold; -fx-text-fill: #d97706;");

            TableColumn<DateActualeItem, String> colVanzari = new TableColumn<>("Vânzări");
            colVanzari.setCellValueFactory(data -> data.getValue().vanzariProperty());

            TableColumn<DateActualeItem, String> colClienti = new TableColumn<>("Clienți");
            colClienti.setCellValueFactory(data -> data.getValue().clientiProperty());

            tabelDate.getColumns().addAll(colAn, colLuna, colSezon, colBuget, colPret, colVanzari, colClienti);

            javafx.collections.ObservableList<DateActualeItem> dateList = javafx.collections.FXCollections
                    .observableArrayList(
                            new DateActualeItem("-", "-", "-", "-", "-", "-", "-"));
            tabelDate.setItems(dateList);

            javafx.beans.value.ChangeListener<String> labelChangeListener = (obs, oldV, newV) -> {
                String a = lblAnActual.getText().contains(":") ? lblAnActual.getText().split(":", 2)[1].trim() : "-";
                String l = lblLunaActual.getText().contains(":") ? lblLunaActual.getText().split(":", 2)[1].trim()
                        : "-";
                String s = lblSezonActual.getText().contains(":") ? lblSezonActual.getText().split(":", 2)[1].trim()
                        : "-";
                String b = lblBugetActual.getText().contains(":") ? lblBugetActual.getText().split(":", 2)[1].trim()
                        : "-";
                String p = lblPretActual.getText().contains(":") ? lblPretActual.getText().split(":", 2)[1].trim()
                        : "-";
                String v = lblVanzariActualDate.getText().contains(":")
                        ? lblVanzariActualDate.getText().split(":", 2)[1].trim()
                        : "-";
                String c = lblClientiActual.getText().contains(":") ? lblClientiActual.getText().split(":", 2)[1].trim()
                        : "-";

                a = a.replace("(încarcă un fișier)", "").replace("(date indisponibile)", "").replace("()", "").trim();
                l = l.replace("(încarcă un fișier)", "").replace("(date indisponibile)", "").replace("()", "").trim();
                s = s.replace("(încarcă un fișier)", "").replace("(date indisponibile)", "").replace("()", "").trim();
                b = b.replace("(încarcă un fișier)", "").replace("(date indisponibile)", "").replace("()", "").trim();
                p = p.replace("(încarcă un fișier)", "").replace("(date indisponibile)", "").replace("()", "").trim();
                v = v.replace("(încarcă un fișier)", "").replace("(date indisponibile)", "").replace("()", "").trim();
                c = c.replace("(încarcă un fișier)", "").replace("(date indisponibile)", "").replace("()", "").trim();

                dateList.set(0, new DateActualeItem(
                        a.isEmpty() ? "-" : a,
                        l.isEmpty() ? "-" : l,
                        s.isEmpty() ? "-" : s,
                        b.isEmpty() ? "-" : b,
                        p.isEmpty() ? "-" : p,
                        v.isEmpty() ? "-" : v,
                        c.isEmpty() ? "-" : c));
            };

            lblAnActual.textProperty().addListener(labelChangeListener);
            lblLunaActual.textProperty().addListener(labelChangeListener);
            lblSezonActual.textProperty().addListener(labelChangeListener);
            lblBugetActual.textProperty().addListener(labelChangeListener);
            lblPretActual.textProperty().addListener(labelChangeListener);
            lblVanzariActualDate.textProperty().addListener(labelChangeListener);
            lblClientiActual.textProperty().addListener(labelChangeListener);

            dateActualeBox.getChildren().addAll(dateActualeTitle, dateActualeForm, tabelDate);

            // Scenariu modificat (what-if) - fără sentiment
            VBox scenariuBox = new VBox(15);
            tutorialCardScenariu = scenariuBox;
            scenariuBox.setPadding(new Insets(15));
            scenariuBox.setStyle(
                    "-fx-background-color: #fffbeb; -fx-border-color: #f59e0b; -fx-border-width: 2; -fx-background-radius: 10; -fx-border-radius: 10;");
            // Marcam cardul pentru tratament special in dark mode — va deveni dark blue
            scenariuBox.getProperties().put("simulator-orange-card", Boolean.TRUE);

            Label scenariuTitle = new Label("Scenariu Simulat");
            scenariuTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));

            GridPane scenariuForm = new GridPane();
            scenariuForm.setHgap(15);
            scenariuForm.setVgap(10);

            Slider sBuget = new Slider(1000, 15000, 5000);
            simBugetSlider = sBuget;
            Slider sPret = new Slider(10, 200, 50);
            simPretSlider = sPret;

            // Campuri text pentru introducere manuala
            TextField txtBugetManual = new TextField("5000");
            simBugetTextField = txtBugetManual;
            TextField txtPretManual = new TextField("50");
            simPretTextField = txtPretManual;

            txtBugetManual.setPrefWidth(120);
            txtPretManual.setPrefWidth(120);

            VBox bugetSliderBox = new VBox(5, new Label("Buget Marketing: 5000€"), sBuget, txtBugetManual);
            VBox pretSliderBox = new VBox(5, new Label("Preț Mediu: 50€"), sPret, txtPretManual);

            scenariuForm.add(bugetSliderBox, 0, 0);
            scenariuForm.add(pretSliderBox, 1, 0);

            // Actualizare etichete la schimbarea sliderelor
            sBuget.valueProperty().addListener((obs, oldVal, newVal) -> {
                bugetSliderBox.getChildren().set(0, new Label("Buget Marketing: " + newVal.intValue() + "€"));
                txtBugetManual.setText(String.valueOf(newVal.intValue()));
                executaSimulareWhatIf();
            });

            sPret.valueProperty().addListener((obs, oldVal, newVal) -> {
                pretSliderBox.getChildren().set(0, new Label("Preț Mediu: " + newVal.intValue() + "€"));
                txtPretManual.setText(String.valueOf(newVal.intValue()));
                executaSimulareWhatIf();
            });

            // Actualizare slidere la schimbarea câmpurilor text
            txtBugetManual.textProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    int valoare = Integer.parseInt(newVal);
                    if (valoare >= 1000 && valoare <= 15000) {
                        sBuget.setValue(valoare);
                        bugetSliderBox.getChildren().set(0, new Label("Buget Marketing: " + valoare + "€"));
                        executaSimulareWhatIf();
                    }
                } catch (NumberFormatException e) {
                    // Ignorăm valorile invalide
                }
            });

            txtPretManual.textProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    int valoare = Integer.parseInt(newVal);
                    if (valoare >= 10 && valoare <= 200) {
                        sPret.setValue(valoare);
                        pretSliderBox.getChildren().set(0, new Label("Preț Mediu: " + valoare + "€"));
                        executaSimulareWhatIf();
                    }
                } catch (NumberFormatException e) {
                    // Ignorăm valorile invalide
                }
            });

            scenariuBox.getChildren().addAll(scenariuTitle, scenariuForm);

            // Buton pentru calcul rezultate
            Button btnCalculeaza = new Button("Calculează Rezultatele");
            tutorialBtnCalculeaza = btnCalculeaza;
            btnCalculeaza.setStyle(
                    "-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 30; -fx-font-size: 16px;");

            // Grafic comparativ
            VBox graficBox = new VBox(15);
            tutorialGraficComparativ = graficBox;
            graficBox.setPadding(new Insets(25));
            graficBox.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");

            Label graficTitle = new Label("Grafic Comparativ Vânzări");
            graficTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));

            // Creăm graficul comparativ
            CategoryAxis xAxisGrafic = new CategoryAxis();
            xAxisGrafic.setLabel("Tip Vânzări");
            NumberAxis yAxisGrafic = new NumberAxis();
            yAxisGrafic.setLabel("Valoare (€)");

            BarChart<String, Number> barChart = new BarChart<>(xAxisGrafic, yAxisGrafic);
            barChart.setTitle("Actual vs Scenariu");
            barChart.setLegendVisible(true);

            XYChart.Series<String, Number> seriesComparativ = new XYChart.Series<>();
            seriesComparativ.setName("Comparativ"); // Legendă (poate fi ascunsă)

            XYChart.Data<String, Number> dataActual = new XYChart.Data<>("Actual", 0);
            XYChart.Data<String, Number> dataScenariu = new XYChart.Data<>("Scenariu", 0);

            seriesComparativ.getData().addAll(dataActual, dataScenariu);

            barChart.getData().add(seriesComparativ);
            barChart.setLegendVisible(false); // Ascundem legenda deoarece avem etichete clare
            barChart.setCategoryGap(50); // Facem barele mai subțiri (spațiu mai mare între categorii)

            // Aplicam culori distincte bar-urilor printr-un listener pe nodul JavaFX
            // (mai robust decat Platform.runLater — se re-aplica si dupa schimbari de tab)
            dataActual.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle("-fx-bar-fill: #e67e22;");
                    newNode.getProperties().put("preserve_bar_style", true);
                }
            });
            dataScenariu.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle("-fx-bar-fill: #f1c40f;");
                    newNode.getProperties().put("preserve_bar_style", true);
                }
            });
            // Aplicare initiala (pentru prima afisare)
            javafx.application.Platform.runLater(() -> {
                if (dataActual.getNode() != null) {
                    dataActual.getNode().setStyle("-fx-bar-fill: #e67e22;");
                    dataActual.getNode().getProperties().put("preserve_bar_style", true);
                }
                if (dataScenariu.getNode() != null) {
                    dataScenariu.getNode().setStyle("-fx-bar-fill: #f1c40f;");
                    dataScenariu.getNode().getProperties().put("preserve_bar_style", true);
                }
            });
            barChart.setPrefHeight(400);

            StackPane barChartWrapper = new StackPane(barChart);
            adaugaButonFullscreen(barChartWrapper, barChart, "Grafic Comparativ Vânzări");
            graficBox.getChildren().addAll(graficTitle, barChartWrapper);

            // Layout principal - HBox pentru a pune graficul pe dreapta
            HBox mainLayout = new HBox(30);
            mainLayout.setPadding(new Insets(20));

            // Partea stângă - controale și rezultate
            VBox leftPanel = new VBox(25);
            leftPanel.setPrefWidth(650);

            // Comparare rezultate
            VBox comparatieBox = new VBox(15);
            tutorialCardRezultate = comparatieBox;
            comparatieBox.setPadding(new Insets(25));
            comparatieBox.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");

            Label comparatieTitle = new Label("Rezultate Comparație");
            comparatieTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));

            HBox rezultateComparatie = new HBox(30);

            VBox vanzariActualBox = new VBox(5);
            vanzariActualBox.setAlignment(Pos.CENTER);
            Label lblVanzariActual = new Label("0.00 €");
            simVanzariActualLabel = lblVanzariActual;
            lblVanzariActual.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
            Label lblVanzariActualText = new Label("Vânzări Actuale");
            lblVanzariActualText.setTextFill(Color.GRAY);
            vanzariActualBox.getChildren().addAll(lblVanzariActual, lblVanzariActualText);

            VBox vanzariScenariuBox = new VBox(5);
            vanzariScenariuBox.setAlignment(Pos.CENTER);
            Label lblVanzariScenariu = new Label("0.00 €");
            simVanzariScenariuLabel = lblVanzariScenariu;
            lblVanzariScenariu.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
            Label lblVanzariScenariuText = new Label("Vânzări Scenariu");
            lblVanzariScenariuText.setTextFill(Color.GRAY);
            vanzariScenariuBox.getChildren().addAll(lblVanzariScenariu, lblVanzariScenariuText);

            VBox diferentaBox = new VBox(5);
            diferentaBox.setAlignment(Pos.CENTER);
            Label lblDiferenta = new Label("0.00 €");
            simDiferentaLabel = lblDiferenta;
            lblDiferenta.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
            Label lblDiferentaText = new Label("Diferență");
            lblDiferentaText.setTextFill(Color.GRAY);
            diferentaBox.getChildren().addAll(lblDiferenta, lblDiferentaText);

            rezultateComparatie.getChildren().addAll(vanzariActualBox, vanzariScenariuBox, diferentaBox);

            comparatieBox.getChildren().addAll(comparatieTitle, rezultateComparatie);

            leftPanel.getChildren().addAll(dateActualeBox, scenariuBox, btnCalculeaza, comparatieBox);

            // Partea dreaptă - grafic
            VBox rightPanel = new VBox(25);
            rightPanel.getChildren().add(graficBox);

            mainLayout.getChildren().addAll(leftPanel, rightPanel);

            // Event handler pentru butonul de calcul
            btnCalculeaza.setOnAction(e -> {
                // Definim o variabilă locală pentru recomandare
                executaCalculWhatIf(
                        rn, txtBugetActual, txtPretActual, comboLunaActual, comboSezonActual,
                        sBuget, sPret, txtBugetManual, txtPretManual,
                        lblVanzariActual, lblVanzariScenariu, lblDiferenta,
                        seriesComparativ, lblRecomandareLocal,
                        lblBugetActual, lblPretActual, lblLunaActual, lblSezonActual, lblAnActual,
                        lblVanzariActualDate);
            });

            // Recomandare AI
            VBox recomandareBox = new VBox(10);
            recomandareBox.setPadding(new Insets(20));
            recomandareBox.setStyle(
                    "-fx-background-color: #eff6ff; -fx-border-color: #3b82f6; -fx-border-width: 0 0 0 5; -fx-background-radius: 0 10 10 0;");

            Label recomandareTitle = new Label("Interpretare Rezultate");
            recomandareTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
            recomandareTitle.setTextFill(Color.web("#1e40af"));

            lblRecomandareLocal = new Label(
                    "Modificați valorile și apăsați butonul 'Calculează Rezultatele' pentru a vedea recomandarea AI");
            lblRecomandareLocal.setWrapText(true);

            recomandareBox.getChildren().addAll(recomandareTitle, lblRecomandareLocal);

            view.getChildren().addAll(title, subtitle, mainLayout, recomandareBox);

            // Inițializare valorile
            executaSimulareWhatIf();

            ecranSimulatorCache = view;
        }

        // Actualizăm datele actuale de fiecare dată când se afișează tabul
        // Găsim componentele din cache pentru actualizare
        HBox mainLayout = (HBox) ecranSimulatorCache.getChildren().get(2); // Acesta este HBox-ul principal
        VBox leftPanel = (VBox) mainLayout.getChildren().get(0); // Partea stângă
        VBox actualDateBox = (VBox) leftPanel.getChildren().get(0); // Cardul cu datele actuale
        GridPane dateActualeForm = (GridPane) actualDateBox.getChildren().get(1); // Formularul cu etichetele

        Label lblBugetActual = (Label) dateActualeForm.getChildren().get(0); // Buget Marketing
        Label lblPretActual = (Label) dateActualeForm.getChildren().get(1); // Preț Mediu
        Label lblLunaActual = (Label) dateActualeForm.getChildren().get(2); // Lună
        Label lblSezonActual = (Label) dateActualeForm.getChildren().get(3); // Sezon
        Label lblAnActual = (Label) dateActualeForm.getChildren().get(4); // An
        Label lblVanzariActualDate = (Label) dateActualeForm.getChildren().get(5); // Vânzări
        Label lblClientiActual = (Label) dateActualeForm.getChildren().get(6); // Clienti

        // Recuperăm și seriile din grafic pentru a le actualiza
        VBox rightPanel = (VBox) mainLayout.getChildren().get(1); // Partea dreaptă
        VBox graficBox = (VBox) rightPanel.getChildren().get(0); // Cutia graficului
        StackPane barChartWrapperRef = (StackPane) graficBox.getChildren().get(1); // Wrapper graficului
        BarChart<String, Number> barChart = (BarChart<String, Number>) barChartWrapperRef.getChildren().get(0); // Graficul
                                                                                                                // propriu-zis
        XYChart.Series<String, Number> seriesComparativ = barChart.getData().get(0);

        // Recuperăm etichetele din secțiunea "Rezultate Comparație"
        VBox comparatieBox = (VBox) leftPanel.getChildren().get(3);
        HBox rezultateComparatie = (HBox) comparatieBox.getChildren().get(1);

        VBox boxActual = (VBox) rezultateComparatie.getChildren().get(0);
        Label lblRaVanzariActual = (Label) boxActual.getChildren().get(0);

        VBox boxScenariu = (VBox) rezultateComparatie.getChildren().get(1);
        Label lblRaVanzariScenariu = (Label) boxScenariu.getChildren().get(0);

        VBox boxDiferenta = (VBox) rezultateComparatie.getChildren().get(2);
        Label lblRaDiferenta = (Label) boxDiferenta.getChildren().get(0);

        // Actualizăm datele actuale din fișierul încărcat
        actualizeazaDateActuale(lblBugetActual, lblPretActual, lblLunaActual, lblSezonActual, lblAnActual,
                lblVanzariActualDate, lblClientiActual, seriesComparativ,
                lblRaVanzariActual, lblRaVanzariScenariu, lblRaDiferenta);

        // Afișăm conținutul din cache
        setContentArea(ecranSimulatorCache);
        contentArea.setVvalue(0.0);
        contentArea.setHvalue(0.0);
    }

    // --- ECRAN TEHNIC (GESTIUNE & ANTRENARE) ---
    private void afiseazaEcranTehnic() {
        // Restaurăm ultimul subtab selectat
        afiseazaEcranTehnic(lastTehnicTabIndex);
    }

    private void afiseazaEcranTehnic(int tabIndex) {
        if (ecranTehnicCache == null) {
            TabPane subTabs = new TabPane();
            subTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            subTabs.setStyle(
                    "-fx-tab-min-height: 32px; -fx-tab-max-height: 32px; -fx-font-size: 13px; -fx-font-weight: bold;");

            Tab tabDate = new Tab("📂 Gestiune Fișiere", creeazaEcranGestiuneDate());
            Tab tabGrafic = new Tab("📈 Antrenare Rețea", creeazaEcranGraficInteractiv());

            subTabs.getTabs().addAll(tabDate, tabGrafic);

            // Memorăm indexul subtabului selectat la fiecare schimbare
            subTabs.getSelectionModel().selectedIndexProperty().addListener((obs, oldIdx, newIdx) -> {
                if (newIdx != null && newIdx.intValue() >= 0) {
                    lastTehnicTabIndex = newIdx.intValue();
                }
            });

            // Salvăm în cache
            ecranTehnicCache = subTabs;
        }

        // Selectăm tab-ul cerut
        if (ecranTehnicCache instanceof TabPane) {
            TabPane tabs = (TabPane) ecranTehnicCache;
            if (tabIndex >= 0 && tabIndex < tabs.getTabs().size()) {
                tabs.getSelectionModel().select(tabIndex);
            }
        }

        // Afișăm conținutul din cache
        setContentArea(ecranTehnicCache);
        contentArea.setVvalue(0.0);
        contentArea.setHvalue(0.0);
    }

    private VBox creeazaEcranGraficInteractiv() {
        VBox root = new VBox(15); // Reduced spacing
        root.setPadding(new Insets(15, 60, 15, 25)); // Reduced top/bottom padding
        root.setFillWidth(true);

        // Bara de comenzi
        HBox config = new HBox(15);
        tutorialConfigBox = config;
        config.setAlignment(Pos.CENTER_LEFT);

        txtIteratii.setPrefWidth(80);

        // System overload warning validation
        txtIteratii.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                // Focus gained
                savedIteratiiValue = txtIteratii.getText();
            } else {
                // Focus lost
                if (!txtIteratii.getText().equals(savedIteratiiValue)) {
                    try {
                        int val = Integer.parseInt(txtIteratii.getText());
                        if (val > 10000 && !suppressIterationWarning) {
                            Alert alert = new Alert(Alert.AlertType.WARNING);
                            alert.setTitle("Avertizare Suprasolicitare");
                            alert.setHeaderText("Atenție: Mărirea considerabilă a numărului de iterații!");

                            // Custom layout for CheckBox inside Alert
                            VBox content = new VBox(10);
                            Label msg = new Label(
                                    "Creșterea numărului de iterații peste valoarea implicită de 10.000 va spori considerabil timpul de execuție.\nDe asemenea, procesul poate solicita intensiv resursele sistemului și ale calculatorului.\n\nMăriți cu prudență!\nSunteți sigur că doriți să continuați?");
                            msg.setWrapText(true);
                            CheckBox chkAscunde = new CheckBox(
                                    "Nu mai afișa acest avertisment până la repornirea aplicației");
                            content.getChildren().addAll(msg, chkAscunde);
                            alert.getDialogPane().setContent(content);

                            alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
                            java.util.Optional<ButtonType> result = alert.showAndWait();

                            if (result.isPresent() && result.get() == ButtonType.CANCEL) {
                                txtIteratii.setText(savedIteratiiValue); // Revert to previous value
                            } else {
                                savedIteratiiValue = txtIteratii.getText(); // Accept new value
                                if (chkAscunde.isSelected()) {
                                    suppressIterationWarning = true;
                                }
                            }
                        } else {
                            savedIteratiiValue = txtIteratii.getText();
                        }
                    } catch (NumberFormatException ex) {
                        txtIteratii.setText(savedIteratiiValue); // If invalid text, revert
                    }
                }
            }
        });

        btnStartAntrenare = new Button("🚀 Antrenează");
        btnStartAntrenare.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold;");

        btnStopAntrenare = new Button("Stop");
        btnStopAntrenare.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        btnStopAntrenare.setDisable(true);
        btnStopAntrenare.setOnAction(e -> {
            opresteAntrenarea = true;
            btnStopAntrenare.setDisable(true);
        });

        Button btnReset = new Button("🔄 Reset Zoom");
        btnReset.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white;");

        Button btnSalveazaModel = new Button("💾 Salvează Model");
        btnSalveazaModel.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");

        chkUseTrainedModel = new CheckBox("Folosește modelul antrenat local");
        chkUseTrainedModel.setDisable(true); // Default disabled
        chkUseTrainedModel.setStyle("-fx-opacity: 0.5; -fx-text-fill: #7f8c8d;"); // Visual cue for disabled

        // Tooltip for validation message
        Tooltip validationTooltip = new Tooltip(
                "Această opțiune este indisponibilă. Antrenați un model pentru a o activa.");
        validationTooltip.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 12px;");
        // Wrapper for hover effect when disabled (since disabled nodes don't get mouse
        // events)
        StackPane chkWrapper = new StackPane(chkUseTrainedModel);
        chkWrapper.setOnMouseEntered(e -> {
            if (chkUseTrainedModel.isDisabled()) {
                Tooltip.install(chkWrapper, validationTooltip);
                // Show immediately
                Point2D p = chkWrapper.localToScreen(chkWrapper.getLayoutBounds().getMaxX(),
                        chkWrapper.getLayoutBounds().getMaxY());
                validationTooltip.show(chkWrapper, p.getX(), p.getY());
            } else {
                // Ensure tooltip is hidden/uninstalled if enabled
                Tooltip.uninstall(chkWrapper, validationTooltip);
                validationTooltip.hide();
            }
        });
        chkWrapper.setOnMouseExited(e -> validationTooltip.hide());

        chkUseTrainedModel.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                if (trainedModel != null) {
                    rn = trainedModel;
                    new Alert(Alert.AlertType.INFORMATION, "Modelul antrenat local este acum activ pentru predicții.")
                            .show();
                }
            } else {
                if (loadedModel != null) {
                    rn = loadedModel;
                    new Alert(Alert.AlertType.INFORMATION,
                            "Modelul preîncărcat este acum activ pentru predicții.").show();
                } else {
                    // Fallback extrem: dacă loadedModel a fost pierdut/nulificat din greșeală
                    // dar există un fisier .ser în tabelul de gestiune fișiere
                    File orphanSer = null;
                    for (File f : fisiereDateSelectate) {
                        if (f.getName().endsWith(".ser") && f.exists()) {
                            orphanSer = f;
                            break;
                        }
                    }
                    if (orphanSer != null) {
                        try {
                            rn = ReteaNeuronala.incarcaModel(orphanSer.getAbsolutePath());
                            loadedModel = rn;
                            loadedModelSourceFile = orphanSer;
                            new Alert(Alert.AlertType.INFORMATION,
                                    "Modelul preîncărcat a fost reactivat pentru predicții!").show();
                        } catch (Exception ex) {
                            new Alert(Alert.AlertType.ERROR, "Eroare la recuperarea modelului: " + ex.getMessage())
                                    .show();
                        }
                    } else if (trainedModel != null) {
                        rn = trainedModel;
                        // Putem forța bifa înapoi pe selectat pentru că nu are la ce să dea fallback
                        javafx.application.Platform.runLater(() -> chkUseTrainedModel.setSelected(true));
                        new Alert(Alert.AlertType.WARNING,
                                "Nu există un model preîncărcat valid! Păstrăm modelul proaspăt antrenat.")
                                .show();
                    } else {
                        rn = null;
                        new Alert(Alert.AlertType.WARNING,
                                "Nu există un model pre-încărcat. Aplicația nu are un model activ acum. Încărcați un model din 'Gestiune Fișiere'.")
                                .show();
                    }
                }
            }
            // Invalidate caches to refresh charts and views with new model
            // ecranPredictiiCache = null; // Removed to preserve calculator state
            ecranSimulatorCache = null;
            ecranAnalizaCache = null;
            invalidateDashboardCache();

        });

        // Remove direct install on checkbox to avoid it showing when enabled
        Tooltip.uninstall(chkUseTrainedModel, validationTooltip);

        // Update style when enabled
        chkUseTrainedModel.disabledProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                chkUseTrainedModel.setStyle("-fx-opacity: 0.5; -fx-text-fill: #7f8c8d;");
            } else {
                chkUseTrainedModel.setStyle("-fx-opacity: 1.0; -fx-text-fill: #2c3e50; -fx-cursor: hand;");
            }
        });

        // --- Buton Setări Rețea ---
        Button btnSetariRetea = new Button("⚙ Setări rețea");
        btnSetariRetea.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold;");
        btnSetariRetea.setOnAction(e -> {
            // --- Dialog cu TabPane ---
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Setări Rețea Neuronală");
            dialog.setHeaderText(null); // Fără header text, taburile sunt suficiente

            TabPane tabPane = new TabPane();
            tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            tabPane.setPrefWidth(460);
            tabPane.setPrefHeight(380);

            // =====================================================
            // TAB 1: TIPUL
            // =====================================================
            Tab tabTipul = new Tab("Tipul");

            ToggleGroup toggleGroupTip = new ToggleGroup();

            RadioButton rbSigmoid = new RadioButton("MLP Sigmoid Clasic");
            rbSigmoid.setToggleGroup(toggleGroupTip);
            rbSigmoid.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            Label lblSigmoidDesc = new Label(
                    "Sigmoid pe toate straturile\n" +
                            "Ini\u021bializare: Gaussian\u0103 \u00d7 0.1\n" +
                            "Rat\u0103 \u00eenv\u0103\u021bare: 0.4 \u2192 0.05\n" +
                            "Recomandat: 100.000+ itera\u021bii");
            lblSigmoidDesc.setStyle("-fx-text-fill: #7f8c8d; -fx-padding: 0 0 0 20;");
            VBox sigmoidBox = new VBox(5, rbSigmoid, lblSigmoidDesc);
            sigmoidBox.setStyle(
                    "-fx-padding: 10; -fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-border-color: #dee2e6; -fx-border-radius: 8; -fx-cursor: hand;");
            sigmoidBox.setOnMouseClicked(ev -> rbSigmoid.setSelected(true));

            RadioButton rbLeakyReLU = new RadioButton("MLP LeakyReLU Optimizat");
            rbLeakyReLU.setToggleGroup(toggleGroupTip);
            rbLeakyReLU.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            Label lblLeakyDesc = new Label(
                    "LeakyReLU pe stratul ascuns + Sigmoid pe ie\u0219ire\n" +
                            "Ini\u021bializare: He (ascuns) / Xavier (ie\u0219ire)\n" +
                            "Rat\u0103 \u00eenv\u0103\u021bare: 0.1 \u2192 0.01\n" +
                            "Recomandat: 10.000 \u2013 40.000 itera\u021bii");
            lblLeakyDesc.setStyle("-fx-text-fill: #7f8c8d; -fx-padding: 0 0 0 20;");
            VBox leakyBox = new VBox(5, rbLeakyReLU, lblLeakyDesc);
            leakyBox.setStyle(
                    "-fx-padding: 10; -fx-background-color: #f0f9ff; -fx-background-radius: 8; -fx-border-color: #bae6fd; -fx-border-radius: 8; -fx-cursor: hand;");
            leakyBox.setOnMouseClicked(ev -> rbLeakyReLU.setSelected(true));

            if (folosesteLeakyReLU) {
                rbLeakyReLU.setSelected(true);
            } else {
                rbSigmoid.setSelected(true);
            }

            VBox tipContent = new VBox(15, sigmoidBox, leakyBox);
            tipContent.setPadding(new Insets(15));
            tabTipul.setContent(tipContent);

            // =====================================================
            // TAB 2: CUSTOMIZEAZĂ
            // =====================================================
            Tab tabCustom = new Tab("Customizeaz\u0103");

            // --- RadioButtons: Implicit / Customizează ---
            ToggleGroup toggleGroupCustom = new ToggleGroup();
            RadioButton rbImplicit = new RadioButton("Implicit");
            rbImplicit.setToggleGroup(toggleGroupCustom);
            rbImplicit.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            Label lblImplicitDesc = new Label("Valorile implicite ale modelului selectat");
            lblImplicitDesc.setStyle("-fx-text-fill: #7f8c8d; -fx-padding: 0 0 0 20;");

            RadioButton rbCustom = new RadioButton("Customizeaz\u0103");
            rbCustom.setToggleGroup(toggleGroupCustom);
            rbCustom.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            Label lblCustomDesc = new Label("Seta\u021bi manual valorile parametrilor");
            lblCustomDesc.setStyle("-fx-text-fill: #7f8c8d; -fx-padding: 0 0 0 20;");

            VBox radioSection = new VBox(8, rbImplicit, lblImplicitDesc, rbCustom, lblCustomDesc);
            radioSection.setStyle(
                    "-fx-padding: 10; -fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-border-color: #dee2e6; -fx-border-radius: 8;");

            // --- Câmpurile editabile ---
            // Determinăm valorile implicite în funcție de modelul selectat
            double defaultLR = folosesteLeakyReLU ? 0.1 : 0.4;
            double defaultMomentum = 0.9;
            int defaultNeuroni = 12;

            // Dacă utilizatorul avea deja setări custom, le folosim pe acelea
            if (folosesteCustomSettings) {
                defaultLR = customInitialLR;
                defaultMomentum = customMomentum;
                defaultNeuroni = customNeuroniAscunsi;
            }

            TextField txtLR = new TextField(String.valueOf(defaultLR));
            txtLR.setPrefWidth(120);
            TextField txtMomentumField = new TextField(String.valueOf(defaultMomentum));
            txtMomentumField.setPrefWidth(120);
            TextField txtNeuroni = new TextField(String.valueOf(defaultNeuroni));
            txtNeuroni.setPrefWidth(120);

            // Labels de avertizare (vizibile doar la erori de validare)
            Label warnLR = new Label();
            warnLR.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 10px;");
            Label warnMomentum = new Label();
            warnMomentum.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 10px;");
            Label warnNeuroni = new Label();
            warnNeuroni.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 10px;");

            // Validare la schimbarea textului
            txtLR.textProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    double v = Double.parseDouble(newVal);
                    if (v < 0.001 || v > 1.0) {
                        warnLR.setText("\u26a0 Recomandat: 0.001 \u2013 1.0");
                    } else {
                        warnLR.setText("");
                    }
                } catch (NumberFormatException ex) {
                    warnLR.setText("\u26a0 Valoare numeric\u0103 invalid\u0103");
                }
            });
            txtMomentumField.textProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    double v = Double.parseDouble(newVal);
                    if (v < 0.0 || v > 0.99) {
                        warnMomentum.setText("\u26a0 Recomandat: 0.0 \u2013 0.99");
                    } else {
                        warnMomentum.setText("");
                    }
                } catch (NumberFormatException ex) {
                    warnMomentum.setText("\u26a0 Valoare numeric\u0103 invalid\u0103");
                }
            });
            txtNeuroni.textProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    int v = Integer.parseInt(newVal);
                    if (v < 2 || v > 128) {
                        warnNeuroni.setText("\u26a0 Recomandat: 2 \u2013 128");
                    } else {
                        warnNeuroni.setText("");
                    }
                } catch (NumberFormatException ex) {
                    warnNeuroni.setText("\u26a0 Valoare numeric\u0103 (întreag\u0103) invalid\u0103");
                }
            });

            // Layout variabile cu GridPane
            javafx.scene.layout.GridPane gridVars = new javafx.scene.layout.GridPane();
            gridVars.setHgap(15);
            gridVars.setVgap(8);
            gridVars.setPadding(new Insets(10));
            gridVars.setStyle(
                    "-fx-background-color: #ffffff; -fx-background-radius: 8; -fx-border-color: #dee2e6; -fx-border-radius: 8; -fx-padding: 15;");

            Label lblTitleVars = new Label(
                    "Parametri re\u021bea (" + (folosesteLeakyReLU ? "LeakyReLU" : "Sigmoid") + "):");
            lblTitleVars.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #2c3e50;");
            gridVars.add(lblTitleVars, 0, 0, 2, 1);

            gridVars.add(new Label("Rata de \u00eenv\u0103\u021bare ini\u021bial\u0103:"), 0, 1);
            gridVars.add(txtLR, 1, 1);
            gridVars.add(warnLR, 1, 2);

            gridVars.add(new Label("Rata de iner\u021bie (Momentum):"), 0, 3);
            gridVars.add(txtMomentumField, 1, 3);
            gridVars.add(warnMomentum, 1, 4);

            gridVars.add(new Label("Num\u0103r neuroni ascun\u0219i:"), 0, 5);
            gridVars.add(txtNeuroni, 1, 5);
            gridVars.add(warnNeuroni, 1, 6);

            // Starea inițială: Implicit = read-only
            if (folosesteCustomSettings) {
                rbCustom.setSelected(true);
                txtLR.setEditable(true);
                txtMomentumField.setEditable(true);
                txtNeuroni.setEditable(true);
            } else {
                rbImplicit.setSelected(true);
                txtLR.setEditable(false);
                txtMomentumField.setEditable(false);
                txtNeuroni.setEditable(false);
            }

            // Schimbarea editabilității la comutarea radio
            toggleGroupCustom.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                boolean isCustom = (newVal == rbCustom);
                txtLR.setEditable(isCustom);
                txtMomentumField.setEditable(isCustom);
                txtNeuroni.setEditable(isCustom);
                if (!isCustom) {
                    // Resetăm la valorile implicite ale modelului selectat
                    boolean leaky = rbLeakyReLU.isSelected();
                    txtLR.setText(String.valueOf(leaky ? 0.1 : 0.4));
                    txtMomentumField.setText("0.9");
                    txtNeuroni.setText("12");
                    warnLR.setText("");
                    warnMomentum.setText("");
                    warnNeuroni.setText("");
                }
            });

            // Actualizare valori implicite când se schimbă tipul de model (din tab Tipul)
            // Funcționează întotdeauna — actualizează valorile implicit vizibile
            toggleGroupTip.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                boolean leaky = (newVal == rbLeakyReLU);
                // Actualizăm titlul secțiunii de parametri
                lblTitleVars.setText("Parametri re\u021bea (" + (leaky ? "LeakyReLU" : "Sigmoid") + "):");
                // Dacă modul Implicit este activ, resetăm valorile la cele implicite
                if (rbImplicit.isSelected()) {
                    txtLR.setText(String.valueOf(leaky ? 0.1 : 0.4));
                    txtMomentumField.setText("0.9");
                    txtNeuroni.setText("12");
                }
            });

            VBox customContent = new VBox(15, radioSection, gridVars);
            customContent.setPadding(new Insets(15));
            tabCustom.setContent(customContent);

            // =====================================================
            // TAB 3: AVANSAT
            // =====================================================
            Tab tabAvansat = new Tab("Avansat");
            Label lblInCurand = new Label("\u23f3 \u00cen cur\u00e2nd...");
            lblInCurand.setStyle("-fx-font-size: 18px; -fx-text-fill: #bdc3c7; -fx-font-weight: bold;");
            VBox avansatContent = new VBox(lblInCurand);
            avansatContent.setAlignment(Pos.CENTER);
            avansatContent.setPadding(new Insets(60));
            tabAvansat.setContent(avansatContent);

            // =====================================================
            // ASAMBLARE DIALOG
            // =====================================================
            tabPane.getTabs().addAll(tabTipul, tabCustom, tabAvansat);

            dialog.getDialogPane().setContent(tabPane);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialog.getDialogPane().setPrefWidth(500);

            dialog.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    // Salvăm selecția tipului
                    folosesteLeakyReLU = rbLeakyReLU.isSelected();

                    // Salvăm setările custom
                    folosesteCustomSettings = rbCustom.isSelected();
                    try {
                        customInitialLR = Double.parseDouble(txtLR.getText());
                    } catch (NumberFormatException ex) {
                        customInitialLR = folosesteLeakyReLU ? 0.1 : 0.4;
                    }
                    try {
                        customMomentum = Double.parseDouble(txtMomentumField.getText());
                    } catch (NumberFormatException ex) {
                        customMomentum = 0.9;
                    }
                    try {
                        customNeuroniAscunsi = Integer.parseInt(txtNeuroni.getText());
                    } catch (NumberFormatException ex) {
                        customNeuroniAscunsi = 12;
                    }

                    String tip = folosesteLeakyReLU ? "LeakyReLU Optimizat" : "Sigmoid Clasic";
                    String setari = folosesteCustomSettings
                            ? " (Custom: LR=" + customInitialLR + ", Momentum=" + customMomentum + ", Neuroni="
                                    + customNeuroniAscunsi + ")"
                            : " (Set\u0103ri implicite)";
                    new Alert(Alert.AlertType.INFORMATION,
                            "Arhitectura: " + tip + setari
                                    + "\n\nUrm\u0103toarea antrenare va folosi aceast\u0103 configura\u021bie.")
                            .show();
                }
            });
        });

        config.getChildren().addAll(new Label("Itera\u021bii:"), txtIteratii, btnStartAntrenare, btnReset,
                btnSalveazaModel,
                btnStopAntrenare, btnSetariRetea, chkWrapper);

        btnSalveazaModel.setOnAction(e -> {
            ReteaNeuronala modelToSave = (trainedModel != null) ? trainedModel : rn;

            if (modelToSave == null) {
                new Alert(Alert.AlertType.WARNING, "Nu există niciun model antrenat de salvat!").show();
                return;
            }
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Salvează Modelul Neuronal");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Model Files", "*.ser"));
            File file = fileChooser.showSaveDialog(root.getScene().getWindow());
            if (file != null) {
                try {
                    modelToSave.salveazaModel(file.getAbsolutePath());
                    new Alert(Alert.AlertType.INFORMATION, "Model salvat cu succes!").show();
                    // Adăugăm la istoric dacă nu există
                    if (!fisiereDateSelectate.contains(file)) {
                        fisiereDateSelectate.add(file);
                        tabelFisierePersistent.getItems().add(new FisierIncarcat(
                                file.getName(),
                                new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date()),
                                file));
                        actualizeazaStatusFisiere();
                        salveazaIstoric();
                    }
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, "Eroare la salvare: " + ex.getMessage()).show();
                }
            }
        });

        xAxis = new NumberAxis(); // Inițializare câmp
        xAxis.setLabel("Număr Iterație");
        xAxis.setAutoRanging(true); // Începe cu auto-range

        yAxisEroare = new NumberAxis(); // Inițializare câmp
        yAxisEroare.setLabel("Eroare (MSE)");
        yAxisEroare.setForceZeroInRange(false); // Permitem axei să facă zoom pe valorile mici (nu pornește de la 0)

        LineChart<Number, Number> chartEroare = new LineChart<>(xAxis, yAxisEroare);
        chartEroare.setTitle("📉 Evoluție Eroare (Loss) pe Iterații");
        chartEroare.setCreateSymbols(false); // Dezactivăm simbolurile pentru performanță și claritate
        chartEroare.setAnimated(false); // Dezactivăm animațiile pentru a elimina efectul de "alunecare" sau lag
        chartEroare.getData().add(serieEroare);
        serieEroare.setName("Eroare (Loss)");
        chartEroare.setPrefHeight(320); // increased height
        chartEroare.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(chartEroare, Priority.ALWAYS);

        // --- INTERACTIVITATE CHART EROARE (ZOOM & PAN) ---
        final Object[] panStateEroare = new Object[2]; // [0] = lastX, [1] = lastY

        chartEroare.setOnMousePressed(event -> {
            if (event.isSecondaryButtonDown())
                return;
            panStateEroare[0] = event.getX();
            panStateEroare[1] = event.getY();
            chartEroare.setCursor(javafx.scene.Cursor.MOVE);
        });

        chartEroare.setOnMouseReleased(event -> chartEroare.setCursor(javafx.scene.Cursor.DEFAULT));

        chartEroare.setOnMouseDragged(event -> {
            if (panStateEroare[0] == null || panStateEroare[1] == null || event.isSecondaryButtonDown())
                return;

            // 1. Panning X
            double oldX = (double) panStateEroare[0];
            double newX = event.getX();
            double deltaX = oldX - newX;
            panStateEroare[0] = newX;

            double xLower = xAxis.getLowerBound();
            double xUpper = xAxis.getUpperBound();
            double xRange = xUpper - xLower;
            double xShift = (deltaX / chartEroare.getWidth()) * xRange;

            xAxis.setAutoRanging(false);
            xAxis.setLowerBound(xLower + xShift);
            xAxis.setUpperBound(xUpper + xShift);

            // 2. Panning Y (activ doar dacă ținem CTRL apăsat sau dacă utilizatorul vrea
            // libertate totală)
            // Pentru a răspunde cererii "posibilitatea sa dau zoom si pe scara y... fara sa
            // stric graficul",
            // permitem panning pe Y implicit, deoarece e util pentru a inspecta graficul.
            double oldY = (double) panStateEroare[1];
            double newY = event.getY();
            double deltaY = newY - oldY; // Axa Y în JavaFX e inversată (0 sus), deci +delta înseamnă tragere în jos ->
                                         // vizualizare valori de sus -> scădere range
            panStateEroare[1] = newY;

            double yLower = yAxisEroare.getLowerBound();
            double yUpper = yAxisEroare.getUpperBound();
            double yRange = yUpper - yLower;
            // deltaY pozitiv înseamnă că mouse-ul coboară.
            // Dacă trag mouse-ul în jos, vreau să văd ce e mai sus. Deci scad valorile
            // axei.
            double yShift = (deltaY / chartEroare.getHeight()) * yRange;

            yAxisEroare.setAutoRanging(false);
            yAxisEroare.setLowerBound(yLower + yShift);
            yAxisEroare.setUpperBound(yUpper + yShift);
        });

        chartEroare.setOnScroll(event -> {
            event.consume();
            double zoomFactor = (event.getDeltaY() > 0) ? 0.9 : 1.1;

            if (event.isControlDown()) {
                // Zoom pe Axa Y (Eroare) - Centrat pe mouse
                yAxisEroare.setAutoRanging(false);
                double yLower = yAxisEroare.getLowerBound();
                double yUpper = yAxisEroare.getUpperBound();
                double yRange = yUpper - yLower;

                // Calculăm poziția relativă a mouse-ului pe axa Y (0 e sus, Height e jos)
                // Valoarea axei Y la poziția mouse-ului
                double mouseY = event.getY();
                double chartHeight = chartEroare.getHeight();
                // Aproximare: axa Y afișează de la Lower (jos) la Upper (sus).
                // JavaFX Y: 0 sus, H jos.
                // Raportul poziției mouse: (chartHeight - mouseY) / chartHeight (aproximativ,
                // ignorând padding)
                // Mai precis: value = min + (max - min) * ((height - y) / height)

                // Varianta simplificată pivot:
                double mousePercentY = (chartHeight - mouseY) / chartHeight;
                double pivotY = yLower + yRange * mousePercentY;

                double newLower = pivotY - (pivotY - yLower) * zoomFactor;
                double newUpper = pivotY + (yUpper - pivotY) * zoomFactor;

                // Limite de bun simț
                if (newUpper - newLower > 0.0000001) {
                    yAxisEroare.setLowerBound(newLower);
                    yAxisEroare.setUpperBound(newUpper);
                }

            } else {
                // Zoom pe Axa X (Iterații) - Centrat pe mouse
                xAxis.setAutoRanging(false);
                double xLower = xAxis.getLowerBound();
                double xUpper = xAxis.getUpperBound();
                double xRange = xUpper - xLower;

                // Poziția mouse pe X
                double mouseX = event.getX();
                double chartWidth = chartEroare.getWidth();
                double pivotX = xLower + (mouseX / chartWidth) * xRange;

                double newLower = pivotX - (pivotX - xLower) * zoomFactor;
                double newUpper = pivotX + (xUpper - pivotX) * zoomFactor;

                if (newUpper - newLower > 2) { // Minim 2 iterații vizibile
                    xAxis.setLowerBound(newLower);
                    xAxis.setUpperBound(newUpper);
                }
            }
        });

        // Wrappere pentru Overlay (Cursor interactiv)
        StackPane stackEroare = new StackPane();
        stackEroare.getChildren().add(chartEroare);
        VBox.setVgrow(stackEroare, Priority.ALWAYS);

        Label fsLblIteratiiEroare = new Label("Iterații:");
        fsLblIteratiiEroare.setTextFill(Color.WHITE);
        fsLblIteratiiEroare.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        TextField fsTxtIteratiiEroare = new TextField();
        fsTxtIteratiiEroare.setPrefWidth(80);
        fsTxtIteratiiEroare.textProperty().bindBidirectional(txtIteratii.textProperty());
        fsTxtIteratiiEroare.disableProperty().bind(txtIteratii.disableProperty());

        Button fsBtnAntreneazaEroare = new Button("🚀 Antrenează");
        fsBtnAntreneazaEroare.setStyle(
                "-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 22; -fx-background-radius: 8; -fx-font-size: 13px; -fx-cursor: hand;");
        fsBtnAntreneazaEroare.setOnAction(ev -> {
            if (btnStartAntrenare != null)
                btnStartAntrenare.fire();
        });
        fsBtnAntreneazaEroare.disableProperty().bind(btnStartAntrenare.disableProperty());

        Button fsBtnStopEroare = new Button("Stop");
        fsBtnStopEroare.setStyle(
                "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 22; -fx-background-radius: 8; -fx-font-size: 13px; -fx-cursor: hand;");
        fsBtnStopEroare.setOnAction(ev -> {
            if (btnStopAntrenare != null)
                btnStopAntrenare.fire();
        });
        fsBtnStopEroare.disableProperty().bind(btnStopAntrenare.disableProperty());

        Button fsBtnResetEroare = new Button("🔄 Reset Zoom");
        fsBtnResetEroare.setStyle(
                "-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-padding: 8 18; -fx-background-radius: 8; -fx-font-size: 13px; -fx-cursor: hand;");
        fsBtnResetEroare.setOnAction(ev -> {
            if (btnReset != null)
                btnReset.fire();
        });

        adaugaButonFullscreen(stackEroare, chartEroare, "Evoluție Eroare (Loss) pe Iterații",
                fsLblIteratiiEroare, fsTxtIteratiiEroare, fsBtnAntreneazaEroare, fsBtnStopEroare, fsBtnResetEroare);

        // --- Card Package for Error Chart ---
        VBox cardEroare = new VBox(15);
        cardEroare.setPadding(new Insets(15));
        cardEroare.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 4);");
        cardEroare.getChildren().add(stackEroare);
        VBox.setVgrow(cardEroare, Priority.ALWAYS); // Ensure card grows

        // Activăm cursorul interactiv pentru Eroare
        activeazaCursorInteractiv(chartEroare, stackEroare, xAxis, yAxisEroare,
                java.util.Collections.singletonList(serieEroare));

        // --- CHART VALIDARE ---
        xAxisValid = new NumberAxis();
        xAxisValid.setLabel("Iterație");

        yAxisValid = new NumberAxis();
        yAxisValid.setLabel("Valoare");

        LineChart<Number, Number> chartValidare = new LineChart<>(xAxisValid, yAxisValid);
        chartValidare.setTitle("🎯 Validare: Țintă (Real) vs. Previziune (AI)");
        chartValidare.setCreateSymbols(false); // Dezactivăm simbolurile
        chartValidare.setAnimated(false); // Dezactivăm animațiile
        chartValidare.getData().addAll(serieTinta, seriePreviziune);
        serieTinta.setName("Valoare Țintă (Real)");
        seriePreviziune.setName("Valoare Previzionată (AI)");
        chartValidare.setPrefHeight(320);
        chartValidare.setMaxWidth(Double.MAX_VALUE);

        // Wrappere pentru Overlay (Cursor interactiv)
        StackPane stackValidare = new StackPane();
        stackValidare.getChildren().add(chartValidare);
        VBox.setVgrow(stackValidare, Priority.ALWAYS);

        Label fsLblIteratiiValidare = new Label("Iterații:");
        fsLblIteratiiValidare.setTextFill(Color.WHITE);
        fsLblIteratiiValidare.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        TextField fsTxtIteratiiValidare = new TextField();
        fsTxtIteratiiValidare.setPrefWidth(80);
        fsTxtIteratiiValidare.textProperty().bindBidirectional(txtIteratii.textProperty());
        fsTxtIteratiiValidare.disableProperty().bind(txtIteratii.disableProperty());

        Button fsBtnAntreneazaValidare = new Button("🚀 Antrenează");
        fsBtnAntreneazaValidare.setStyle(
                "-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 22; -fx-background-radius: 8; -fx-font-size: 13px; -fx-cursor: hand;");
        fsBtnAntreneazaValidare.setOnAction(ev -> {
            if (btnStartAntrenare != null)
                btnStartAntrenare.fire();
        });
        fsBtnAntreneazaValidare.disableProperty().bind(btnStartAntrenare.disableProperty());

        Button fsBtnStopValidare = new Button("Stop");
        fsBtnStopValidare.setStyle(
                "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 22; -fx-background-radius: 8; -fx-font-size: 13px; -fx-cursor: hand;");
        fsBtnStopValidare.setOnAction(ev -> {
            if (btnStopAntrenare != null)
                btnStopAntrenare.fire();
        });
        fsBtnStopValidare.disableProperty().bind(btnStopAntrenare.disableProperty());

        Button fsBtnResetValidare = new Button("🔄 Reset Zoom");
        fsBtnResetValidare.setStyle(
                "-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-padding: 8 18; -fx-background-radius: 8; -fx-font-size: 13px; -fx-cursor: hand;");
        fsBtnResetValidare.setOnAction(ev -> {
            if (btnReset != null)
                btnReset.fire();
        });

        adaugaButonFullscreen(stackValidare, chartValidare, "Validare: Țintă (Real) vs. Previziune (AI)",
                fsLblIteratiiValidare, fsTxtIteratiiValidare, fsBtnAntreneazaValidare, fsBtnStopValidare,
                fsBtnResetValidare);

        // --- Card Package for Validation Chart ---
        VBox cardValidare = new VBox(15);
        cardValidare.setPadding(new Insets(15));
        cardValidare.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 4);");
        cardValidare.getChildren().add(stackValidare);
        VBox.setVgrow(cardValidare, Priority.ALWAYS); // Ensure card grows

        // Activăm cursorul interactiv pentru Validare (pentru ambele serii)
        activeazaCursorInteractiv(chartValidare, stackValidare, xAxisValid, yAxisValid,
                java.util.Arrays.asList(serieTinta, seriePreviziune));

        // --- INTERACTIVITATE CHART VALIDARE (ZOOM & PAN) ---
        final Object[] panStateValid = new Object[2]; // Acum reținem și X și Y

        chartValidare.setOnMousePressed(event -> {
            if (event.isSecondaryButtonDown())
                return;
            panStateValid[0] = event.getX();
            panStateValid[1] = event.getY();
            chartValidare.setCursor(javafx.scene.Cursor.MOVE);
        });

        chartValidare.setOnMouseReleased(event -> chartValidare.setCursor(javafx.scene.Cursor.DEFAULT));

        chartValidare.setOnMouseDragged(event -> {
            if (panStateValid[0] == null || panStateValid[1] == null || event.isSecondaryButtonDown())
                return;

            // Panning X
            double oldX = (double) panStateValid[0];
            double newX = event.getX();
            double deltaX = newX - oldX;
            panStateValid[0] = newX;

            double lowerX = xAxisValid.getLowerBound();
            double upperX = xAxisValid.getUpperBound();
            double rangeX = upperX - lowerX;
            double shiftX = -(deltaX / xAxisValid.getWidth()) * rangeX;

            double newLowerX = lowerX + shiftX;
            double newUpperX = upperX + shiftX;

            if (newLowerX < 0) {
                newLowerX = 0;
                newUpperX = newLowerX + rangeX;
            }

            xAxisValid.setLowerBound(newLowerX);
            xAxisValid.setUpperBound(newUpperX);

            // Panning Y
            double oldY = (double) panStateValid[1];
            double newY = event.getY();
            double deltaY = newY - oldY;
            panStateValid[1] = newY;

            double lowerY = yAxisValid.getLowerBound();
            double upperY = yAxisValid.getUpperBound();
            double rangeY = upperY - lowerY;
            double shiftY = (deltaY / chartValidare.getHeight()) * rangeY;

            yAxisValid.setAutoRanging(false);
            yAxisValid.setLowerBound(lowerY + shiftY);
            yAxisValid.setUpperBound(upperY + shiftY);
        });

        chartValidare.setOnScroll(event -> {
            event.consume();
            if (event.getDeltaY() == 0)
                return;

            double zoomFactor = (event.getDeltaY() > 0) ? 0.9 : 1.1;

            if (event.isControlDown()) {
                // Zoom pe Axa Y (Validare) - Centrat pe mouse
                yAxisValid.setAutoRanging(false);
                double yLower = yAxisValid.getLowerBound();
                double yUpper = yAxisValid.getUpperBound();
                double yRange = yUpper - yLower;

                double mouseY = event.getY();
                double chartHeight = chartValidare.getHeight();

                double mousePercentY = (chartHeight - mouseY) / chartHeight;
                double pivotY = yLower + yRange * mousePercentY;

                double newLower = pivotY - (pivotY - yLower) * zoomFactor;
                double newUpper = pivotY + (yUpper - pivotY) * zoomFactor;

                if (newUpper - newLower > 0.0000001) {
                    yAxisValid.setLowerBound(newLower);
                    yAxisValid.setUpperBound(newUpper);
                }
            } else {
                // Zoom pe Axa X (Iterații)
                xAxisValid.setAutoRanging(false);
                double lower = xAxisValid.getLowerBound();
                double upper = xAxisValid.getUpperBound();
                double range = upper - lower;

                if (range < 5 && zoomFactor < 1)
                    return;

                double mouseX = event.getX();
                double chartWidth = chartValidare.getWidth();
                double pivotX = lower + (mouseX / chartWidth) * range;

                double newLower = pivotX - (pivotX - lower) * zoomFactor;
                double newUpper = pivotX + (upper - pivotX) * zoomFactor;

                if (newLower < 0) {
                    newLower = 0;
                    newUpper = newUpper - newLower; // Păstrează range-ul calculat
                }

                xAxisValid.setLowerBound(newLower);
                xAxisValid.setUpperBound(newUpper);
            }
        });

        VBox chartsBox = new VBox(20, cardEroare, cardValidare);
        chartsBox.setFillWidth(true);
        VBox.setVgrow(chartsBox, Priority.ALWAYS);

        // --- 3. BUTON RESET (Smart Reset) ---
        btnReset.setOnAction(e -> {
            // Reset for the First Chart
            xAxis.setAutoRanging(true);

            // Smart Reset pentru axa Y: Ignorăm primele valori (spike-ul inițial)
            if (serieEroare.getData().size() > 50) {
                yAxisEroare.setAutoRanging(false);

                double minEroare = Double.MAX_VALUE;
                double maxEroare = Double.MIN_VALUE;

                int skipCount = Math.min(100, serieEroare.getData().size() / 10);

                for (int i = skipCount; i < serieEroare.getData().size(); i++) {
                    double val = serieEroare.getData().get(i).getYValue().doubleValue();
                    if (val < minEroare)
                        minEroare = val;
                    if (val > maxEroare)
                        maxEroare = val;
                }

                double margin = (maxEroare - minEroare) * 0.1;
                if (margin == 0)
                    margin = maxEroare * 0.1;

                yAxisEroare.setLowerBound(Math.max(0, minEroare - margin));
                yAxisEroare.setUpperBound(maxEroare + margin);

            } else {
                yAxisEroare.setAutoRanging(true);
            }

            // Reset for the Second Chart (Validation)
            xAxisValid.setAutoRanging(true);
            yAxisValid.setAutoRanging(true);
        });

        btnStartAntrenare.setOnAction(e -> pornesteAntrenareaDinamica(Integer.parseInt(txtIteratii.getText())));

        // Adăugăm și informațiile despre antrenament
        lblInfoAntrenament
                .setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 0;");

        root.getChildren().addAll(new Label("Monitorizare Antrenare"), config, lblInfoAntrenament, chartsBox);
        return root;
    }

    private VBox creeazaEcranGestiuneDate() {
        // 1. Verificăm dacă ecranul este deja în memorie (Cache)
        if (ecranGestiuneDateCache != null) {
            return ecranGestiuneDateCache;
        }

        VBox root = new VBox(25);
        root.setPadding(new Insets(40));

        Label title = new Label("Administrare Date Antrenare");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));

        // 2. Configurăm coloanele tabelului DOAR DACĂ nu au fost deja configurate
        if (tabelFisierePersistent.getColumns().isEmpty()) {
            TableColumn<FisierIncarcat, String> numeCol = new TableColumn<>("Nume Fișier");
            numeCol.setCellValueFactory(cellData -> cellData.getValue().numeProperty());

            TableColumn<FisierIncarcat, String> dataCol = new TableColumn<>("Data Încărcare");
            dataCol.setCellValueFactory(cellData -> cellData.getValue().dataProperty());

            TableColumn<FisierIncarcat, String> actiuniCol = new TableColumn<>("Acțiuni");
            actiuniCol.setCellFactory(col -> new TableCell<FisierIncarcat, String>() {
                private final Button btnSterge = new Button("Șterge");
                {
                    btnSterge.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 10px;");
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty)
                        setGraphic(null);
                    else {
                        btnSterge.setOnAction(e -> {
                            FisierIncarcat currFisier = getTableRow().getItem();
                            if (currFisier == null)
                                return;

                            fisiereDateSelectate.remove(currFisier.getFile());
                            tabelFisierePersistent.getItems().remove(currFisier);

                            // Check if deleted file is the loaded model
                            if (loadedModelSourceFile != null && currFisier.getFile().equals(loadedModelSourceFile)) {
                                loadedModel = null;
                                loadedModelSourceFile = null;
                                if (rn == loadedModel) {
                                    rn = null;
                                }
                                new Alert(Alert.AlertType.INFORMATION,
                                        "Modelul încărcat a fost șters din listă și dezactivat.").show();
                            }

                            actualizeazaStatusFisiere();
                            salveazaIstoric();
                            ecranAnalizaCache = null; // Invalidate cache to force refresh
                            // ecranPredictiiCache = null; // Persist calculator state
                            ecranSimulatorCache = null;
                        });
                        setGraphic(btnSterge);
                    }
                }
            });
            tabelFisierePersistent.getColumns().addAll(numeCol, dataCol, actiuniCol);
            tabelFisierePersistent.setPrefHeight(200);
        }

        // 3. Butoanele de acțiune
        Button btnIncarca = new Button("Încarcă Fișier Excel (.xlsx)");
        tutorialBtnIncarca = btnIncarca;
        btnIncarca.setStyle(
                "-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        btnIncarca.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Selectați setul de date");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
            File selectedFile = fileChooser.showOpenDialog(root.getScene().getWindow());

            if (selectedFile != null && !fisiereDateSelectate.contains(selectedFile)) {
                fisiereDateSelectate.add(selectedFile);
                tabelFisierePersistent.getItems().add(new FisierIncarcat(
                        selectedFile.getName(),
                        new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date()),
                        selectedFile));
                actualizeazaStatusFisiere();
                salveazaIstoric(); // Adaugat pentru a salva istoric la incarcare excel
                ecranAnalizaCache = null; // Invalidate cache to force refresh
                // ecranPredictiiCache = null; // Persist calculator state
                ecranSimulatorCache = null;
            }
        });

        Button btnIncarcaModel = new Button("Încarcă Model (.ser)");
        btnIncarcaModel.setStyle(
                "-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        btnIncarcaModel.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Selectați modelul neuronal");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Model Files", "*.ser"));
            File selectedFile = fileChooser.showOpenDialog(root.getScene().getWindow());

            if (selectedFile != null) {
                try {
                    rn = ReteaNeuronala.incarcaModel(selectedFile.getAbsolutePath());
                    loadedModel = rn; // Save as loaded model
                    loadedModelSourceFile = selectedFile; // Track source file

                    // If checkbox is NOT selected, this becomes the active model
                    if (chkUseTrainedModel == null || !chkUseTrainedModel.isSelected()) {
                        rn = loadedModel;
                    }
                    new Alert(Alert.AlertType.INFORMATION, "Model încărcat cu succes!").show();

                    if (!fisiereDateSelectate.contains(selectedFile)) {
                        fisiereDateSelectate.add(selectedFile);
                        tabelFisierePersistent.getItems().add(new FisierIncarcat(
                                selectedFile.getName(),
                                new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date()),
                                selectedFile));
                        actualizeazaStatusFisiere();
                        salveazaIstoric();
                    }
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, "Eroare la încărcare model: " + ex.getMessage()).show();
                }
            }
        });

        Button btnStergeMultiple = new Button("Șterge Selecția");
        btnStergeMultiple.setStyle(
                "-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        btnStergeMultiple.setOnAction(e -> {
            List<FisierIncarcat> deSters = new ArrayList<>(
                    tabelFisierePersistent.getSelectionModel().getSelectedItems());
            for (FisierIncarcat f : deSters) {
                fisiereDateSelectate.remove(f.getFile());
                tabelFisierePersistent.getItems().remove(f);
            }
            actualizeazaStatusFisiere();
            salveazaIstoric();
            ecranAnalizaCache = null; // Invalidate cache to force refresh
        });

        HBox actionButtons = new HBox(15, btnIncarca, btnIncarcaModel, btnStergeMultiple);
        VBox infoBox = new VBox(10, new Label("Cerințe fișier:"), new Label("• Format: .xlsx"), lblStatus);
        infoBox.setStyle("-fx-background-color: #ecf0f1; -fx-padding: 20; -fx-background-radius: 10;");
        // Marcam pentru dark mode — fondul gri deschis devine #1e293b
        infoBox.getProperties().put("gestiune-info-card", Boolean.TRUE);

        root.getChildren().addAll(title, actionButtons, infoBox, tabelFisierePersistent);

        // 4. Salvăm ecranul complet în Cache înainte de a-l returna
        ecranGestiuneDateCache = root;
        return root;
    }

    private void pornesteAntrenareaDinamica(int maxIteratii) {

        if (fisiereDateSelectate.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING,
                    "Vă rugăm să încărcați mai întâi un fișier Excel din tab-ul 'Gestiune Date'!");
            alert.show();
            return;
        }

        // Lock UI controls
        btnStartAntrenare.setDisable(true);
        txtIteratii.setDisable(true);
        chkUseTrainedModel.setDisable(true);
        btnStopAntrenare.setDisable(false);
        opresteAntrenarea = false;

        serieEroare.getData().clear();
        serieTinta.getData().clear();
        seriePreviziune.getData().clear();

        // Folosim primul fișier pentru antrenare
        File fisierAntrenare = getFisierDateExcel();

        // --- FIX AXA X: Setăm range-ul fix de la început ---
        // Astfel graficul se va completa de la stânga la dreapta, fără să se
        // "lungească" sau să se mute.
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(maxIteratii);
        // Resetăm și zoom-ul pe Y la auto pentru început
        yAxisEroare.setAutoRanging(true);

        // --- FIX AXA X VALIDARE ---
        // Aplicăm aceeași logică și pentru graficul de validare
        xAxisValid.setAutoRanging(false);
        xAxisValid.setLowerBound(0);
        xAxisValid.setUpperBound(maxIteratii);
        yAxisValid.setAutoRanging(true);

        new Thread(() -> {
            try {
                ManagerDate manager = new ManagerDate();
                // Folosim fișierul selectat de utilizator
                manager.incarcaDate(fisierAntrenare.getAbsolutePath());
                // Create new instance for training
                int neuroniAscunsi = folosesteCustomSettings ? customNeuroniAscunsi : 12;
                ReteaNeuronala trainingNet = new ReteaNeuronala(4, neuroniAscunsi, 1, folosesteLeakyReLU);
                // rn = trainingNet; // REMOVED: Do not overwrite active model
                trainedModel = trainingNet; // Keep reference as trained model

                // TRANSMITEM FACTORII DETECTAȚI DIN EXCEL CĂTRE REȚEA
                trainingNet.setFactoriNormalizare(manager.maxLuna, manager.maxSezon, manager.maxBuget, manager.maxPret,
                        manager.maxVanzari);

                // Explicitly update internal factor for denormalization
                trainingNet.factorVanzari = manager.maxVanzari;

                // Aplicăm momentum-ul custom (sau implicit 0.9)
                if (folosesteCustomSettings) {
                    trainingNet.setMomentum(customMomentum);
                }

                // Calculăm intervalul de actualizare pe baza iterațiilor
                int intervalActualizare = 100;
                if (maxIteratii > 10000)
                    intervalActualizare = 250;
                if (maxIteratii > 50000)
                    intervalActualizare = 500;
                if (maxIteratii > 200000)
                    intervalActualizare = 1000;

                // Wrapper pentru ultima eroare (final array pt a fi accesibil în lambda/final
                // block)
                final double[] ultimaEroareWrapper = { 0.0 };

                for (int i = 1; i <= maxIteratii && !opresteAntrenarea; i++) {
                    // Rata de Învațare Adaptivă — configurată în funcție de setări
                    double initialLR, finalLR;
                    if (folosesteCustomSettings) {
                        // Valori custom din dialogul de setări
                        initialLR = customInitialLR;
                        finalLR = customInitialLR / 10.0; // Scădere cu un ordin de mărime
                    } else if (folosesteLeakyReLU) {
                        // LeakyReLU: nu atenuează gradientul, rată mai mică
                        initialLR = 0.1;
                        finalLR = 0.01;
                    } else {
                        // Sigmoid Clasic: necesită rată agresivă pentru a compensa vanishing gradient
                        initialLR = 0.4;
                        finalLR = 0.05;
                    }
                    double currentLR = initialLR * Math.pow(finalLR / initialLR, (double) i / maxIteratii);
                    trainingNet.setRataInvatare(currentLR);

                    // Amestecăm datele la fiecare epocă pentru a îmbunătăți antrenamentul
                    manager.amestecaSetAntrenament();

                    double eroare = trainingNet.antreneazaEpoca(manager.intrari, manager.tinte);
                    ultimaEroareWrapper[0] = eroare;

                    if (i % intervalActualizare == 0 || i == 1) {
                        final int iter = i;
                        final double err = eroare;

                        // Predict a random sample for validation chart from the training set
                        int randomIndex = (int) (Math.random() * manager.intrari.length);
                        double[] inputSample = manager.intrari[randomIndex];
                        double targetSample = manager.tinte[randomIndex][0];
                        double predictedSample = trainingNet.prezice(inputSample)[0];

                        javafx.application.Platform.runLater(() -> {
                            // 1. Seria Eroare Antrenare
                            XYChart.Data<Number, Number> dataEroare = new XYChart.Data<>(iter, err);
                            serieEroare.getData().add(dataEroare);

                            // 2. Seria Tinta (Real)
                            XYChart.Data<Number, Number> dataTinta = new XYChart.Data<>(iter,
                                    targetSample * trainingNet.factorVanzari);
                            serieTinta.getData().add(dataTinta);

                            // 3. Seria Previziune (AI)
                            XYChart.Data<Number, Number> dataPrev = new XYChart.Data<>(iter,
                                    predictedSample * trainingNet.factorVanzari);
                            seriePreviziune.getData().add(dataPrev);

                            // Actualizăm informațiile despre antrenament
                            lblInfoAntrenament
                                    .setText("Iterația: " + iter + " | Eroare: " + String.format("%.5f", err));

                            // --- SMART ZOOM (AUTO-RANGE LOGIC) ---
                            // Dacă am trecut de primele 50 de iterații (unde eroarea scade drastic),
                            // ajustăm axa Y să se concentreze pe valorile curente.
                            if (iter > 50) {
                                yAxisEroare.setAutoRanging(false);
                                // Setăm limita superioară la 150% din eroarea curentă pentru a vedea variațiile
                                // dar asigurăm un minim de vizibilitate (să nu fie prea strâns).
                                double upper = Math.max(0.001, err * 2.0);
                                yAxisEroare.setUpperBound(upper);
                                // Setăm limita inferioară aproape de 0 sau 80% din eroare
                                yAxisEroare.setLowerBound(Math.max(0, err * 0.5));
                            }
                        });
                    }
                } // sfarsit bucla for (i = 1 ... maxIteratii)
                javafx.application.Platform.runLater(() -> {
                    double accuracy = (1.0 - ultimaEroareWrapper[0]) * 100;
                    if (accuracy < 0)
                        accuracy = 0; // Clamp min
                    if (opresteAntrenarea) {
                        lblInfoAntrenament.setText("Antrenament OPRIT MANUAL! Iterații parcurse: "
                                + serieEroare.getData().size() * 100 + " | Eroare: "
                                + String.format("%.5f", ultimaEroareWrapper[0]) + " | Acuratețe: "
                                + String.format("%.2f%%", accuracy));
                    } else {
                        lblInfoAntrenament
                                .setText("Antrenament complet! Iterații: " + maxIteratii + " | Eroare finală: "
                                        + String.format("%.5f", ultimaEroareWrapper[0]) + " | Acuratețe: "
                                        + String.format("%.2f%%", accuracy));
                    }
                    // La final, putem lăsa auto-ranging sau păstra ultimul zoom
                    // yAxisEroare.setAutoRanging(true); // Opțional: reset la final

                    // Enable checkbox and unlock UI
                    chkUseTrainedModel.setDisable(false);
                    btnStartAntrenare.setDisable(false);
                    txtIteratii.setDisable(false);
                    btnStopAntrenare.setDisable(true);

                    if (chkUseTrainedModel.isSelected()) {
                        rn = trainedModel;
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    lblInfoAntrenament.setText("Eroare antrenament: " + ex.getMessage());
                    btnStartAntrenare.setDisable(false);
                    txtIteratii.setDisable(false);
                    btnStopAntrenare.setDisable(true);
                });
            }
        }).start();
    }

    // --- HELPERS ---

    private void activeazaCursorInteractiv(LineChart<Number, Number> chart, StackPane container, NumberAxis xAxis,
            NumberAxis yAxis, java.util.List<XYChart.Series<Number, Number>> seriesList) {
        // Layer pentru markeri (puncte)
        Pane markerPane = new Pane();
        markerPane.setMouseTransparent(true); // Lăsăm mouse-ul să treacă la grafic pentru zoom/pan
        markerPane.setManaged(false); // CRITIC: Previne bucla infinită de layout (stretching) pe axa Y !
        markerPane.layoutXProperty().bind(container.layoutXProperty());
        markerPane.layoutYProperty().bind(container.layoutYProperty());
        markerPane.prefWidthProperty().bind(container.widthProperty());
        markerPane.prefHeightProperty().bind(container.heightProperty());

        // Layer transparent pentru a prinde evenimentele de 'hover' (fără click) - dacă
        // chart-ul consumă events
        // Dar chart-ul trebuie să consume pt zoom/pan.
        // Deci folosim setOnMouseMoved pe chart direct.

        container.getChildren().add(markerPane);

        // Creăm markere refolosibile (câte unul pentru fiecare serie)
        java.util.Map<XYChart.Series, javafx.scene.shape.Circle> markers = new java.util.HashMap<>();
        for (XYChart.Series s : seriesList) {
            javafx.scene.shape.Circle c = new javafx.scene.shape.Circle(5);
            c.setVisible(false);
            // Culoare? Luăm din stilul implicit JavaFX după ce se randează sau setăm manual
            c.setStyle("-fx-fill: white; -fx-stroke: "
                    + (s == serieEroare ? "red" : (s == serieTinta ? "blue" : "orange")) + "; -fx-stroke-width: 2px;");
            markerPane.getChildren().add(c);
            markers.put(s, c);
        }

        Label tooltipLabel = new Label();
        tooltipLabel.setStyle(
                "-fx-background-color: rgba(30, 30, 30, 0.8); -fx-text-fill: white; -fx-padding: 5px; -fx-background-radius: 5px; -fx-font-size: 12px;");
        tooltipLabel.setVisible(false);
        markerPane.getChildren().add(tooltipLabel);

        chart.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_MOVED, event -> {
            // Coordonate relative la chart
            double mouseX = event.getX();
            // double mouseY = event.getY();

            // FIX: Convertim coordonata X din Chart-local în Axis-local
            // getValueForDisplay așteaptă o coordonată relativă la Axă (nu la Chart)
            // Chart-ul poate avea padding sau axe deplasate.
            Point2D mouseInScene = chart.localToScene(mouseX, 0);
            Point2D mouseInAxis = xAxis.sceneToLocal(mouseInScene);
            double xInAxis = mouseInAxis.getX();

            // Calculăm valoarea X corespunzătoare mouse-ului
            Number xValueNum = xAxis.getValueForDisplay(xInAxis);
            if (xValueNum == null)
                return;
            double xValue = xValueNum.doubleValue();

            StringBuilder tooltipText = new StringBuilder();
            boolean foundAny = false;

            double labelX = 0;
            double labelY = 0;

            for (XYChart.Series<Number, Number> s : seriesList) {
                // Căutăm cel mai apropiat punct
                // Optimizare: binary search ar fi ideal, dar datele se schimbă.
                // Pentru UI responsiveness, un scan linear pe "coada" listei sau binary search
                // e ok.
                // Presupunem date sortate pe X.

                javafx.collections.ObservableList<XYChart.Data<Number, Number>> data = s.getData();
                if (data.isEmpty()) {
                    markers.get(s).setVisible(false);
                    continue;
                }

                // Binary Search Custom
                int low = 0;
                int high = data.size() - 1;
                XYChart.Data<Number, Number> nearest = null;

                while (low <= high) {
                    int mid = (low + high) / 2;
                    double midVal = data.get(mid).getXValue().doubleValue();

                    if (midVal < xValue) {
                        low = mid + 1;
                    } else if (midVal > xValue) {
                        high = mid - 1;
                    } else {
                        nearest = data.get(mid);
                        break;
                    }
                }

                // Dacă nu am găsit exact, verificăm vecinii (high și low)
                if (nearest == null) {
                    if (high < 0)
                        high = 0;
                    if (low >= data.size())
                        low = data.size() - 1;

                    XYChart.Data<Number, Number> d1 = data.get(high);
                    XYChart.Data<Number, Number> d2 = data.get(low);

                    if (Math.abs(d1.getXValue().doubleValue() - xValue) < Math
                            .abs(d2.getXValue().doubleValue() - xValue)) {
                        nearest = d1;
                    } else {
                        nearest = d2;
                    }
                }

                // Verificăm distanța maximă acceptată (snap distance)
                // xPixel este în coordonate Axis-local. xInAxis este tot Axis-local.
                double xPixel = xAxis.getDisplayPosition(nearest.getXValue());
                if (Math.abs(xPixel - xInAxis) > 50) { // 50 pixeli distanță maximă
                    markers.get(s).setVisible(false);
                    continue;
                }

                double yPixel = yAxis.getDisplayPosition(nearest.getYValue());

                // Convertim coordonatele din Axis-local în MarkerPane-local
                // xAxis/yAxis sunt copii ai Chart-ului. MarkerPane e copil al StackPane
                // (părinte Chart).
                Point2D posInScene = xAxis.localToScene(xPixel, 0); // X-ul e corect, Y-ul e 0 pe axa X? Nu,
                                                                    // displayPosition e relative la axă?
                // getDisplayPosition returnează offset.
                // Să folosim transformări sigure.
                // Truc: Chart Plot Area coordinates.

                // Soluție robustă:
                // xAxis.getDisplayPosition returnează coordonata X relativă la AXĂ?
                // Docs: "allowable pixel position of the value".
                // La LineChart, 0 pe X Axis corespunde cu marginea stângă a PlotContent.

                // Să încercăm o abordare simplificată cu boundsInParent.
                // StackPane centrează Chart-ul.
                // Putem întreba Axis unde este punctul în Scene.
                Point2D axisInScene = xAxis.localToScene(0, 0);
                Point2D chartInScene = chart.localToScene(0, 0);

                // Dar getDisplayPosition returnează offset. Trebuie adunat la layout-ul axei?
                // Nu, e mai complicat.
                // Metoda 'getValueForDisplay' face inversul lui 'getDisplayPosition'.

                // Cea mai sigură metodă vizuală:
                // Punctul de pe ecran (xPixel) este relativ la Axă. Axa X e jos.
                // Punctul yPixel este relativ la Axă Y. Axa Y e stânga.
                // Intersecția lor dă punctul în grafic.

                // Trebuie mapping: Node axisNode -> Scene -> MarkerPane
                Point2D pointOnXAxis = xAxis.localToScene(xPixel, 0);
                Point2D pointOnYAxis = yAxis.localToScene(0, yPixel);

                // Combinăm: X de la xAxis, Y de la yAxis.
                double finalSceneX = pointOnXAxis.getX();
                double finalSceneY = pointOnYAxis.getY();

                Point2D finalPointInMarkerPane = markerPane.sceneToLocal(finalSceneX, finalSceneY);

                javafx.scene.shape.Circle c = markers.get(s);
                c.setVisible(true);
                c.setCenterX(finalPointInMarkerPane.getX());
                c.setCenterY(finalPointInMarkerPane.getY());

                tooltipText.append(s.getName()).append(": ")
                        .append(String.format("%.4f", nearest.getYValue().doubleValue())).append("\n");
                if (!foundAny) {
                    tooltipText.insert(0, "Iterație: " + nearest.getXValue().intValue() + "\n");
                    foundAny = true;
                    labelX = finalPointInMarkerPane.getX();
                    labelY = finalPointInMarkerPane.getY();
                }
            }

            if (foundAny) {
                tooltipLabel.setVisible(true);
                tooltipLabel.setText(tooltipText.toString().trim());
                tooltipLabel.setLayoutX(labelX + 10);
                tooltipLabel.setLayoutY(labelY - 10);
                tooltipLabel.toFront();
            } else {
                tooltipLabel.setVisible(false);
            }
        });

        // Ascundem la ieșire
        chart.setOnMouseExited(e -> {
            markers.values().forEach(c -> c.setVisible(false));
            tooltipLabel.setVisible(false);
        });
    }

    private void executaSimulareWhatIf() {
        // Metodă păstrată pentru compatibilitate, dar nu este folosită în scenariul
        // actual
    }

    private void executaCalculWhatIf(
            ReteaNeuronala rn,
            TextField txtBugetActual, TextField txtPretActual,
            ComboBox<String> comboLunaActual, ComboBox<String> comboSezonActual,
            Slider sBuget, Slider sPret,
            TextField txtBugetManual, TextField txtPretManual, // Câmpuri text adiționale
            Label lblVanzariActual, Label lblVanzariScenariu, Label lblDiferenta,
            XYChart.Series<String, Number> seriesComparativ,
            Label lblRecomandareLocal,
            Label lblBugetActual, Label lblPretActual, Label lblLunaActual, Label lblSezonActual, Label lblAnActual,
            Label lblVanzariActualDate) {

        if (rn == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Vă rugăm să antrenați mai întâi rețeaua neuronală!");
            alert.show();
            return;
        }

        try {
            // Obținem valorile pentru scenariu (din câmpurile text, cu fallback la slidere)

            double bugetScenariu;
            double pretScenariu;

            try {
                bugetScenariu = Double.parseDouble(txtBugetManual.getText());
            } catch (NumberFormatException e) {
                bugetScenariu = sBuget.getValue();
            }

            try {
                pretScenariu = Double.parseDouble(txtPretManual.getText());
            } catch (NumberFormatException e) {
                pretScenariu = sPret.getValue();
            }

            // Calculăm vânzările actuale din fișierul Excel (luna precedentă) - acestea
            // sunt vânzările reale
            double vanzariActual = 0;
            if (!fisiereDateSelectate.isEmpty()) {
                File fisier = getFisierDateExcel();
                FileInputStream fis = new FileInputStream(fisier);
                Workbook workbook = new XSSFWorkbook(fis);
                Sheet sheet = workbook.getSheetAt(0);

                // Determinăm luna precedentă
                java.time.LocalDate dataCurenta = java.time.LocalDate.now();
                int lunaPrecedenta = dataCurenta.getMonthValue() - 1;
                int anPrecedent = dataCurenta.getYear();

                // Dacă suntem în ianuarie, luna precedentă este decembrie anul trecut
                if (lunaPrecedenta == 0) {
                    lunaPrecedenta = 12;
                    anPrecedent = dataCurenta.getYear() - 1;
                }

                // Căutăm vânzările reale din luna precedentă
                Row randFound = null;
                int ultimaLinie = sheet.getLastRowNum();

                // 1. Încercăm să găsim luna precedentă exactă
                for (int i = 1; i <= ultimaLinie; i++) {
                    Row row = sheet.getRow(i);
                    if (row != null) {
                        org.apache.poi.ss.usermodel.Cell cellLuna = getCellSafely(row, 1);
                        org.apache.poi.ss.usermodel.Cell cellVanzari = getCellSafely(row, 5);

                        if (cellLuna != null && cellVanzari != null) {
                            int lunaDinFisier = (int) cellLuna.getNumericCellValue();
                            int anDinFisier = getAnValue(sheet, i);

                            if (lunaDinFisier == lunaPrecedenta && anDinFisier == anPrecedent) {
                                randFound = row;
                                break;
                            }
                        }
                    }
                }

                // 2. Fallback: Dacă nu am găsit luna precedentă, folosim ultimul rând valid din
                // fișier
                if (randFound == null) {
                    for (int i = ultimaLinie; i >= 1; i--) {
                        Row row = sheet.getRow(i);
                        if (row != null) {
                            org.apache.poi.ss.usermodel.Cell cellVanzari = getCellSafely(row, 5);
                            if (cellVanzari != null) {
                                randFound = row;
                                break;
                            }
                        }
                    }
                }

                // 3. Extragem valoarea dacă am găsit un rând
                if (randFound != null) {
                    org.apache.poi.ss.usermodel.Cell cellVanzari = getCellSafely(randFound, 5);
                    if (cellVanzari != null) {
                        vanzariActual = cellVanzari.getNumericCellValue();
                    }
                }

                workbook.close();
                fis.close();
            }

            // Calculăm vânzările pentru scenariu
            // Folosim datele din cardul verde (luna precedentă) pentru luna și sezonul de
            // bază
            double lunaDeBaza = 1; // Ianuarie implicit
            double sezonDeBaza = 1; // Iarnă implicit

            // Extragem luna și sezonul din etichetele cardului verde
            String textLuna = lblLunaActual.getText();
            String textSezon = lblSezonActual.getText();

            // Parsăm luna din text (ex: "Lună: Decembrie")
            if (textLuna.contains(":")) {
                String numeLuna = textLuna.split(":")[1].trim();
                String[] luni = { "Ianuarie", "Februarie", "Martie", "Aprilie", "Mai", "Iunie",
                        "Iulie", "August", "Septembrie", "Octombrie", "Noiembrie", "Decembrie" };
                for (int i = 0; i < luni.length; i++) {
                    if (luni[i].equalsIgnoreCase(numeLuna)) {
                        lunaDeBaza = i + 1; // 1-12
                        break;
                    }
                }
            }

            // Parsăm sezonul din text (ex: "Sezon: Toamnă (4)")
            if (textSezon.contains(":")) {
                String numeSezon = textSezon.split(":")[1].trim();
                if (numeSezon.contains("(1)"))
                    sezonDeBaza = 1;
                else if (numeSezon.contains("(2)"))
                    sezonDeBaza = 2;
                else if (numeSezon.contains("(3)"))
                    sezonDeBaza = 3;
                else if (numeSezon.contains("(4)"))
                    sezonDeBaza = 4;
            }

            // Verificăm dacă rețeaua este antrenată și dacă factorii sunt setați corect
            if (rn == null || rn.factorLuna == 0 || rn.factorSezon == 0 || rn.factorBuget == 0 || rn.factorPret == 0
                    || rn.factorVanzari == 0) {
                Alert alert = new Alert(Alert.AlertType.WARNING,
                        "Rețeaua neuronală nu este antrenată corect. Vă rugăm să antrenați modelul din tab-ul 'Setări & Date'.");
                alert.show();
                return;
            }

            // Normalizăm input-urile și le limităm la intervalul [0,1]

            double lunaNorm = Math.min(Math.max(lunaDeBaza / rn.factorLuna, 0.0), 1.0);
            double sezonNorm = Math.min(Math.max(sezonDeBaza / rn.factorSezon, 0.0), 1.0);
            double bugetNorm = Math.min(Math.max(bugetScenariu / rn.factorBuget, 0.0), 1.0);
            double pretNorm = Math.min(Math.max(pretScenariu / rn.factorPret, 0.0), 1.0);
            double[] inputScenariu = { lunaNorm, sezonNorm, bugetNorm, pretNorm };

            System.out.println("Vector input normalizat (după clipping): " + java.util.Arrays.toString(inputScenariu));

            // Testăm și predicția pentru buget minim (0) pentru comparație
            double[] inputMinim = { lunaNorm, sezonNorm, 0.0, pretNorm };
            double[] predMinim = rn.prezice(inputMinim);
            System.out.println("Predicție pentru buget minim (0): " + java.util.Arrays.toString(predMinim) + " = "
                    + (predMinim[0] * rn.factorVanzari) + "€");

            double[] predScenariu = rn.prezice(inputScenariu);

            System.out.println("Predicție normalizată (buget maxim 1.0): " + java.util.Arrays.toString(predScenariu));
            System.out.println("Factor vânzări: " + rn.factorVanzari);

            double vanzariScenariu = predScenariu[0] * rn.factorVanzari;

            System.out.println("Vânzări scenariu finale: " + vanzariScenariu);
            System.out.println("Vânzări actuale (din fișier): " + vanzariActual);
            System.out.println("Diferență: " + (vanzariScenariu - vanzariActual));
            System.out.println("NOTĂ: Diferențele mici (sub 1-2%) sunt normale pentru sistemele de predicție.");
            System.out.println("Rețeaua învață din datele reale și face estimări bazate pe pattern-uri.");
            System.out.println("============================");

            // Asigurăm că vânzările scenariului nu sunt 0 sau foarte mici
            if (vanzariScenariu == 0 || Double.isNaN(vanzariScenariu) || Double.isInfinite(vanzariScenariu)) {
                vanzariScenariu = 0.01; // Valoare minimă pentru a evita 0
            }

            // Calculăm diferența
            double diferenta = vanzariScenariu - vanzariActual;
            double diferentaProcent = vanzariActual != 0 ? (diferenta / vanzariActual) * 100 : 0;

            // Actualizăm etichetele

            lblVanzariActual.setText(String.format("%.2f €", vanzariActual));
            lblVanzariScenariu.setText(String.format("%.2f €", vanzariScenariu));
            lblDiferenta.setText(String.format("%+.2f € (%+.1f%%)", diferenta, diferentaProcent));

            // Actualizăm culorile în funcție de diferență
            if (diferenta > 0) {
                lblDiferenta.setTextFill(Color.web("#10b981")); // Verde pentru creștere
            } else if (diferenta < 0) {
                lblDiferenta.setTextFill(Color.web("#ef4444")); // Roșu pentru scădere
            } else {
                lblDiferenta.setTextFill(Color.GRAY);
            }

            // Actualizăm graficul (o singură serie, index 0 = actual, index 1 = scenariu)
            // Verificăm dacă seria are date

            if (!seriesComparativ.getData().isEmpty()) {
                XYChart.Data<String, Number> dataActual = seriesComparativ.getData().get(0);
                XYChart.Data<String, Number> dataScenariu = seriesComparativ.getData().get(1);
                dataActual.setYValue(vanzariActual);
                dataScenariu.setYValue(vanzariScenariu);

                // Aplicăm culorile (pentru siguranță, le reaplicăm la modificare)
                javafx.application.Platform.runLater(() -> {
                    if (dataActual.getNode() != null)
                        dataActual.getNode().setStyle("-fx-bar-fill: #e67e22;");
                    if (dataScenariu.getNode() != null)
                        dataScenariu.getNode().setStyle("-fx-bar-fill: #f1c40f;");
                });
            }

            // Actualizăm recomandarea AI
            // Parsăm bugetul actual din etichetă pentru a avea o recomandare corectă
            double bugetActualVal = 5000; // Valoare implicită
            try {
                String txt = lblBugetActual.getText(); // "Buget Marketing: 1800€"
                if (txt.contains(":")) {
                    String val = txt.split(":")[1].trim().replace("€", "").trim();
                    bugetActualVal = Double.parseDouble(val);
                }
            } catch (Exception e) {
                // fallback
            }

            String recomandare = genereazaRecomandareAI(bugetScenariu, pretScenariu, diferenta, diferentaProcent,
                    bugetActualVal);
            lblRecomandareLocal.setText(recomandare);

        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Eroare la calcul: " + ex.getMessage());
            alert.show();
            ex.printStackTrace();
        }
    }

    private String genereazaRecomandareAI(double bugetScenariu, double pretScenariu, double diferenta,
            double diferentaProcent, double bugetActual) {
        StringBuilder recomandare = new StringBuilder();

        // Calculăm diferența de buget pentru a determina acțiunea corectă (Creștere vs
        // Reducere)
        double diferentaBuget = bugetScenariu - bugetActual;
        String actiuneBuget = (diferentaBuget >= 0) ? "Creșterea" : "Reducerea";
        double absDiferentaBuget = Math.abs(diferentaBuget);

        if (diferenta > 0) {
            // Dacă vânzările cresc
            if (diferentaProcent > 1) {
                recomandare.append(String.format(
                        "%s bugetului de marketing cu %.0f€ (de la %.0f€ la %.0f€) generează o creștere a vânzărilor de %.2f%%. ",
                        actiuneBuget, absDiferentaBuget, bugetActual, bugetScenariu, diferentaProcent));
            } else {
                recomandare.append("Modificarea parametrilor nu are un impact semnificativ asupra vânzărilor. ");
            }
        } else {
            // Dacă vânzările scad sau rămân la fel
            if (diferentaProcent < -1) {
                recomandare.append(String.format(
                        "%s bugetului de marketing cu %.0f€ (de la %.0f€ la %.0f€) poate reduce vânzările cu %.2f%%. ",
                        actiuneBuget, absDiferentaBuget, bugetActual, bugetScenariu, Math.abs(diferentaProcent)));
            } else {
                recomandare.append(String.format(
                        "%s parametrilor selectați poate duce la o ușoară scădere a vânzărilor. ",
                        actiuneBuget));
            }
        }

        return recomandare.toString().trim();
    }

    // --- 1. DASHBOARD GENERAL (Pagina Principală) ---
    private void afiseazaDashboard() {
        // Daca am cache si anul nu s-a schimbat, refolosim cache-ul (fara flash)
        if (ecranDashboardCache != null && dashboardCachedYear != null
                && dashboardCachedYear.equals(selectedDashboardYear)) {
            setContentArea(ecranDashboardCache);
            contentArea.setVvalue(0.0);
            contentArea.setHvalue(0.0);
            return;
        }

        // Daca doar s-a schimbat ANUL si structura e deja construita, actualizam inline
        // (fara rebuild)
        if (ecranDashboardCache != null && dashboardKpiRow != null && dashboardTrendSection != null) {
            actualizezaDashboardAnSelectat();
            dashboardCachedYear = selectedDashboardYear;
            setContentArea(ecranDashboardCache);
            contentArea.setVvalue(0.0);
            contentArea.setHvalue(0.0);
            return;
        }

        // Prima construire completa
        Node view = createDashboardView(true, false);
        if (view instanceof ScrollPane) {
            ecranDashboardCache = (ScrollPane) view;
        }
        dashboardCachedYear = selectedDashboardYear;
        setContentArea(view);
        contentArea.setVvalue(0.0);
        contentArea.setHvalue(0.0);
    }

    /** Invalideaza cache-ul Dashboard (la incarcare fisiere noi, logout etc.) */
    private void invalidateDashboardCache() {
        ecranDashboardCache = null;
        dashboardKpiRow = null;
        dashboardTrendSection = null;
        dashboardCachedYear = null;
    }

    /**
     * Actualizeaza doar datele KPI si Trend fara a reconstrui structura (zero
     * flash)
     */
    private void actualizezaDashboardAnSelectat() {
        if (fisiereDateSelectate.isEmpty() || dashboardKpiRow == null || dashboardTrendSection == null)
            return;

        java.util.Map<String, Object> stats = calculeazaStatisticiGenerale(selectedDashboardYear);
        double totalS = stats.containsKey("total") ? (double) stats.get("total") : 0.0;
        double avgS = stats.containsKey("medie") ? (double) stats.get("medie") : 0.0;
        double maxS = stats.containsKey("max_sales") ? (double) stats.get("max_sales") : 0.0;
        String bestM = stats.containsKey("max_sales_month") ? (String) stats.get("max_sales_month") : "-";

        dashboardKpiRow.getChildren().setAll(
                creeazaCardKPI("Vânzări Totale", String.format("%.0f €", totalS), "Total Anual", "#3b82f6"),
                creeazaCardKPI("Vânzări Medii", String.format("%.0f €", avgS), "Medie Lunară", "#10b981"),
                creeazaCardKPI("Vânzări Record", String.format("%.0f €", maxS), "Maxim Înregistrat", "#8b5cf6"),
                creeazaCardKPI("Vârf de sezon", bestM, "Lună Activă", "#f59e0b"));
        for (Node n : dashboardKpiRow.getChildren())
            HBox.setHgrow(n, Priority.ALWAYS);

        VBox newTrend = creeazaSectiuneTrend(selectedDashboardYear, false);
        newTrend.setPadding(new Insets(20, 0, 0, 0));
        dashboardTrendSection.getChildren().setAll(newTrend.getChildren());

        // Aplicam tema pe nodurile noi daca dark mode e activ
        if (isDarkMode) {
            aplicaTemaRecursiv(dashboardKpiRow, true);
            aplicaTemaRecursiv(dashboardTrendSection, true);
        }
    }

    private Node createDashboardView(boolean includeActions, boolean forExport) {
        // NU mai folosim cache pentru Dashboard pentru a reflecta date actualizate
        // mereu
        // Daca e export, returnam VBox-ul direct, fara ScrollPane, si cu layout
        // modificat

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox dashboard = new VBox(20); // Spacing redus
        dashboard.setPadding(new Insets(25)); // Padding redus
        dashboard.setAlignment(Pos.TOP_LEFT);
        dashboard.setStyle("-fx-background-color: #f4f7f6;");

        if (forExport) {
            dashboard.setPrefWidth(2480); // Match export width
            dashboard.setStyle("-fx-background-color: white;"); // White background for print
        }

        // --- CALCULE PRELIMINARE ANI ---
        java.util.Set<Integer> aniDisponibili = new java.util.TreeSet<>((a, b) -> b - a);
        if (!fisiereDateSelectate.isEmpty()) {
            aniDisponibili = extractAvailableYears(getFisierDateExcel());
        } else {
            aniDisponibili.add(java.time.Year.now().getValue());
        }

        // Inițializare an selectat dacă e null sau invalid
        if (selectedDashboardYear == null || !aniDisponibili.contains(selectedDashboardYear)) {
            if (!aniDisponibili.isEmpty()) {
                selectedDashboardYear = aniDisponibili.iterator().next(); // Primul an (cel mai recent)
            } else {
                selectedDashboardYear = java.time.Year.now().getValue();
            }
        }

        // --- HEADER & STATUS COMPACT ---
        HBox headerRow = new HBox(20);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        // Titlu
        VBox titleBox = new VBox(5);
        String welcomeMessage = "Bine ai venit";
        if (currentUser != null && currentUser.name != null && !currentUser.name.trim().isEmpty()) {
            welcomeMessage += ", " + currentUser.name.trim();
        }
        Label titlu = new Label(forExport ? "Raport Detaliat - Dashboard" : welcomeMessage);
        titlu.setFont(Font.font("Segoe UI", FontWeight.BOLD, forExport ? 64 : 28)); // Huge title for export
        titlu.setTextFill(Color.web("#2c3e50"));

        Label subTitlu = new Label("Centrul t\u0103u de comand\u0103 decizional\u0103");
        subTitlu.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        subTitlu.setTextFill(Color.GRAY);
        titleBox.getChildren().addAll(titlu, subTitlu);

        ComboBox<Integer> cmbAn = new ComboBox<>();
        tutorialComboAn = cmbAn;
        cmbAn.getItems().addAll(aniDisponibili);
        cmbAn.setValue(selectedDashboardYear);
        cmbAn.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        cmbAn.setOnAction(e -> {
            if (cmbAn.getValue() != null) {
                selectedDashboardYear = cmbAn.getValue();
                // Actualizam inline (fara rebuild complet) pentru a evita flash-ul
                actualizezaDashboardAnSelectat();
                dashboardCachedYear = selectedDashboardYear;
            }
        });

        int fisiereCount = fisiereDateSelectate.size();

        // Asamblare Header (FĂRĂ Year Selector si FARA Status Bar)
        headerRow.getChildren().addAll(titleBox, new Region());
        HBox.setHgrow(headerRow.getChildren().get(1), Priority.ALWAYS); // Spacer intre titlu si restul elementelor

        // --- SECȚIUNEA 1: PERFORMANȚĂ GENERALĂ VÂNZĂRI ---
        VBox kpiSection = new VBox(15);
        kpiSection.setAlignment(Pos.CENTER_LEFT);

        // --- SECȚIUNEA 2: PERFORMANȚA VÂNZĂRILOR LA MOMENT (Current) ---
        VBox currentPerfSection = new VBox(15);
        currentPerfSection.setAlignment(Pos.CENTER_LEFT);

        VBox trendSection = new VBox(); // Initial empty

        if (fisiereCount > 0) {
            if (selectedDashboardYear == null) {
                selectedDashboardYear = java.time.Year.now().getValue();
            }

            // Folosim ANUL SELECTAT pentru statisticile generale
            java.util.Map<String, Object> stats = calculeazaStatisticiGenerale(selectedDashboardYear);

            // 1. KPI Header cu Selector An
            HBox kpiHeaderContainer = new HBox(15);
            kpiHeaderContainer.setAlignment(Pos.CENTER_LEFT);

            Label kpiHeader = new Label("Performanță Generală Vânzări");
            kpiHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, forExport ? 64 : 18));
            kpiHeader.setTextFill(Color.web("#334155"));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            if (!forExport) {
                kpiHeaderContainer.getChildren().addAll(kpiHeader, spacer, new Label("Anul: "), cmbAn);
            } else {
                kpiHeaderContainer.getChildren().addAll(kpiHeader, spacer, new Label("Anul: " + selectedDashboardYear));
            }

            // KPI Cards Row — salvam referinta pentru update rapid fara rebuild
            dashboardKpiRow = new HBox(20);
            dashboardKpiRow.setAlignment(Pos.CENTER_LEFT);

            // Use getOrDefault to prevent NPEs if keys are missing
            double totalS = stats.containsKey("total") ? (double) stats.get("total") : 0.0;
            double avgS = stats.containsKey("medie") ? (double) stats.get("medie") : 0.0;
            double maxS = stats.containsKey("max_sales") ? (double) stats.get("max_sales") : 0.0;
            String bestMonth = stats.containsKey("max_sales_month") ? (String) stats.get("max_sales_month") : "-";

            if (forExport) {
                dashboardKpiRow.getChildren().addAll(
                        creeazaCardKPIExport("Vânzări Totale", String.format("%.0f €", totalS),
                                "Total Anual", "#3b82f6"),
                        creeazaCardKPIExport("Vânzări Medii", String.format("%.0f €", avgS),
                                "Medie Lunară", "#10b981"),
                        creeazaCardKPIExport("Vânzări Record", String.format("%.0f €", maxS),
                                "Maxim Întregistrat", "#8b5cf6"),
                        creeazaCardKPIExport("Vârf de sezon", bestMonth, "Lună Activă", "#f59e0b"));
            } else {
                dashboardKpiRow.getChildren().addAll(
                        creeazaCardKPI("Vânzări Totale", String.format("%.0f €", totalS),
                                "Total Anual", "#3b82f6"),
                        creeazaCardKPI("Vânzări Medii", String.format("%.0f €", avgS),
                                "Medie Lunară", "#10b981"),
                        creeazaCardKPI("Vânzări Record", String.format("%.0f €", maxS),
                                "Maxim Întregistrat", "#8b5cf6"),
                        creeazaCardKPI("Vârf de sezon", bestMonth, "Lună Activă", "#f59e0b"));
            }

            for (Node n : dashboardKpiRow.getChildren()) {
                HBox.setHgrow(n, Priority.ALWAYS);
            }

            kpiSection.getChildren().addAll(kpiHeaderContainer, dashboardKpiRow);

            // 2. Current Performance Section
            Label currentHeader = new Label("Performanță Vânzări (Actual)");
            currentHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, forExport ? 64 : 18));
            currentHeader.setTextFill(Color.web("#334155"));

            // Această secțiune rămâne neschimbată de selectorul de an
            currentPerfSection.getChildren().addAll(currentHeader, creeazaGraficePerformanta(forExport));

            // Trend section — salvam referinta pentru update rapid fara rebuild
            dashboardTrendSection = new VBox();
            VBox trendContent = creeazaSectiuneTrend(selectedDashboardYear, forExport);
            trendContent.setPadding(new Insets(20, 0, 0, 0));
            if (forExport) {
                trendContent.setStyle("-fx-font-size: 24px;");
                for (Node n : trendContent.getChildren()) {
                    if (n instanceof LineChart) {
                        LineChart<?, ?> lc = (LineChart<?, ?>) n;
                        lc.getXAxis().setTickLabelFont(Font.font(20));
                        lc.getYAxis().setTickLabelFont(Font.font(20));
                        lc.getXAxis().setTickLabelGap(15);
                        lc.getYAxis().setTickLabelGap(15);
                    }
                }
            }
            dashboardTrendSection.getChildren().add(trendContent);
            trendSection = dashboardTrendSection;

        } else {
            Label lblNoData = new Label("Încărcați un fișier de date pentru a vedea statisticile.");
            lblNoData.setFont(Font.font("Segoe UI", FontPosture.ITALIC, 14));
            lblNoData.setTextFill(Color.GRAY);
            kpiSection.getChildren().add(lblNoData);
        }

        dashboard.getChildren().addAll(headerRow, new Separator(), kpiSection, currentPerfSection, trendSection);

        if (includeActions && !forExport) {
            dashboard.getChildren().addAll(new Separator(), createActionsSection());
        }

        if (forExport) {
            return dashboard;
        } else {
            scrollPane.setContent(dashboard);
            return scrollPane;
        }
    }

    // Extracted method to keep code clean
    private VBox createActionsSection() {
        VBox actionsSection = new VBox(15);
        actionsSection.setAlignment(Pos.CENTER_LEFT);

        Label actionsHeader = new Label("Acțiuni Rapide");
        actionsHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        actionsHeader.setTextFill(Color.web("#475569"));

        HBox actionsContainer = new HBox(15);
        actionsContainer.setAlignment(Pos.CENTER_LEFT);

        Button btnActImport = creeazaButonActiunePro("📥 Importă", "Gestionează fișiere", "#2563eb");
        btnActImport.setOnAction(e -> {
            afiseazaEcranTehnic(0); // Tab Gestiune
        });

        Button btnActTrain = creeazaButonActiunePro("🚀 Antrenează", "Rețea Neuronală", "#db2777");
        btnActTrain.setOnAction(e -> {
            afiseazaEcranTehnic(1); // Tab Grafic/Antrenare
        });

        Button btnActSim = creeazaButonActiunePro("🧪 Simulare", "Simulator Scenarii", "#ca8a04");
        btnActSim.setOnAction(e -> afiseazaEcranSimulator());

        Button btnActExport = creeazaButonActiunePro("📤 Export", "Rapoarte PDF", "#10b981");
        btnActExport.setOnAction(e -> afiseazaExport());

        actionsContainer.getChildren().addAll(btnActImport, btnActTrain, btnActSim, btnActExport);
        for (javafx.scene.Node n : actionsContainer.getChildren()) {
            javafx.scene.layout.HBox.setHgrow(n, javafx.scene.layout.Priority.ALWAYS);
        }
        actionsSection.getChildren().addAll(actionsHeader, actionsContainer);

        return actionsSection;
    }

    // --- Helper Methods pentru Dashboard ---

    private Node creeazaGraficePerformanta() {
        return creeazaGraficePerformanta(false);
    }

    private Node creeazaGraficePerformanta(boolean forExport) {
        HBox currContainer = new HBox(15);
        currContainer.setAlignment(Pos.CENTER_LEFT);

        if (fisiereDateSelectate.isEmpty()) {
            return new Label("Nu sunt date disponibile.");
        }

        try (FileInputStream fis = new FileInputStream(getFisierDateExcel());
                Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRowIdx = sheet.getLastRowNum();

            // Găsim ultimul rând valid
            Row lastRow = null;
            Row prevRow = null;

            // Căutăm de jos în sus ultimul rând valid (cu vânzări)
            for (int i = lastRowIdx; i >= 1; i--) {
                Row r = sheet.getRow(i);
                if (r != null && getCellSafely(r, 5) != null) {
                    if (lastRow == null)
                        lastRow = r;
                    else {
                        prevRow = r;
                        break; // Am găsit și penultimul
                    }
                }
            }

            if (lastRow != null) {
                // Date curente
                double curBudg = getCellSafely(lastRow, 3).getNumericCellValue();
                double curPrice = getCellSafely(lastRow, 4).getNumericCellValue();
                double curSales = getCellSafely(lastRow, 5).getNumericCellValue();
                double curClients = (getCellSafely(lastRow, 6) != null)
                        ? getCellSafely(lastRow, 6).getNumericCellValue()
                        : 0;

                String monthName = "Ultima Lună";
                org.apache.poi.ss.usermodel.Cell cM = getCellSafely(lastRow, 1);
                if (cM != null) {
                    int m = (int) cM.getNumericCellValue();
                    if (m >= 1 && m <= 12) {
                        String[] luniN = { "", "Ian", "Feb", "Mar", "Apr", "Mai", "Iun", "Iul", "Aug", "Sep",
                                "Oct", "Noi", "Dec" };
                        monthName = luniN[m];
                    }
                }
                int yearVal = getAnValue(sheet, lastRow.getRowNum());
                monthName += " " + yearVal;

                // Date anterioare (pentru evoluție)
                double prevBudg = (prevRow != null) ? getCellSafely(prevRow, 3).getNumericCellValue() : curBudg;
                double prevPrice = (prevRow != null) ? getCellSafely(prevRow, 4).getNumericCellValue() : curPrice;
                double prevSales = (prevRow != null) ? getCellSafely(prevRow, 5).getNumericCellValue() : curSales;
                double prevClients = (prevRow != null && getCellSafely(prevRow, 6) != null)
                        ? getCellSafely(prevRow, 6).getNumericCellValue()
                        : curClients;

                // Construire carduri
                currContainer.getChildren()
                        .add(creeazaCardPerformanta("Buget Marketing", curBudg, prevBudg, "€", monthName, forExport));
                currContainer.getChildren()
                        .add(creeazaCardPerformanta("Preț Mediu", curPrice, prevPrice, "€", monthName, forExport));
                currContainer.getChildren()
                        .add(creeazaCardPerformanta("Vânzări", curSales, prevSales, "€", monthName, forExport));
                currContainer.getChildren()
                        .add(creeazaCardPerformanta("Nr. Clienți", curClients, prevClients, "", monthName, forExport));

                for (javafx.scene.Node n : currContainer.getChildren()) {
                    javafx.scene.layout.HBox.setHgrow(n, javafx.scene.layout.Priority.ALWAYS);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Label("Eroare la încărcarea datelor.");
        }
        return currContainer;
    }

    private VBox creeazaCardKPI(String titlu, String valoare, String detalii, String accentColor) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10, 15, 10, 15));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: " + accentColor
                + "; -fx-border-width: 0 0 0 4; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 3, 0, 0, 1);");
        // Marcare speciala: textul din acest card trebuie sa ramana inchis (fundalul e
        // alb fix)
        card.getProperties().put("fixed-bg-card", Boolean.TRUE);

        Label lblTitlu = new Label(titlu);
        lblTitlu.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblTitlu.setTextFill(Color.GRAY); // #808080 — va fi protejat

        Label lblVal = new Label(valoare);
        lblVal.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        lblVal.setTextFill(Color.web("#1e293b")); // Inchis — va fi protejat

        Label lblDetalii = new Label(detalii);
        lblDetalii.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        lblDetalii.setTextFill(Color.web(accentColor)); // Culoare accent — va fi protejata

        card.getChildren().addAll(lblTitlu, lblVal, lblDetalii);
        return card;
    }

    private VBox creeazaCardKPIExport(String titlu, String valoare, String detalii, String accentColor) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20, 30, 20, 30)); // Larger padding
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-border-color: " + accentColor
                + "; -fx-border-width: 0 0 0 8;"); // Thicker border

        Label lblTitlu = new Label(titlu);
        lblTitlu.setFont(Font.font("Segoe UI", FontWeight.BOLD, 64)); // Balanced (was 84)
        lblTitlu.setTextFill(Color.GRAY);

        Label lblVal = new Label(valoare);
        lblVal.setFont(Font.font("Segoe UI", FontWeight.BOLD, 120)); // Balanced (was 168)
        lblVal.setTextFill(Color.web("#1e293b"));

        Label lblDetalii = new Label(detalii);
        lblDetalii.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 48)); // Balanced (was 72)
        lblDetalii.setTextFill(Color.web(accentColor));

        card.getChildren().addAll(lblTitlu, lblVal, lblDetalii);
        card.getProperties().put("kpi_type", "KPI_EXPORT");
        card.getProperties().put("kpi_title", titlu);
        card.getProperties().put("kpi_val", valoare);
        card.getProperties().put("kpi_detalii", detalii);
        return card;
    }

    private VBox creeazaCardPerformanta(String titlu, double valoareCurenta, double valoareAnterioara, String unitate,
            String luna, boolean forExport) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);");
        // Marcare speciala: textul din acest card trebuie sa ramana inchis (fundalul e
        // alb fix)
        card.getProperties().put("fixed-bg-card", Boolean.TRUE);

        Label lblTitlu = new Label(titlu);
        lblTitlu.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        lblTitlu.setTextFill(Color.GRAY);

        Label lblVal = new Label(
                (titlu.contains("Clienți") ? (int) valoareCurenta : String.format("%.0f", valoareCurenta)) + " "
                        + unitate);
        lblVal.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        lblVal.setTextFill(Color.web("#0f172a"));

        if (forExport) {
            lblTitlu.setFont(Font.font("Segoe UI", FontWeight.BOLD, 64)); // Matched KPI (was 56)
            lblVal.setFont(Font.font("Segoe UI", FontWeight.BOLD, 120)); // Matched KPI (was 100)
        }

        // Calcul evoluție
        double diff = valoareCurenta - valoareAnterioara;
        double procent = (valoareAnterioara != 0) ? (diff / valoareAnterioara) * 100 : 0;

        HBox evolutieBox = new HBox(5);
        evolutieBox.setAlignment(Pos.CENTER_LEFT);

        String icon = (diff >= 0) ? "▲" : "▼";
        String color = (diff >= 0) ? "#16a34a" : "#dc2626"; // Green / Red
        if (diff == 0) {
            icon = "▬";
            color = "gray";
        }

        Label lblIcon = new Label(icon);
        lblIcon.setTextFill(Color.web(color));
        // Scale icon for export
        lblIcon.setFont(Font.font("Segoe UI", FontWeight.BOLD, forExport ? 48 : 14));

        Label lblProc = new Label(String.format("%+.1f%% vs luna ant.", procent));
        lblProc.setTextFill(Color.web(color));
        // Scale percentage text for export
        lblProc.setFont(Font.font("Segoe UI", FontWeight.BOLD, forExport ? 40 : 13));

        evolutieBox.getChildren().addAll(lblIcon, lblProc);

        Label lblLuna = new Label(luna);

        lblLuna.setFont(Font.font("Segoe UI", javafx.scene.text.FontPosture.ITALIC, forExport ? 32 : 10)); // Scale date
        lblLuna.setTextFill(Color.GRAY);

        card.getChildren().addAll(lblTitlu, lblVal, evolutieBox, lblLuna);
        card.getProperties().put("kpi_type", "KPI_PERF");
        card.getProperties().put("kpi_title", titlu);
        card.getProperties().put("kpi_val", lblVal.getText());
        card.getProperties().put("kpi_diff", String.format("%+.1f%%", procent));
        card.getProperties().put("kpi_luna", luna);
        return card;
    }

    private java.util.Set<Integer> extractAvailableYears(File file) {
        java.util.Set<Integer> ani = new java.util.TreeSet<>((a, b) -> b - a); // Descrescător
        try (FileInputStream fis = new FileInputStream(file);
                Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0)
                    continue;
                int y = getAnValue(sheet, row.getRowNum());
                if (y > 0)
                    ani.add(y);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Dacă nu găsim nimic, adăugăm anul curent
        if (ani.isEmpty()) {
            ani.add(java.time.Year.now().getValue());
        }
        return ani;
    }

    private Button creeazaButonActiunePro(String titlu, String descriere, String colorHex) {
        Button btn = new Button();

        HBox content = new HBox(15);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(10)); // Padding intern

        // 1. Icon Box (Left)
        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(45, 45); // Fixed square size
        iconBox.setMinSize(45, 45);
        iconBox.setMaxSize(45, 45);
        iconBox.setStyle("-fx-background-color: " + colorHex + "; -fx-background-radius: 10;");

        // Icon symbol (First letter or specific)
        // Extract emoji if present
        String iconText = titlu.substring(0, Math.min(titlu.length(), 2)).trim();
        Label lblIcon = new Label(iconText);
        lblIcon.setTextFill(Color.WHITE);
        lblIcon.setFont(Font.font("Segoe UI Emoji", FontWeight.BOLD, 20));
        iconBox.getChildren().add(lblIcon);

        // 2. Text (Center)
        VBox textContainer = new VBox(3);
        textContainer.setAlignment(Pos.CENTER_LEFT);

        String cleanTitle = titlu.replaceAll("[^a-zA-Z0-9 ăâîșțTb ]", "").trim();
        Label lblTitlu = new Label(cleanTitle.isEmpty() ? titlu : cleanTitle);
        lblTitlu.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblTitlu.setTextFill(Color.web("#334155"));

        Label lblDesc = new Label(descriere);
        lblDesc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        lblDesc.setTextFill(Color.web("#94a3b8"));

        textContainer.getChildren().addAll(lblTitlu, lblDesc);

        // 3. Arrow (Right)
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label arrow = new Label("➜"); // Simple arrow
        arrow.setTextFill(Color.web("#cbd5e1"));
        arrow.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));

        content.getChildren().addAll(iconBox, textContainer, spacer, arrow);

        btn.setGraphic(content);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(70); // Taller for card look
        btn.setStyle("-fx-background-color: white; " +
                "-fx-background-radius: 15; " + // More rounded
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 4); " +
                "-fx-cursor: hand; -fx-alignment: CENTER_LEFT;");

        // Hover animations
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: white; " +
                "-fx-background-radius: 15; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 6); " + // Deeper shadow
                "-fx-cursor: hand; -fx-alignment: CENTER_LEFT;"));

        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: white; " +
                "-fx-background-radius: 15; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 4); " +
                "-fx-cursor: hand; -fx-alignment: CENTER_LEFT;"));

        return btn;
    }

    private java.util.Map<String, Object> calculeazaStatisticiGenerale(int targetYear) {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        double total = 0;
        double maxSales = 0;
        String maxSalesMonth = "-";
        int maxSalesYear = 0;

        double maxClients = 0;
        String maxClientsMonth = "-";
        int maxClientsYear = 0;

        int count = 0;
        // int maxYearFound = 0; // Nu mai calculăm max year aici, folosim targetYear

        if (!fisiereDateSelectate.isEmpty()) {
            try (FileInputStream fis = new FileInputStream(getFisierDateExcel());
                    Workbook workbook = new XSSFWorkbook(fis)) {
                Sheet sheet = workbook.getSheetAt(0);

                String[] luni = { "", "Ianuarie", "Februarie", "Martie", "Aprilie", "Mai", "Iunie",
                        "Iulie", "August", "Septembrie", "Octombrie", "Noiembrie", "Decembrie" };

                // Pasul 2: Calculăm statistici DOAR pentru targetYear
                for (Row row : sheet) {
                    if (row.getRowNum() == 0)
                        continue;

                    int currentYear = getAnValue(sheet, row.getRowNum());
                    if (currentYear != targetYear)
                        continue; // Ignorăm anii care nu sunt selectați

                    // Sales - Col 5
                    org.apache.poi.ss.usermodel.Cell cellSales = getCellSafely(row, 5);
                    // Clients - Col 6
                    org.apache.poi.ss.usermodel.Cell cellClients = getCellSafely(row, 6);
                    // Month - Col 1
                    org.apache.poi.ss.usermodel.Cell cellMonth = getCellSafely(row, 1);

                    if (cellSales != null) {
                        try {
                            double val = cellSales.getNumericCellValue();
                            total += val;
                            if (val > maxSales) {
                                maxSales = val;
                                maxSalesYear = currentYear;
                                if (cellMonth != null) {
                                    int m = (int) cellMonth.getNumericCellValue();
                                    if (m >= 1 && m <= 12)
                                        maxSalesMonth = luni[m];
                                }
                            }
                            count++;
                        } catch (Exception e) {
                        }
                    }

                    if (cellClients != null) {
                        try {
                            double val = cellClients.getNumericCellValue();
                            if (val > maxClients) {
                                maxClients = val;
                                maxClientsYear = currentYear;
                                if (cellMonth != null) {
                                    int m = (int) cellMonth.getNumericCellValue();
                                    if (m >= 1 && m <= 12)
                                        maxClientsMonth = luni[m];
                                }
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        stats.put("total", total);
        stats.put("medie", count > 0 ? total / count : 0.0);
        stats.put("year", targetYear);

        stats.put("max_sales", maxSales);
        stats.put("max_sales_month", maxSalesMonth);
        stats.put("max_sales_year", maxSalesYear);

        stats.put("max_clients", maxClients);
        stats.put("max_clients_month", maxClientsMonth);
        stats.put("max_clients_year", maxClientsYear);

        return stats;
    }

    private VBox creeazaSectiuneTrend(int year) {
        return creeazaSectiuneTrend(year, false);
    }

    private VBox creeazaSectiuneTrend(int year, boolean forExport) {
        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setAlignment(Pos.CENTER_LEFT);
        container.setMaxWidth(Double.MAX_VALUE);
        container.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);");

        Label lbTt = new Label("    Evoluția Vânzărilor (" + year + ")");
        lbTt.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lbTt.setTextFill(Color.web("#475569"));

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Vânzări (€)");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setLegendVisible(false);
        lineChart.setAnimated(true);
        lineChart.setMaxHeight(300); // Fixed height for the chart
        lineChart.setMinHeight(300);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Vânzări");

        // Populate data
        if (!fisiereDateSelectate.isEmpty()) {
            try (FileInputStream fis = new FileInputStream(getFisierDateExcel());
                    Workbook workbook = new XSSFWorkbook(fis)) {
                Sheet sheet = workbook.getSheetAt(0);

                String[] luni = { "", "Ian", "Feb", "Mar", "Apr", "Mai", "Iun", "Iul", "Aug", "Sep", "Oct", "Noi",
                        "Dec" };

                java.util.TreeMap<Integer, Double> salesByMonth = new java.util.TreeMap<>();

                for (Row row : sheet) {
                    if (row.getRowNum() == 0)
                        continue;
                    int y = getAnValue(sheet, row.getRowNum());
                    if (y != year)
                        continue;

                    org.apache.poi.ss.usermodel.Cell cM = getCellSafely(row, 1);
                    org.apache.poi.ss.usermodel.Cell cS = getCellSafely(row, 5);

                    if (cM != null && cS != null) {
                        int m = (int) cM.getNumericCellValue();
                        double val = cS.getNumericCellValue();
                        if (m >= 1 && m <= 12) {
                            salesByMonth.put(m, val);
                        }
                    }
                }

                for (java.util.Map.Entry<Integer, Double> entry : salesByMonth.entrySet()) {
                    series.getData().add(new XYChart.Data<>(luni[entry.getKey()], entry.getValue()));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        lineChart.getData().add(series);

        // Adăugare interactivitate (tooltips) pentru graficul de vânzări
        javafx.application.Platform.runLater(() -> {
            for (XYChart.Data<String, Number> d : series.getData()) {
                javafx.scene.Node n = d.getNode();
                if (n != null) {
                    Tooltip t = new Tooltip(String.format("Luna: %s\nVânzări: %s €", d.getXValue(), d.getYValue()));
                    t.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-color: #2c3e50;");
                    Tooltip.install(n, t);
                    n.setOnMouseEntered(e -> n.setStyle(
                            "-fx-scale-x: 1.5; -fx-scale-y: 1.5; -fx-background-color: #3b82f6; -fx-cursor: hand;"));
                    n.setOnMouseExited(e -> n.setStyle(""));
                }
            }
        });

        if (forExport) {
            lbTt.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));
            lineChart.setStyle("-fx-font-size: 28px;");
            xAxis.setTickLabelFont(Font.font(28));
            yAxis.setTickLabelFont(Font.font(28));
            xAxis.setTickLabelGap(10);
            yAxis.setTickLabelGap(10);
            // Make chart properly sized for export
            lineChart.setMinHeight(900);
            lineChart.setPrefHeight(900);
            lineChart.setMaxHeight(900);
            lineChart.setMinWidth(1400);
            lineChart.setPrefWidth(1400);
            lineChart.setAnimated(false);
        }

        if (!forExport) {
            StackPane trendChartWrapper = new StackPane(lineChart);
            adaugaButonFullscreen(trendChartWrapper, lineChart, "Evoluția Vânzărilor (" + year + ")");
            container.getChildren().addAll(lbTt, trendChartWrapper);
        } else {
            container.getChildren().addAll(lbTt, lineChart);
        }

        // HGrow logic
        javafx.scene.layout.HBox.setHgrow(container, javafx.scene.layout.Priority.ALWAYS);

        return container;
    }

    private VBox creeazaHeatmapSezonalitate(java.util.Map<Integer, java.util.Map<Integer, Double>> dateVanzari,
            boolean forExport) {
        VBox container = new VBox(15);
        container.setId("heatmapBox");
        container.setPadding(new Insets(20));
        container.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);");

        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label titlu = new Label("Harta Termică a Sezonalității");
        titlu.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        if (forExport) {
            titlu.setFont(Font.font("Segoe UI", FontWeight.BOLD, 64)); // Reduced from 128 to match Chart 1
        }
        titlu.setTextFill(Color.web("#475569"));

        Button btnCustomize = new Button("🎨 Personalizare Culori");
        btnCustomize.setStyle(
                "-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-cursor: hand; -fx-border-color: #cbd5e1; -fx-border-radius: 4; -fx-background-radius: 4;");
        btnCustomize.setOnAction(e -> {
            javafx.scene.control.Dialog<Boolean> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Culori Heatmap");
            dialog.setHeaderText("Alege culorile pentru Min și Max");

            ButtonType btnTypeOk = new ButtonType("Aplică", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(btnTypeOk, ButtonType.CANCEL);

            GridPane gridC = new GridPane();
            gridC.setHgap(10);
            gridC.setVgap(10);
            gridC.setPadding(new Insets(20));

            ColorPicker cpLow = new ColorPicker(heatmapLow);
            ColorPicker cpHigh = new ColorPicker(heatmapHigh);

            gridC.add(new Label("Culoare Minim (Low):"), 0, 0);
            gridC.add(cpLow, 1, 0);
            gridC.add(new Label("Culoare Maxim (High):"), 0, 1);
            gridC.add(cpHigh, 1, 1);

            dialog.getDialogPane().setContent(gridC);

            dialog.setResultConverter(btn -> {
                if (btn == btnTypeOk) {
                    heatmapLow = cpLow.getValue();
                    heatmapHigh = cpHigh.getValue();

                    // Save Persistence
                    try {
                        java.util.prefs.Preferences prefs = java.util.prefs.Preferences
                                .userNodeForPackage(DashboardApp.class);
                        prefs.put("heatmapLow", heatmapLow.toString());
                        prefs.put("heatmapHigh", heatmapHigh.toString());
                    } catch (Exception ex) {
                        System.err.println("Failed to save heatmap preferences.");
                    }

                    return true;
                }
                return false;
            });

            dialog.showAndWait().ifPresent(res -> {
                if (res) {
                    afiseazaAnaliza();
                }
            });
        });

        headerBox.getChildren().addAll(titlu, btnCustomize);

        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);
        grid.setAlignment(Pos.CENTER);

        // Header Luni
        String[] luni = { "Ian", "Feb", "Mar", "Apr", "Mai", "Iun", "Iul", "Aug", "Sep", "Oct", "Noi", "Dec" };
        for (int i = 0; i < luni.length; i++) {
            Label lbl = new Label(luni[i]);
            lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, forExport ? 36 : 12));
            lbl.setMinWidth(forExport ? 120 : 40);
            lbl.setAlignment(Pos.CENTER);
            grid.add(lbl, i + 1, 0);
        }

        // Găsim maximul global pentru normalizare culoare
        double maxSales = 0;
        for (java.util.Map<Integer, Double> map : dateVanzari.values()) {
            for (double val : map.values()) {
                if (val > maxSales)
                    maxSales = val;
            }
        }

        int row = 1;
        // Sortăm anii descrescător
        java.util.List<Integer> ani = new ArrayList<>(dateVanzari.keySet());
        ani.sort((a, b) -> b - a);

        for (int an : ani) {
            Label lblAn = new Label(String.valueOf(an));
            lblAn.setFont(Font.font("Segoe UI", FontWeight.BOLD, forExport ? 36 : 12));
            grid.add(lblAn, 0, row);

            java.util.Map<Integer, Double> luniVanzari = dateVanzari.get(an);
            for (int m = 1; m <= 12; m++) {
                double rWidth = forExport ? 135 : 45;
                double rHeight = forExport ? 90 : 30;
                Rectangle rect = new Rectangle(rWidth, rHeight);
                rect.setArcWidth(5);
                rect.setArcHeight(5);

                if (luniVanzari.containsKey(m)) {
                    double val = luniVanzari.get(m);
                    double intensitate = (maxSales > 0) ? (val / maxSales) : 0;

                    // Interpolare liniară simplă între heatmapLow și heatmapHigh
                    double r = heatmapLow.getRed() + (heatmapHigh.getRed() - heatmapLow.getRed()) * intensitate;
                    double g = heatmapLow.getGreen() + (heatmapHigh.getGreen() - heatmapLow.getGreen()) * intensitate;
                    double b = heatmapLow.getBlue() + (heatmapHigh.getBlue() - heatmapLow.getBlue()) * intensitate;

                    // Clamp values to [0,1]
                    r = Math.max(0, Math.min(1, r));
                    g = Math.max(0, Math.min(1, g));
                    b = Math.max(0, Math.min(1, b));

                    rect.setFill(new Color(r, g, b, 1.0));

                    installTooltip(rect, String.format("Data: %s %d\nVânzări: %.0f €", luni[m - 1], an, val));

                    // Hover effect
                    rect.setOnMouseEntered(e -> rect.setStroke(Color.BLACK));
                    rect.setOnMouseExited(e -> rect.setStroke(null));
                } else {
                    rect.setFill(Color.web("#f1f5f9")); // Gri deschis pentru lipsă date
                }
                grid.add(rect, m, row);
            }
            row++;
        }

        // Legendă
        HBox legend = new HBox(10);
        legend.setAlignment(Pos.CENTER);
        legend.setPadding(new Insets(10, 0, 0, 0));

        Rectangle gradientRect = new Rectangle(forExport ? 400 : 200, 15);
        // Linear gradient from Low to High
        javafx.scene.paint.Stop[] stops = new javafx.scene.paint.Stop[] { new javafx.scene.paint.Stop(0, heatmapLow),
                new javafx.scene.paint.Stop(1, heatmapHigh) };
        javafx.scene.paint.LinearGradient lg = new javafx.scene.paint.LinearGradient(0, 0, 1, 0, true,
                javafx.scene.paint.CycleMethod.NO_CYCLE, stops);
        gradientRect.setFill(lg);
        gradientRect.setArcWidth(5);
        gradientRect.setArcHeight(5);

        legend.getChildren().addAll(new Label("Min"), gradientRect, new Label("Max"));

        container.getChildren().addAll(headerBox, grid, legend);
        return container;
    }

    private VBox creeazaScatterMarketing(List<File> fisiere, boolean forExport) {
        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);");
        Label titlu = new Label("Eficiență Marketing (Buget vs Vânzări)");
        titlu.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        if (forExport) {
            titlu.setFont(Font.font("Segoe UI", FontWeight.BOLD, 64));
        }
        titlu.setTextFill(Color.web("#475569"));

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Buget Marketing (€)");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Vânzări (€)");

        ScatterChart<Number, Number> scatter = new ScatterChart<>(xAxis, yAxis);
        scatter.setLegendVisible(false);
        scatter.setAnimated(false);

        if (!forExport) {
            scatter.setMaxHeight(300);
        }

        if (forExport) {
            scatter.setStyle("-fx-font-size: 28px;");
            xAxis.setTickLabelFont(Font.font(28));
            yAxis.setTickLabelFont(Font.font(28));
            xAxis.setLabel("Buget Marketing (\u20AC)"); // Ensure full label
            yAxis.setLabel("V\u00E2nz\u0103ri (\u20AC)");
            scatter.setMinHeight(900);
            scatter.setPrefHeight(900);
            scatter.setMaxHeight(900);
            scatter.setMinWidth(1400);
            scatter.setPrefWidth(1400);
        }

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Data Points");

        File excelFile = getFisierDateExcel();
        if (excelFile != null) {
            try (FileInputStream fis = new FileInputStream(excelFile);
                    Workbook workbook = new XSSFWorkbook(fis)) {
                Sheet sheet = workbook.getSheetAt(0);
                for (Row row : sheet) {
                    if (row.getRowNum() == 0)
                        continue;

                    org.apache.poi.ss.usermodel.Cell cBuget = getCellSafely(row, 3);
                    org.apache.poi.ss.usermodel.Cell cVanzari = getCellSafely(row, 5);

                    if (cBuget != null && cVanzari != null) {
                        try {
                            double b = cBuget.getNumericCellValue();
                            double v = cVanzari.getNumericCellValue();
                            XYChart.Data<Number, Number> point = new XYChart.Data<>(b, v);
                            series.getData().add(point);

                            int an = getAnValue(sheet, row.getRowNum());
                            org.apache.poi.ss.usermodel.Cell cM = getCellSafely(row, 1);
                            int luna = (cM != null) ? (int) cM.getNumericCellValue() : 0;

                            // Tooltip hack
                            // Nodes are null until chart is displayed.
                            // We'll use a property listener or just runLater after adding all.
                            point.nodeProperty().addListener((obs, oldNode, newNode) -> {
                                if (newNode != null) {
                                    installTooltip(newNode, String
                                            .format("Data: L%d %d\nBuget: %.0f €\nVânzări: %.0f €", luna, an, b, v));
                                    newNode.setOnMouseEntered(e -> newNode.setStyle(
                                            "-fx-scale-x: 1.5; -fx-scale-y: 1.5; -fx-background-color: #8b5cf6;"));
                                    newNode.setOnMouseExited(e -> newNode.setStyle(""));
                                }
                            });

                        } catch (Exception ex) {
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        scatter.getData().add(series);
        if (!forExport) {
            StackPane scatterWrapper = new StackPane(scatter);
            adaugaButonFullscreen(scatterWrapper, scatter, "Eficiență Marketing (Buget vs Vânzări)");
            container.getChildren().addAll(titlu, scatterWrapper);
        } else {
            container.getChildren().addAll(titlu, scatter);
        }
        return container;
    }

    // --- 2. ANALIZA VÂNZĂRILOR ---
    private void afiseazaAnaliza() {
        afiseazaAnaliza(false);
    }

    private VBox afiseazaAnaliza(boolean forExport) {
        // Re-creăm ecranul de fiecare dată pentru a reflecta datele noi
        // sau putem face un mecanism de update. Pentru simplitate și robustețe,
        // recreăm.

        VBox view = new VBox(20);
        view.setPadding(new Insets(30));
        view.setFillWidth(true); // Asigurăm că elementele ocupă toată lățimea

        // Header cu Titlu și Selectori An (YoY)
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titlu = new Label("Analiză Comparativă An-peste-An (YoY)");
        titlu.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));

        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER_LEFT);

        ComboBox<Integer> cmbAn1 = new ComboBox<>();
        cmbAn1.setStyle("-fx-font-size: 14px;");
        ComboBox<Integer> cmbAn2 = new ComboBox<>();
        cmbAn2.setStyle("-fx-font-size: 14px;");

        Button btnResetZoom = new Button("🔄 Reset Zoom");
        btnResetZoom.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-font-size: 12px;");

        controls.getChildren().addAll(new Label("An 1:"), cmbAn1, new Label("vs An 2:"), cmbAn2, btnResetZoom);
        header.getChildren().addAll(titlu, controls);

        // Grafic de vânzări

        NumberAxis xAxis = new NumberAxis(0.5, 12.5, 1);
        xAxis.setLabel("Luna");
        xAxis.setMinorTickVisible(false);
        xAxis.setForceZeroInRange(false);
        xAxis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            private final String[] luno = { "", "Ian", "Feb", "Mar", "Apr", "Mai",
                    "Iun", "Iul", "Aug", "Sep", "Oct",
                    "Noi", "Dec" };

            @Override
            public String toString(Number object) {
                int val = object.intValue();
                if (val >= 1 && val <= 12)
                    return luno[val];
                return "";
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        });

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Vânzări (€)");
        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Comparație Vânzări Lunare");
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // --- Card Package for YoY Chart ---
        VBox yoyCard = new VBox(15);
        yoyCard.setPadding(new Insets(20));
        yoyCard.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 4);");
        if (!forExport) {
            StackPane yoyChartWrapper = new StackPane(chart);
            VBox.setVgrow(yoyChartWrapper, Priority.ALWAYS);
            adaugaButonFullscreen(yoyChartWrapper, chart, "Analiză Comparativă An-peste-An (YoY)");
            yoyCard.getChildren().addAll(header, new Separator(), yoyChartWrapper);
        } else {
            VBox.setVgrow(chart, Priority.ALWAYS);
            yoyCard.getChildren().addAll(header, new Separator(), chart);
        }
        VBox.setVgrow(yoyCard, Priority.ALWAYS);

        if (forExport) {
            chart.setStyle("-fx-font-size: 64px;"); // Balanced (was 16/48)
            xAxis.setTickLabelFont(Font.font(64));
            yAxis.setTickLabelFont(Font.font(64));
            chart.setMinHeight(1200); // Fixed large height for export, but not 2000px
            chart.setMaxHeight(1200);
        }

        XYChart.Series<Number, Number> series1 = new XYChart.Series<>();
        XYChart.Series<Number, Number> series2 = new XYChart.Series<>();
        chart.getData().addAll(series1, series2);

        // Structură date
        java.util.Map<Integer, java.util.Map<Integer, Double>> dateVanzari = new java.util.HashMap<>();

        // Încărcare Date
        if (!fisiereDateSelectate.isEmpty()) {
            try (FileInputStream fis = new FileInputStream(getFisierDateExcel());
                    Workbook workbook = new XSSFWorkbook(fis)) {
                Sheet sheet = workbook.getSheetAt(0);
                for (Row row : sheet) {
                    if (row.getRowNum() == 0)
                        continue;
                    org.apache.poi.ss.usermodel.Cell cM = getCellSafely(row, 1);
                    org.apache.poi.ss.usermodel.Cell cV = getCellSafely(row, 5);
                    if (cM != null && cV != null) {
                        try {
                            int an = getAnValue(sheet, row.getRowNum());
                            int luna = (int) cM.getNumericCellValue();
                            if (luna < 1 || luna > 12)
                                continue;
                            double val = cV.getNumericCellValue();
                            dateVanzari.putIfAbsent(an, new java.util.TreeMap<>());
                            dateVanzari.get(an).put(luna, val);
                        } catch (Exception ex) {
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Logică actualizare chart
        Runnable updateChart = () -> {
            Integer y1 = cmbAn1.getValue();
            Integer y2 = cmbAn2.getValue();

            series1.getData().clear();
            series2.getData().clear();

            if (y1 != null) {
                series1.setName("Vânzări " + y1);
                if (dateVanzari.containsKey(y1)) {
                    for (var entry : dateVanzari.get(y1).entrySet()) {
                        XYChart.Data<Number, Number> d = new XYChart.Data<>(entry.getKey(), entry.getValue());
                        series1.getData().add(d);
                        // Tooltip logic needs to be attached after node creation or via a common
                        // handler
                    }
                }
            }
            if (y2 != null) {
                series2.setName("Vânzări " + y2);
                if (dateVanzari.containsKey(y2)) {
                    for (var entry : dateVanzari.get(y2).entrySet()) {
                        XYChart.Data<Number, Number> d = new XYChart.Data<>(entry.getKey(), entry.getValue());
                        series2.getData().add(d);
                    }
                }
            }

            // Re-apply tooltips (hacky via runLater)
            javafx.application.Platform.runLater(() -> {
                for (var s : chart.getData()) {
                    for (var d : s.getData()) {
                        javafx.scene.Node n = d.getNode();
                        if (n != null) {
                            Tooltip t = new Tooltip("L: " + d.getXValue() + "\nV: " + d.getYValue() + "€");
                            Tooltip.install(n, t);
                            n.setOnMouseEntered(e -> n.setStyle("-fx-scale-x: 1.5; -fx-scale-y: 1.5;"));
                            n.setOnMouseExited(e -> n.setStyle(""));
                        }
                    }
                }
            });

            // Reset Zoom
            xAxis.setAutoRanging(false);
            xAxis.setLowerBound(0.5);
            xAxis.setUpperBound(12.5);
        };

        // Populare ComboBox

        if (!dateVanzari.isEmpty()) {
            java.util.List<Integer> ani = new ArrayList<>(dateVanzari.keySet());
            ani.sort((a, b) -> b - a);
            cmbAn1.getItems().addAll(ani);
            cmbAn2.getItems().addAll(ani);

            if (ani.size() > 0)
                cmbAn1.getSelectionModel().select(0);
            if (ani.size() > 1)
                cmbAn2.getSelectionModel().select(1);
            else if (ani.size() > 0)
                cmbAn2.getSelectionModel().select(0);
        } else {
            int curr = java.time.Year.now().getValue();
            cmbAn1.getItems().add(curr);
            cmbAn2.getItems().add(curr);
            cmbAn1.getSelectionModel().selectFirst();
            cmbAn2.getSelectionModel().selectFirst();
        }

        // --- YoY Stats Card & MoM Card Container ---
        HBox statsContainer = new HBox(10);
        statsContainer.setAlignment(Pos.CENTER_LEFT);

        // Style for cards
        String cardStyle = "-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 4);";

        // 1. MoM Card
        VBox analizaMoM = new VBox(10);
        analizaMoM.setPadding(new Insets(15));
        analizaMoM.setStyle(cardStyle);
        HBox headerMoM = new HBox(5);
        headerMoM.setAlignment(Pos.CENTER_LEFT);
        Label tMoM = new Label("Evoluție Lunară (MoM)");
        tMoM.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        tMoM.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        Label helpMoM = new Label("?");
        helpMoM.setStyle(
                "-fx-background-color: #e2e8f0; -fx-text-fill: #475569; -fx-background-radius: 10; -fx-padding: 0 5 0 5; -fx-font-weight: bold; -fx-font-size: 12;");
        installTooltip(helpMoM,
                "Compară vânzările din luna selectată cu luna anterioară.\nFormula: (Vânzări Luna Curentă - Vânzări Luna Anterioară) / Vânzări Luna Anterioară * 100");
        headerMoM.getChildren().addAll(tMoM, helpMoM);

        Label lMoM = new Label("Selectați Anul 1 pentru detalii.");
        lMoM.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lMoM.setWrapText(true);
        lMoM.setWrapText(true);
        if (forExport) {
            tMoM.setFont(Font.font("Segoe UI", FontWeight.BOLD, 64)); // Increased from 32
            lMoM.setFont(Font.font("Segoe UI", FontWeight.BOLD, 48)); // Increased from 28
        }
        analizaMoM.getChildren().addAll(headerMoM, lMoM);

        // Metadata for PDF export
        analizaMoM.getProperties().put("kpi_type", "KPI_ANALYSIS");
        analizaMoM.getProperties().put("kpi_title", "Evoluție MoM");
        analizaMoM.getProperties().put("kpi_val", lMoM.getText());

        // 2. YoY Comparison Card
        VBox analizaYoY = new VBox(10);
        analizaYoY.setPadding(new Insets(15));
        analizaYoY.setStyle(cardStyle);
        HBox headerYoY = new HBox(5);
        headerYoY.setAlignment(Pos.CENTER_LEFT);
        Label tYoY = new Label("Evoluție Anuală (YoY)");
        tYoY.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        tYoY.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        Label helpYoY = new Label("?");
        helpYoY.setStyle(
                "-fx-background-color: #e2e8f0; -fx-text-fill: #475569; -fx-background-radius: 10; -fx-padding: 0 5 0 5; -fx-font-weight: bold; -fx-font-size: 12;");
        installTooltip(helpYoY,
                "Compară vânzările totale dintre cei doi ani selectați.\nFormula: (Total An 1 - Total An 2) / Total An 2 * 100");
        headerYoY.getChildren().addAll(tYoY, helpYoY);

        Label lYoY = new Label("-");
        lYoY.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        if (forExport) {
            tYoY.setFont(Font.font("Segoe UI", FontWeight.BOLD, 64)); // Increased from 32
            lYoY.setFont(Font.font("Segoe UI", FontWeight.BOLD, 48)); // Increased from 28
        }
        analizaYoY.getChildren().addAll(headerYoY, lYoY);

        // Metadata for PDF export
        analizaYoY.getProperties().put("kpi_type", "KPI_ANALYSIS");
        analizaYoY.getProperties().put("kpi_title", "Evoluție YoY");
        analizaYoY.getProperties().put("kpi_val", lYoY.getText());

        // 3. Peak Month Card
        VBox analizaPeak = new VBox(10);
        analizaPeak.setPadding(new Insets(15));
        analizaPeak.setStyle(cardStyle);
        HBox headerPeak = new HBox(5);
        headerPeak.setAlignment(Pos.CENTER_LEFT);
        Label tPeak = new Label("Vârf de Sezon");
        tPeak.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        tPeak.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        Label helpPeak = new Label("?");
        helpPeak.setStyle(
                "-fx-background-color: #e2e8f0; -fx-text-fill: #475569; -fx-background-radius: 10; -fx-padding: 0 5 0 5; -fx-font-weight: bold; -fx-font-size: 12;");
        installTooltip(helpPeak, "Luna cu cele mai mari vânzări din fiecare an selectat.");
        headerPeak.getChildren().addAll(tPeak, helpPeak);

        // Peak Month
        Label lPeak = new Label("-");
        lPeak.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        if (forExport) {
            tPeak.setFont(Font.font("Segoe UI", FontWeight.BOLD, 64)); // Balanced
            lPeak.setFont(Font.font("Segoe UI", FontWeight.BOLD, 48));
        }
        analizaPeak.getChildren().addAll(headerPeak, lPeak);

        // Metadata for PDF export
        analizaPeak.getProperties().put("kpi_type", "KPI_ANALYSIS");
        analizaPeak.getProperties().put("kpi_title", "Vârf de Sezon");
        analizaPeak.getProperties().put("kpi_val", lPeak.getText());

        // --- NEW KPI CARDS ---

        // 4. ROI Card
        VBox analizaROI = new VBox(10);
        analizaROI.setPadding(new Insets(15));
        analizaROI.setStyle(cardStyle);
        HBox headerROI = new HBox(5);
        headerROI.setAlignment(Pos.CENTER_LEFT);
        Label tROI = new Label("(ROI) Marketing");
        tROI.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        tROI.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        Label helpROI = new Label("?");
        helpROI.setStyle(
                "-fx-background-color: #e2e8f0; -fx-text-fill: #475569; -fx-background-radius: 10; -fx-padding: 0 5 0 5; -fx-font-weight: bold; -fx-font-size: 12;");
        installTooltip(helpROI,
                "Return on Investment.\nArată câți euro ai generat pentru fiecare euro investit în reclame.\nFormula: Vânzări / Buget");
        headerROI.getChildren().addAll(tROI, helpROI);
        Label lROI = new Label("-");
        lROI.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        Label lRoiLuna = new Label("");
        lRoiLuna.setFont(Font.font("Segoe UI", javafx.scene.text.FontPosture.ITALIC, 10));
        lRoiLuna.setTextFill(Color.GRAY);
        if (forExport) {
            tROI.setFont(Font.font("Segoe UI", FontWeight.BOLD, 64)); // Balanced
            lROI.setFont(Font.font("Segoe UI", FontWeight.BOLD, 64)); // Reduced from 120 to match top row
            lRoiLuna.setFont(Font.font("Segoe UI", javafx.scene.text.FontPosture.ITALIC, 40));
        }
        analizaROI.getChildren().addAll(headerROI, lROI, lRoiLuna);

        // Metadata for PDF export
        analizaROI.getProperties().put("kpi_type", "KPI_ANALYSIS");
        analizaROI.getProperties().put("kpi_title", "(ROI) Marketing");
        analizaROI.getProperties().put("kpi_val", lROI.getText());
        analizaROI.getProperties().put("kpi_detalii", lRoiLuna.getText());

        // 5. CAC Card
        VBox analizaCAC = new VBox(10);
        analizaCAC.setPadding(new Insets(15));
        analizaCAC.setStyle(cardStyle);
        HBox headerCAC = new HBox(5);
        headerCAC.setAlignment(Pos.CENTER_LEFT);
        Label tCAC = new Label("(CAC) Cost/Client");
        tCAC.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        tCAC.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        Label helpCAC = new Label("?");
        helpCAC.setStyle(
                "-fx-background-color: #e2e8f0; -fx-text-fill: #475569; -fx-background-radius: 10; -fx-padding: 0 5 0 5; -fx-font-weight: bold; -fx-font-size: 12;");
        installTooltip(helpCAC,
                "Customer Acquisition Cost.\nCostul mediu pentru a aduce un client nou.\nFormula: Buget Marketing / Număr Clienți");
        headerCAC.getChildren().addAll(tCAC, helpCAC);
        Label lCAC = new Label("-");
        lCAC.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        Label lCacLuna = new Label("");
        lCacLuna.setFont(Font.font("Segoe UI", javafx.scene.text.FontPosture.ITALIC, 10));
        lCacLuna.setTextFill(Color.GRAY);
        if (forExport) {
            tCAC.setFont(Font.font("Segoe UI", FontWeight.BOLD, 64)); // Balanced
            lCAC.setFont(Font.font("Segoe UI", FontWeight.BOLD, 64)); // Reduced from 120 to match top row
            lCacLuna.setFont(Font.font("Segoe UI", javafx.scene.text.FontPosture.ITALIC, 40));
        }
        analizaCAC.getChildren().addAll(headerCAC, lCAC, lCacLuna);

        // Metadata for PDF export
        analizaCAC.getProperties().put("kpi_type", "KPI_ANALYSIS");
        analizaCAC.getProperties().put("kpi_title", "(CAC) Cost/Client");
        analizaCAC.getProperties().put("kpi_val", lCAC.getText());
        analizaCAC.getProperties().put("kpi_detalii", lCacLuna.getText());

        // 6. AOV Card
        VBox analizaAOV = new VBox(10);
        analizaAOV.setPadding(new Insets(15));
        analizaAOV.setStyle(cardStyle);
        HBox headerAOV = new HBox(5);
        headerAOV.setAlignment(Pos.CENTER_LEFT);
        Label tAOV = new Label("(AOV) Bon Mediu");
        tAOV.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        tAOV.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        Label helpAOV = new Label("?");
        helpAOV.setStyle(
                "-fx-background-color: #e2e8f0; -fx-text-fill: #475569; -fx-background-radius: 10; -fx-padding: 0 5 0 5; -fx-font-weight: bold; -fx-font-size: 12;");
        installTooltip(helpAOV,
                "Average Order Value.\nValoarea medie a unei tranzacții.\nFormula: Vânzări / Număr Clienți");
        headerAOV.getChildren().addAll(tAOV, helpAOV);
        Label lAOV = new Label("-");
        lAOV.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        Label lAovLuna = new Label("");
        lAovLuna.setFont(Font.font("Segoe UI", javafx.scene.text.FontPosture.ITALIC, 10));
        lAovLuna.setTextFill(Color.GRAY);
        if (forExport) {
            tAOV.setFont(Font.font("Segoe UI", FontWeight.BOLD, 64)); // Balanced
            lAOV.setFont(Font.font("Segoe UI", FontWeight.BOLD, 64)); // Reduced from 120 to match top row
            lAovLuna.setFont(Font.font("Segoe UI", javafx.scene.text.FontPosture.ITALIC, 40));
        }
        analizaAOV.getChildren().addAll(headerAOV, lAOV, lAovLuna);

        // Metadata for PDF export
        analizaAOV.getProperties().put("kpi_type", "KPI_ANALYSIS");
        analizaAOV.getProperties().put("kpi_title", "(AOV) Bon Mediu");
        analizaAOV.getProperties().put("kpi_val", lAOV.getText());
        analizaAOV.getProperties().put("kpi_detalii", lAovLuna.getText());

        // Configurare latime carduri pentru a incapea 6 pe rand sau grid
        VBox[] allCards = { analizaMoM, analizaYoY, analizaPeak, analizaROI, analizaCAC, analizaAOV };
        for (VBox card : allCards) {
            if (!forExport) {
                HBox.setHgrow(card, Priority.ALWAYS);
            }
            card.setMaxWidth(Double.MAX_VALUE);
        }

        statsContainer.getChildren().clear();
        if (forExport) {
            // Use GridPane for Export (3 columns x 2 rows)
            GridPane gridStats = new GridPane();
            gridStats.setHgap(40); // Large gap for export
            gridStats.setVgap(40);
            gridStats.setAlignment(Pos.CENTER);

            // Row 1
            gridStats.add(analizaMoM, 0, 0);
            gridStats.add(analizaYoY, 1, 0);
            gridStats.add(analizaPeak, 2, 0);

            // Row 2
            gridStats.add(analizaROI, 0, 1);
            gridStats.add(analizaCAC, 1, 1);
            gridStats.add(analizaAOV, 2, 1);

            // Column Constraints to ensure equal width
            ColumnConstraints col1 = new ColumnConstraints();
            col1.setPercentWidth(33.3);
            ColumnConstraints col2 = new ColumnConstraints();
            col2.setPercentWidth(33.3);
            ColumnConstraints col3 = new ColumnConstraints();
            col3.setPercentWidth(33.3);
            gridStats.getColumnConstraints().addAll(col1, col2, col3);

            // Row Constraints - LEAVE EMPTY/DEFAULT to let them size to the children's
            // fixed height
            // RowConstraints row1 = new RowConstraints(); row1.setPercentHeight(50);
            // RowConstraints row2 = new RowConstraints(); row2.setPercentHeight(50);
            // gridStats.getRowConstraints().addAll(row1, row2);

            // Force cards to fill their grid cells entirely AND have fixed height
            for (javafx.scene.Node node : gridStats.getChildren()) {
                if (node instanceof VBox) {
                    VBox card = (VBox) node;

                    // FIXED HEIGHT STRATEGY
                    double fixedHeight = 450.0;
                    card.setMinHeight(fixedHeight);
                    card.setPrefHeight(fixedHeight);
                    card.setMaxHeight(fixedHeight);

                    card.setMaxWidth(Double.MAX_VALUE);

                    GridPane.setHgrow(card, Priority.ALWAYS);
                    GridPane.setVgrow(card, Priority.ALWAYS);
                    GridPane.setFillWidth(card, true);
                    GridPane.setFillHeight(card, true);
                }
            }

            statsContainer.getChildren().add(gridStats);
        } else {
            // Standard HBox for UI
            for (VBox card : allCards) {
                HBox.setHgrow(card, Priority.ALWAYS);
            }
            statsContainer.getChildren().addAll(analizaMoM, analizaYoY, analizaPeak, analizaROI, analizaCAC,
                    analizaAOV);
        }
        Runnable updateStats = () -> {
            Integer y1 = cmbAn1.getValue();
            Integer y2 = cmbAn2.getValue();

            // Save state
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(DashboardApp.class);
            if (y1 != null) {
                prefs.putInt("selectedYear1", y1);
                selectedAnalysisYear1 = y1;
            }
            if (y2 != null) {
                prefs.putInt("selectedYear2", y2);
                selectedAnalysisYear2 = y2;
            }

            // MoM Logic (Year 1)
            if (y1 != null && dateVanzari.containsKey(y1)) {
                var map = dateVanzari.get(y1);
                int maxL = 0;
                for (int k : map.keySet())
                    if (k > maxL)
                        maxL = k;

                if (maxL > 1 && map.containsKey(maxL) && map.containsKey(maxL - 1)) {
                    double c = map.get(maxL);
                    double p = map.get(maxL - 1);
                    double d = c - p;
                    double pr = (p != 0) ? (d / p) * 100 : 0;
                    lMoM.setText(String.format("Luna %d vs %d (%d):\n%+.0f € (%+.1f%%)", maxL, maxL - 1, y1, d, pr));
                    lMoM.setTextFill(d >= 0 ? Color.web("#10b981") : Color.web("#ef4444"));
                } else {
                    lMoM.setText("Date insuficiente pentru MoM (" + y1 + ").");
                    lMoM.setTextFill(Color.GRAY);
                }

                String[] luniNume = { "", "Ian", "Feb", "Mar", "Apr", "Mai", "Iun", "Iul", "Aug", "Sep", "Oct", "Noi",
                        "Dec" };
                String lunaCurentaText = (maxL >= 1 && maxL <= 12) ? luniNume[maxL] : "";

                // --- KPI: ROI, CAC, AOV ---
                // Necesita recitire date pt Buget si Clienti pt luna curenta (maxL) din anul y1
                // Optimizare: Putem citi o singura data fisierul la incarcare, dar aici citim
                // on-demand simulat.
                // Pentru simplitate, citim din nou row-ul corespunzator din fisier
                try (FileInputStream fis = new FileInputStream(getFisierDateExcel());
                        Workbook workbook = new XSSFWorkbook(fis)) {
                    Sheet sheet = workbook.getSheetAt(0);
                    // Cautam randul cu An=y1 si Luna=maxL
                    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                        Row r = sheet.getRow(i);
                        if (r != null) {
                            int rAn = getAnValue(sheet, i);
                            org.apache.poi.ss.usermodel.Cell cLuna = getCellSafely(r, 1);
                            if (cLuna != null && (int) cLuna.getNumericCellValue() == maxL && rAn == y1) {
                                double rVanzari = dateVanzari.get(y1).get(maxL);
                                double rBuget = getCellSafely(r, 3) != null ? getCellSafely(r, 3).getNumericCellValue()
                                        : 0;
                                double rClienti = getCellSafely(r, 6) != null
                                        ? getCellSafely(r, 6).getNumericCellValue()
                                        : 0;

                                // ROI
                                if (rBuget > 0) {
                                    double roi = (rVanzari / rBuget) * 100; // ca procent 520%
                                    lROI.setText(String.format("%.0f%% (x%.1f)", roi, rVanzari / rBuget));
                                    lROI.setTextFill(roi >= 300 ? Color.web("#10b981") : Color.web("#f59e0b")); // Green
                                    lRoiLuna.setText("Luna: " + lunaCurentaText);
                                } else {
                                    lROI.setText("N/A");
                                    lRoiLuna.setText("");
                                }

                                // CAC
                                if (rClienti > 0) {
                                    double cac = rBuget / rClienti;
                                    lCAC.setText(String.format("%.2f € / client", cac));
                                    lCacLuna.setText("Luna: " + lunaCurentaText);
                                } else {
                                    lCAC.setText("N/A");
                                    lCacLuna.setText("");
                                }

                                // AOV
                                if (rClienti > 0) {
                                    double aov = rVanzari / rClienti;
                                    lAOV.setText(String.format("%.2f €", aov));
                                    lAovLuna.setText("Luna: " + lunaCurentaText);
                                } else {
                                    lAOV.setText("N/A");
                                    lAovLuna.setText("");
                                }

                                break;
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            } else {
                lMoM.setText("Selectați Anul 1.");
                lROI.setText("-");
                lRoiLuna.setText("");
                lCAC.setText("-");
                lCacLuna.setText("");
                lAOV.setText("-");
                lAovLuna.setText("");
            }

            // YoY Logic
            if (y1 != null && y2 != null && dateVanzari.containsKey(y1) && dateVanzari.containsKey(y2)) {
                // Total Sales
                double t1 = dateVanzari.get(y1).values().stream().mapToDouble(Double::doubleValue).sum();
                double t2 = dateVanzari.get(y2).values().stream().mapToDouble(Double::doubleValue).sum();
                double diff = t1 - t2;
                double perc = (t2 != 0) ? (diff / t2) * 100 : 0;
                lYoY.setText(String.format("%d vs %d:\n%+.0f € (%+.1f%%)", y1, y2, diff, perc));
                lYoY.setTextFill(diff >= 0 ? Color.web("#10b981") : Color.web("#ef4444"));

                // Peak Month Logic
                double max1 = 0;
                String m1 = "-";
                double max2 = 0;
                String m2 = "-";
                String[] luno = { "", "Ian", "Feb", "Mar", "Apr", "Mai", "Iun", "Iul", "Aug", "Sep", "Oct", "Noi",
                        "Dec" };

                for (var e : dateVanzari.get(y1).entrySet()) {
                    if (e.getValue() > max1) {
                        max1 = e.getValue();
                        if (e.getKey() <= 12)
                            m1 = luno[e.getKey()];
                    }
                }
                for (var e : dateVanzari.get(y2).entrySet()) {
                    if (e.getValue() > max2) {
                        max2 = e.getValue();
                        if (e.getKey() <= 12)
                            m2 = luno[e.getKey()];
                    }
                }

                lPeak.setText(String.format("%d: %s (%.0f €)\n%d: %s (%.0f €)", y1, m1, max1, y2, m2, max2));
                lPeak.setTextFill(Color.web("#475569"));
            } else {
                lYoY.setText("Selectați ambii ani.");
                lPeak.setText("-");
            }

            // Sync values to properties for PDF extraction
            analizaMoM.getProperties().put("kpi_val", lMoM.getText());
            analizaYoY.getProperties().put("kpi_val", lYoY.getText());
            analizaPeak.getProperties().put("kpi_val", lPeak.getText());
            analizaROI.getProperties().put("kpi_val", lROI.getText());
            analizaROI.getProperties().put("kpi_detalii", lRoiLuna.getText());
            analizaCAC.getProperties().put("kpi_val", lCAC.getText());
            analizaCAC.getProperties().put("kpi_detalii", lCacLuna.getText());
            analizaAOV.getProperties().put("kpi_val", lAOV.getText());
            analizaAOV.getProperties().put("kpi_detalii", lAovLuna.getText());
        };

        cmbAn1.setOnAction(e -> {
            updateChart.run();
            updateStats.run();
        });
        cmbAn2.setOnAction(e -> {
            updateChart.run();
            updateStats.run();
        });

        // Restore saved year selection
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(DashboardApp.class);
        int savedY1 = prefs.getInt("selectedYear1", -1);
        int savedY2 = prefs.getInt("selectedYear2", -1);
        if (savedY1 != -1 && cmbAn1.getItems().contains(savedY1))
            cmbAn1.setValue(savedY1);
        if (savedY2 != -1 && cmbAn2.getItems().contains(savedY2))
            cmbAn2.setValue(savedY2);

        // FIX: Force update chart with the restored values
        updateChart.run();
        updateStats.run(); // Initial

        // Normal HBox
        // Update visual container based on mode
        statsContainer.getChildren().clear();
        if (forExport) {
            // Re-build grid if needed, but easier to just clear and re-add logic if we
            // wanted dynamic updates
            // actually the structure is static, only content updates.
            // We just need to ensure the grid is there.
            // The previous logic constructed it.
            GridPane gridStats = new GridPane();
            gridStats.setHgap(40);
            gridStats.setVgap(40);
            gridStats.setAlignment(Pos.CENTER);
            gridStats.add(analizaMoM, 0, 0);
            gridStats.add(analizaYoY, 1, 0);
            gridStats.add(analizaPeak, 2, 0);
            gridStats.add(analizaROI, 0, 1);
            gridStats.add(analizaCAC, 1, 1);
            gridStats.add(analizaAOV, 2, 1);

            ColumnConstraints col1 = new ColumnConstraints();
            col1.setPercentWidth(33.3);
            ColumnConstraints col2 = new ColumnConstraints();
            col2.setPercentWidth(33.3);
            ColumnConstraints col3 = new ColumnConstraints();
            col3.setPercentWidth(33.3);
            gridStats.getColumnConstraints().addAll(col1, col2, col3);

            statsContainer.getChildren().add(gridStats);
        } else {
            statsContainer.getChildren().addAll(analizaMoM, analizaYoY, analizaPeak, analizaROI, analizaCAC,
                    analizaAOV);
        }
        // Removed view.getChildren().addAll(header, new Separator(), statsContainer);
        // as we add everything at the end now

        // --- LOGICA ZOOM Interactivă & PAN ---
        // (Copy-paste logic from before or verify if it needs re-attaching to chart)
        final Object[] panState = new Object[1];
        chart.setOnMousePressed(event -> {
            if (event.isSecondaryButtonDown())
                return;
            panState[0] = event.getX();
            chart.setCursor(javafx.scene.Cursor.MOVE);
        });
        chart.setOnMouseReleased(event -> chart.setCursor(javafx.scene.Cursor.DEFAULT));
        chart.setOnMouseDragged(event -> {
            if (panState[0] == null || event.isSecondaryButtonDown())
                return;
            double oldX = (double) panState[0];
            double newX = event.getX();
            double deltaPixels = newX - oldX;
            panState[0] = newX;
            double lower = xAxis.getLowerBound();
            double upper = xAxis.getUpperBound();
            double range = upper - lower;
            double axisWidth = xAxis.getWidth();
            double shift = -(deltaPixels / axisWidth) * range;
            double newLower = lower + shift;
            double newUpper = upper + shift;
            if (newLower < 0.5) {
                newLower = 0.5;
                newUpper = newLower + range;
            }
            if (newUpper > 12.5) {
                newUpper = 12.5;
                newLower = newUpper - range;
                if (newLower < 0.5)
                    newLower = 0.5;
            }
            xAxis.setLowerBound(newLower);
            xAxis.setUpperBound(newUpper);
        });
        chart.setOnScroll(event -> {
            event.consume();
            if (event.getDeltaY() == 0)
                return;
            double zoomFactor = (event.getDeltaY() > 0) ? 0.9 : 1.1;
            xAxis.setAutoRanging(false);
            double lower = xAxis.getLowerBound();
            double upper = xAxis.getUpperBound();
            double range = upper - lower;
            if (range < 2 && zoomFactor < 1)
                return;
            if (range > 15 && zoomFactor > 1)
                return;
            double center = (upper + lower) / 2;
            double newRange = range * zoomFactor;
            double newLower = center - newRange / 2;
            double newUpper = center + newRange / 2;
            if (newLower < 0.5) {
                newLower = 0.5;
                newUpper = newRange + 0.5;
                if (newUpper > 12.5)
                    newUpper = 12.5;
            }
            if (newUpper > 12.5) {
                newUpper = 12.5;
                newLower = 12.5 - newRange;
                if (newLower < 0.5)
                    newLower = 0.5;
            }
            xAxis.setLowerBound(newLower);
            xAxis.setUpperBound(newUpper);
        });
        btnResetZoom.setOnAction(e -> {
            javafx.application.Platform.runLater(() -> {
                xAxis.setAutoRanging(false);
                xAxis.setLowerBound(0.5);
                xAxis.setUpperBound(12.5);
            });
        });

        view.getChildren().addAll(yoyCard, statsContainer,
                creeazaHeatmapSezonalitate(dateVanzari, forExport),
                creeazaScatterMarketing(fisiereDateSelectate, forExport));

        ecranAnalizaCache = view;

        // Afișăm conținutul din cache DOAR dacă nu exportăm
        if (!forExport) {
            setContentArea(ecranAnalizaCache);
            contentArea.setVvalue(0.0);
            contentArea.setHvalue(0.0);
        }
        return view;

        // Daca s-a schimbat ceva (fisiere noi), forțăm recrearea la următorul apel
        // setând cache null
        // Deocamdată lăsăm așa, dar ideal ar fi un observer pe 'fisiereDateSelectate'.
    }

    // --- 3. PREDICȚII AI ---
    private void afiseazaPredictii() {
        // 1. Create View if not in cache (Lazy Initialization)
        if (ecranPredictiiCache == null) {
            VBox view = new VBox(20);
            view.setPadding(new Insets(30));

            Label titlu = new Label("Predicții Inteligente cu Rețea Neuronală");
            titlu.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));

            // --- LAYOUT PRINCIPAL: STÂNGA (Calculator) | DREAPTA (Optimizer) ---
            HBox mainLayout = new HBox(30);
            mainLayout.setAlignment(Pos.TOP_LEFT);

            // --- PARTEA STÂNGĂ: CALCULATOR PREDICȚII (EXISTENT) ---
            VBox leftPanel = new VBox(15);
            tutorialCalcPredictie = leftPanel;
            leftPanel.setPadding(new Insets(20));
            // Stil RED (Roșu) pentru a se potrivi cu cerința
            leftPanel.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);");

            HBox.setHgrow(leftPanel, Priority.ALWAYS); // Permite expandarea

            Label lblCalcTitle = new Label("1. Calculator Predicții (Standard)");
            lblCalcTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
            lblCalcTitle.setTextFill(Color.web("#064e3b"));

            Label lblCalcDesc = new Label("Estimează vânzările pe baza scenariului ales.");
            lblCalcDesc.setTextFill(Color.web("#047857"));
            lblCalcDesc.setWrapText(true);

            GridPane configPane = new GridPane();
            configPane.setHgap(15);
            configPane.setVgap(10);
            // configPane.setPadding(new Insets(20)); // Removed inner padding
            // configPane.setStyle(...); // Removed inner card style

            ComboBox<String> comboLunaPred = new ComboBox<>();
            predLunaCombo = comboLunaPred;
            comboLunaPred.getItems().setAll("Ianuarie", "Februarie", "Martie", "Aprilie", "Mai", "Iunie", "Iulie",
                    "August", "Septembrie", "Octombrie", "Noiembrie", "Decembrie");
            comboLunaPred.getSelectionModel().select(0);
            comboLunaPred.setMaxWidth(Double.MAX_VALUE);

            ComboBox<String> comboSezonPred = new ComboBox<>();
            predSezonCombo = comboSezonPred;
            comboSezonPred.getItems().setAll("Iarnă (1)", "Primăvară (2)", "Vară (3)", "Toamnă (4)");
            comboSezonPred.getSelectionModel().select(0);
            comboSezonPred.setMaxWidth(Double.MAX_VALUE);

            TextField txtBugetPred = new TextField("5000");
            predBugetField = txtBugetPred;
            TextField txtPretPred = new TextField("50");
            predPretField = txtPretPred;

            configPane.add(new Label("Lună:"), 0, 0);
            configPane.add(comboLunaPred, 1, 0);
            configPane.add(new Label("Sezon:"), 2, 0);
            configPane.add(comboSezonPred, 3, 0);

            configPane.add(new Separator(), 0, 1, 4, 1);

            configPane.add(new Label("Buget Marketing (€):"), 0, 2);
            configPane.add(txtBugetPred, 1, 2);
            configPane.add(new Label("Preț Mediu (€):"), 2, 2);
            configPane.add(txtPretPred, 3, 2);

            Button btnPredic = new Button("Execută Predicția");
            btnPredic.setStyle(
                    "-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 25;");

            // Wrap button for alignment
            // HBox btnBox = new HBox(btnPredic); // Not needed in VBox flow
            // btnBox.setAlignment(Pos.CENTER_LEFT);

            Label lblRezultatPred = new Label("0.00 €");
            predRezultatLabel = lblRezultatPred;
            lblRezultatPred.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
            lblRezultatPred.setTextFill(Color.web("#10b981"));

            btnPredic.setOnAction(e -> {
                if (rn == null) {
                    lblRezultatPred.setText("Antrenați modelul!");
                    return;
                }
                try {
                    double luna = comboLunaPred.getSelectionModel().getSelectedIndex() + 1;
                    double sezon = comboSezonPred.getSelectionModel().getSelectedIndex() + 1;
                    double buget = Double.parseDouble(txtBugetPred.getText());
                    double pret = Double.parseDouble(txtPretPred.getText());
                    double[] input = { luna / rn.factorLuna,
                            sezon / rn.factorSezon,
                            buget / rn.factorBuget,
                            pret / rn.factorPret };
                    double[] pred = rn.prezice(input);
                    lblRezultatPred.setText(String.format("%.2f €", pred[0] * rn.factorVanzari));
                } catch (Exception ex) {
                    lblRezultatPred.setText("Eroare!");
                }
            });

            VBox rezultateBox = new VBox(5);
            rezultateBox.setPadding(new Insets(15));
            rezultateBox.setStyle(
                    "-fx-background-color: white; -fx-border-color: #a7f3d0; -fx-border-width: 2; -fx-background-radius: 8; -fx-border-radius: 8;");
            Label kpiTitle = new Label("Vânzări Previzionate:");
            kpiTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            // kpiTitle.setTextFill(Color.GRAY);
            rezultateBox.getChildren().addAll(kpiTitle, new Separator(), lblRezultatPred);

            leftPanel.getChildren().addAll(lblCalcTitle, lblCalcDesc, configPane, btnPredic, rezultateBox);

            // --- PARTEA DREAPTĂ: OPTIMIZER (NOU) ---
            VBox rightPanel = creeazaPanouOptimizare(comboLunaPred, comboSezonPred);
            tutorialCalcGoal = rightPanel;
            HBox.setHgrow(rightPanel, Priority.ALWAYS); // Permite expandarea

            // Adaugam panourile in layout
            mainLayout.getChildren().addAll(leftPanel, rightPanel);

            // Placeholder for chart at index 2 (titlu, mainLayout, chart)
            view.getChildren().addAll(titlu, mainLayout,
                    new VBox()); // Placeholder for chart (Index 2)

            ecranPredictiiCache = view;
        }

        // 2. ALWAYS Update the Chart Section (Index 2) in the cached view
        if (ecranPredictiiCache instanceof VBox) {
            VBox view = (VBox) ecranPredictiiCache;
            if (view.getChildren().size() > 2) {
                view.getChildren().set(2, creeazaSectiunePrognoza());
            }
        }

        setContentArea(ecranPredictiiCache);
        contentArea.setVvalue(0.0);
        contentArea.setHvalue(0.0);
    }

    private VBox creeazaPanouOptimizare(ComboBox<String> sourceLuna, ComboBox<String> sourceSezon) {
        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);");

        Label lblTitle = new Label("2. Calculator Optimizer (Țintă Vânzări)");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        lblTitle.setTextFill(Color.web("#064e3b"));

        Label lblDesc = new Label("Găsește bugetul și prețul optim pentru a atinge o țintă.");
        lblDesc.setTextFill(Color.web("#047857"));
        lblDesc.setWrapText(true);

        GridPane form = new GridPane();
        form.setHgap(15);
        form.setVgap(10);

        // Target Sales
        TextField txtTarget = new TextField("100000");
        predTargetField = txtTarget;
        txtTarget.setStyle("-fx-font-weight: bold; -fx-text-fill: #064e3b;");

        // Constraints
        TextField txtMinBudget = new TextField("1000");
        TextField txtMaxBudget = new TextField("15000");
        TextField txtMinPrice = new TextField("40");
        TextField txtMaxPrice = new TextField("100");

        // Independent ComboBoxes for Month and Season
        ComboBox<String> comboLunaOpt = new ComboBox<>();
        comboLunaOpt.getItems().setAll("Ianuarie", "Februarie", "Martie", "Aprilie", "Mai", "Iunie", "Iulie",
                "August", "Septembrie", "Octombrie", "Noiembrie", "Decembrie");
        comboLunaOpt.getSelectionModel().select(0);
        comboLunaOpt.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> comboSezonOpt = new ComboBox<>();
        comboSezonOpt.getItems().setAll("Iarnă (1)", "Primăvară (2)", "Vară (3)", "Toamnă (4)");
        comboSezonOpt.getSelectionModel().select(0);
        comboSezonOpt.setMaxWidth(Double.MAX_VALUE);

        // Layout Form
        form.add(new Label("Țintă Vânzări (€):"), 0, 0);
        form.add(txtTarget, 1, 0);

        form.add(new Separator(), 0, 1, 2, 1);

        form.add(new Label("Lună:"), 0, 2);
        form.add(comboLunaOpt, 1, 2);
        form.add(new Label("Sezon:"), 0, 3);
        form.add(comboSezonOpt, 1, 3);

        form.add(new Separator(), 0, 4, 2, 1);

        form.add(new Label("Limită Buget (Min-Max):"), 0, 5);
        HBox budgetBox = new HBox(5, txtMinBudget, new Label("-"), txtMaxBudget);
        form.add(budgetBox, 1, 5);

        form.add(new Label("Limită Preț (Min-Max):"), 0, 6);
        HBox priceBox = new HBox(5, txtMinPrice, new Label("-"), txtMaxPrice);
        form.add(priceBox, 1, 6);

        Button btnOptimize = new Button("Optimizează");
        btnOptimize.setStyle(
                "-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 25;");

        // Results Area
        VBox resultBox = new VBox(5);
        resultBox.setStyle(
                "-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8; -fx-border-color: #a7f3d0; -fx-border-width: 2; -fx-border-radius: 8;");

        Label lblResTitle = new Label("Vânzări Est.:");
        lblResTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));

        Label lblResBudget = new Label();
        predOptBugetLabel = lblResBudget;
        lblResBudget.setVisible(false);
        lblResBudget.setManaged(false);

        Label lblResPrice = new Label();
        predOptPretLabel = lblResPrice;
        lblResPrice.setVisible(false);
        lblResPrice.setManaged(false);

        Label lblResSales = new Label("0.00 €");
        predOptSalesLabel = lblResSales;
        lblResSales.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lblResSales.setTextFill(Color.web("#10b981"));

        resultBox.getChildren().addAll(lblResTitle, lblResBudget, lblResPrice, new Separator(), lblResSales);

        // Logic
        btnOptimize.setOnAction(e -> {
            if (rn == null) {
                new Alert(Alert.AlertType.WARNING, "Modelul nu este antrenat!").show();
                return;
            }

            try {
                double target = Double.parseDouble(txtTarget.getText());
                double minB = Double.parseDouble(txtMinBudget.getText());
                double maxB = Double.parseDouble(txtMaxBudget.getText());
                double minP = Double.parseDouble(txtMinPrice.getText());
                double maxP = Double.parseDouble(txtMaxPrice.getText());

                // Get Selected Values
                double luna = comboLunaOpt.getSelectionModel().getSelectedIndex() + 1;
                double sezon = comboSezonOpt.getSelectionModel().getSelectedIndex() + 1;

                // Normalize generic inputs
                double lunaNorm = luna / rn.factorLuna;
                double sezonNorm = sezon / rn.factorSezon;

                // Simulation: 5000 iterations
                double bestError = Double.MAX_VALUE;
                double bestBudget = 0;
                double bestPrice = 0;
                double bestSales = 0;

                int iterations = 5000;
                java.util.Random rand = new java.util.Random();

                // 1. First Pass: Find closest match to target
                // Store "good" candidates (within 1% error) to optimize for efficiency later
                java.util.List<double[]> candidates = new ArrayList<>();

                for (int i = 0; i < iterations; i++) {
                    double randB = minB + (maxB - minB) * rand.nextDouble();
                    double randP = minP + (maxP - minP) * rand.nextDouble();

                    double[] input = {
                            lunaNorm,
                            sezonNorm,
                            randB / rn.factorBuget,
                            randP / rn.factorPret
                    };

                    double[] out = rn.prezice(input);
                    double predictedSales = out[0] * rn.factorVanzari;

                    double error = Math.abs(target - predictedSales);

                    if (error < bestError) {
                        bestError = error;
                        bestBudget = randB;
                        bestPrice = randP;
                        bestSales = predictedSales;
                    }

                    // If error is small (e.g. < 2% of target), add to candidates
                    if (error < (target * 0.02)) {
                        candidates.add(new double[] { randB, randP, predictedSales, error });
                    }
                }

                // 2. Second Pass: "Sweet Spot" - If we have multiple good candidates, pick the
                // one with lowest budget

                if (!candidates.isEmpty()) {
                    candidates.sort((a, b) -> Double.compare(a[0], b[0]));
                    double[] bestEfficient = candidates.get(0);
                    bestBudget = bestEfficient[0];
                    bestPrice = bestEfficient[1];
                    bestSales = bestEfficient[2];
                }
                lblResTitle.setText("Rezultat Optim:");
                lblResBudget.setText(String.format("Buget Recomandat: %.0f €", bestBudget));
                lblResBudget.setVisible(true);
                lblResBudget.setManaged(true);
                lblResPrice.setText(String.format("Preț Mediu: %.0f €", bestPrice));
                lblResPrice.setVisible(true);
                lblResPrice.setManaged(true);
                lblResSales.setText(String.format("Vânzări Est.: %.0f €", bestSales));

                double diff = bestSales - target;
                double diffP = (diff / target) * 100;
                String diffStr = String.format("(%+.1f%% față de țintă)", diffP);
                if (Math.abs(diffP) < 1)
                    diffStr = " (Țintă Atinsă!)";
                lblResSales.setText(lblResSales.getText() + " " + diffStr);

            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Eroare date intrare: " + ex.getMessage()).show();
            }
        });

        container.getChildren().addAll(lblTitle, lblDesc, form, btnOptimize, resultBox);
        return container;
    }

    private VBox creeazaSectiunePrognoza() {
        return creeazaSectiunePrognoza(false);
    }

    private VBox creeazaSectiunePrognoza(boolean forExport) {
        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);");

        Label titlu = new Label("Prognoză Vânzări (Următoarele 6 Luni)");
        titlu.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        if (forExport) {
            titlu.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));
        }
        titlu.setTextFill(Color.web("#475569"));

        if (rn == null) {
            Label err = new Label("Modelul AI nu este antrenat.");
            err.setTextFill(Color.RED);
            container.getChildren().addAll(titlu, err);
            return container;
        }

        if (fisiereDateSelectate.isEmpty()) {
            Label err = new Label("Nu există date încărcate.");
            err.setTextFill(Color.RED);
            container.getChildren().addAll(titlu, err);
            return container;
        }

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Vânzări Estimate (€)");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setLegendVisible(false);
        lineChart.setMaxHeight(300);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Prognoză 6 Luni");

        try (FileInputStream fis = new FileInputStream(getFisierDateExcel());
                Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            int lastRowIdx = sheet.getLastRowNum();
            Row lastRow = sheet.getRow(lastRowIdx);

            // Fallback find last valid row with strict checks
            if (lastRow == null || getCellSafely(lastRow, 1) == null) {
                lastRow = null; // Reset to be sure
                for (int i = lastRowIdx; i >= 0; i--) {
                    Row r = sheet.getRow(i);
                    if (r != null) {
                        org.apache.poi.ss.usermodel.Cell cMonth = getCellSafely(r, 1);
                        org.apache.poi.ss.usermodel.Cell cYear = getCellSafely(r, 0); // Assuming year is 0? No,
                                                                                      // getAnValue uses row num or
                                                                                      // cell?
                        // getAnValue uses sheet and row index.

                        if (cMonth != null && cMonth.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                            lastRow = r;
                            break;
                        }
                    }
                }
            }

            if (lastRow != null) {
                int currentMonth = (int) getCellSafely(lastRow, 1).getNumericCellValue();
                int currentYear = getAnValue(sheet, lastRow.getRowNum());

                System.out.println("DEBUG PROGNOZA: Ultimul rand gasit: Luna=" + currentMonth + ", An=" + currentYear);

                double avgBudget = 5000; // default
                double avgPrice = 50; // default

                org.apache.poi.ss.usermodel.Cell cB = getCellSafely(lastRow, 3);
                org.apache.poi.ss.usermodel.Cell cP = getCellSafely(lastRow, 4);
                if (cB != null)
                    avgBudget = cB.getNumericCellValue();
                if (cP != null)
                    avgPrice = cP.getNumericCellValue();

                System.out.println("DEBUG PROGNOZA: Buget ref=" + avgBudget + ", Pret ref=" + avgPrice);

                String[] monthNames = { "", "Ian", "Feb", "Mar", "Apr", "Mai", "Iun", "Iul", "Aug", "Sep", "Oct", "Noi",
                        "Dec" };

                for (int i = 1; i <= 6; i++) {
                    int nextM = currentMonth + i;
                    int nextY = currentYear;
                    if (nextM > 12) {
                        nextM -= 12;
                        nextY++;
                    }

                    // Sezon simplificat: 1=Iarna(12,1,2), 2=Prim(3,4,5), 3=Vara(6,7,8),
                    // 4=Toamna(9,10,11)
                    int sezon = 0;
                    if (nextM == 12 || nextM == 1 || nextM == 2)
                        sezon = 1;
                    else if (nextM >= 3 && nextM <= 5)
                        sezon = 2;
                    else if (nextM >= 6 && nextM <= 8)
                        sezon = 3;
                    else
                        sezon = 4;

                    double[] input = {
                            (double) nextM / rn.factorLuna,
                            (double) sezon / rn.factorSezon,
                            avgBudget / rn.factorBuget,
                            avgPrice / rn.factorPret
                    };

                    double[] out = rn.prezice(input);
                    double val = out[0] * rn.factorVanzari;

                    System.out.println("DEBUG PROGNOZA (" + nextM + "/" + nextY + "): Input="
                            + java.util.Arrays.toString(input) + " -> Val=" + val);

                    XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(monthNames[nextM] + " " + nextY, val);
                    series.getData().add(dataPoint);

                    final int finalNextM = nextM;
                    final int finalNextY = nextY;

                    // Add tooltip to data point
                    dataPoint.nodeProperty().addListener((obs, oldNode, newNode) -> {
                        if (newNode != null) {
                            installTooltip(newNode,
                                    String.format("Prognoză: %s %d\nVânzări: %.2f €", monthNames[finalNextM],
                                            finalNextY, val));
                            newNode.setOnMouseEntered(e -> newNode
                                    .setStyle("-fx-scale-x: 1.5; -fx-scale-y: 1.5; -fx-background-color: #f59e0b;"));
                            newNode.setOnMouseExited(e -> newNode.setStyle(""));
                        }
                    });
                }
            } else {
                System.out.println("DEBUG PROGNOZA: Nu s-a putut gasi ultimul rand valid.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            Label err = new Label("Eroare la generarea prognozei: " + e.getMessage());
            err.setTextFill(Color.RED);
            container.getChildren().add(err);
        }

        lineChart.getData().add(series);

        if (forExport) {
            lineChart.setStyle("-fx-font-size: 28px;");
            xAxis.setTickLabelFont(Font.font(28));
            yAxis.setTickLabelFont(Font.font(28));
            xAxis.setTickLabelGap(10);
            yAxis.setTickLabelGap(10);
            lineChart.setMinHeight(900);
            lineChart.setPrefHeight(900);
            lineChart.setMaxHeight(900);
            lineChart.setMinWidth(1400);
            lineChart.setPrefWidth(1400);
            lineChart.setAnimated(false);
        }

        // Tooltip logic for forecast
        for (XYChart.Data<String, Number> d : series.getData()) {
            javafx.scene.Node n = d.getNode();
            if (n != null) {
                Tooltip t = new Tooltip(String.format("Data: %s\nEstimare: %.2f €", d.getXValue(), d.getYValue()));
                Tooltip.install(n, t);
            }
        }
        // Hack: Run later to attach tooltips if nodes are null initially
        javafx.application.Platform.runLater(() -> {
            for (XYChart.Data<String, Number> d : series.getData()) {
                javafx.scene.Node n = d.getNode();
                if (n != null) {
                    Tooltip t = new Tooltip(String.format("Data: %s\nEstimare: %.2f €", d.getXValue(), d.getYValue()));
                    Tooltip.install(n, t);
                    n.setOnMouseEntered(
                            e -> n.setStyle("-fx-scale-x: 1.5; -fx-scale-y: 1.5; -fx-background-color: #f59e0b;"));
                    n.setOnMouseExited(e -> n.setStyle(""));
                }
            }
        });
        if (!forExport) {
            StackPane prognozaWrapper = new StackPane(lineChart);
            adaugaButonFullscreen(prognozaWrapper, lineChart, "Prognoză Vânzări (Următoarele 6 Luni)");
            container.getChildren().addAll(titlu, prognozaWrapper);
        } else {
            container.getChildren().addAll(titlu, lineChart);
        }

        // Add forecast data table for export
        if (!series.getData().isEmpty()) {
            container.getProperties().put("forecast_data", "true");
            // Store forecast data as properties for PDF extraction
            StringBuilder forecastText = new StringBuilder();
            for (XYChart.Data<String, Number> d : series.getData()) {
                forecastText.append(d.getXValue()).append("|")
                        .append(String.format(java.util.Locale.US, "%.2f", d.getYValue())).append(";");
            }
            container.getProperties().put("forecast_values", forecastText.toString());
        }

        return container;
    }

    // --- 6. EXPORT (Redesigned) ---
    private String selectedReportType = null;
    private Button btnGenereazaRaport; // Class level to update state

    // New Helper Method for Training Report
    private VBox creeazaRaportAntrenare(boolean forExport) {
        VBox container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setStyle("-fx-background-color: white;");

        Label title = new Label("Raport Antrenare Rețea Neuronală");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        if (forExport)
            title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 48));

        // Info Antrenament
        Label info = new Label(lblInfoAntrenament.getText());
        info.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        if (forExport)
            info.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 28));

        // Recreate Charts for Export
        // Chart 1: Eroare
        NumberAxis xAxisE = new NumberAxis();
        xAxisE.setLabel("Iterații");
        NumberAxis yAxisE = new NumberAxis();
        yAxisE.setLabel("Eroare (MSE)");

        LineChart<Number, Number> chartEroare = new LineChart<>(xAxisE, yAxisE);
        chartEroare.setTitle("Evoluție Eroare Antrenament");
        chartEroare.setCreateSymbols(false);
        chartEroare.setAnimated(false);

        // Clone Data for Error
        XYChart.Series<Number, Number> sE = new XYChart.Series<>();
        sE.setName("Eroare");
        for (XYChart.Data<Number, Number> d : serieEroare.getData()) {
            sE.getData().add(new XYChart.Data<>(d.getXValue(), d.getYValue()));
        }
        chartEroare.getData().add(sE);

        // Chart 2: Validare
        NumberAxis xAxisV = new NumberAxis();
        xAxisV.setLabel("Iterații");
        NumberAxis yAxisV = new NumberAxis();
        yAxisV.setLabel("Valoare");

        LineChart<Number, Number> chartValid = new LineChart<>(xAxisV, yAxisV);
        chartValid.setTitle("Validare: Țintă (Real) vs Predictie (AI)");
        chartValid.setCreateSymbols(false);
        chartValid.setAnimated(false);

        XYChart.Series<Number, Number> sT = new XYChart.Series<>();
        sT.setName("Target (Real)");
        for (XYChart.Data<Number, Number> d : serieTinta.getData()) {
            sT.getData().add(new XYChart.Data<>(d.getXValue(), d.getYValue()));
        }

        XYChart.Series<Number, Number> sP = new XYChart.Series<>();
        sP.setName("Predicție (AI)");
        for (XYChart.Data<Number, Number> d : seriePreviziune.getData()) {
            sP.getData().add(new XYChart.Data<>(d.getXValue(), d.getYValue()));
        }

        chartValid.getData().addAll(sT, sP);

        if (forExport) {
            chartEroare.setStyle("-fx-font-size: 24px;");
            xAxisE.setTickLabelFont(Font.font(24));
            yAxisE.setTickLabelFont(Font.font(24));
            chartEroare.setMinHeight(600);
            chartEroare.setMaxHeight(600);

            chartValid.setStyle("-fx-font-size: 24px;");
            xAxisV.setTickLabelFont(Font.font(24));
            yAxisV.setTickLabelFont(Font.font(24));
            chartValid.setMinHeight(600);
            chartValid.setMaxHeight(600);
        }

        container.getChildren().addAll(title, info, chartEroare, chartValid);
        return container;
    }

    private void afiseazaExport() {
        if (ecranRapoarteCache == null) {
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

            VBox view = new VBox(30);
            view.setPadding(new Insets(40));
            view.setStyle("-fx-background-color: #f8fafc;"); // Light background

            // 1. Header Section
            VBox headerBox = new VBox(5);
            Label titlu = new Label("Export & Raportare");
            titlu.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
            titlu.setTextFill(Color.web("#1e293b"));

            Label subTitlu = new Label("Exportați starea curentă a modulelor aplicației în format PDF sau CSV.");
            subTitlu.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
            subTitlu.setTextFill(Color.web("#64748b"));

            headerBox.getChildren().addAll(titlu, subTitlu);

            // 2. Report Type Selection (Grid)
            Label lblGrid = new Label("Selectați Modulul pentru Export");
            lblGrid.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
            lblGrid.setTextFill(Color.web("#334155"));

            FlowPane cardsGrid = new FlowPane();
            tutorialExportContainer = cardsGrid;
            cardsGrid.setHgap(20);
            cardsGrid.setVgap(20);
            cardsGrid.setPrefWrapLength(1000); // Allow wrapping

            // Card 1: Dashboard
            // User provided "home.png" in icons_export
            VBox cardDashboard = createReportCard("Dashboard General",
                    "Snapshot al KPI-urilor, statisticilor și graficelor generale.",
                    "icons_export/home.png", "DASHBOARD");

            // Card 2: Sales Analysis
            VBox cardSales = createReportCard("Analiza Vânzărilor",
                    "Grafice detaliate, evoluția în timp și tendințe istorice.",
                    "icons_export/chart-simple.png", "SALES_ANALYSIS");

            // Card 3: Predictions
            VBox cardPredictions = createReportCard("Predicții Inteligente",
                    "Rezultatele prognozelor generate de rețeaua neuronală.",
                    "icons_export/crystal-ball.png", "PREDICTIONS");

            // Card 4: What-If
            VBox cardWhatIf = createReportCard("Simulator Scenarii",
                    "Starea curentă a simulatorului și scenariilor rulate.",
                    "icons_export/magic-wand.png", "WHAT_IF");

            // Card 5: Management
            VBox cardManagement = createReportCard("Gestiune & Antrenare",
                    "Configurarea rețelei, setările de antrenare și starea fișierelor.",
                    "icons_export/database-management.png", "MANAGEMENT");

            // Card 6: Executive Report (New!)
            VBox cardExecutive = createReportCard("Raport Executiv",
                    "Raport managerial complet cu concluzii AI și KPIs esențiali.",
                    "icons_export/document-signed.png", "EXECUTIVE");
            // Special style for Executive to stand out slightly?
            // cardExecutive.setStyle(
            // "-fx-background-color: #f0fdf4; -fx-background-radius: 12; -fx-effect:
            // dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2); -fx-cursor: hand;
            // -fx-border-color: #22c55e; -fx-border-width: 1; -fx-border-radius: 12;");

            cardsGrid.getChildren().addAll(cardDashboard, cardSales, cardPredictions, cardWhatIf, cardManagement,
                    cardExecutive);

            // 3. Export Options & Action
            VBox actionSection = new VBox(20);
            tutorialFormExport = actionSection;
            actionSection.setPadding(new Insets(30));
            actionSection.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 5);");

            Label lblExport = new Label("Format Export");
            lblExport.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));

            HBox exportOptions = new HBox(10);
            ToggleGroup formatGroup = new ToggleGroup();

            ToggleButton tbPDF = styleToggleButton("PDF", formatGroup, true);
            ToggleButton tbCSV = styleToggleButton("CSV", formatGroup, false);

            // Actualizare dinamică a textului butonului la schimbarea formatului
            formatGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null) {
                    // Prevent deselection
                    if (oldVal != null)
                        oldVal.setSelected(true);
                    return;
                }
                ToggleButton selected = (ToggleButton) newVal;
                String fmt = selected.getText();
                if (btnGenereazaRaport != null && selectedReportType != null) {
                    btnGenereazaRaport.setText("Generează Raport " + fmt);
                }
            });

            exportOptions.getChildren().addAll(tbPDF, tbCSV);

            btnGenereazaRaport = new Button("Generează Raport PDF");
            btnGenereazaRaport.setDisable(true); // Initially disabled
            btnGenereazaRaport.setStyle(
                    "-fx-background-color: #cbd5e1; -fx-text-fill: #1e293b; -fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 15 40; -fx-background-radius: 8;");
            btnGenereazaRaport.setCursor(Cursor.DEFAULT);

            btnGenereazaRaport.setOnAction(e -> {
                ToggleButton selFmt = (ToggleButton) formatGroup.getSelectedToggle();
                String format = (selFmt != null) ? selFmt.getText() : "PDF";
                if ("CSV".equals(format)) {
                    generareRaportCSV();
                } else {
                    generareRaportPDF();
                }
            });

            actionSection.getChildren().addAll(lblExport, exportOptions, new Separator(), btnGenereazaRaport);

            view.getChildren().addAll(headerBox, lblGrid, cardsGrid, actionSection);

            scrollPane.setContent(view);
            ecranRapoarteCache = scrollPane;
        }

        setContentArea(ecranRapoarteCache);
        contentArea.setVvalue(0.0);
        contentArea.setHvalue(0.0);
    }

    private VBox createReportCard(String title, String description, String iconName, String typeId) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(25));
        card.setPrefWidth(300);
        card.setMinHeight(180);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2); -fx-cursor: hand; -fx-border-color: transparent; -fx-border-width: 2; -fx-border-radius: 12;");

        // Icon
        ImageView icon = incarcaIcon(iconName, 48); // Large icon
        if (icon == null) {
            // Fallback if specific icon not found, use branding icon or generic
            icon = incarcaIcon("big-data-analytics.png", 48);
        }

        Label lblTitle = new Label(title);
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lblTitle.setTextFill(Color.web("#1e293b"));

        Label lblDesc = new Label(description);
        lblDesc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        lblDesc.setTextFill(Color.web("#64748b"));
        lblDesc.setWrapText(true);

        card.getChildren().addAll(icon != null ? icon : new Region(), lblTitle, lblDesc);

        // Interaction
        card.setOnMouseEntered(e -> {
            if (!typeId.equals(selectedReportType)) {
                card.setTranslateY(-5);
                String hoverBg = isDarkMode ? "#334155" : "white";
                card.setStyle(
                        "-fx-background-color: " + hoverBg
                                + "; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 4); -fx-cursor: hand; -fx-border-color: transparent; -fx-border-width: 2; -fx-border-radius: 12;");
            }
        });

        card.setOnMouseExited(e -> {
            if (!typeId.equals(selectedReportType)) {
                card.setTranslateY(0);
                String normalBg = isDarkMode ? "#1e293b" : "white";
                card.setStyle(
                        "-fx-background-color: " + normalBg
                                + "; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2); -fx-cursor: hand; -fx-border-color: transparent; -fx-border-width: 2; -fx-border-radius: 12;");
            }
        });

        card.setOnMouseClicked(e -> {
            selectReportCard(card, typeId);
        });

        // Store type ID in user data for easy retrieval if needed
        card.setUserData(typeId);

        return card;
    }

    private void selectReportCard(VBox selectedCard, String typeId) {
        // Deselect all others in the grid
        String normalBg = isDarkMode ? "#1e293b" : "white";
        if (selectedCard.getParent() instanceof FlowPane) {
            FlowPane grid = (FlowPane) selectedCard.getParent();
            for (Node node : grid.getChildren()) {
                if (node instanceof VBox) {
                    // Reset style — respect dark mode
                    node.setStyle(
                            "-fx-background-color: " + normalBg
                                    + "; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2); -fx-cursor: hand; -fx-border-color: transparent; -fx-border-width: 2; -fx-border-radius: 12;");
                    node.setTranslateY(0);
                    // Re-salvam stilul original pentru dark mode toggle ulterior
                    node.getProperties().put("original_style", node.getStyle());
                }
            }
        }

        // Select this one
        selectedReportType = typeId;
        String selectedBg = isDarkMode ? "#1e3a5f" : "#eff6ff";
        selectedCard.setStyle(
                "-fx-background-color: " + selectedBg
                        + "; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(37, 99, 235, 0.2), 10, 0, 0, 2); -fx-cursor: hand; -fx-border-color: #3b82f6; -fx-border-width: 2; -fx-border-radius: 12;");
        selectedCard.setTranslateY(-5);
        selectedCard.getProperties().put("original_style", selectedCard.getStyle());

        // Enable Generate Button
        if (btnGenereazaRaport != null) {
            btnGenereazaRaport.setDisable(false);
            btnGenereazaRaport.setStyle(
                    "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 15 40; -fx-background-radius: 8; -fx-cursor: hand;");
            btnGenereazaRaport.setCursor(Cursor.HAND);
        }
    }

    private ToggleButton styleToggleButton(String text, ToggleGroup group, boolean isSelected) {
        ToggleButton tb = new ToggleButton(text);
        tb.setToggleGroup(group);
        tb.setSelected(isSelected);
        tb.setStyle(isSelected
                ? "-fx-background-color: #e2e8f0; -fx-text-fill: #1e293b; -fx-font-weight: bold; -fx-background-radius: 6; -fx-border-color: #94a3b8; -fx-border-radius: 6;"
                : "-fx-background-color: white; -fx-text-fill: #64748b; -fx-font-weight: normal; -fx-background-radius: 6; -fx-border-color: #cbd5e1; -fx-border-radius: 6;");

        tb.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                tb.setStyle(
                        "-fx-background-color: #e2e8f0; -fx-text-fill: #1e293b; -fx-font-weight: bold; -fx-background-radius: 6; -fx-border-color: #94a3b8; -fx-border-radius: 6;");
            } else {
                tb.setStyle(
                        "-fx-background-color: white; -fx-text-fill: #64748b; -fx-font-weight: normal; -fx-background-radius: 6; -fx-border-color: #cbd5e1; -fx-border-radius: 6;");
            }
        });

        return tb;
    }

    // Clasa auxiliară pentru modelul fișierului Date Actuale tip Excel
    public static class DateActualeItem {
        private final SimpleStringProperty an;
        private final SimpleStringProperty luna;
        private final SimpleStringProperty sezon;
        private final SimpleStringProperty buget;
        private final SimpleStringProperty pret;
        private final SimpleStringProperty vanzari;
        private final SimpleStringProperty clienti;

        public DateActualeItem(String an, String luna, String sezon, String buget, String pret, String vanzari,
                String clienti) {
            this.an = new SimpleStringProperty(an);
            this.luna = new SimpleStringProperty(luna);
            this.sezon = new SimpleStringProperty(sezon);
            this.buget = new SimpleStringProperty(buget);
            this.pret = new SimpleStringProperty(pret);
            this.vanzari = new SimpleStringProperty(vanzari);
            this.clienti = new SimpleStringProperty(clienti);
        }

        public StringProperty anProperty() {
            return an;
        }

        public StringProperty lunaProperty() {
            return luna;
        }

        public StringProperty sezonProperty() {
            return sezon;
        }

        public StringProperty bugetProperty() {
            return buget;
        }

        public StringProperty pretProperty() {
            return pret;
        }

        public StringProperty vanzariProperty() {
            return vanzari;
        }

        public StringProperty clientiProperty() {
            return clienti;
        }
    }

    // Clasa auxiliară pentru fișiere
    public static class FisierIncarcat {
        private final String nume;
        private final String data;
        private final File file;

        public FisierIncarcat(String nume, String data, File file) {
            this.nume = nume;
            this.data = data;
            this.file = file;
        }

        public String getNume() {
            return nume;
        }

        public String getData() {
            return data;
        }

        public File getFile() {
            return file;
        }

        public StringProperty numeProperty() {
            return new SimpleStringProperty(nume);
        }

        public StringProperty dataProperty() {
            return new SimpleStringProperty(data);
        }
    }

    private void actualizeazaStatusFisiere() {
        lblStatus.setText(fisiereDateSelectate.size()
                + (fisiereDateSelectate.size() == 1 ? " fișier încărcat" : " fișiere încărcate"));
    }

    // Metodă pentru actualizarea datelor actuale din fișierul Excel
    private void actualizeazaDateActuale(Label lblBuget, Label lblPret, Label lblLuna, Label lblSezon, Label lblAn,
            Label lblVanzari, Label lblClienti,
            XYChart.Series<String, Number> seriesComparativ,
            Label lblRaVanzariActual, Label lblRaVanzariScenariu, Label lblRaDiferenta) {
        if (fisiereDateSelectate.isEmpty()) {
            lblBuget.setText("Buget Marketing: - (încarcă un fișier)");
            lblPret.setText("Preț Mediu: - (încarcă un fișier)");
            lblLuna.setText("Lună: - (încarcă un fișier)");
            lblSezon.setText("Sezon: - (încarcă un fișier)");
            lblAn.setText("An: - (încarcă un fișier)");
            lblVanzari.setText("Vânzări: - (încarcă un fișier)");
            lblClienti.setText("Nr. Clienți: - (încarcă un fișier)");
            return;
        }

        try {
            // Folosim primul fișier încărcat
            File fisier = getFisierDateExcel();

            // Citim direct datele din fișierul Excel pentru a obține toate coloanele
            // inclusiv Anul și Numărul de clienți
            FileInputStream fis = new FileInputStream(fisier);
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheetAt(0);

            // Determinăm luna precedentă calendaristică
            java.time.LocalDate dataCurenta = java.time.LocalDate.now();
            int lunaPrecedenta = dataCurenta.getMonthValue() - 1;
            int anPrecedent = dataCurenta.getYear();

            // Dacă suntem în ianuarie, luna precedentă este decembrie anul trecut
            if (lunaPrecedenta == 0) {
                lunaPrecedenta = 12;
                anPrecedent = dataCurenta.getYear() - 1;
            }

            // Căutăm rândul care corespunde lunii precedente
            Row randLunaPrecedenta = null;
            int ultimaLinie = sheet.getLastRowNum();

            for (int i = 1; i <= ultimaLinie; i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    // Verificăm dacă avem cel puțin coloanele esențiale completate
                    org.apache.poi.ss.usermodel.Cell cellLuna = getCellSafely(row, 1); // Col B - Luna
                    org.apache.poi.ss.usermodel.Cell cellSezon = getCellSafely(row, 2); // Col C - Sezon
                    org.apache.poi.ss.usermodel.Cell cellBuget = getCellSafely(row, 3); // Col D - Buget
                    org.apache.poi.ss.usermodel.Cell cellPret = getCellSafely(row, 4); // Col E - Pret
                    org.apache.poi.ss.usermodel.Cell cellVanzari = getCellSafely(row, 5); // Col F - Vanzari
                    org.apache.poi.ss.usermodel.Cell cellClienti = getCellSafely(row, 6); // Col G - Clienti

                    if (cellLuna != null && cellSezon != null && cellBuget != null &&
                            cellPret != null && cellVanzari != null) {

                        // Verificăm dacă acest rând corespunde lunii precedente
                        int lunaDinFisier = (int) cellLuna.getNumericCellValue();
                        int anDinFisier = getAnValue(sheet, i);

                        if (lunaDinFisier == lunaPrecedenta && anDinFisier == anPrecedent) {
                            randLunaPrecedenta = row;
                            break;
                        }
                    }
                }
            }

            // Dacă nu găsim luna precedentă, folosim ultimul rând disponibil
            Row ultimulRand = randLunaPrecedenta;
            if (ultimulRand == null) {
                // Căutăm ultimul rând completat ca fallback
                for (int i = ultimaLinie; i >= 1; i--) {
                    Row row = sheet.getRow(i);
                    if (row != null) {
                        org.apache.poi.ss.usermodel.Cell cellLuna = getCellSafely(row, 1);
                        org.apache.poi.ss.usermodel.Cell cellSezon = getCellSafely(row, 2);
                        org.apache.poi.ss.usermodel.Cell cellBuget = getCellSafely(row, 3);
                        org.apache.poi.ss.usermodel.Cell cellPret = getCellSafely(row, 4);
                        org.apache.poi.ss.usermodel.Cell cellVanzari = getCellSafely(row, 5);

                        if (cellLuna != null && cellSezon != null && cellBuget != null &&
                                cellPret != null && cellVanzari != null) {
                            ultimulRand = row;
                            break;
                        }
                    }
                }
            }

            if (ultimulRand != null) {
                // Citim datele din ultimul rând completat
                // Pentru an, trebuie să căutăm în sus până găsim valoarea (pentru celule
                // merged)
                int an = getAnValue(sheet, ultimulRand.getRowNum()); // Coloana A - Anul
                int luna = (int) getCellSafely(ultimulRand, 1).getNumericCellValue(); // Coloana B - Luna
                int sezon = (int) getCellSafely(ultimulRand, 2).getNumericCellValue(); // Coloana C - Sezon
                double buget = getCellSafely(ultimulRand, 3).getNumericCellValue(); // Coloana D - Buget Marketing
                double pret = getCellSafely(ultimulRand, 4).getNumericCellValue(); // Coloana D - Buget Marketing
                double vanzari = getCellSafely(ultimulRand, 5).getNumericCellValue(); // Coloana F - Vanzari

                double clienti = 0;
                org.apache.poi.ss.usermodel.Cell cellClienti = getCellSafely(ultimulRand, 6);
                if (cellClienti != null && cellClienti.getCellType() == CellType.NUMERIC) {
                    clienti = cellClienti.getNumericCellValue();
                }

                // Debug
                System.out.println("Date actuale găsite: Luna=" + luna + ", An=" + an + ", Vanzari=" + vanzari
                        + ", Clienti=" + clienti);

                // Actualizăm UI
                lblBuget.setText("Buget Marketing: " + (int) buget + "€");
                lblPret.setText("Preț Mediu: " + (int) pret + "€");

                String[] luni = { "Ianuarie", "Februarie", "Martie", "Aprilie", "Mai", "Iunie",
                        "Iulie", "August", "Septembrie", "Octombrie", "Noiembrie", "Decembrie" };
                String numeLuna = (luna >= 1 && luna <= 12) ? luni[luna - 1] : String.valueOf(luna);
                lblLuna.setText("Lună: " + numeLuna);

                String[] sezoane = { "Iarnă (1)", "Primăvară (2)", "Vară (3)", "Toamnă (4)" };
                String numeSezon = (sezon >= 1 && sezon <= 4) ? sezoane[sezon - 1] : String.valueOf(sezon);
                lblSezon.setText("Sezon: " + numeSezon);

                lblAn.setText("An: " + an);
                lblVanzari.setText("Vânzări: " + String.format("%.2f €", vanzari));
                lblClienti.setText("Nr. Clienți: " + (int) clienti);
                // --- ACTUALIZARE GRAFIC COMPARATIV ---
                if (seriesComparativ != null && !seriesComparativ.getData().isEmpty()) {
                    // Actualizăm Actual (index 0)
                    XYChart.Data<String, Number> dataActual = seriesComparativ.getData().get(0);
                    dataActual.setYValue(vanzari);

                    // Actualizăm Scenariu (index 1) - inițial la fel ca actualul
                    XYChart.Data<String, Number> dataScenariu = seriesComparativ.getData().get(1);
                    if (dataScenariu.getYValue().doubleValue() == 0) {
                        dataScenariu.setYValue(vanzari);
                    }

                    // Forțăm culorile
                    javafx.application.Platform.runLater(() -> {
                        if (dataActual.getNode() != null)
                            dataActual.getNode().setStyle("-fx-bar-fill: #e67e22;");
                        if (dataScenariu.getNode() != null)
                            dataScenariu.getNode().setStyle("-fx-bar-fill: #f1c40f;");
                    });
                }

                // --- ACTUALIZARE ETICHETE REZULTATE COMPARAȚIE ---
                // Vanzările actuale se actualizează MEREU, pentru că s-ar fi putut schimba
                // fișierul
                if (lblRaVanzariActual != null) {
                    lblRaVanzariActual.setText(String.format("%.2f €", vanzari));
                }

                // Doar dacă Scenariul este "0.00 €" (adică nu s-a făcut încă nicio simulare sau
                // s-a resetat explicit),
                // îl actualizăm la valoarea curentă pentru a arăta "no change".
                // Altfel, dacă utilizatorul a rulat deja o simulare, PĂSTRĂM rezultatele ei.
                if (lblRaVanzariScenariu != null && lblRaVanzariScenariu.getText().equals("0.00 €")) {
                    lblRaVanzariScenariu.setText(String.format("%.2f €", vanzari));
                    if (lblRaDiferenta != null) {
                        lblRaDiferenta.setText("0.00 € (0.0%)");
                        lblRaDiferenta.setTextFill(Color.GRAY);
                    }
                } else {
                    // Dacă avem deja o simulare rulată, nu suprascriem cu valorile default!
                    // Lăsăm valorile calculate anterior.
                }

            } else {
                lblBuget.setText("Buget Marketing: - (date indisponibile)");
                lblPret.setText("Preț Mediu: - (date indisponibile)");
                lblLuna.setText("Lună: - (date indisponibile)");
                lblSezon.setText("Sezon: - (date indisponibile)");
                lblAn.setText("An: - (date indisponibile)");
                lblVanzari.setText("Vânzări: - (date indisponibile)");
            }

            workbook.close();
            fis.close();

        } catch (Exception e) {
            lblBuget.setText("Buget Marketing: - (eroare citire)");
            lblPret.setText("Preț Mediu: - (eroare citire)");
            lblLuna.setText("Lună: - (eroare citire)");
            lblSezon.setText("Sezon: - (eroare citire)");
            lblAn.setText("An: - (eroare citire)");
            lblVanzari.setText("Vânzări: - (eroare citire)");
            e.printStackTrace();
        }
    }

    // Metodă helper pentru a obține celula în siguranță
    private org.apache.poi.ss.usermodel.Cell getCellSafely(org.apache.poi.ss.usermodel.Row row, int columnIndex) {
        try {
            org.apache.poi.ss.usermodel.Cell cell = row.getCell(columnIndex);
            if (cell == null) {
                return null;
            }
            // Verificăm dacă celula are valoare numerică
            if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                return cell;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // Metodă pentru a obține valoarea anului din celulele merged
    private int getAnValue(org.apache.poi.ss.usermodel.Sheet sheet, int rowIndex) {
        try {
            // Căutăm în sus până găsim o valoare în coloana A
            for (int i = rowIndex; i >= 0; i--) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(i);
                if (row != null) {
                    org.apache.poi.ss.usermodel.Cell cell = row.getCell(0); // Coloana A
                    if (cell != null && cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                        return (int) cell.getNumericCellValue();
                    }
                }
            }
            return java.time.LocalDate.now().getYear(); // Valoare implicită
        } catch (Exception e) {
            return java.time.LocalDate.now().getYear(); // Valoare implicită
        }
    }

    private void installTooltip(javafx.scene.Node node, String text) {
        Tooltip t = new Tooltip(text);
        t.setStyle(
                "-fx-background-color: #e2e8f0; -fx-text-fill: #475569; -fx-background-radius: 10; -fx-padding: 10; -fx-font-size: 14px; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 5);");
        t.setShowDelay(Duration.millis(100));
        t.setShowDuration(Duration.INDEFINITE); // Tooltip stays open as long as mouse is over
        Tooltip.install(node, t);
    }

    // --- METODE PENTRU BOTTOM MENU (USER & HELP) ---

    // 1. Actualizare Sidebar Bottom - DEPRECATED (Folosim butoane standard acum)
    private void actualizeazaSidebarBottom() {
        // Metoda aceasta nu mai face nimic vizual in sidebar, dar poate fi pastrata
        // daca avem nevoie de refresh global
    }

    // 2. Ecran User (Login / Register / Profile)
    private void afiseazaEcranUser() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.TOP_CENTER);

        // Header
        Label title = new Label("Gestiune Utilizator: " + ((currentUser != null) ? currentUser.name : "Guest"));
        title.setFont(Font.font("Inter", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#1e293b"));

        if (currentUser != null) {
            // --- LOGGED IN VIEW ---
            VBox profileBox = new VBox(15);
            profileBox.setAlignment(Pos.CENTER);
            profileBox.setStyle(
                    "-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");
            profileBox.setMaxWidth(500);

            ImageView avatar = incarcaIcon("icons_bottom_menu/user_icon.png", 64);
            Label lblName = new Label(currentUser.name + " " + currentUser.surname);
            lblName.setFont(Font.font("Inter", FontWeight.BOLD, 18));

            Label lblRole = new Label(currentUser.role + " @ " + currentUser.company);
            lblRole.setTextFill(Color.GRAY);

            Button btnLogout = new Button("Deconectare");
            btnLogout.setStyle(
                    "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand;");
            btnLogout.setOnAction(e -> {
                currentUser = null;
                // actualizeazaSidebarBottom(); // Not needed
                afiseazaEcranUser(); // Refresh current view
            });

            profileBox.getChildren().addAll(avatar, lblName, lblRole, new Separator(), btnLogout);
            root.getChildren().addAll(title, profileBox);

        } else {
            // --- GUEST VIEW (LOGIN & REGISTER TOGGLE) ---
            showLoginView(root, title);
        }

        setContentArea(root);
        contentArea.setVvalue(0.0);
        contentArea.setHvalue(0.0);
    }

    private void showLoginView(VBox root, Label title) {
        root.getChildren().clear();
        root.getChildren().add(title);

        VBox loginBox = new VBox(15);
        loginBox.setStyle(
                "-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");
        loginBox.setMaxWidth(400);
        loginBox.setAlignment(Pos.CENTER);

        Label lblHeader = new Label("Autentificare");
        lblHeader.setFont(Font.font("Inter", FontWeight.BOLD, 18));

        TextField tfUser = new TextField();
        tfUser.setPromptText("Username");
        PasswordField pfPass = new PasswordField();
        pfPass.setPromptText("Parola");

        Button btnLogin = new Button("Log In");
        btnLogin.setStyle(
                "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand; -fx-pref-width: 200;");

        Label lblLoginErr = new Label();
        lblLoginErr.setTextFill(Color.RED);

        btnLogin.setOnAction(e -> {
            String u = tfUser.getText();
            String p = pfPass.getText();
            boolean found = false;
            for (User user : registeredUsers) {
                if (user.username.equals(u) && user.password.equals(p)) {
                    currentUser = user;
                    found = true;
                    break;
                }
            }
            if (found) {
                afiseazaEcranUser(); // Refresh -> goes to profile
            } else {
                lblLoginErr.setText("Username sau parolă incorecte!");
            }
        });

        // Link to Register
        javafx.scene.control.Hyperlink linkReg = new javafx.scene.control.Hyperlink("Nu ai cont? Sign-In!");
        linkReg.setOnAction(e -> showRegisterView(root, title));

        loginBox.getChildren().addAll(lblHeader, tfUser, pfPass, btnLogin, lblLoginErr, new Separator(), linkReg);
        root.getChildren().add(loginBox);
    }

    private void showRegisterView(VBox root, Label title) {
        root.getChildren().clear();
        root.getChildren().add(title);

        VBox regBox = new VBox(15);
        regBox.setStyle(
                "-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");
        regBox.setMaxWidth(400);
        regBox.setAlignment(Pos.CENTER);

        Label lblHeader = new Label("Înregistrare Nouă");
        lblHeader.setFont(Font.font("Inter", FontWeight.BOLD, 18));

        TextField tfNume = new TextField();
        tfNume.setPromptText("Nume");
        TextField tfPrenume = new TextField();
        tfPrenume.setPromptText("Prenume");
        TextField tfRegUser = new TextField();
        tfRegUser.setPromptText("Username dorit");

        ComboBox<String> cbRole = new ComboBox<>();
        // Removed "Vânzări", added "Contabil"
        cbRole.getItems().addAll("Manager", "Analist Date", "Administrator", "Contabil");
        cbRole.setPromptText("Funcție");
        cbRole.setMaxWidth(Double.MAX_VALUE);

        TextField tfCompany = new TextField();
        tfCompany.setPromptText("Numele Companiei");
        PasswordField pfRegPass = new PasswordField();
        pfRegPass.setPromptText("Parola");

        Button btnReg = new Button("Înregistrează-te");
        btnReg.setStyle(
                "-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand; -fx-pref-width: 200;");

        Label lblRegMsg = new Label();

        btnReg.setOnAction(e -> {
            if (tfNume.getText().isEmpty() || tfRegUser.getText().isEmpty() || pfRegPass.getText().isEmpty()) {
                lblRegMsg.setText("Completați toate datele!");
                lblRegMsg.setTextFill(Color.RED);
                return;
            }
            for (User u : registeredUsers) {
                if (u.username.equals(tfRegUser.getText())) {
                    lblRegMsg.setText("Username deja existent!");
                    lblRegMsg.setTextFill(Color.RED);
                    return;
                }
            }

            User newUser = new User(
                    tfNume.getText(), tfPrenume.getText(), tfRegUser.getText(),
                    cbRole.getValue() != null ? cbRole.getValue() : "User",
                    tfCompany.getText(), pfRegPass.getText());
            registeredUsers.add(newUser);
            salveazaUtilizatori();

            lblRegMsg.setText("Cont creat! Acum te poți autentifica.");
            lblRegMsg.setTextFill(Color.GREEN);
        });

        // Link to Login
        javafx.scene.control.Hyperlink linkLogin = new javafx.scene.control.Hyperlink("Ai deja cont? Log-in");
        linkLogin.setOnAction(e -> showLoginView(root, title));

        regBox.getChildren().addAll(lblHeader, tfNume, tfPrenume, tfRegUser, cbRole, tfCompany, pfRegPass, btnReg,
                lblRegMsg, new Separator(), linkLogin);
        root.getChildren().add(regBox);
    }

    // 3. Ecran Help / Despre
    private void afiseazaEcranHelp() {
        VBox mainContainer = new VBox(0);
        mainContainer.setStyle("-fx-background-color: #f8fafc;");

        // 1. Header Hero Section
        VBox headerHero = new VBox(15);
        headerHero.setPadding(new Insets(50, 60, 50, 60));
        headerHero.setStyle("-fx-background-color: linear-gradient(to right, #1e293b, #334155);");

        Label lblAppTitle = new Label("NEUROCAST v3.1 (beta)");
        lblAppTitle.setFont(Font.font("Inter", FontWeight.BOLD, 36));
        lblAppTitle.setTextFill(Color.WHITE);

        Label lblAppSub = new Label(
                "Instrument de Business Intelligence și Predictive Analytics pentru Managementul Vânzărilor");
        lblAppSub.setFont(Font.font("Inter", FontWeight.LIGHT, 18));
        lblAppSub.setTextFill(Color.web("#94a3b8"));

        headerHero.getChildren().addAll(lblAppTitle, lblAppSub);

        // 2. Content Area
        VBox contentBody = new VBox(40);
        contentBody.setPadding(new Insets(40, 60, 60, 60));
        contentBody.setMaxWidth(1100);

        // --- SECTIUNI (Create them first to reference in TOC) ---
        VBox sMisiune = creeazaSectiuneDespre("Misiunea Aplicației",
                "NeuroCast v3.1 reprezintă o soluție avansată de suport decizional, concepută pentru a asista managerii și analiștii de date în procesul de fundamentare a strategiilor comerciale. "
                        +
                        "Misiunea proiectului este de a democratiza accesul la algoritmi de Predictive Analytics, oferind o interfață intuitivă pentru explorarea corelațiilor complexe dintre variabilele de piață și performanța vânzărilor. "
                        +
                        "Sistemul nu doar monitorizează trecutul, ci simulează viitorul, oferind un avantaj competitiv prin anticiparea tendințelor și optimizarea resurselor financiare.");

        VBox sArhitectura = creeazaSectiuneDespre("Arhitectura Rețelei Neuronale (RNA)",
                "La baza aplicației stă o Rețea Neuronale Artificială de tip Multi-Layer Perceptron (MLP) cu propagare înainte (Feed-Forward). "
                        +
                        "Sistemul este antrenat să proceseze un vector de trăsături format din:\n" +
                        "• Luna calendaristică (sezonalitate temporală)\n" +
                        "• Sezonul (impactul condițiilor macro/climatice)\n" +
                        "• Bugetul de Marketing (investiția în vizibilitate)\n" +
                        "• Prețul de vânzare (elasticitatea cererii)\n\n" +
                        "Prin utilizarea algoritmului Backpropagation, rețeaua își ajustează automat erorile în timpul fazei de antrenare, reușind să aproximeze funcții non-lineare pe care modelele statistice clasice le-ar omite.");

        VBox sLogica = creeazaSectiuneDespre("Logica de Funcționare",
                "Modelul MLP implementat organizează neuronii pe straturi succesive: intrare, ascuns și ieșire. Fiecare conexiune poartă o pondere (weight) și o deplasare (bias). "
                        +
                        "Fiecare neuron utilizează funcția de activare Sigmoid: f(x) = 1 / (1 + e^-x). Această funcție permite rețelei să învețe praguri de decizie complexe. "
                        +
                        "În timpul antrenamentului, datele brute sunt normalizate în intervalul [0,1], procesate de rețea, iar rezultatul este ulterior denormalizat pentru a oferi valori reale în euro sau unități vândute, asigurând o precizie optimă indiferent de magnitudinea datelor de intrare.");

        VBox sModule = new VBox(20);
        Label lblModT = new Label("Descrierea Modulelor (Tab-uri)");
        lblModT.setFont(Font.font("Inter", FontWeight.BOLD, 22));
        lblModT.setTextFill(Color.web("#1e293b"));

        GridPane gridModule = new GridPane();
        gridModule.setHgap(20);
        gridModule.setVgap(20);
        gridModule.add(creeazaCardModul("Dashboard General",
                "Monitorizarea centralizată a indicatorilor de performanță (KPI) și vizualizarea trendurilor de vânzări prin diagrame dinamice.",
                "#3b82f6"), 0, 0);
        gridModule.add(creeazaCardModul("Analiza Vânzărilor",
                "Identificarea corelațiilor profunde prin scatter plot-uri Buget vs Vânzări și heatmap-uri pentru intensitatea sezonalității.",
                "#8b5cf6"), 1, 0);
        gridModule.add(creeazaCardModul("Predicții Inteligente",
                "Motorul de prognoză bazat pe RNA. Permite introducerea oricărui scenariu pentru a obține o estimare instantanee generată de modelul antrenat.",
                "#ec4899"), 0, 1);
        gridModule.add(creeazaCardModul("Simulator Scenarii",
                "Laborator virtual 'What-if' unde se pot compara proiecțiile de profit în funcție de ajustările prețului și bugetului de marketing.",
                "#f59e0b"), 1, 1);
        gridModule.add(creeazaCardModul("Gestiune & Antrenare",
                "Secțiunea tehnică de management a seturilor de date. Include controlul fin asupra procesului de învățare (iterații, loss function).",
                "#10b981"), 0, 2);
        gridModule.add(creeazaCardModul("Export & Raportare",
                "Generarea rapoartelor profesionale în formate PDF (high-resolution), Excel și CSV pentru auditare și prezentare.",
                "#64748b"), 1, 2);
        sModule.getChildren().addAll(lblModT, gridModule);

        VBox sGhid = creeazaSectiuneDespre("Ghid Quick Start (5 pași)",
                "1. Încărcare Date: Accesează 'Gestiune' pentru a importa istoricul tău de vânzări din fișier Excel.\n"
                        +
                        "2. Antrenare Model: Configurează numărul de iterații și pornește procesul de învățare a rețelei neuronale.\n"
                        +
                        "3. Validare Rezultate: Verifică eroarea (Loss) și graficul de validare pentru a te asigura că modelul este precis.\n"
                        +
                        "4. Simulare Strategie: Utilizează Simulatorul pentru a testa impactul unor noi prețuri sau bugete asupra vânzărilor.\n"
                        +
                        "5. Export Documentație: Salvează raportul final sub formă de PDF pentru a-ți susține strategia în fața echipei.");

        javafx.scene.control.Hyperlink btnRestartTutorial = new javafx.scene.control.Hyperlink("Rulează ghidul interactiv");
        btnRestartTutorial.setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold; -fx-underline: true; -fx-font-size: 14px;");
        btnRestartTutorial.setOnAction(e -> {
            // Când apasă, sărim direct în tabul Principal/Dashboard, pentru că acolo rulează ghidul
            Button btnDash = (Button) sidebar.getChildren().get(1); 
            btnDash.fire();
            pornesteTutorial();
        });
        sGhid.getChildren().add(btnRestartTutorial);

        VBox sTech = creeazaSectiuneDespre("Stiva Tehnologică",
                "Aplicația este construită pe o arhitectură modulară Java, utilizând următoarele tehnologii:\n" +
                        "• Java Core & JavaFX: Pentru logică de business robustă și interfață grafică accelerată hardware.\n"
                        +
                        "• iText7: Motor de randare PDF de înaltă calitate pentru exportul graficelor și statisticilor.\n"
                        +
                        "• Apache POI: Manipularea eficientă a datelor din foile de calcul Microsoft Excel.\n" +
                        "• Custom Neural Engine: Implementare nativă a unui model MLP, fără dependințe externe masive, optimizată pentru performanță.");

        VBox sAutor = new VBox(15);
        sAutor.setPadding(new Insets(30));
        sAutor.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-width: 1 0 0 0;");

        Label lblAutorT = new Label("Informații Autor");
        lblAutorT.setFont(Font.font("Inter", FontWeight.BOLD, 22));

        VBox autorDetails = new VBox(5);
        autorDetails.getChildren().addAll(
                new Label("Student: Lupu Ion"),
                new Label("Facultatea de Economie și Administrarea Afacerilor (FEAA)"),
                new Label("Specializare: Informatică Economică (IE)"),
                new Label("Coordonator Știintific: Lect. univ. dr. Paul PAȘCU"));
        autorDetails.getChildren().forEach(n -> ((Label) n).setFont(Font.font("Inter", 14)));
        sAutor.getChildren().addAll(lblAutorT, autorDetails);

        // --- CUPRINS (Navigation) ---
        VBox tocBox = new VBox(10);
        tocBox.setPadding(new Insets(20));
        tocBox.setStyle(
                "-fx-background-color: #f1f5f9; -fx-background-radius: 10; -fx-border-color: #cbd5e1; -fx-border-radius: 10;");

        Label tocTitle = new Label("Cuprins");
        tocTitle.setFont(Font.font("Inter", FontWeight.BOLD, 18));

        FlowPane tocLinks = new FlowPane(25, 10);
        tocLinks.getChildren().addAll(
                creeazaAnchorLink("Misiune", sMisiune, mainContainer),
                creeazaAnchorLink("Arhitectură RNA", sArhitectura, mainContainer),
                creeazaAnchorLink("Logică Funcționare", sLogica, mainContainer),
                creeazaAnchorLink("Module Aplicație", sModule, mainContainer),
                creeazaAnchorLink("Ghid Quick Start", sGhid, mainContainer),
                creeazaAnchorLink("Stivă Tehnologică", sTech, mainContainer),
                creeazaAnchorLink("Informații Autor", sAutor, mainContainer));
        tocBox.getChildren().addAll(tocTitle, tocLinks);

        contentBody.getChildren().addAll(tocBox, sMisiune, sArhitectura, sLogica, sModule, sGhid, sTech, sAutor);
        mainContainer.getChildren().addAll(headerHero, contentBody);

        setContentArea(mainContainer);
        contentArea.setVvalue(0.0);
        contentArea.setHvalue(0.0);
    }

    private javafx.scene.control.Hyperlink creeazaAnchorLink(String text, Node target, Node container) {
        javafx.scene.control.Hyperlink link = new javafx.scene.control.Hyperlink(text);
        link.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 14px; -fx-underline: false;");
        link.setOnAction(e -> {
            if (target != null && contentArea != null) {
                // Calculăm poziția relativă a secțiunii față de containerul principal
                double targetY = target.localToScene(0, 0).getY() - container.localToScene(0, 0).getY();
                double totalHeight = container.getBoundsInLocal().getHeight();
                double viewportHeight = contentArea.getViewportBounds().getHeight();

                // Setăm noua valoare de scroll (proporție 0-1)
                if (totalHeight > viewportHeight) {
                    contentArea.setVvalue(targetY / (totalHeight - viewportHeight));
                }
            }
        });
        return link;
    }

    private VBox creeazaSectiuneDespre(String title, String contentText) {
        VBox section = new VBox(15);
        Label lblTitle = new Label(title);
        lblTitle.setFont(Font.font("Inter", FontWeight.BOLD, 22));
        lblTitle.setTextFill(Color.web("#1e293b"));

        Label lblContent = new Label(contentText);
        lblContent.setFont(Font.font("Inter", 15));
        lblContent.setTextFill(Color.web("#475569"));
        lblContent.setWrapText(true);
        lblContent.setLineSpacing(5);

        section.getChildren().addAll(lblTitle, lblContent);
        return section;
    }

    private VBox creeazaCardModul(String title, String description, String accentColor) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setPrefWidth(450);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 4); " +
                "-fx-border-color: " + accentColor + "; -fx-border-width: 0 0 0 4;");

        Label lblT = new Label(title);
        lblT.setFont(Font.font("Inter", FontWeight.BOLD, 16));
        lblT.setTextFill(Color.web("#1e293b"));

        Label lblD = new Label(description);
        lblD.setFont(Font.font("Inter", 13));
        lblD.setTextFill(Color.web("#64748b"));
        lblD.setWrapText(true);

        card.getChildren().addAll(lblT, lblD);
        return card;
    }

    // --- Persistence User ---
    private void incarcaUtilizatori() {
        File f = new File(USERS_FILE);
        if (!f.exists())
            return;
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";;"); // Simple delimiter
                if (parts.length >= 6) {
                    registeredUsers.add(new User(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void salveazaUtilizatori() {
        try (java.io.BufferedWriter bw = new java.io.BufferedWriter(new java.io.FileWriter(USERS_FILE))) {
            for (User u : registeredUsers) {
                bw.write(u.name + ";;" + u.surname + ";;" + u.username + ";;" + u.role + ";;" + u.company + ";;"
                        + u.password);
                bw.newLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- User Class ---
    public static class User {
        public String name, surname, username, role, company, password;

        public User(String n, String s, String u, String r, String c, String p) {
            this.name = n;
            this.surname = s;
            this.username = u;
            this.role = r;
            this.company = c;
            this.password = p;
        }
    }

    // --- FULLSCREEN CHART UTILITIES ---

    private void adaugaButonFullscreen(StackPane chartContainer, javafx.scene.chart.Chart chart, String title,
            javafx.scene.Node... extraTopBarElements) {
        Button btnExpand = new Button("\u26F6"); // ⛶ Square Four Corners (fullscreen icon)
        String normalStyle = "-fx-background-color: rgba(30,41,59,0.75); -fx-text-fill: white; -fx-font-size: 16px; " +
                "-fx-cursor: hand; -fx-background-radius: 8; -fx-padding: 4 10; -fx-min-width: 34; -fx-min-height: 28; "
                +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);";
        String hoverStyle = "-fx-background-color: rgba(59,130,246,0.95); -fx-text-fill: white; -fx-font-size: 16px; " +
                "-fx-cursor: hand; -fx-background-radius: 8; -fx-padding: 4 10; -fx-min-width: 34; -fx-min-height: 28; "
                +
                "-fx-effect: dropshadow(gaussian, rgba(59,130,246,0.5), 10, 0, 0, 3);";
        btnExpand.setStyle(normalStyle);
        btnExpand.setOnMouseEntered(ev -> btnExpand.setStyle(hoverStyle));
        btnExpand.setOnMouseExited(ev -> btnExpand.setStyle(normalStyle));

        Tooltip tip = new Tooltip("Deschide în Fullscreen");
        tip.setShowDelay(Duration.millis(300));
        Tooltip.install(btnExpand, tip);

        StackPane.setAlignment(btnExpand, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(btnExpand, new Insets(0, 15, 15, 0));
        btnExpand.setPickOnBounds(false);
        chartContainer.getChildren().add(btnExpand);

        btnExpand.setOnAction(ev -> {
            if (extraTopBarElements != null && extraTopBarElements.length > 0) {
                deschideChartFullscreen(chart, title, chartContainer, extraTopBarElements);
            } else {
                deschideChartFullscreen(chart, title, chartContainer);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void deschideChartFullscreen(javafx.scene.chart.Chart chart, String title, StackPane originalContainer,
            javafx.scene.Node... extraTopBarElements) {
        // Salvăm starea originală a graficului
        final double origMinH = chart.getMinHeight();
        final double origMaxH = chart.getMaxHeight();
        final double origPrefH = chart.getPrefHeight();
        final double origMinW = chart.getMinWidth();
        final double origMaxW = chart.getMaxWidth();
        final double origPrefW = chart.getPrefWidth();
        final boolean origAnimated = chart.getAnimated();
        final String origStyle = chart.getStyle();

        // Dezactivăm animațiile pentru fullscreen
        chart.setAnimated(false);

        // Resetăm constrângerile de dimensiune pentru a umple ecranul
        chart.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        chart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        chart.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);

        // Stage fullscreen (undecorated + maximized)
        javafx.stage.Stage fsStage = new javafx.stage.Stage();
        fsStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
        fsStage.setMaximized(true);
        fsStage.setTitle(title);

        // --- Top Bar ---
        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(12, 25, 12, 25));
        topBar.setStyle("-fx-background-color: linear-gradient(to right, #0f172a, #1e293b); " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 12, 0, 0, 3);");
        topBar.setMinHeight(56);

        ImageView fsIcon = incarcaIcon("artificial-intelligence.png", 28);
        Label lblTitle = new Label(title);
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        lblTitle.setTextFill(Color.WHITE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnExportPDF = new Button("\uD83D\uDCC4 Export PDF");
        String pdfNormal = "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 8 22; -fx-background-radius: 8; -fx-font-size: 13px; -fx-cursor: hand;";
        String pdfHover = "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 8 22; -fx-background-radius: 8; -fx-font-size: 13px; -fx-cursor: hand;";
        btnExportPDF.setStyle(pdfNormal);
        btnExportPDF.setOnMouseEntered(ev -> btnExportPDF.setStyle(pdfHover));
        btnExportPDF.setOnMouseExited(ev -> btnExportPDF.setStyle(pdfNormal));

        Button btnClose = new Button("\u2715  Închide (ESC)");
        String closeNormal = "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 8 22; -fx-background-radius: 8; -fx-font-size: 13px; -fx-cursor: hand;";
        String closeHover = "-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 8 22; -fx-background-radius: 8; -fx-font-size: 13px; -fx-cursor: hand;";
        btnClose.setStyle(closeNormal);
        btnClose.setOnMouseEntered(ev -> btnClose.setStyle(closeHover));
        btnClose.setOnMouseExited(ev -> btnClose.setStyle(closeNormal));

        if (fsIcon != null) {
            topBar.getChildren().addAll(fsIcon, lblTitle, spacer);
        } else {
            topBar.getChildren().addAll(lblTitle, spacer);
        }

        // Adăugăm elementele suplimentare (butoane specifice) dacă există
        if (extraTopBarElements != null && extraTopBarElements.length > 0) {
            topBar.getChildren().addAll(extraTopBarElements);
        }

        // La final adăugăm butoanele standard
        topBar.getChildren().addAll(btnExportPDF, btnClose);

        // Găsim butonul expand și reținem restul nodurilor (graficul propriu-zis +
        // overlay-uri de tooltip/markere)
        java.util.List<javafx.scene.Node> nodesToMove = new java.util.ArrayList<>();
        for (javafx.scene.Node n : new java.util.ArrayList<>(originalContainer.getChildren())) {
            if (n instanceof javafx.scene.control.Button
                    && ((javafx.scene.control.Button) n).getText().equals("\u26F6")) {
                continue; // Lăsăm butonul în urmă
            } else {
                nodesToMove.add(n);
            }
        }
        originalContainer.getChildren().removeAll(nodesToMove);

        // --- Zona Graficului ---
        StackPane chartArea = new StackPane();
        chartArea.getChildren().addAll(nodesToMove);
        chartArea.setPadding(new Insets(25));
        chartArea.setStyle("-fx-background-color: #f8fafc;");

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(chartArea);
        root.setStyle("-fx-background-color: #f8fafc;");

        Scene scene = new Scene(root);
        fsStage.setScene(scene);

        // --- Acțiune de Închidere (Restaurare completă) ---
        Runnable closeAction = () -> {
            chartArea.getChildren().removeAll(nodesToMove);
            // Restaurăm dimensiunile originale ale graficului
            chart.setMinHeight(origMinH);
            chart.setMaxHeight(origMaxH);
            chart.setPrefHeight(origPrefH);
            chart.setMinWidth(origMinW);
            chart.setMaxWidth(origMaxW);
            chart.setPrefWidth(origPrefW);
            chart.setAnimated(origAnimated);
            chart.setStyle(origStyle != null ? origStyle : "");

            // Readăugăm complet nodurile în containerul original (la început, în fața
            // butonului Expand)
            int insertIdx = 0;
            for (javafx.scene.Node n : nodesToMove) {
                if (!originalContainer.getChildren().contains(n)) {
                    originalContainer.getChildren().add(insertIdx++, n);
                }
            }
            fsStage.close();
        };

        btnClose.setOnAction(ev -> closeAction.run());
        scene.setOnKeyPressed(ev -> {
            if (ev.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                closeAction.run();
            }
        });
        fsStage.setOnCloseRequest(ev -> {
            ev.consume();
            closeAction.run();
        });

        // --- Export PDF din Fullscreen ---
        btnExportPDF.setOnAction(ev -> exportChartFullscreenPDF(chart, title, fsStage));

        fsStage.show();
    }

    private void exportChartFullscreenPDF(javafx.scene.chart.Chart chart, String title, javafx.stage.Stage ownerStage) {
        try {
            // Ajustăm temporar fonturile pentru vizibilitate bună pe PDF a axelor (16pt)
            javafx.scene.text.Font origXFont = null;
            javafx.scene.text.Font origYFont = null;
            javafx.scene.text.Font largeFont = javafx.scene.text.Font.font("Segoe UI",
                    javafx.scene.text.FontWeight.BOLD, 16);

            javafx.scene.chart.Axis xAxis = null;
            javafx.scene.chart.Axis yAxis = null;
            if (chart instanceof javafx.scene.chart.XYChart) {
                xAxis = ((javafx.scene.chart.XYChart) chart).getXAxis();
                yAxis = ((javafx.scene.chart.XYChart) chart).getYAxis();
            }

            if (xAxis != null) {
                origXFont = xAxis.getTickLabelFont();
                xAxis.setTickLabelFont(largeFont);
            }
            if (yAxis != null) {
                origYFont = yAxis.getTickLabelFont();
                yAxis.setTickLabelFont(largeFont);
            }

            // Forțăm relayout-ul pentru ca fontul mare să se aplice înainte de snapshot
            chart.applyCss();
            chart.layout();

            // 1. Snapshot la rezoluție ridicată (factor 3.0 ≈ 300 DPI)
            javafx.scene.SnapshotParameters snapParams = new javafx.scene.SnapshotParameters();
            snapParams.setTransform(new javafx.scene.transform.Scale(3.0, 3.0));
            snapParams.setFill(javafx.scene.paint.Color.WHITE);

            javafx.scene.image.WritableImage snapshot = chart.snapshot(snapParams, null);

            // Restaurăm fonturile originale imediat după terminarea snapshot-ului
            if (xAxis != null && origXFont != null) {
                xAxis.setTickLabelFont(origXFont);
            }
            if (yAxis != null && origYFont != null) {
                yAxis.setTickLabelFont(origYFont);
            }

            // 2. Convertim snapshot-ul în bytes PNG
            java.io.ByteArrayOutputStream byteOutput = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(snapshot, null), "png", byteOutput);
            byte[] imageBytes = byteOutput.toByteArray();

            // 3. Curățăm diacriticele pentru font-ul PDF implicit
            String safeTitle = title.replace("\u0103", "a").replace("\u0102", "A")
                    .replace("\u00e2", "a").replace("\u00c2", "A")
                    .replace("\u00ee", "i").replace("\u00ce", "I")
                    .replace("\u0219", "s").replace("\u0218", "S")
                    .replace("\u021b", "t").replace("\u021a", "T")
                    .replace("\u0163", "t").replace("\u0162", "T")
                    .replace("\u015f", "s").replace("\u015e", "S");

            // 4. Generăm numele fișierului
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "Chart_" + safeTitle.replaceAll("[^a-zA-Z0-9 ]", "").replaceAll("\\s+", "_") + "_"
                    + timestamp + ".pdf";

            // 5. Creăm PDF-ul (A4 Landscape)
            com.itextpdf.kernel.geom.PageSize pageSize = com.itextpdf.kernel.geom.PageSize.A4.rotate();

            PdfWriter writer = new PdfWriter(fileName);
            PdfDocument pdf = new PdfDocument(writer);
            pdf.setDefaultPageSize(pageSize);
            Document document = new Document(pdf);
            document.setMargins(30, 40, 30, 40);

            // Header branding
            document.add(new Paragraph("NEUROCAST")
                    .setFontSize(20).setBold()
                    .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(14, 51, 134)));
            document.add(new Paragraph("AI-Driven Decision Support for Market Excellence")
                    .setFontSize(10).setItalic().setMarginBottom(12));

            // Titlu grafic
            document.add(new Paragraph(safeTitle)
                    .setFontSize(18).setBold()
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                    .setMarginBottom(5));
            document.add(new Paragraph("Generat la: " + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date()))
                    .setFontSize(9).setItalic()
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                    .setMarginBottom(20));

            // 6. Imagine grafic - 80% din lățimea paginii, aspect ratio păstrat
            float availableWidth = pageSize.getWidth() - 80; // margini
            float targetWidth = availableWidth * 0.80f;

            com.itextpdf.layout.element.Image pdfImage = new com.itextpdf.layout.element.Image(
                    ImageDataFactory.create(imageBytes));

            float imgOrigWidth = pdfImage.getImageWidth();
            float imgOrigHeight = pdfImage.getImageHeight();
            float aspectRatio = imgOrigHeight / imgOrigWidth;
            float targetHeight = targetWidth * aspectRatio;

            // Verificăm să nu depășim înălțimea disponibilă
            float availableHeight = pageSize.getHeight() - 160; // margini + header
            if (targetHeight > availableHeight) {
                targetHeight = availableHeight;
                targetWidth = targetHeight / aspectRatio;
            }

            pdfImage.setWidth(targetWidth);
            pdfImage.setHeight(targetHeight);
            pdfImage.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);

            document.add(pdfImage);

            document.close();

            // 7. Confirmare și deschidere fișier
            new Alert(Alert.AlertType.INFORMATION, "PDF exportat cu succes:\n" + fileName).show();
            try {
                Desktop.getDesktop().open(new File(fileName));
            } catch (Exception ex) {
                // Ignorăm dacă nu se poate deschide automat
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Eroare la exportul PDF: " + ex.getMessage()).show();
        }
    }

    private void setContentArea(Node content) {
        contentArea.setContent(content);
        // Aplicam imediat tema curenta pe noul continut
        aplicaTemaRecursiv(content, isDarkMode);
        // Al doilea pass dupa ce JavaFX termina layout-ul lazy (ex. celule tabele,
        // viewport ScrollPane)
        PauseTransition delay = new PauseTransition(javafx.util.Duration.millis(80));
        delay.setOnFinished(e -> aplicaTemaRecursiv(content, isDarkMode));
        delay.play();
    }

    private void setDarkMode(boolean dark) {
        isDarkMode = dark;
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(DashboardApp.class);
        prefs.putBoolean("darkMode", dark);

        if (dark) {
            if (!contentArea.getStyleClass().contains("dark-theme")) {
                contentArea.getStyleClass().add("dark-theme");
            }
        } else {
            contentArea.getStyleClass().remove("dark-theme");
        }

        // Aplicam pe contentArea si tot ce e vizibil acum
        aplicaTemaRecursiv(contentArea, dark);

        // Aplicam pe toate cache-urile existente (chiar daca nu sunt vizibile)
        if (ecranSimulatorCache != null)
            aplicaTemaRecursiv(ecranSimulatorCache, dark);
        if (ecranTehnicCache != null)
            aplicaTemaRecursiv(ecranTehnicCache, dark);
        if (ecranAnalizaCache != null)
            aplicaTemaRecursiv(ecranAnalizaCache, dark);
        if (ecranPredictiiCache != null)
            aplicaTemaRecursiv(ecranPredictiiCache, dark);
        if (ecranRapoarteCache != null)
            aplicaTemaRecursiv(ecranRapoarteCache, dark);
        if (ecranGestiuneDateCache != null)
            aplicaTemaRecursiv(ecranGestiuneDateCache, dark);

        // Al doilea pass dupa layout lazy pentru noduri construite de JavaFX intern
        PauseTransition delay = new PauseTransition(javafx.util.Duration.millis(80));
        delay.setOnFinished(e -> {
            aplicaTemaRecursiv(contentArea, dark);
            if (ecranSimulatorCache != null)
                aplicaTemaRecursiv(ecranSimulatorCache, dark);
            if (ecranTehnicCache != null)
                aplicaTemaRecursiv(ecranTehnicCache, dark);
            if (ecranAnalizaCache != null)
                aplicaTemaRecursiv(ecranAnalizaCache, dark);
            if (ecranPredictiiCache != null)
                aplicaTemaRecursiv(ecranPredictiiCache, dark);
            if (ecranRapoarteCache != null)
                aplicaTemaRecursiv(ecranRapoarteCache, dark);
            if (ecranGestiuneDateCache != null)
                aplicaTemaRecursiv(ecranGestiuneDateCache, dark);
        });
        delay.play();
    }

    /**
     * Aplica sau revoca tema dark pe un nod si toti descendentii sai recursiv.
     * Foloseste getProperties() pentru a salva stilul/culorile originale o singura
     * data si a le restaura exact la revenire, evitand degradarea la toggle
     * repetat.
     */
    private void aplicaTemaRecursiv(Node node, boolean dark) {
        if (node == null)
            return;

        // Flag: daca nodul a fost tratat special, sarim handlerul generic de Region
        boolean handledSpecial = false;

        // --- TRATAMENT SPECIAL: Carduri portocalii din Simulator (Scenariu Simulat +
        // Date Actuale) ---
        if (node instanceof javafx.scene.layout.Region) {
            javafx.scene.layout.Region region = (javafx.scene.layout.Region) node;

            // 1. Card "Scenariu Simulat" (portocaliu → dark blue in dark mode)
            if (Boolean.TRUE.equals(region.getProperties().get("simulator-orange-card"))) {
                if (dark) {
                    region.setStyle(
                            "-fx-background-color: #1e293b; -fx-border-color: #3b82f6; -fx-border-width: 2; -fx-background-radius: 10; -fx-border-radius: 10;");
                } else {
                    region.setStyle(
                            "-fx-background-color: #fffbeb; -fx-border-color: #f59e0b; -fx-border-width: 2; -fx-background-radius: 10; -fx-border-radius: 10;");
                    // Forțăm restaurarea culorilor de text ale label-urilor din card
                    forceazaRestaurareLabeluri(region);
                }
                handledSpecial = true;
                // NU facem return — lăsăm recursia generală să proceseze copiii
            }

            // 2. Card "Cerinte fisier" din Gestiune (gri deschis → dark blue in dark mode)
            if (Boolean.TRUE.equals(region.getProperties().get("gestiune-info-card"))) {
                if (dark) {
                    region.setStyle("-fx-background-color: #1e293b; -fx-padding: 20; -fx-background-radius: 10;");
                } else {
                    region.setStyle("-fx-background-color: #ecf0f1; -fx-padding: 20; -fx-background-radius: 10;");
                    forceazaRestaurareLabeluri(region);
                }
                handledSpecial = true;
            }
        }

        // 3. Coloane evidențiate (Buget, Preț Med.) din tabelul Date Actuale Simulator
        // TableColumn nu este Node — trebuie tratat când întâlnim TableView-ul
        if (node instanceof javafx.scene.control.TableView) {
            @SuppressWarnings("rawtypes")
            javafx.scene.control.TableView table = (javafx.scene.control.TableView) node;

            // Fix pentru banda alba (filler) la table view header in dark mode
            javafx.application.Platform.runLater(() -> {
                for (Node n : table.lookupAll(".filler")) {
                    n.setStyle(dark ? "-fx-background-color: #1e293b;" : "");
                }
            });

            boolean columnsChanged = false;
            for (Object colObj : table.getColumns()) {
                if (colObj instanceof javafx.scene.control.TableColumn) {
                    @SuppressWarnings("rawtypes")
                    javafx.scene.control.TableColumn col = (javafx.scene.control.TableColumn) colObj;
                    if (Boolean.TRUE.equals(col.getProperties().get("highlight-column"))) {
                        if (dark) {
                            col.setStyle(
                                    "-fx-background-color: #1e3a5f; -fx-font-weight: bold; -fx-text-fill: #93c5fd;");
                        } else {
                            col.setStyle(
                                    "-fx-background-color: #fffbeb; -fx-font-weight: bold; -fx-text-fill: #d97706;");
                        }
                        columnsChanged = true;
                    }
                }
            }
            // Forțăm reîmprospătarea tabelului pentru a aplica stilurile modificate
            if (columnsChanged) {
                table.refresh();
                // Ștergem orice original_style salvat anterior pe headerele de coloane
                // pentru a preveni restaurarea greșită de către handlerul generic
                for (Node headerNode : table.lookupAll(".column-header")) {
                    headerNode.getProperties().remove("original_style");
                }
            }
        }

        // --- Tratam stilurile inline ale regiunilor (Pane, VBox, HBox, ScrollPane
        // etc.) ---
        if (node instanceof javafx.scene.layout.Region && !handledSpecial) {
            javafx.scene.layout.Region region = (javafx.scene.layout.Region) node;
            // Sarim nodurile interne ale TableView (headere de coloane) — stilul lor e
            // gestionat de handlerul de TableColumn
            if (region.getStyleClass().contains("column-header")
                    || region.getStyleClass().contains("column-header-background")) {
                // Nu modificam — stilul e controlat explicit de col.setStyle()
            } else if (Boolean.TRUE.equals(region.getProperties().get("preserve_bar_style"))) {
                // Sarim nodurile cu stiluri de bara (bar-fill) setate manual — nu le alternam
            } else {
                String currentStyle = region.getStyle();
                if (currentStyle == null)
                    currentStyle = "";

                if (dark) {
                    // Salvam stilul original NUMAI daca nu l-am salvat inca
                    if (!region.getProperties().containsKey("original_style")) {
                        region.getProperties().put("original_style", currentStyle);
                    }
                    String orig = (String) region.getProperties().get("original_style");
                    // Transformam NUMAI daca exista un stil original concret (non-gol)
                    // Daca orig e gol, inseamna ca nodul nu a avut stil inline —
                    // nu ii stergem culorile setate manual (ex. -fx-bar-fill pe bare de grafic)
                    if (orig != null && !orig.isEmpty()) {
                        String newStyle = orig;
                        newStyle = newStyle.replaceAll("-fx-background-color:\\s*white;?",
                                "-fx-background-color: #1e293b;");
                        newStyle = newStyle.replaceAll("-fx-background-color:\\s*#ffffff;?",
                                "-fx-background-color: #1e293b;");
                        newStyle = newStyle.replaceAll("-fx-background-color:\\s*#f4f7f6;?",
                                "-fx-background-color: #0f172a;");
                        newStyle = newStyle.replaceAll("-fx-background-color:\\s*#eff6ff;?",
                                "-fx-background-color: #1e293b;");
                        newStyle = newStyle.replaceAll("-fx-background-color:\\s*#f8fafc;?",
                                "-fx-background-color: #0f172a;");
                        newStyle = newStyle.replaceAll("-fx-background-color:\\s*#f1f5f9;?",
                                "-fx-background-color: #1e293b;");
                        newStyle = newStyle.replaceAll("-fx-background-color:\\s*#e2e8f0;?",
                                "-fx-background-color: #1e293b;");
                        newStyle = newStyle.replaceAll("-fx-background-color:\\s*#ecf0f1;?",
                                "-fx-background-color: #1e293b;");
                        newStyle = newStyle.replaceAll("-fx-background-color:\\s*#fffbeb;?",
                                "-fx-background-color: #1e293b;");
                        newStyle = newStyle.replaceAll("-fx-background-color:\\s*transparent;?",
                                "-fx-background-color: transparent;");
                        newStyle = newStyle.replaceAll("-fx-border-color:\\s*#e2e8f0;?", "-fx-border-color: #334155;");
                        newStyle = newStyle.replaceAll("-fx-border-color:\\s*#cbd5e1;?", "-fx-border-color: #334155;");
                        region.setStyle(newStyle);
                    }
                } else {
                    // Restauram stilul original NUMAI daca era non-gol
                    // Daca era gol, nu stergem stilul curent (bar fills etc.)
                    if (region.getProperties().containsKey("original_style")) {
                        String orig = (String) region.getProperties().get("original_style");
                        if (orig != null && !orig.isEmpty()) {
                            region.setStyle(orig);
                        }
                    }
                }
            }
        }

        // --- Tratam culorile de text ale Label-urilor ---
        if (node instanceof Label) {
            Label label = (Label) node;
            // Daca label-ul se afla intr-un card cu fundal fix (alb), NU schimbam culoarea
            // textului
            // altfel textul alb pe fundal alb devine invizibil
            if (!esteInCardCuFundalFix(label)) {
                javafx.scene.paint.Paint fill = label.getTextFill();
                if (dark) {
                    // Salvam culoarea originala NUMAI o singura data
                    if (!label.getProperties().containsKey("original_fill")) {
                        label.getProperties().put("original_fill", fill);
                    }
                    // Luam MEREU originalul pentru comparatie
                    javafx.scene.paint.Paint origFill = (javafx.scene.paint.Paint) label.getProperties()
                            .get("original_fill");
                    if (origFill instanceof Color) {
                        Color c = (Color) origFill;
                        // Orice text mai inchis decat 72% luminozitate devine alb-deschis
                        if (c.getBrightness() < 0.72 && !isAlbSauTransparent(c)) {
                            label.setTextFill(Color.web("#f1f5f9"));
                        }
                    }
                } else {
                    if (label.getProperties().containsKey("original_fill")) {
                        label.setTextFill((javafx.scene.paint.Paint) label.getProperties().get("original_fill"));
                    }
                }
            }
        }

        // --- Tratam culorile de text ale nodurilor Text (grafice, etichete axe) ---
        // IMPORTANT: Sarim LabeledText (nodul intern al Label-ului) al carui fill e
        // bound la textFill-ul parintelui
        if (node instanceof javafx.scene.text.Text && !(node.getClass().getSimpleName().equals("LabeledText"))) {
            javafx.scene.text.Text textNode = (javafx.scene.text.Text) node;
            // Verificare suplimentara: sarim daca proprietatea e bound (previne
            // RuntimeException)
            if (!textNode.fillProperty().isBound()) {
                javafx.scene.paint.Paint fill = textNode.getFill();
                if (dark) {
                    if (!textNode.getProperties().containsKey("original_fill")) {
                        textNode.getProperties().put("original_fill", fill);
                    }
                    javafx.scene.paint.Paint origFill = (javafx.scene.paint.Paint) textNode.getProperties()
                            .get("original_fill");
                    if (origFill instanceof Color) {
                        Color c = (Color) origFill;
                        if (c.getBrightness() < 0.72 && !isAlbSauTransparent(c)) {
                            textNode.setFill(Color.web("#f1f5f9"));
                        }
                    }
                } else {
                    if (textNode.getProperties().containsKey("original_fill")) {
                        textNode.setFill((javafx.scene.paint.Paint) textNode.getProperties().get("original_fill"));
                    }
                }
            }
        }

        // --- Recursie pe copii ---
        if (node instanceof javafx.scene.Parent) {
            javafx.scene.Parent parent = (javafx.scene.Parent) node;
            for (Node child : parent.getChildrenUnmodifiable()) {
                aplicaTemaRecursiv(child, dark);
            }
        }
        // --- Caz special: TabPane — continutul taburilor e incarcat lazy si poate
        // sa nu apara in arborele vizual pana cand tabul e selectat prima data.
        // Traversam tab.getContent() direct pentru a acoperi si taburile neaccesate.
        // ---
        if (node instanceof javafx.scene.control.TabPane) {
            for (javafx.scene.control.Tab tab : ((javafx.scene.control.TabPane) node).getTabs()) {
                if (tab.getContent() != null) {
                    aplicaTemaRecursiv(tab.getContent(), dark);
                }
            }
        }
    }

    /** Returneaza true daca culoarea e alba, aproape alba sau transparenta */
    private boolean isAlbSauTransparent(Color c) {
        return c.getOpacity() < 0.01
                || (c.getRed() > 0.85 && c.getGreen() > 0.85 && c.getBlue() > 0.85);
    }

    /**
     * Verifica daca un nod se afla intr-un container marcat cu 'fixed-bg-card'.
     * Aceste carduri au fundal alb fix — textul trebuie sa ramana inchis chiar si
     * in dark mode.
     */
    private boolean esteInCardCuFundalFix(Node node) {
        javafx.scene.Parent p = node.getParent();
        int depth = 0;
        while (p != null && depth < 6) { // Cautam maxim 6 nivele in sus
            if (Boolean.TRUE.equals(p.getProperties().get("fixed-bg-card"))) {
                return true;
            }
            p = p.getParent();
            depth++;
        }
        return false;
    }

    /**
     * Forțează restaurarea culorilor de text ale label-urilor dintr-un subtree
     * la culoarea lor originală. Folosit când se revine de la dark mode la light
     * mode
     * pentru carduri cu tratament special (ex: Scenariu Simulat, Cerințe fișier).
     * Label-urile create dinamic (ex: de slider listeners) pot să nu aibă
     * original_fill salvat,
     * așa că le resetăm la negru explicit.
     */
    private void forceazaRestaurareLabeluri(Node node) {
        if (node instanceof Label) {
            Label label = (Label) node;
            if (label.getProperties().containsKey("original_fill")) {
                label.setTextFill((javafx.scene.paint.Paint) label.getProperties().get("original_fill"));
            } else {
                // Label creat dinamic fara original_fill — resetam la negru
                label.setTextFill(Color.BLACK);
            }
            // Stergem proprietatea pentru un ciclu curat la următoarea comutare
            label.getProperties().remove("original_fill");
        }
        if (node instanceof javafx.scene.text.Text && !(node.getClass().getSimpleName().equals("LabeledText"))) {
            javafx.scene.text.Text textNode = (javafx.scene.text.Text) node;
            if (!textNode.fillProperty().isBound()) {
                if (textNode.getProperties().containsKey("original_fill")) {
                    textNode.setFill((javafx.scene.paint.Paint) textNode.getProperties().get("original_fill"));
                } else {
                    textNode.setFill(Color.BLACK);
                }
                textNode.getProperties().remove("original_fill");
            }
        }
        if (node instanceof javafx.scene.Parent) {
            for (Node child : ((javafx.scene.Parent) node).getChildrenUnmodifiable()) {
                forceazaRestaurareLabeluri(child);
            }
        }
    }

    // =====================================================================
    // === TUTORIAL INTERACTIV (7 PAȘI) ===
    // =====================================================================

    /**
     * Pornește tutorialul interactiv — creează overlay-ul semitransparent
     * și caseta de tutorial, apoi afișează primul pas.
     */
    private void pornesteTutorial() {
        tutorialStep = 0;

        // Overlay semitransparent (fără fundal fix, va fi desenat de
        // evidentiazaElemente)
        tutorialOverlay = new Pane();
        tutorialOverlay.setMouseTransparent(true); // Lăsăm click-urile să treacă la conținut

        // Caseta de tutorial
        tutorialCard = new VBox(12);
        tutorialCard.setMaxWidth(540);
        tutorialCard.setMaxHeight(Region.USE_PREF_SIZE);
        tutorialCard.setPadding(new Insets(28, 35, 22, 35));
        tutorialCard.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #ffffff, #f8fafc); "
                        + "-fx-background-radius: 18; "
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.40), 35, 0.15, 0, 10); "
                        + "-fx-border-color: #3b82f6; -fx-border-width: 0 0 4 0; -fx-border-radius: 18;");

        StackPane.setAlignment(tutorialCard, Pos.CENTER);

        centerStack.getChildren().addAll(tutorialOverlay, tutorialCard);

        afiseazaPassTutorial(0);
    }

    /**
     * Aplică "High-Contrast Contouring" (Spotlight) peste elementele vizate.
     * Desenează un fundal negru decupat (Shape.subtract) și adaugă contururi neon.
     */
    private void evidentiazaElemente(javafx.scene.Node... targetNodes) {
        // Curățăm overlay-ul
        tutorialOverlay.getChildren().clear();

        if (targetNodes == null || targetNodes.length == 0) {
            javafx.scene.shape.Rectangle fullDark = new javafx.scene.shape.Rectangle();
            fullDark.widthProperty().bind(centerStack.widthProperty());
            fullDark.heightProperty().bind(centerStack.heightProperty());
            fullDark.setFill(Color.rgb(0, 0, 0, 0.65));
            tutorialOverlay.getChildren().add(fullDark);
            return;
        }

        // Așteptăm așezarea layout-ului
        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(150));
        delay.setOnFinished(ev -> {
            tutorialOverlay.getChildren().clear();
            javafx.scene.shape.Shape background = new javafx.scene.shape.Rectangle(centerStack.getWidth(),
                    centerStack.getHeight());

            for (javafx.scene.Node node : targetNodes) {
                if (node == null)
                    continue;
                // Folosim getLayoutBounds() pentru a ignora drop shadow-urile care extind
                // artificial cutia
                javafx.geometry.Bounds boundsInScene = node.localToScene(node.getLayoutBounds());
                if (boundsInScene == null)
                    continue;
                javafx.geometry.Bounds boundsInStack = centerStack.sceneToLocal(boundsInScene);
                if (boundsInStack == null)
                    continue;

                double x = boundsInStack.getMinX();
                double y = boundsInStack.getMinY();
                double w = boundsInStack.getWidth();
                double h = boundsInStack.getHeight();

                javafx.geometry.Insets trim = (javafx.geometry.Insets) node.getProperties()
                        .get("tutorial-overlay-trim");
                if (trim != null) {
                    x += trim.getLeft();
                    y += trim.getTop();
                    w -= (trim.getLeft() + trim.getRight());
                    h -= (trim.getTop() + trim.getBottom());
                }

                double padding = 3; // Padding redus pentru a evita suprapunerile între elemente
                javafx.scene.shape.Rectangle cutout = new javafx.scene.shape.Rectangle(
                        x - padding,
                        y - padding,
                        w + padding * 2,
                        h + padding * 2);

                // Decupăm din background
                background = javafx.scene.shape.Shape.subtract(background, cutout);

                // Creăm conturul
                javafx.scene.shape.Rectangle contour = new javafx.scene.shape.Rectangle(
                        x - padding,
                        y - padding,
                        w + padding * 2,
                        h + padding * 2);
                contour.setFill(Color.TRANSPARENT);
                contour.setStroke(Color.web("#00f3ff")); // Neon blue
                contour.setStrokeWidth(4);
                contour.setStrokeType(javafx.scene.shape.StrokeType.OUTSIDE);
                contour.setArcWidth(10);
                contour.setArcHeight(10);

                tutorialOverlay.getChildren().add(contour);
            }

            background.setFill(Color.rgb(0, 0, 0, 0.65));
            tutorialOverlay.getChildren().add(0, background); // Adăugăm pe primul plan
        });
        delay.play();
    }

    /**
     * Afișează conținutul unui pas din tutorial.
     * Navighează automat la tab-ul corespunzător și actualizează caseta.
     */
    private void afiseazaPassTutorial(int step) {
        tutorialStep = step;
        tutorialCard.getChildren().clear();

        String titlu = "";
        String descriere = "";
        boolean showBack = step > 0;
        boolean isLast = (step == TUTORIAL_TOTAL_STEPS - 1);

        switch (step) {
            case 0: // Bun Venit
                titlu = "Bun venit în NeuroCast!";
                descriere = "Hai să facem un tur rapid pentru a descoperi cum poți prognoza "
                        + "veniturile firmei tale folosind Rețelele Neuronale Artificiale.\n\n"
                        + "La fiecare pas vei putea interacționa liber cu interfața!";
                afiseazaDashboard();
                activateazaButonSidebar(0);
                evidentiazaElemente();
                break;

            case 1: // Încărcarea Datelor
                titlu = "Încărcarea Datelor";
                descriere = "Totul începe cu datele tale. Încarcă istoricul vânzărilor "
                        + "dintr-un fișier Excel sau importă un model deja antrenat (.ser). \n\n"
                        + "➜ Explorează acest tab — vezi butoanele de import!";
                afiseazaEcranTehnic(0); // Gestiune Fișiere
                activateazaButonSidebar(4);
                evidentiazaElemente(tutorialBtnIncarca, tabelFisierePersistent);
                break;

            case 2: // Antrenarea Rețelei
                titlu = "Antrenarea Rețelei";
                descriere = "Dacă ai introdus un Excel nou, trebuie să antrenăm rețeaua.\n\n"
                        + "➜ Poți configura iterațiile și funcția de activare înainte de start.";
                afiseazaEcranTehnic(1); // Tab Antrenare Rețea
                activateazaButonSidebar(4);
                evidentiazaElemente(tutorialConfigBox);
                break;

            case 3: // Simulator Scenarii
                titlu = "Simulatorul de Scenarii";
                descriere = "Aici testezi scenarii! Ajustează sliderele (Marketing & Preț) "
                        + "şi urmăreşte pe grafic cum se modifică venitul estimat "
                        + "în funcție de strategia ta.\n\n"
                        + "➜ Trage un slider pentru a vedea predicția!";
                afiseazaEcranSimulator();
                activateazaButonSidebar(3);
                evidentiazaElemente(tutorialCardScenariu, tutorialBtnCalculeaza, tutorialCardRezultate,
                        tutorialGraficComparativ);
                break;

            case 4: // Goal Seeking / Predicții
                titlu = "Predicții Inteligente";
                descriere = "Ai un obiectiv clar de profit, dar nu știi ce buget să aloci? "
                        + "Această secțiune folosește modelul antrenat pentru a-ți calcula "
                        + "automat strategia optimă.\n\n"
                        + "➜ Introdu un obiectiv de vânzări și lasă rețeaua neuronală să calculeze!";
                afiseazaPredictii();
                activateazaButonSidebar(2);
                evidentiazaElemente(tutorialCalcPredictie, tutorialCalcGoal);
                break;

            case 5: // Dashboard Economic
                titlu = "Dashboard-ul Economic";
                descriere = "Explorează Dashboard-ul interactiv pentru a vizualiza grafic "
                        + "toți indicatorii economici și istoricul extras din fișierul tău Excel.\n\n"
                        + "➜ Selectează un an din dropdown pentru a filtra datele!";
                afiseazaDashboard();
                activateazaButonSidebar(0);
                evidentiazaElemente(tutorialComboAn);
                break;

            case 6: // Export
                titlu = "Exportul Datelor";
                descriere = "La final, transformă-ți munca într-un raport profesional. "
                        + "Generează un fișier PDF cu toate graficele, analizele și predicțiile, "
                        + "gata de prezentat.\n\n"
                        + "Ești gata să începi! Succes cu analiza ta!";
                afiseazaExport();
                activateazaButonSidebar(5);
                evidentiazaElemente(tutorialExportContainer, tutorialFormExport);
                break;
        }

        // --- Construim UI-ul casetei ---

        // Progress indicator text
        Label lblProgress = new Label("Pasul " + (step + 1) + " din " + TUTORIAL_TOTAL_STEPS);
        lblProgress.setFont(Font.font("Inter", FontWeight.NORMAL, 11));
        lblProgress.setTextFill(Color.web("#94a3b8"));

        // Progress bar vizual (segmente)
        HBox progressBar = new HBox(5);
        progressBar.setAlignment(Pos.CENTER_LEFT);
        progressBar.setPadding(new Insets(0, 0, 5, 0));
        for (int i = 0; i < TUTORIAL_TOTAL_STEPS; i++) {
            Region segment = new Region();
            double w = (i == step) ? 28 : 10;
            segment.setPrefSize(w, 5);
            segment.setMaxSize(w, 5);
            segment.setMinSize(w, 5);
            String color = (i <= step) ? "#3b82f6" : "#e2e8f0";
            segment.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 3;");
            progressBar.getChildren().add(segment);
        }

        // Titlu
        Label lblTitlu = new Label(titlu);
        lblTitlu.setFont(Font.font("Inter", FontWeight.BOLD, 21));
        lblTitlu.setTextFill(Color.web("#0f172a"));
        lblTitlu.setWrapText(true);

        // Separator decorativ sub titlu
        Region separatorLine = new Region();
        separatorLine.setPrefHeight(2);
        separatorLine.setMaxHeight(2);
        separatorLine.setMaxWidth(60);
        separatorLine.setStyle(
                "-fx-background-color: linear-gradient(to right, #3b82f6, #8b5cf6); -fx-background-radius: 1;");
        HBox separatorBox = new HBox(separatorLine);
        separatorBox.setPadding(new Insets(0, 0, 5, 0));

        // Descriere
        Label lblDescriere = new Label(descriere);
        lblDescriere.setFont(Font.font("Inter", FontWeight.NORMAL, 14));
        lblDescriere.setTextFill(Color.web("#475569"));
        lblDescriere.setWrapText(true);
        lblDescriere.setLineSpacing(3);

        // Butoane
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(12, 0, 0, 0));

        if (showBack) {
            Button btnBack = new Button("\u2190 Înapoi");
            btnBack.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #64748b; "
                            + "-fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; "
                            + "-fx-border-color: #cbd5e1; -fx-border-radius: 8; -fx-background-radius: 8; "
                            + "-fx-padding: 8 18;");
            btnBack.setOnMouseEntered(ev -> btnBack.setStyle(
                    "-fx-background-color: #f1f5f9; -fx-text-fill: #334155; "
                            + "-fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; "
                            + "-fx-border-color: #94a3b8; -fx-border-radius: 8; -fx-background-radius: 8; "
                            + "-fx-padding: 8 18;"));
            btnBack.setOnMouseExited(ev -> btnBack.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #64748b; "
                            + "-fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; "
                            + "-fx-border-color: #cbd5e1; -fx-border-radius: 8; -fx-background-radius: 8; "
                            + "-fx-padding: 8 18;"));
            btnBack.setOnAction(e -> afiseazaPassTutorial(step - 1));
            buttonBar.getChildren().add(btnBack);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        buttonBar.getChildren().add(spacer);

        // Buton Skip / Închide
        Button btnSkip = new Button(isLast ? "Închide Tutorialul" : "Omite Tutorialul");
        btnSkip.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #94a3b8; "
                        + "-fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 8 14; "
                        + "-fx-underline: true;");
        btnSkip.setOnMouseEntered(ev -> btnSkip.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #ef4444; "
                        + "-fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 8 14; "
                        + "-fx-underline: true;"));
        btnSkip.setOnMouseExited(ev -> btnSkip.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #94a3b8; "
                        + "-fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 8 14; "
                        + "-fx-underline: true;"));
        btnSkip.setOnAction(e -> inchideTutorial());
        buttonBar.getChildren().add(btnSkip);

        // Buton Next (doar dacă nu e ultimul pas)
        if (!isLast) {
            Button btnNext = new Button("Următorul \u2192");
            btnNext.setStyle(
                    "-fx-background-color: linear-gradient(to right, #3b82f6, #2563eb); "
                            + "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; "
                            + "-fx-cursor: hand; -fx-background-radius: 10; -fx-padding: 9 24;");
            btnNext.setOnMouseEntered(ev -> btnNext.setStyle(
                    "-fx-background-color: linear-gradient(to right, #2563eb, #1d4ed8); "
                            + "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; "
                            + "-fx-cursor: hand; -fx-background-radius: 10; -fx-padding: 9 24;"));
            btnNext.setOnMouseExited(ev -> btnNext.setStyle(
                    "-fx-background-color: linear-gradient(to right, #3b82f6, #2563eb); "
                            + "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; "
                            + "-fx-cursor: hand; -fx-background-radius: 10; -fx-padding: 9 24;"));
            btnNext.setOnAction(e -> afiseazaPassTutorial(step + 1));
            buttonBar.getChildren().add(btnNext);
        } else {
            // Ultimul pas — buton „Începe!" mai mare
            Button btnFinish = new Button("Începe Analiza");
            btnFinish.setStyle(
                    "-fx-background-color: linear-gradient(to right, #10b981, #059669); "
                            + "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; "
                            + "-fx-cursor: hand; -fx-background-radius: 10; -fx-padding: 10 28;");
            btnFinish.setOnMouseEntered(ev -> btnFinish.setStyle(
                    "-fx-background-color: linear-gradient(to right, #059669, #047857); "
                            + "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; "
                            + "-fx-cursor: hand; -fx-background-radius: 10; -fx-padding: 10 28;"));
            btnFinish.setOnMouseExited(ev -> btnFinish.setStyle(
                    "-fx-background-color: linear-gradient(to right, #10b981, #059669); "
                            + "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; "
                            + "-fx-cursor: hand; -fx-background-radius: 10; -fx-padding: 10 28;"));
            btnFinish.setOnAction(e -> inchideTutorial());
            buttonBar.getChildren().add(btnFinish);
        }

        tutorialCard.getChildren().addAll(
                lblProgress, progressBar, lblTitlu, separatorBox, lblDescriere, buttonBar);

        // Animație fade-in subtilă la schimbarea pasului
        tutorialCard.setOpacity(0);
        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(
                Duration.millis(350), tutorialCard);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    /**
     * Evidențiază vizual butonul din sidebar la indexul specificat.
     * Indexare: 0=Dashboard, 1=Analiza, 2=Predicții, 3=Simulator, 4=Gestiune,
     * 5=Export
     */
    private void activateazaButonSidebar(int index) {
        if (index < 0 || index >= sidebarButtons.size())
            return;

        // Reset toate butoanele
        for (Button b : sidebarButtons) {
            b.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; "
                            + "-fx-cursor: hand; -fx-background-radius: 0; -fx-border-color: transparent; "
                            + "-fx-border-width: 0 0 0 5;");
        }

        // Activează butonul curent
        Button active = sidebarButtons.get(index);
        active.setStyle(
                "-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-size: 14px; "
                        + "-fx-cursor: hand; -fx-background-radius: 0; -fx-border-color: #3498db; "
                        + "-fx-border-width: 0 0 0 5;");
    }

    /**
     * Închide tutorialul, elimină overlay-ul și salvează preferința
     * ca utilizatorul să nu mai vadă tutorialul la următoarea deschidere.
     */
    private void inchideTutorial() {
        // Animație fade-out elegantă
        javafx.animation.FadeTransition fadeOutCard = new javafx.animation.FadeTransition(
                Duration.millis(250), tutorialCard);
        fadeOutCard.setToValue(0);

        javafx.animation.FadeTransition fadeOutOverlay = new javafx.animation.FadeTransition(
                Duration.millis(350), tutorialOverlay);
        fadeOutOverlay.setToValue(0);

        fadeOutCard.setOnFinished(e -> {
            centerStack.getChildren().removeAll(tutorialOverlay, tutorialCard);
            tutorialOverlay = null;
            tutorialCard = null;

            // Salvăm că tutorialul a fost văzut
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences
                    .userNodeForPackage(DashboardApp.class);
            prefs.putBoolean("tutorialVazut", true);

            // Revenim la Dashboard
            afiseazaDashboard();
            activateazaButonSidebar(0);
        });

        fadeOutCard.play();
        fadeOutOverlay.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
