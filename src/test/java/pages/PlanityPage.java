package pages;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.support.PageFactory;
import utils.OS;

import static utils.Driver.getCurrentDriver;

public class PlanityPage extends BasePage {

    public PlanityPage() {
        super(getCurrentDriver());
        PageFactory.initElements(getCurrentDriver(), this);
    }

    public void cliquerLienCoiffeur() {
        By coiffeurLink = OS.isAndroid() ?
                AppiumBy.androidUIAutomator("new UiSelector().text(\"Coiffeur\")") :
                By.xpath("//a[@id='nav-item-0'][@href='/coiffeur']");
        click(coiffeurLink);
    }

    public void saisirLocalisation(String location) {
        By locationInput = OS.isAndroid() ?
                AppiumBy.androidUIAutomator("new UiSelector().text(\"Adresse, ville...\")") :
                By.cssSelector("input#main-where-input_1730471228793");
        sendKeys(locationInput, location);
    }

    public void cliquerBtnRechercher() {
        By searchButton = OS.isAndroid() ?
                AppiumBy.androidUIAutomator("new UiSelector().text(\"Recherche\")") :
                By.xpath("//span[text()='Rechercher']");
        click(searchButton);
    }

    public void cliquerLienCoiffeurParis() {
        By coiffeurLink = OS.isAndroid() ?
                AppiumBy.androidUIAutomator("new UiSelector().text(\"Coiffeur\")") :
                By.xpath("//a[@id='nav-item-0'][@href='/coiffeur']");
        click(coiffeurLink);
    }

    public boolean verifierResultatsCoiffeurs(String ville) {
        By resultTitle = OS.isAndroid() ?
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Coiffeurs à " + ville + "\")") :
                By.cssSelector("h2#place-title-0-category-page");
        return isDisplayed(resultTitle);
    }
}
