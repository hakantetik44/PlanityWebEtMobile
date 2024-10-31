package stepdefinitions;

import io.appium.java_client.android.AndroidDriver;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.ConfigReader;
import utils.Driver;
import utils.OS;
import utils.TestManager;
import org.openqa.selenium.By;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Properties;

public class Hooks {
    public static final String NOM_APK = "radio-france.apk";
    public static final String URL_WEB = "https://www.radiofrance.fr/franceculture";
    protected WebDriverWait attente;
    private TestManager infosTest;
    private static boolean isFirstTest = true;

    @Before
    public void avantTout(Scenario scenario) {

        try {
            // Ortam bilgilerini Allure'a yükler
            loadConfigurationProperties();

            // Platformu belirle
            OS.OS = ConfigReader.getProperty("platformName");

            // Rapor bilgilerini başlat
            infosTest = TestManager.getInstance();
            infosTest.setNomScenario(scenario.getName());
            infosTest.setNomEtape("Début du Test");
            infosTest.setPlateforme(OS.OS);
            infosTest.setStatut("DÉMARRÉ");

            if (OS.isWeb()) {
                infosTest.setResultatAttendu("Le navigateur web doit être lancé");
                if (Driver.Web == null) {
                    Driver.Web = Driver.getWebDriver(ConfigReader.getProperty("browser"));
                    this.attente = new WebDriverWait(Driver.Web, Duration.ofSeconds(10));
                }
            } else if (OS.isAndroid()) {
                infosTest.setResultatAttendu("L'application Android doit être lancée");
                if (Driver.Android == null) {
                    Driver.Android = Driver.getAndroidDriver(Driver.getAndroidApps());
                }
            }

            TestManager.getInstance().ajouterInfosTest(infosTest);

            // Test önerilerini göster
            if (!infosTest.getTestSuggestions().isEmpty()) {
                System.out.println("\n🤖 Suggestions pour ce test:");
                infosTest.getTestSuggestions().forEach(s -> System.out.println("• " + s));
            }

        } catch (Exception e) {
            infosTest.setStatut("ECHEC");
            infosTest.setMessageErreur("Erreur d'initialisation: " + e.getMessage());
            TestManager.getInstance().ajouterInfosTest(infosTest);
            throw new RuntimeException(e);
        }
    }

    // Configuration properties dosyasından Allure environment değişkenlerini yükleyen metot
    private void loadConfigurationProperties() {
        Properties properties = new Properties();

        try (FileInputStream input = new FileInputStream("config/configuration.properties")) {
            properties.load(input);

            // browser ve platformName bilgilerini Allure raporuna ekler
            Allure.parameter("Browser", properties.getProperty("browser"));
            Allure.parameter("Platform Name", properties.getProperty("platformName"));

            System.out.println("Allure environment değişkenleri configuration.properties dosyasından yüklendi.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Given("Je lance l'application")
    public void lanceApp() {
        infosTest = TestManager.getInstance();
        infosTest.setNomEtape("Lancement de l'Application");
        System.out.println("Lancement de l'application web : " + URL_WEB);

        try {
            WebDriver driver = Driver.getCurrentDriver();
            if (driver != null) {
                if (OS.isWeb()) {
                    driver.get(URL_WEB);
                    this.attente = new WebDriverWait(driver, Duration.ofSeconds(10));
                    gererPopupsEtCookies();
                    infosTest.setStatut("REUSSI");
                    infosTest.setResultatReel("L'application web a été lancée avec succès");
                    infosTest.setUrl(URL_WEB);
                }
            } else {
                throw new RuntimeException("Driver non initialisé");
            }
        } catch (Exception e) {
            infosTest.setStatut("ECHEC");
            infosTest.setMessageErreur("Erreur de lancement: " + e.getMessage());
            throw e;
        } finally {
            TestManager.getInstance().ajouterInfosTest(infosTest);
        }
    }

    private void gererPopupsEtCookies() {
        TestManager infosPopup = TestManager.getInstance();
        infosPopup.setNomEtape("Gestion des Popups et Cookies");
        StringBuilder resultats = new StringBuilder();

        try {
            // Gérer les popups avec des tentatives multiples
            for (String xpath : new String[]{
                    "//span[text()='Tout refuser']",
                    "//span[text()='Tout accepter']"
            }) {
                try {
                    WebElement element = attente.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
                    element.click();
                    resultats.append("Élément cliqué: ").append(xpath).append("\n");
                    Thread.sleep(1000); // Petit délai entre les clics
                } catch (Exception e) {
                    resultats.append("Élément non trouvé ou déjà géré: ").append(xpath).append("\n");
                }
            }

            infosPopup.setStatut("REUSSI");
            infosPopup.setResultatReel(resultats.toString());
        } catch (Exception e) {
            infosPopup.setStatut("ECHEC");
            infosPopup.setMessageErreur("Gestion des popups: " + e.getMessage());
        } finally {
            TestManager.getInstance().ajouterInfosTest(infosPopup);
        }
    }

    @After
    public void terminer(Scenario scenario) {
        try {
            infosTest = TestManager.getInstance();
            infosTest.setNomEtape("Fin du Test");
            infosTest.setNomScenario(scenario.getName());

            WebDriver driver = Driver.getCurrentDriver();
            if (driver != null) {
                infosTest.setUrl(driver.getCurrentUrl());

                if (scenario.isFailed()) {
                    infosTest.setStatut("ECHEC");
                    if (driver instanceof TakesScreenshot) {
                        byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                        scenario.attach(screenshot, "image/png", "screenshot-erreur");
                        infosTest.setResultatReel("Test échoué - Capture d'écran ajoutée");

                        // Hata analizini göster
                        System.out.println("\n🔍 Analyse de l'échec:");
                        System.out.println(infosTest.getMessageErreur());
                    }
                } else {
                    infosTest.setStatut("REUSSI");
                    infosTest.setResultatReel("Test terminé avec succès");
                }
            }

        } catch (Exception e) {
            infosTest.setStatut("ECHEC");
            infosTest.setMessageErreur("Erreur finale: " + e.getMessage());
        } finally {
            TestManager.getInstance().ajouterInfosTest(infosTest);

            // Test pattern analizi
            System.out.println("\n📊 Résumé du test:");
            System.out.println("• Scénario: " + scenario.getName());
            System.out.println("• Statut: " + infosTest.getStatut());

            // Rapor oluştur
            TestManager.getInstance().genererRapport("RadioFrance");
            quitterDriver();
        }
    }

    private void quitterDriver() {
        try {
            WebDriver driver = Driver.getCurrentDriver();
            if (driver != null) {
                if (OS.isAndroid() && Driver.Android != null) {
                    Driver.Android.terminateApp(getAppPackage());
                    Driver.Android = null;
                } else if (OS.isWeb() && Driver.Web != null) {
                    Driver.Web.quit();
                    Driver.Web = null;
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la fermeture du driver: " + e.getMessage());
        }
    }

    public static String getAppPackage() {
        return "com.radiofrance.radio.radiofrance.android";
    }
}
