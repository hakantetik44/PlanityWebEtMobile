
        package utils;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionRequest;
import org.json.JSONObject;
import org.json.JSONArray;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.net.http.*;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;

public class TestGenerator {
    private Map<String, List<String>> availableSteps;
    private static final String STEPDEFS_PACKAGE = "stepdefinitions";
    private final String OPENAI_API_KEY;
    private final OpenAiService openAiService;
    private final Map<String, Double> stepScores = new HashMap<>();
    private JSONObject testHistory;

    public TestGenerator(String openAiKey) {
        this.OPENAI_API_KEY = openAiKey;
        this.openAiService = new OpenAiService(OPENAI_API_KEY);
        this.availableSteps = new HashMap<>();
        this.testHistory = new JSONObject();
        scanAvailableSteps();
        initializeAIModel();
        loadTestHistory();
    }

    private void scanAvailableSteps() {
        try {
            Reflections reflections = new Reflections(STEPDEFS_PACKAGE, new MethodAnnotationsScanner());

            // Given steps
            processAnnotatedMethods(reflections.getMethodsAnnotatedWith(Given.class), Given.class, "Given");

            // When steps
            processAnnotatedMethods(reflections.getMethodsAnnotatedWith(When.class), When.class, "When");

            // Then steps
            processAnnotatedMethods(reflections.getMethodsAnnotatedWith(Then.class), Then.class, "Then");

            // And steps
            processAnnotatedMethods(reflections.getMethodsAnnotatedWith(And.class), And.class, "And");

            // Her step için başlangıç skoru ata
            availableSteps.values().stream()
                    .flatMap(List::stream)
                    .forEach(step -> stepScores.put(step, 1.0));

        } catch (Exception e) {
            System.err.println("Erreur lors du scan des steps: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private <T extends java.lang.annotation.Annotation> void processAnnotatedMethods(
            Set<Method> methods, Class<T> annotationClass, String stepType) {
        methods.forEach(method -> {
            T annotation = method.getAnnotation(annotationClass);
            try {
                Method valueMethod = annotationClass.getMethod("value");
                String stepDefinition = (String) valueMethod.invoke(annotation);
                availableSteps.computeIfAbsent(stepType, k -> new ArrayList<>())
                        .add(stepDefinition);
            } catch (Exception e) {
                System.err.println("Erreur lors du traitement de l'annotation " + stepType + ": " + e.getMessage());
            }
        });
    }

    private void initializeAIModel() {
        try {
            StringBuilder context = new StringBuilder();
            context.append("Context pour la génération de tests Cucumber:\n\n");
            context.append("1. Best Practices:\n");
            context.append("- Utiliser des steps atomiques\n");
            context.append("- Éviter les dépendances entre scénarios\n");
            context.append("- Suivre le pattern Given-When-Then\n\n");
            context.append("2. Steps disponibles:\n");

            availableSteps.forEach((type, steps) -> {
                context.append(type).append(":\n");
                steps.forEach(step -> context.append("  - ").append(step).append("\n"));
            });

            CompletionRequest request = CompletionRequest.builder()
                    .model("gpt-3.5-turbo")
                    .prompt(context.toString())
                    .maxTokens(500)
                    .temperature(0.7)
                    .build();

            openAiService.createCompletion(request);

        } catch (Exception e) {
            System.err.println("Erreur d'initialisation du modèle IA: " + e.getMessage());
        }
    }

    public void generateFeatureFileWithAI(String description) {
        try {
            // AI önerisi al
            String aiSuggestion = getAISuggestion(description);

            // Senaryoyu geliştir
            String enhancedScenario = enhanceScenarioWithAI(aiSuggestion);

            // Kalite kontrolü yap
            if (validateScenarioQuality(enhancedScenario)) {
                // Feature dosyasını kaydet
                saveFeatureFile(description, enhancedScenario);

                // Metrikleri hesapla ve kaydet
                Map<String, Double> metrics = calculateMetrics(enhancedScenario);
                saveMetrics(metrics);

                // Test geçmişini güncelle
                updateTestHistory(description, enhancedScenario, metrics);
            } else {
                System.out.println("Le scénario généré ne répond pas aux critères de qualité. Génération d'une nouvelle version...");
                generateFeatureFileWithAI(description); // Recursive call for new attempt
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la génération du feature file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getAISuggestion(String description) {
        String prompt = String.format(
                "Générer un scénario Cucumber en français pour: %s\n" +
                        "Utilisez uniquement les steps disponibles suivants:\n%s\n" +
                        "Respectez les meilleures pratiques Cucumber et assurez une bonne couverture fonctionnelle.",
                description,
                formatAvailableSteps()
        );

        CompletionRequest request = CompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .prompt(prompt)
                .maxTokens(500)
                .temperature(0.7)
                .build();

        return openAiService.createCompletion(request)
                .getChoices().get(0).getText();
    }

    private String enhanceScenarioWithAI(String baseScenario) {
        String prompt = String.format(
                "Améliorez ce scénario Cucumber avec:\n" +
                        "1. Des données de test pertinentes\n" +
                        "2. Des validations robustes\n" +
                        "3. Des commentaires explicatifs\n" +
                        "4. Une meilleure lisibilité\n\n" +
                        "Scénario original:\n%s",
                baseScenario
        );

        CompletionRequest request = CompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .prompt(prompt)
                .maxTokens(500)
                .temperature(0.5)
                .build();

        return openAiService.createCompletion(request)
                .getChoices().get(0).getText();
    }

    private boolean validateScenarioQuality(String scenario) {
        Map<String, Double> metrics = calculateMetrics(scenario);

        // Minimum kalite kriterleri
        return metrics.get("coverage") >= 0.7 &&
                metrics.get("complexity") <= 0.8 &&
                metrics.get("maintainability") >= 0.6;
    }

    private Map<String, Double> calculateMetrics(String scenario) {
        Map<String, Double> metrics = new HashMap<>();

        // Test coverage
        metrics.put("coverage", calculateCoverage(scenario));

        // Complexity
        metrics.put("complexity", calculateComplexity(scenario));

        // Maintainability
        metrics.put("maintainability", calculateMaintainability(scenario));

        return metrics;
    }

    private double calculateCoverage(String scenario) {
        long usedSteps = availableSteps.values().stream()
                .flatMap(List::stream)
                .filter(step -> scenario.contains(step))
                .count();

        long totalSteps = availableSteps.values().stream()
                .mapToLong(List::size)
                .sum();

        return (double) usedSteps / totalSteps;
    }

    private double calculateComplexity(String scenario) {
        int stepCount = scenario.split("\n").length;
        int dataCount = countTestData(scenario);
        return (stepCount * 0.7 + dataCount * 0.3) / 10.0;
    }

    private int countTestData(String scenario) {
        return (int) scenario.chars().filter(ch -> ch == '"').count() / 2;
    }

    private double calculateMaintainability(String scenario) {
        int lineCount = scenario.split("\n").length;
        int commentCount = (int) Arrays.stream(scenario.split("\n"))
                .filter(line -> line.trim().startsWith("#"))
                .count();

        return Math.min(1.0, (commentCount * 0.3) / lineCount + 0.4);
    }

    private void updateTestHistory(String description, String scenario, Map<String, Double> metrics) {
        JSONObject testCase = new JSONObject();
        testCase.put("timestamp", System.currentTimeMillis());
        testCase.put("description", description);
        testCase.put("scenario", scenario);
        testCase.put("metrics", new JSONObject(metrics));

        testHistory.append("tests", testCase);
        saveTestHistory();
    }

    private void loadTestHistory() {
        Path historyPath = Paths.get("target/test-history.json");
        try {
            if (Files.exists(historyPath)) {
                testHistory = new JSONObject(Files.readString(historyPath));
            } else {
                testHistory = new JSONObject().put("tests", new JSONArray());
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'historique: " + e.getMessage());
            testHistory = new JSONObject().put("tests", new JSONArray());
        }
    }

    private void saveTestHistory() {
        try {
            Path historyPath = Paths.get("target/test-history.json");
            Files.writeString(historyPath, testHistory.toString(2));
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde de l'historique: " + e.getMessage());
        }
    }

    private void saveMetrics(Map<String, Double> metrics) {
        try {
            Path metricsPath = Paths.get("target/test-metrics.json");
            JSONObject json = new JSONObject(metrics);
            Files.writeString(metricsPath, json.toString(2));
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde des métriques: " + e.getMessage());
        }
    }

    private String formatAvailableSteps() {
        StringBuilder sb = new StringBuilder();
        availableSteps.forEach((type, steps) -> {
            sb.append(type).append(":\n");
            steps.forEach(step -> sb.append("  - ").append(step).append("\n"));
        });
        return sb.toString();
    }

    private void saveFeatureFile(String description, String content) {
        try {
            Path featuresDir = Paths.get("src/test/resources/features");
            Files.createDirectories(featuresDir);

            String fileName = description.toLowerCase()
                    .replaceAll("[^a-z0-9]", "_")
                    .replaceAll("_+", "_")
                    + ".feature";

            Path filePath = featuresDir.resolve(fileName);
            Files.writeString(filePath, content);

            System.out.println("Feature file créé: " + filePath);
            System.out.println("\nContenu du fichier:\n" + content);

        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde du feature file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void analyzeTestHistory() {
        JSONArray tests = testHistory.getJSONArray("tests");
        Map<String, Double> averageMetrics = new HashMap<>();
        int totalTests = tests.length();

        tests.forEach(test -> {
            JSONObject testCase = (JSONObject) test;
            JSONObject metrics = testCase.getJSONObject("metrics");

            metrics.keys().forEachRemaining(key -> {
                double value = metrics.getDouble(key);
                averageMetrics.merge(key, value, (old, newVal) -> old + newVal);
            });
        });

        System.out.println("\nAnalyse de l'historique des tests:");
        averageMetrics.forEach((key, value) -> {
            double average = value / totalTests;
            System.out.printf("%s: %.2f\n", key, average);
        });
    }

    public void showAvailableSteps() {
        System.out.println("\nSteps disponibles:");
        availableSteps.forEach((type, steps) -> {
            System.out.println("\n" + type + ":");
            steps.forEach(step -> {
                double score = stepScores.getOrDefault(step, 1.0);
                System.out.printf("  • %s (Score: %.2f)\n", step, score);
            });
        });
    }

    public void addCustomStep(String type, String stepDefinition) {
        availableSteps.computeIfAbsent(type, k -> new ArrayList<>())
                .add(stepDefinition);
        stepScores.put(stepDefinition, 1.0);
    }
}
