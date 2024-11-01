
package utils;

import io.qameta.allure.Allure;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class TestManager {
    // Singleton instance
    private static TestManager instance;

    // Test bilgileri
    private String nomScenario;
    private String nomEtape;
    private String statut;
    private String plateforme;
    private String resultatAttendu;
    private String resultatReel;
    private String url;
    private String messageErreur;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime dateExecution;

    // Koleksiyonlar
    private final List<TestManager> rapportsTests;
    private final Map<String, String> analysisResults;
    private final Map<String, Integer> stepPatterns;
    private final List<String> testSuggestions;

    // Sabitler
    private static final String EXCEL_REPORTS_DIR = "target/rapports-tests";
    private static final String PLATFORM = System.getProperty("platformName", "Web");

    // Yapay zeka analiz sabitleri
    private static final Map<String, String> ERROR_PATTERNS = new HashMap<>();
    private static final Map<String, List<String>> STEP_SUGGESTIONS = new HashMap<>();

    static {
        // Hata pattern'leri
        ERROR_PATTERNS.put("element_not_found",
                "• Vérifier si l'élément est présent dans la page\n" +
                        "• Augmenter le temps d'attente\n" +
                        "• Vérifier le sélecteur utilisé");

        ERROR_PATTERNS.put("click_error",
                "• Vérifier si l'élément est cliquable\n" +
                        "• Attendre que l'élément soit interactif\n" +
                        "• Vérifier s'il n'y a pas de popup qui bloque");

        ERROR_PATTERNS.put("timeout",
                "• Augmenter le timeout\n" +
                        "• Vérifier la connexion réseau\n" +
                        "• Vérifier la performance de la page");

        // Step önerileri
        STEP_SUGGESTIONS.put("page_accueil", Arrays.asList(
                "Je clique sur le bouton \"Recherche\"",
                "Je vérifie le menu principal",
                "Je vérifie le logo Radio France"
        ));

        STEP_SUGGESTIONS.put("recherche", Arrays.asList(
                "Je saisis un terme de recherche",
                "Je vérifie les résultats",
                "Je clique sur un résultat"
        ));
    }



    // Constructor'da başlangıç zamanını set edelim
    private TestManager() {
        rapportsTests = new ArrayList<>();
        analysisResults = new HashMap<>();
        stepPatterns = new HashMap<>();
        testSuggestions = new ArrayList<>();
        dateExecution = LocalDateTime.now();
        startTime = LocalDateTime.now();
        createReportsDirectory();
        this.plateforme = PLATFORM;
    }

    public static TestManager getInstance() {
        if (instance == null) {
            instance = new TestManager();
        }
        return instance;
    }

    private void createReportsDirectory() {
        File directory = new File(EXCEL_REPORTS_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    // Getter ve Setter metodları
    public String getNomScenario() {
        return nomScenario;
    }

    public void setNomScenario(String nomScenario) {
        this.nomScenario = nomScenario;
    }

    public String getNomEtape() {
        return nomEtape;
    }

    public void setNomEtape(String nomEtape) {
        this.nomEtape = nomEtape;
        updateStepPattern(nomEtape);
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
        if ("ECHEC".equalsIgnoreCase(statut)) {
            analyzeFailure();
        }
    }

    public String getPlateforme() {
        return plateforme;
    }

    public void setPlateforme(String plateforme) {
        this.plateforme = plateforme;
    }

    public String getResultatAttendu() {
        return resultatAttendu;
    }

    public void setResultatAttendu(String resultatAttendu) {
        this.resultatAttendu = resultatAttendu;
    }

    public String getResultatReel() {
        return resultatReel;
    }

    public void setResultatReel(String resultatReel) {
        this.resultatReel = resultatReel;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMessageErreur() {
        return messageErreur;
    }

    public void setMessageErreur(String messageErreur) {
        this.messageErreur = messageErreur;
        if (messageErreur != null) {
            analyzeError(messageErreur);
        }
    }

    // Test adımı ekleme ve analiz
    public void ajouterInfosTest(TestManager testInfo) {
        if (testInfo == null) return;

        boolean isDuplicate = rapportsTests.stream()
                .anyMatch(existing -> isSameStep(existing, testInfo));

        if (!isDuplicate) {
            testInfo.dateExecution = LocalDateTime.now();
            endTime = testInfo.dateExecution; // Her adımda bitiş zamanını güncelle
            rapportsTests.add(testInfo);
            updateAnalysis(testInfo);
            suggestNextSteps(testInfo);
        }
    }

    // Dinamik test analizi
    private void updateAnalysis(TestManager testInfo) {
        // Step pattern analizi
        stepPatterns.merge(testInfo.getNomEtape(), 1, Integer::sum);

        // Başarı oranı analizi
        int totalTests = rapportsTests.size();
        long successCount = rapportsTests.stream()
                .filter(t -> "REUSSI".equalsIgnoreCase(t.getStatut()))
                .count();

        double successRate = (successCount * 100.0) / totalTests;

        // Analiz sonuçlarını kaydet
        analysisResults.put("success_rate", String.format("%.1f%%", successRate));
        analysisResults.put("most_used_step", getMostUsedStep());
        analysisResults.put("test_duration", calculateTestDuration());
    }

    // Dinamik sonraki adım önerisi
    private void suggestNextSteps(TestManager currentTest) {
        String currentStep = currentTest.getNomEtape().toLowerCase();
        List<String> suggestions = new ArrayList<>();

        // Başarılı test akışlarından öğren
        Map<String, List<String>> successfulFlows = analyzeSuccessfulFlows();

        if (successfulFlows.containsKey(currentStep)) {
            suggestions.addAll(successfulFlows.get(currentStep));
        }

        // Önceden tanımlanmış öneriler
        if (STEP_SUGGESTIONS.containsKey(getStepType(currentStep))) {
            suggestions.addAll(STEP_SUGGESTIONS.get(getStepType(currentStep)));
        }

        // Test geçmişinden öneriler
        List<String> historicalNextSteps = findHistoricalNextSteps(currentStep);
        suggestions.addAll(historicalNextSteps);

        // Önerileri kaydet
        testSuggestions.clear();
        testSuggestions.addAll(suggestions.stream()
                .distinct()
                .limit(3)
                .collect(Collectors.toList()));
    }

    // Test akışı analizi
    private Map<String, List<String>> analyzeSuccessfulFlows() {
        Map<String, List<String>> flows = new HashMap<>();
        List<TestManager> successfulTests = rapportsTests.stream()
                .filter(t -> "REUSSI".equalsIgnoreCase(t.getStatut()))
                .collect(Collectors.toList());

        for (int i = 0; i < successfulTests.size() - 1; i++) {
            String currentStep = successfulTests.get(i).getNomEtape().toLowerCase();
            String nextStep = successfulTests.get(i + 1).getNomEtape();

            flows.computeIfAbsent(currentStep, k -> new ArrayList<>()).add(nextStep);
        }

        return flows;
    }

    // Geçmiş test adımlarından öneriler
    private List<String> findHistoricalNextSteps(String currentStep) {
        List<String> nextSteps = new ArrayList<>();
        boolean foundCurrent = false;

        for (TestManager test : rapportsTests) {
            if (foundCurrent) {
                nextSteps.add(test.getNomEtape());
                foundCurrent = false;
            }
            if (test.getNomEtape().toLowerCase().equals(currentStep)) {
                foundCurrent = true;
            }
        }

        return nextSteps;
    }

    // Hata analizi ve öneriler
    private void analyzeFailure() {
        String stepType = getStepType(nomEtape);
        String errorType = getErrorType(messageErreur);

        StringBuilder analysis = new StringBuilder();
        analysis.append("\n🔍 Analyse d'Échec:\n");
        analysis.append("Type d'étape: ").append(stepType).append("\n");
        analysis.append("Type d'erreur: ").append(errorType).append("\n");

        // Hata önerileri
        if (ERROR_PATTERNS.containsKey(errorType)) {
            analysis.append("\nSuggestions:\n").append(ERROR_PATTERNS.get(errorType));
        }

        // Başarılı örneklerden öğren
        List<TestManager> similarSuccessfulTests = findSimilarSuccessfulTests(stepType);
        if (!similarSuccessfulTests.isEmpty()) {
            analysis.append("\nExemples réussis:\n");
            similarSuccessfulTests.forEach(t ->
                    analysis.append("• ").append(t.getNomEtape())
                            .append(" (").append(t.getResultatReel()).append(")\n")
            );
        }

        analysisResults.put("failure_analysis", analysis.toString());
    }

    // Benzer başarılı testleri bul
    private List<TestManager> findSimilarSuccessfulTests(String stepType) {
        return rapportsTests.stream()
                .filter(t -> "REUSSI".equalsIgnoreCase(t.getStatut()))
                .filter(t -> getStepType(t.getNomEtape()).equals(stepType))
                .limit(3)
                .collect(Collectors.toList());
    }

    // Step tipini belirle
    private String getStepType(String step) {
        step = step.toLowerCase();
        if (step.contains("page") && step.contains("accueil")) return "page_accueil";
        if (step.contains("recherche")) return "recherche";
        if (step.contains("clic")) return "click";
        if (step.contains("verif")) return "verification";
        return "other";
    }

    // Hata tipini belirle
    private String getErrorType(String error) {
        if (error == null) return "unknown";
        error = error.toLowerCase();
        if (error.contains("element") && error.contains("not found")) return "element_not_found";
        if (error.contains("click")) return "click_error";
        if (error.contains("timeout")) return "timeout";
        return "unknown";
    }

    // Test süresini hesapla
    private String calculateTestDuration() {
        if (rapportsTests.isEmpty()) return "0s";

        TestManager firstTest = rapportsTests.get(0);
        TestManager lastTest = rapportsTests.get(rapportsTests.size() - 1);

        long seconds = java.time.Duration.between(
                firstTest.dateExecution,
                lastTest.dateExecution
        ).getSeconds();

        return String.format("%ds", seconds);
    }

    // En çok kullanılan adımı bul
    private String getMostUsedStep() {
        return stepPatterns.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Aucun");
    }

    // Önerileri al
    public List<String> getTestSuggestions() {
        return new ArrayList<>(testSuggestions);
    }

    // Excel raporu oluştur
    public void genererRapport(String nomRapport) {
        String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String fileName = String.format("%s/%s_%s.xlsx", EXCEL_REPORTS_DIR, nomRapport, timeStamp);

        try (Workbook workbook = new XSSFWorkbook()) {
            // Test sonuçları sayfası
            createTestResultsSheet(workbook.createSheet("Résultats des Tests"));

            // Analiz sayfası
            createAnalysisSheet(workbook.createSheet("Analyse"));

            // Öneriler sayfası
            createSuggestionsSheet(workbook.createSheet("Suggestions"));

            // Kaydet
            try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
                workbook.write(outputStream);
                System.out.println("Rapport généré: " + fileName);
            }
        } catch (IOException e) {
            System.err.println("Erreur rapport: " + e.getMessage());
        }
    }

    // Excel sayfalarını oluşturma metodları
    // Excel rapor oluşturma metodunu güncelleyelim
    private void createTestResultsSheet(Sheet sheet) {
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        CellStyle successStyle = createSuccessStyle(sheet.getWorkbook());
        CellStyle failureStyle = createFailureStyle(sheet.getWorkbook());

        // Başlık satırı
        Row headerRow = sheet.createRow(0);
        String[] columns = {
                "Scénario", "Étape", "Statut", "Plateforme",
                "Résultat Attendu", "Résultat Réel", "URL",
                "Message d'Erreur", "Date d'Exécution", "Durée"
        };

        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 6000);
        }

        // Test verileri
        int rowNum = 1;
        LocalDateTime previousTime = startTime;

        for (TestManager info : rapportsTests) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(info.getNomScenario() != null ? info.getNomScenario() : "");
            row.createCell(1).setCellValue(info.getNomEtape() != null ? info.getNomEtape() : "");

            Cell statutCell = row.createCell(2);
            statutCell.setCellValue(info.getStatut() != null ? info.getStatut() : "");

            if ("REUSSI".equalsIgnoreCase(info.getStatut())) {
                statutCell.setCellStyle(successStyle);
            } else if ("ECHEC".equalsIgnoreCase(info.getStatut())) {
                statutCell.setCellStyle(failureStyle);
            }

            row.createCell(3).setCellValue(info.getPlateforme() != null ? info.getPlateforme() : "");

            // Beklenen sonuç ve URL'yi zorunlu olarak ekle
            String expectedResult = info.getResultatAttendu();
            if (expectedResult == null || expectedResult.trim().isEmpty()) {
                expectedResult = "Vérification de " + info.getNomEtape();
            }
            row.createCell(4).setCellValue(expectedResult);

            row.createCell(5).setCellValue(info.getResultatReel() != null ? info.getResultatReel() : "");

            // URL'yi zorunlu olarak ekle
            String url = info.getUrl();
            if (url == null || url.trim().isEmpty()) {
                url = "https://www.radiofrance.fr/";  // Varsayılan URL
            }
            row.createCell(6).setCellValue(url);

            row.createCell(7).setCellValue(info.getMessageErreur() != null ? info.getMessageErreur() : "");
            row.createCell(8).setCellValue(
                    info.dateExecution.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );

            // Süre hesaplama
            Duration duration = Duration.between(previousTime, info.dateExecution);
            long seconds = duration.getSeconds();
            row.createCell(9).setCellValue(String.format("%ds", seconds));
            previousTime = info.dateExecution;
        }
    }
    private void createAnalysisSheet(Sheet sheet) {
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        int rowNum = 0;

        // Genel İstatistikler
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Statistiques Générales");
        titleCell.setCellStyle(headerStyle);

        rowNum = addAnalysisSection(sheet, rowNum, "Taux de Réussite", analysisResults.get("success_rate"));
        rowNum = addAnalysisSection(sheet, rowNum, "Étape la Plus Utilisée", analysisResults.get("most_used_step"));
        rowNum = addAnalysisSection(sheet, rowNum, "Durée Totale", analysisResults.get("test_duration"));

        rowNum++; // Boş satır

        // Hata Analizi
        if (analysisResults.containsKey("failure_analysis")) {
            Row analysisTitle = sheet.createRow(rowNum++);
            Cell analysisTitleCell = analysisTitle.createCell(0);
            analysisTitleCell.setCellValue("Analyse des Échecs");
            analysisTitleCell.setCellStyle(headerStyle);

            String[] analysisLines = analysisResults.get("failure_analysis").split("\n");
            for (String line : analysisLines) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(line);
            }
        }

        sheet.setColumnWidth(0, 15000);
        sheet.setColumnWidth(1, 10000);
    }

    private void createSuggestionsSheet(Sheet sheet) {
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        int rowNum = 0;

        // Başlık
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Suggestions d'Amélioration");
        titleCell.setCellStyle(headerStyle);

        rowNum++; // Boş satır

        // Test önerileri
        if (!testSuggestions.isEmpty()) {
            Row suggestionsTitle = sheet.createRow(rowNum++);
            suggestionsTitle.createCell(0).setCellValue("Prochaines Étapes Suggérées:");

            for (String suggestion : testSuggestions) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue("• " + suggestion);
            }
        }

        rowNum++; // Boş satır

        // Pattern analizi
        Row patternsTitle = sheet.createRow(rowNum++);
        patternsTitle.createCell(0).setCellValue("Patterns de Test Identifiés:");

        for (Map.Entry<String, Integer> pattern : stepPatterns.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(pattern.getKey());
            row.createCell(1).setCellValue(pattern.getValue() + " fois");
        }

        sheet.setColumnWidth(0, 15000);
        sheet.setColumnWidth(1, 5000);
    }

    private int addAnalysisSection(Sheet sheet, int startRow, String title, String value) {
        Row row = sheet.createRow(startRow);
        row.createCell(0).setCellValue(title);
        row.createCell(1).setCellValue(value != null ? value : "N/A");
        return startRow + 1;
    }

    // Excel stil metodları
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createSuccessStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createFailureStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    // Test adımlarının aynı olup olmadığını kontrol et
    private boolean isSameStep(TestManager existing, TestManager newInfo) {
        return Objects.equals(existing.getNomEtape(), newInfo.getNomEtape()) &&
                Objects.equals(existing.getNomScenario(), newInfo.getNomScenario()) &&
                Objects.equals(existing.getUrl(), newInfo.getUrl());
    }
    // Step pattern'lerini güncelle
    private void updateStepPattern(String stepName) {
        if (stepName != null) {
            stepPatterns.merge(stepName, 1, Integer::sum);

            // En sık kullanılan step'leri analiz et
            if (stepPatterns.get(stepName) > 5) {
                analysisResults.put("frequent_step",
                        "Step '" + stepName + "' is used frequently: " +
                                stepPatterns.get(stepName) + " times");
            }
        }
    }

    // Hata analizi yap
    private void analyzeError(String error) {
        if (error == null) return;

        // Hata tipini belirle
        String errorType = "unknown";
        if (error.toLowerCase().contains("element")) {
            errorType = "element_not_found";
        } else if (error.toLowerCase().contains("timeout")) {
            errorType = "timeout";
        } else if (error.toLowerCase().contains("click")) {
            errorType = "click_error";
        }

        // Hata analizini kaydet
        StringBuilder analysis = new StringBuilder();
        analysis.append("Type d'erreur: ").append(errorType).append("\n");

        // Öneriler ekle
        if (ERROR_PATTERNS.containsKey(errorType)) {
            analysis.append("Suggestions:\n")
                    .append(ERROR_PATTERNS.get(errorType));
        }

        // Benzer başarılı testleri bul
        List<TestManager> similarSuccessfulTests = findSimilarSuccessfulTests(getStepType(nomEtape));
        if (!similarSuccessfulTests.isEmpty()) {
            analysis.append("\nExemples de tests réussis similaires:\n");
            similarSuccessfulTests.forEach(t ->
                    analysis.append("• ").append(t.getNomEtape())
                            .append(" (").append(t.getResultatReel())
                            .append(")\n")
            );
        }

        // Analiz sonucunu kaydet
        analysisResults.put("error_analysis", analysis.toString());

        // Hata sıklığını takip et
        String finalErrorType = errorType;
        stepPatterns.compute(
                "error_" + errorType,
                (k, v) -> v == null ? 1 : v + 1
        );

        // Sık tekrarlanan hatalar için uyarı
        if (stepPatterns.get("error_" + errorType) > 3) {
            analysisResults.put("recurring_error",
                    "Attention: L'erreur '" + errorType +
                            "' s'est produite plusieurs fois. Une révision du test peut être nécessaire.");
        }
    }

    // Yardımcı metodlar
    private List<String> getStepHistory(String currentStep) {
        return rapportsTests.stream()
                .map(TestManager::getNomEtape)
                .filter(step -> !step.equals(currentStep))
                .distinct()
                .collect(Collectors.toList());
    }

    private Map<String, Integer> getErrorStatistics() {
        return stepPatterns.entrySet().stream()
                .filter(e -> e.getKey().startsWith("error_"))
                .collect(Collectors.toMap(
                        e -> e.getKey().replace("error_", ""),
                        Map.Entry::getValue
                ));
    }

    public void loadConfigurationProperties() {
        Properties properties = new Properties();
        String configPath = System.getProperty("user.dir") + "/config/configuration.properties";

        try (FileInputStream input = new FileInputStream(configPath)) {
            properties.load(input);

            // Platform bilgisini al
            String platformName = properties.getProperty("platformName", "Web");
            this.plateforme = platformName;

            // Environment dosyası oluştur
            File allureResultsDir = new File("target/allure-results");
            if (!allureResultsDir.exists()) {
                allureResultsDir.mkdirs();
            }

            // environment.properties dosyasını oluştur
            File envFile = new File(allureResultsDir, "environment.properties");
            try (FileOutputStream output = new FileOutputStream(envFile)) {
                Properties envProperties = new Properties();
                envProperties.setProperty("Platform", platformName);
                envProperties.setProperty("Browser", properties.getProperty("browser", "chrome"));
                envProperties.setProperty("Environment", properties.getProperty("environment", "QA"));
                envProperties.setProperty("Test Suite", properties.getProperty("testSuite", "Regression"));
                envProperties.store(output, "Allure Environment Properties");
            }

            // Allure parametrelerini de ekle
            Allure.parameter("Platform", platformName);
            Allure.parameter("Browser", properties.getProperty("browser", "chrome"));
            Allure.parameter("Environment", properties.getProperty("environment", "QA"));
            Allure.parameter("Test Suite", properties.getProperty("testSuite", "Regression"));

            System.out.println("Configuration loaded - Platform: " + platformName);
        } catch (IOException e) {
            System.err.println("Error loading configuration.properties: " + e.getMessage());
            setDefaultEnvironment();
        }
    }

    private void setDefaultEnvironment() {
        this.plateforme = "Web";
        try {
            File allureResultsDir = new File("target/allure-results");
            if (!allureResultsDir.exists()) {
                allureResultsDir.mkdirs();
            }

            File envFile = new File(allureResultsDir, "environment.properties");
            try (FileOutputStream output = new FileOutputStream(envFile)) {
                Properties envProperties = new Properties();
                envProperties.setProperty("Platform", "Web");
                envProperties.setProperty("Browser", "chrome");
                envProperties.setProperty("Environment", "QA");
                envProperties.setProperty("Test Suite", "Regression");
                envProperties.store(output, "Allure Environment Properties");
            }
        } catch (IOException e) {
            System.err.println("Error creating default environment.properties: " + e.getMessage());
        }

        Allure.parameter("Platform", "Web");
        Allure.parameter("Browser", "chrome");
        Allure.parameter("Environment", "QA");
        Allure.parameter("Test Suite", "Regression");
    }
    // Test önerileri güncelle
    private void updateTestSuggestions() {
        List<String> suggestions = new ArrayList<>();

        // Son başarılı adımlardan öneriler
        rapportsTests.stream()
                .filter(t -> "REUSSI".equalsIgnoreCase(t.getStatut()))
                .map(TestManager::getNomEtape)
                .distinct()
                .limit(3)
                .forEach(suggestions::add);

        // Hata istatistiklerine göre öneriler
        Map<String, Integer> errorStats = getErrorStatistics();
        if (!errorStats.isEmpty()) {
            suggestions.add("Vérifier les erreurs fréquentes: " +
                    errorStats.entrySet().stream()
                            .map(e -> e.getKey() + " (" + e.getValue() + " fois)")
                            .collect(Collectors.joining(", ")));
        }

        testSuggestions.clear();
        testSuggestions.addAll(suggestions);
    }
}


