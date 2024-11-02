package stepdefinitions;

import io.cucumber.java.Before;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.Scenario;
import pages.PlanityPage;
import utils.Driver;
import utils.TestManager;

public class PlanityStep {
    private PlanityPage planityPage = new PlanityPage();
    private TestManager testManager;
    private static String currentScenarioName;

    public PlanityStep() {
        testManager = TestManager.getInstance();
    }



    private void executeStep(String stepName, String expectedResult, Runnable action) {
        try {
            testManager.setNomScenario(currentScenarioName);
            testManager.setNomEtape(stepName);
            testManager.setResultatAttendu(expectedResult);
            action.run();
            testManager.setStatut("REUSSI");
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

    @When("Je clique sur le lien {string} dans le menu")
    public void jeCliqueSurLeLienDansLeMenu(String lien) {
        executeStep(
                "Clic sur le lien " + lien + " dans le menu",
                "Le lien du menu doit être cliqué",
                () -> {
                    planityPage.cliquerLienCoiffeur();
                    testManager.setResultatReel("Clic effectué sur le lien " + lien);
                }
        );
    }

    @When("Je saisis {string} dans la recherche")
    public void jeSaisisDansLaRecherche(String location) {
        executeStep(
                "Saisie de la localisation",
                "Le champ de recherche doit être rempli",
                () -> {
                    planityPage.saisirLocalisation(location);
                    testManager.setResultatReel("Localisation saisie: " + location);
                }
        );
    }

    @When("Je clique sur le bouton {string}")
    public void jeCliqueSurLeBouton(String Rechercher) {
        executeStep(
                "Clic sur le bouton " + Rechercher,
                "Le bouton doit être cliqué",
                () -> {
                    try {
                        Thread.sleep(2000);
                        planityPage.cliquerBtnRechercher();
                        testManager.setResultatReel("Clic effectué sur le bouton " + Rechercher);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    @Then("Je devrais voir une liste de coiffeurs à Paris")
    public void jeDevraisVoirUneListeDeCoiffeursAParis() {
        executeStep(
                "Vérification des résultats de recherche",
                "La liste des coiffeurs doit être affichée",
                () -> {
                    boolean isDisplayed = planityPage.verifierResultatsCoiffeurs("Paris");
                    if (!isDisplayed) {
                        throw new AssertionError("La liste des coiffeurs n'est pas affichée");
                    }
                    testManager.setResultatReel("Liste des coiffeurs affichée avec succès");
                    planityPage.cliquerLienCoiffeurParis();
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }
}