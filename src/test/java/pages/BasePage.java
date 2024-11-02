package pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.PerformsTouchActions;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.touch.WaitOptions;
import io.appium.java_client.touch.offset.PointOption;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.List;

public class BasePage {
    protected WebDriver driver;
    protected WebDriverWait wait;
    protected WebDriverWait shortWait;
    protected WebDriverWait longWait;
    protected JavascriptExecutor js;

    public BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        this.shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
        this.longWait = new WebDriverWait(driver, Duration.ofSeconds(30));
        this.js = (JavascriptExecutor) driver;
    }

    // Attente améliorée avec gestion des erreurs
    protected void waitForElement(By locator) {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        } catch (TimeoutException e) {
            throw new TimeoutException("L'élément n'a pas été trouvé après 15 secondes: " + locator);
        }
    }


    protected void waitForElements(By locator) {
        try {
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(locator));
        } catch (TimeoutException e) {
            throw new TimeoutException("Les éléments n'ont pas été trouvés après 15 secondes: " + locator);
        }
    }

    protected boolean isDisplayed(By locator) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            return element.isDisplayed();
        } catch (TimeoutException | NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }

    // Méthodes d'attente avancées
    protected WebElement waitForElementClickable(By locator) {
        try {
            return wait.until(ExpectedConditions.elementToBeClickable(locator));
        } catch (TimeoutException e) {
            throw new TimeoutException("L'élément n'est pas cliquable après 15 secondes: " + locator);
        }
    }

    protected WebElement waitForElementVisible(By locator) {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (TimeoutException e) {
            throw new TimeoutException("L'élément n'est pas visible après 15 secondes: " + locator);
        }
    }

    protected WebElement waitForElementPresent(By locator) {
        try {
            return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        } catch (TimeoutException e) {
            throw new TimeoutException("L'élément n'est pas présent après 15 secondes: " + locator);
        }
    }

    protected boolean waitForElementToDisappear(By locator) {
        try {
            return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
        } catch (TimeoutException e) {
            return false;
        }
    }

    // Méthodes d'action améliorées
    protected void click(By locator) {
        try {
            waitForElementClickable(locator).click();
        } catch (ElementClickInterceptedException e) {
            // Retry with JavaScript if normal click fails
            WebElement element = waitForElementPresent(locator);
            js.executeScript("arguments[0].click();", element);
        }
    }

    protected void sendKeys(By locator, String text) {
        try {
            WebElement element = waitForElementVisible(locator);
            element.clear();
            element.sendKeys(text);
        } catch (Exception e) {
            throw new RuntimeException("Impossible de saisir le texte dans l'élément: " + locator, e);
        }
    }

    protected String getText(By locator) {
        return waitForElementVisible(locator).getText();
    }

    protected String getValue(By locator) {
        return waitForElementVisible(locator).getAttribute("value");
    }

    // Méthodes de vérification améliorées
    protected boolean isElementDisplayed(By locator) {
        try {
            return shortWait.until(ExpectedConditions.visibilityOfElementLocated(locator)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean isElementPresent(By locator) {
        try {
            return !driver.findElements(locator).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean isElementEnabled(By locator) {
        try {
            return waitForElementPresent(locator).isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    // Méthodes de scroll améliorées
    protected void scrollToElement(By locator) {
        try {
            if (isWeb()) {
                WebElement element = driver.findElement(locator);
                js.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
            } else if (isAndroid()) {
                ((AndroidDriver) driver).findElement(new AppiumBy.ByAndroidUIAutomator(
                        "new UiScrollable(new UiSelector().scrollable(true).instance(0))" +
                                ".scrollIntoView(new UiSelector().descriptionContains(\"" + locator + "\").instance(0))"
                ));
            } else if (isIOS()) {
                // Implement iOS scroll
                WebElement element = driver.findElement(locator);
                String elementID = element.getAttribute("id");
                js.executeScript("mobile: scroll", "{direction: 'down', element: '" + elementID + "'}");
            }
        } catch (Exception e) {
            throw new RuntimeException("Impossible de faire défiler jusqu'à l'élément: " + locator, e);
        }
    }

    // Gestes tactiles améliorés
    protected void swipeVertical(double startPercentage, double endPercentage, double anchorPercentage) {
        if (driver instanceof AppiumDriver) {
            try {
                AppiumDriver appiumDriver = (AppiumDriver) driver;
                Dimension size = driver.manage().window().getSize();
                int anchor = (int) (size.width * anchorPercentage);
                int startPoint = (int) (size.height * startPercentage);
                int endPoint = (int) (size.height * endPercentage);

                new TouchAction((PerformsTouchActions) appiumDriver)
                        .press(PointOption.point(anchor, startPoint))
                        .waitAction(WaitOptions.waitOptions(Duration.ofMillis(1000)))
                        .moveTo(PointOption.point(anchor, endPoint))
                        .release()
                        .perform();
            } catch (Exception e) {
                throw new RuntimeException("Erreur lors du swipe vertical", e);
            }
        }
    }

    // Nouvelles méthodes utiles
    protected void waitForPageLoad() {
        wait.until(webDriver -> ((JavascriptExecutor) webDriver)
                .executeScript("return document.readyState").equals("complete"));
    }

    protected void waitForAjax() {
        wait.until(webDriver -> (Boolean) ((JavascriptExecutor) webDriver)
                .executeScript("return jQuery.active == 0"));
    }

    protected void waitIsElementVisibleAndClick(By locator, String errorMessage) {
        try {
            // 1. Normal yol: Elemanın görünür ve tıklanabilir olmasını bekle
            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
            element.click();
        } catch (TimeoutException | ElementClickInterceptedException e1) {
            try {
                // 2. JavaScript ile scroll ve click
                WebElement element = driver.findElement(locator);
                js.executeScript("arguments[0].scrollIntoView(true);", element);
                Thread.sleep(500); // Scroll işleminin tamamlanması için kısa bekleme
                js.executeScript("arguments[0].click();", element);
            } catch (Exception e2) {
                try {
                    // 3. JavaScript ile direk click
                    WebElement element = driver.findElement(locator);
                    js.executeScript("arguments[0].click();", element);
                } catch (Exception e3) {
                    try {
                        // 4. JavaScript ile tüm engelleri kaldırıp click
                        WebElement element = driver.findElement(locator);
                        js.executeScript(
                                "arguments[0].style.border='2px solid red';" +
                                        "arguments[0].style.visibility='visible';" +
                                        "arguments[0].style.opacity='1';" +
                                        "arguments[0].style.display='block';" +
                                        "arguments[0].style.pointerEvents='auto';" +
                                        "return arguments[0].click();", element);
                    } catch (Exception e4) {
                        // Son durumda hata fırlat
                        throw new RuntimeException("Element tıklanamadı: " + errorMessage, e4);
                    }
                }
            }
        }
    }

    protected void selectByVisibleText(By locator, String text) {
        List<WebElement> options = driver.findElements(locator);
        for (WebElement option : options) {
            if (option.getText().trim().equals(text)) {
                option.click();
                break;
            }
        }
    }

    protected void clearField(By locator) {
        WebElement element = waitForElementVisible(locator);
        element.clear();
        // Double check clear with JavaScript
        js.executeScript("arguments[0].value = '';", element);
    }

    // Utilitaires pour le type de driver
    protected boolean isAndroid() {
        return driver instanceof AndroidDriver;
    }

    protected boolean isIOS() {
        return driver instanceof IOSDriver;
    }

    protected boolean isWeb() {
        return !(driver instanceof AppiumDriver);
    }
}