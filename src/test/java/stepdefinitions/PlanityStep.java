package stepdefinitions;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import pages.RadioPage;
import utils.Driver;
import utils.TestManager;

import static org.junit.Assert.assertTrue;

public class RadioStep {
    private RadioPage pageRadio = new RadioPage();
    private TestManager testManager;
    private static String currentScenarioName;

    public RadioStep() {
        testManager = TestManager.getInstance();
    }

    @Before
    public void setUp(Scenario scenario) {
        currentScenarioName = scenario.getName();
    }

    private void executeStep(String stepName, String expectedResult, Runnable action) {
        try {
            // Step bilgilerini ayarla
            testManager.setNomScenario(currentScenarioName);
            testManager.setNomEtape(stepName);
            testManager.setResultatAttendu(expectedResult);

            // Step'i çalıştır
            action.run();

            // Başarılı sonuç
            testManager.setStatut("REUSSI");

            // URL varsa kaydet
            String currentUrl = Driver.getCurrentDriver().getCurrentUrl();
            if (currentUrl != null) {
                testManager.setUrl(currentUrl);
            }

        } catch (Exception e) {
            testManager.setStatut("ECHEC");
            testManager.setMessageErreur(e.getMessage());
            throw e;
        } finally {
            TestManager.getInstance().ajouterInfosTest(testManager);
        }
    }

    @Then("Je vérifie que je suis sur la page d'accueil")
    public void verifierPageAccueil() {
        executeStep(
                "Vérification de la page d'accueil",
                "La page d'accueil doit être affichée",
                () -> {
                    String urlAttendue = "https://www.radiofrance.fr";
                    String urlReelle = Driver.getCurrentDriver().getCurrentUrl();
                    assertTrue(urlReelle.startsWith(urlAttendue));
                    testManager.setResultatReel("Page d'accueil vérifiée");
                }
        );
    }

    @When("Je clique sur le bouton {string}")
    public void cliquerSurLeBouton(String nomBouton) {
        executeStep(
                "Clic sur " + nomBouton,
                "Le bouton doit être cliqué",
                () -> {
                    pageRadio.cliquerBtnRechercher();
                    testManager.setResultatReel("Clic effectué sur " + nomBouton);
                }
        );
    }

    @When("Je saisis {string} dans le champ de recherche")
    public void effectuerRecherche(String terme) {
        executeStep(
                "Saisie recherche",
                "Saisir: " + terme,
                () -> {
                    pageRadio.effectuerRecherche(terme);
                    testManager.setResultatReel("Recherche: " + terme);
                }
        );
    }

    @Then("Les résultats pour {string} doivent être affichés")
    public void verifierResultatsRecherche(String terme) throws InterruptedException {
        executeStep(
                "Vérification résultats",
                "Voir les résultats pour: " + terme,
                () -> {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    String urlAttendue = "https://www.radiofrance.fr/recherche";
                    String urlReelle = Driver.getCurrentDriver().getCurrentUrl();
                    assertTrue(urlReelle.startsWith(urlAttendue));
                    testManager.setResultatReel("Résultats vérifiés pour: " + terme);
                }
        );
    }
}