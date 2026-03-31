package co.com.sura.cotizador.stages;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.serenitybdd.screenplay.actors.OnStage;
import net.serenitybdd.screenplay.actors.OnlineCast;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public class StageConfig {
    public static void setTheStage() {
        OnStage.setTheStage(new OnlineCast());
    }

    public static Actor theUser(WebDriver driver) {
        Actor actor = OnStage.theActorCalled("Usuario");
        actor.can(BrowseTheWeb.with(driver));
        return actor;
    }
}
