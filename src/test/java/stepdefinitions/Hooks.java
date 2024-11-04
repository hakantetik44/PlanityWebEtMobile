package stepdefinitions;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;


public class Hooks {
    public static final String NOM_APK = "planity.apk";
    public static final String NOM_IPA = "planity.ipa"; // iOS app dosyası
    public static final String URL_WEB = "https://www.planity.com/";
    protected WebDriverWait attente;
    private TestManager infosTest;
    private static boolean isFirstTest = true;

    // Configuration de l'enregistrement vidéo
    private Process videoProcess;
    private static final String VIDEO_DIR = "target/videos";
    private static final String FFMPEG_COMMAND = "ffmpeg";
    private static final int FRAME_RATE = 30;
    private static final String VIDEO_RESOLUTION = "1920x1080";

    @Before
    public void avantTout(Scenario scenario) {
        try {
            loadConfigurationProperties();
            OS.OS = ConfigReader.getProperty("platformName");

            infosTest = TestManager.getInstance();
            infosTest.setNomScenario(scenario.getName());
            infosTest.setNomEtape("Début du Test");
            infosTest.setPlateforme(OS.OS);
            infosTest.setStatut("DÉMARRÉ");

            if (OS.isWeb()) {
                startVideoRecording(scenario.getName());

                if (Driver.Web == null) {
                    Driver.Web = Driver.getWebDriver(ConfigReader.getProperty("browser"));
                    this.attente = new WebDriverWait(Driver.Web, Duration.ofSeconds(10));
                }
            } else if (OS.isAndroid()) {
                infosTest.setResultatAttendu("L'application Android doit être lancée");
                if (Driver.Android == null) {
                    Driver.Android = Driver.getAndroidDriver(Driver.getAndroidApps());
                }
            } else if (OS.isIOS()) {
                infosTest.setResultatAttendu("L'application iOS doit être lancée");
                if (Driver.iOS == null) {
                    Driver.iOS = Driver.getIOSDriver(Driver.getIOSApps());
                }
            }

            TestManager.getInstance().ajouterInfosTest(infosTest);

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

    private void startVideoRecording(String scenarioName) {
        try {
            // Création du répertoire vidéo
            new File(VIDEO_DIR).mkdirs();

            // Génération du nom de fichier vidéo
            String videoFileName = VIDEO_DIR + File.separator +
                    scenarioName.replaceAll("[^a-zA-Z0-9-_\\.]", "_") + "_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".mp4";

            // Configuration de la commande ffmpeg selon l'OS
            String[] command;
            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                command = new String[]{
                        FFMPEG_COMMAND,
                        "-f", "avfoundation",
                        "-i", "1",
                        "-r", String.valueOf(FRAME_RATE),
                        "-vcodec", "libx264",
                        "-pix_fmt", "yuv420p",
                        "-y",
                        videoFileName
                };
            } else {
                command = new String[]{
                        FFMPEG_COMMAND,
                        "-f", "x11grab",
                        "-video_size", VIDEO_RESOLUTION,
                        "-i", ":0.0",
                        "-r", String.valueOf(FRAME_RATE),
                        "-vcodec", "libx264",
                        "-pix_fmt", "yuv420p",
                        "-y",
                        videoFileName
                };
            }

            // Démarrage de l'enregistrement
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            videoProcess = processBuilder.start();

            System.out.println("📹 Enregistrement vidéo démarré: " + videoFileName);

        } catch (Exception e) {
            System.err.println("⚠️ Erreur lors du démarrage de l'enregistrement vidéo: " + e.getMessage());
        }
    }
    private void stopVideoRecording() {
        if (videoProcess != null) {
            try {
                // Determine the operating system
                String osName = System.getProperty("os.name").toLowerCase();

                // Stop ffmpeg based on OS
                if (osName.contains("mac") || osName.contains("linux")) {
                    // For MacOS and Linux systems
                    Runtime.getRuntime().exec("pkill -SIGINT -f ffmpeg");
                } else {
                    // For Windows systems
                    Runtime.getRuntime().exec("taskkill /F /IM ffmpeg.exe");
                    videoProcess.destroy();
                }

                // Wait for the process to end
                boolean processStopped = videoProcess.waitFor(5, TimeUnit.SECONDS);

                if (processStopped) {
                    System.out.println("📹 Enregistrement vidéo terminé avec succès");
                } else {
                    System.out.println("⚠️ Le processus d'enregistrement vidéo n'a pas pu être arrêté dans le délai imparti");
                    videoProcess.destroyForcibly();
                }

            } catch (Exception e) {
                System.err.println("⚠️ Erreur lors de l'arrêt de l'enregistrement vidéo: " + e.getMessage());
                e.printStackTrace();
            } finally {
                videoProcess = null;
            }
        }
    }
    private void loadConfigurationProperties() {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream("config/configuration.properties")) {
            properties.load(input);
            Allure.parameter("Browser", properties.getProperty("browser"));
            Allure.parameter("Platform Name", properties.getProperty("platformName"));
            System.out.println("📝 Variables d'environnement Allure chargées");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void gererPopupsEtCookies() {
        TestManager infosPopup = TestManager.getInstance();
        infosPopup.setNomEtape("Gestion des Popups et Cookies");
        StringBuilder resultats = new StringBuilder();

        try {
            // Web için popup ve cookie yönetimi
            if (OS.isWeb()) {
                for (String xpath : new String[]{
                        "//button[contains(.,'Accepter & Fermer')]",
                        "//button[contains(.,'Tout accepter')]",
                        "//button[contains(.,'Accept All')]"
                }) {
                    try {
                        WebElement element = attente.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
                        element.click();
                        resultats.append("✓ Élément cliqué: ").append(xpath).append("\n");
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        resultats.append("⚠️ Élément non trouvé ou déjà géré: ").append(xpath).append("\n");
                    }
                }
            }
            // iOS için popup yönetimi
            else if (OS.isIOS()) {
                try {
                    // iOS'ta izin popup'larını yönetme
                    String[] iosPermissions = {
                            "Allow",
                            "OK",
                            "Accept",
                            "Continue"
                    };

                    for (String permission : iosPermissions) {
                        try {
                            WebElement element = attente.until(ExpectedConditions.elementToBeClickable(
                                    By.xpath("//*[@label='" + permission + "']")
                            ));
                            element.click();
                            resultats.append("✓ iOS permission handled: ").append(permission).append("\n");
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            resultats.append("⚠️ iOS permission not found or already handled: ")
                                    .append(permission).append("\n");
                        }
                    }
                } catch (Exception e) {
                    resultats.append("⚠️ iOS permissions handling skipped\n");
                }
            }

            infosPopup.setStatut("REUSSI");
            infosPopup.setResultatReel(resultats.toString());
        } catch (Exception e) {
            infosPopup.setStatut("ECHEC");
            infosPopup.setMessageErreur("Erreur lors de la gestion des popups: " + e.getMessage());
        } finally {
            TestManager.getInstance().ajouterInfosTest(infosPopup);
        }
    }
    @Given("Je lance l'application")
    public void lanceApp() {
        infosTest = TestManager.getInstance();
        infosTest.setNomEtape("Lancement de l'Application");

        try {
            WebDriver driver = Driver.getCurrentDriver();
            if (driver != null) {
                if (OS.isWeb()) {
                    System.out.println("🚀 Lancement de l'application web : " + URL_WEB);
                    driver.get(URL_WEB);
                    this.attente = new WebDriverWait(driver, Duration.ofSeconds(10));
                    gererPopupsEtCookies();
                } else if (OS.isAndroid() || OS.isIOS()) {
                    System.out.println("🚀 Lancement de l'application mobile");
                }
                infosTest.setStatut("REUSSI");
                infosTest.setResultatReel("L'application a été lancée avec succès");
                if (OS.isWeb()) {
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


    @After
    public void terminer(Scenario scenario) {
        try {
            infosTest = TestManager.getInstance();
            infosTest.setNomEtape("Fin du Test");
            infosTest.setNomScenario(scenario.getName());

            WebDriver driver = Driver.getCurrentDriver();
            if (driver != null) {
                if (OS.isWeb()) {
                    infosTest.setUrl(driver.getCurrentUrl());
                }

                if (scenario.isFailed()) {
                    infosTest.setStatut("ECHEC");
                    if (driver instanceof TakesScreenshot) {
                        byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                        scenario.attach(screenshot, "image/png", "screenshot-erreur");
                        infosTest.setResultatReel("Test échoué - Capture d'écran ajoutée");
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
            if (OS.isWeb()) {
                stopVideoRecording();
            }
            TestManager.getInstance().ajouterInfosTest(infosTest);

            System.out.println("\n📊 Résumé du test:");
            System.out.println("• Scénario: " + scenario.getName());
            System.out.println("• Statut: " + infosTest.getStatut());

            TestManager.getInstance().genererRapport("Planity");
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
                } else if (OS.isIOS() && Driver.iOS != null) {
                    Driver.iOS.terminateApp(getBundleId());
                    Driver.iOS = null;
                } else if (OS.isWeb() && Driver.Web != null) {
                    Driver.Web.quit();
                    Driver.Web = null;
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur lors de la fermeture du driver: " + e.getMessage());
        }
    }

    public static String getAppPackage() {
        return "com.planity.android";
    }

    public static String getBundleId() {
        return "com.yourcompany.planity";
    }
}
