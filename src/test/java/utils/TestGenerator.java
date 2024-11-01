
package utils;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;

public class TestGenerator {
    private Map<String, List<String>> availableSteps;
    private static final String STEPDEFS_PACKAGE = "stepdefinitions";

    public TestGenerator() {
        this.availableSteps = new HashMap<>();
        scanAvailableSteps();
    }

    // Varolan step tanımlarını tara
    private void scanAvailableSteps() {
        try {
            Reflections reflections = new Reflections(STEPDEFS_PACKAGE, new MethodAnnotationsScanner());

            // Given steps
            Set<Method> givenMethods = reflections.getMethodsAnnotatedWith(Given.class);
            for (Method method : givenMethods) {
                Given annotation = method.getAnnotation(Given.class);
                availableSteps.computeIfAbsent("Given", k -> new ArrayList<>())
                        .add(annotation.value());
            }

            // When steps
            Set<Method> whenMethods = reflections.getMethodsAnnotatedWith(When.class);
            for (Method method : whenMethods) {
                When annotation = method.getAnnotation(When.class);
                availableSteps.computeIfAbsent("When", k -> new ArrayList<>())
                        .add(annotation.value());
            }

            // Then steps
            Set<Method> thenMethods = reflections.getMethodsAnnotatedWith(Then.class);
            for (Method method : thenMethods) {
                Then annotation = method.getAnnotation(Then.class);
                availableSteps.computeIfAbsent("Then", k -> new ArrayList<>())
                        .add(annotation.value());
            }

            // And steps
            Set<Method> andMethods = reflections.getMethodsAnnotatedWith(And.class);
            for (Method method : andMethods) {
                And annotation = method.getAnnotation(And.class);
                availableSteps.computeIfAbsent("And", k -> new ArrayList<>())
                        .add(annotation.value());
            }

        } catch (Exception e) {
            System.err.println("Erreur lors du scan des steps: " + e.getMessage());
        }
    }

    public void generateFeatureFile(String description) {
        try {
            StringBuilder featureContent = new StringBuilder();
            String featureName = generateFeatureName(description);
            String scenarioName = generateScenarioName(description);

            featureContent.append("# language: fr\n\n");
            featureContent.append("Feature: ").append(featureName).append("\n\n");
            featureContent.append("  Scenario: ").append(scenarioName).append("\n");

            // Mevcut step'lerden uygun olanları seç
            if (availableSteps.containsKey("Given")) {
                featureContent.append("    Given ").append(findMostRelevantStep("Given", description)).append("\n");
            }

            if (availableSteps.containsKey("When")) {
                availableSteps.get("When").stream()
                        .filter(step -> isStepRelevant(step, description))
                        .forEach(step -> featureContent.append("    When ").append(step).append("\n"));
            }

            if (availableSteps.containsKey("Then")) {
                featureContent.append("    Then ").append(findMostRelevantStep("Then", description)).append("\n");
            }

            saveFeatureFile(description, featureContent.toString());

        } catch (Exception e) {
            System.err.println("Erreur lors de la génération du feature file: " + e.getMessage());
        }
    }

    private String generateFeatureName(String description) {
        // İlk kelimeyi büyük harf yap ve "Feature" ekle
        String[] words = description.trim().split("\\s+");
        return words[0].substring(0, 1).toUpperCase() + words[0].substring(1) + " " +
                String.join(" ", Arrays.copyOfRange(words, 1, words.length));
    }

    private String generateScenarioName(String description) {
        // Açıklama metnini senaryo adına dönüştür
        return "Vérifier " + description.toLowerCase();
    }

    private String findMostRelevantStep(String stepType, String description) {
        List<String> steps = availableSteps.get(stepType);
        if (steps == null || steps.isEmpty()) {
            return "# Step à implémenter";
        }

        // En uygun step'i bul
        return steps.stream()
                .filter(step -> isStepRelevant(step, description))
                .findFirst()
                .orElse(steps.get(0)); // Uygun step bulunamazsa ilkini kullan
    }

    private boolean isStepRelevant(String step, String description) {
        // Step'in açıklama ile ilgili olup olmadığını kontrol et
        String[] keywords = description.toLowerCase().split("\\s+");
        String stepLower = step.toLowerCase();

        return Arrays.stream(keywords)
                .anyMatch(keyword -> stepLower.contains(keyword));
    }

    private void saveFeatureFile(String description, String content) throws Exception {
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
    }

    // Mevcut step'leri göster
    public void showAvailableSteps() {
        System.out.println("\nSteps disponibles:");
        availableSteps.forEach((type, steps) -> {
            System.out.println("\n" + type + ":");
            steps.forEach(step -> System.out.println("  • " + step));
        });
    }

    // Yeni step ekle
    public void addCustomStep(String type, String stepDefinition) {
        availableSteps.computeIfAbsent(type, k -> new ArrayList<>())
                .add(stepDefinition);
    }
}