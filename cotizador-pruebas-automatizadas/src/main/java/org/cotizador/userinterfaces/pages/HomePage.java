package org.cotizador.userinterfaces.pages;

import net.serenitybdd.core.pages.PageObject;
import net.serenitybdd.screenplay.targets.Target;
import net.serenitybdd.annotations.DefaultUrl;

import static org.openqa.selenium.By.cssSelector;

@DefaultUrl("http://localhost:4200/")
public class HomePage extends PageObject {


    public static final Target BTN_CREAR_NUEVA_COTIZACION = Target.the("boton crear nueva cotizacion")
            .located(cssSelector("body > app-root > main > app-home > section > div.home-container > div.hero-section > div > button"));

}
