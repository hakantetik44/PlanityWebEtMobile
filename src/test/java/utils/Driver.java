package utils;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.options.BaseOptions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.edge.EdgeDriver;

import java.net.MalformedURLException;
import java.net.URL;



public class Driver {
    private Driver() {
    }

    public static AndroidDriver Android;
    public static WebDriver Web;

    public static BaseOptions getAndroidApps() {
        BaseOptions options = new BaseOptions()
                .amend("appium:platformName", "Android")
                .amend("appium:platformVersion", "11.0")
                .amend("appium:deviceName", "emulator-5554")
                .amend("appium:automationName", "UiAutomator2")
                //.amend("appium:app","/Users/hakan/IdeaProjects/AliExpressWebAndMobil/apps/radio-france.apk")
                .amend("appium:appPackage", "com.radiofrance.radio.radiofrance.android")
                .amend("appium:appActivity", "com.radiofrance.radio.radiofrance.android.screen.splash.SplashActivity")
                .amend("appium:noReset", true)
                .amend("appium:autoGrantPermissions", true)
                .amend("appium:newCommandTimeout", 3600)
                .amend("appium:appWaitDuration", 20000)
                .amend("appium:autoAcceptAlerts", true)
                .amend("appium:dontStopAppOnReset", true);

        return options;
    }

    public static AndroidDriver getAndroidDriver(BaseOptions capabilities)
            throws MalformedURLException {
        URL remoteUrl = new URL("http://127.0.0.1:4723/");
        return new AndroidDriver(remoteUrl, capabilities);
    }

    public static WebDriver getWebDriver(String browser) {
        WebDriver driver;
        switch (browser.toLowerCase()) {
            case "chrome":
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("--disable-search-engine-choice-screen");
               // chromeOptions.addArguments("--headless"); // Enable headless mode
                chromeOptions.addArguments("--disable-gpu"); // Disable GPU acceleration
                chromeOptions.addArguments("--window-size=1920,1080"); // Set window size

                driver = new ChromeDriver(chromeOptions);
                break;
            case "firefox":
                driver = new FirefoxDriver();
                break;
            case "edge":
                driver = new EdgeDriver();
                break;
            default:
                throw new IllegalArgumentException("Unsupported browser: " + browser);
        }

        // Tarayıcıyı maksimize et
        driver.manage().window().maximize();

        return driver;

    }

    /**
     * get driver.
     *
     * @return driver
     */
    public static WebDriver getCurrentDriver() {
        if (OS.OS.equals("Android")) {
            return Android;
        } else if (OS.OS.equals("Web")) {
            return Web;
        } else {
            throw new IllegalStateException("Unsupported operating system: " + OS.OS);
        }
    }

}
