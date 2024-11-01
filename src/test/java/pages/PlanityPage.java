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

    public void effectuerRecherche(String termeRecherche) {
        By champRecherche = OS.isAndroid() ?
                AppiumBy.androidUIAutomator("new UiSelector().text(\"Recherche\")") :
                By.xpath("//input[@aria-label='Rechercher un podcast, un épisode, une personnalité...']");
        sendKeys(champRecherche, termeRecherche);
    }

    public void cliquerBtnRechercher() {
        By btnRechercher = OS.isAndroid() ?
                AppiumBy.accessibilityId("Recherche") :
                By.xpath("//span[normalize-space()='Rechercher']");
        getCurrentDriver().findElement(btnRechercher).click();
    }

}
