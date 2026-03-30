package org.cotizador.userinterfaces.pages;

import net.serenitybdd.core.pages.PageObject;
import net.serenitybdd.screenplay.targets.Target;
import net.serenitybdd.annotations.DefaultUrl;

import static org.openqa.selenium.By.cssSelector;

@DefaultUrl("http://localhost:4200/")
public class HomePage extends PageObject {

    public static final Target PAGE = Target.the("pagina de inicio")
            .located(cssSelector("app-root, body"));

    public static final Target COTIZADOR_CTA_BUTTON = Target.the("boton principal Cotizador")
            .located(cssSelector(".cta-button"));

    public static final Target BTN_CREAR_NUEVA_COTIZACION = Target.the("boton crear nueva cotizacion")
            .located(cssSelector(".cta-button"));

    public static final Target NAVBAR_COTIZADOR_LINK = Target.the("link de navbar a cotizador")
            .located(cssSelector("nav a[href*='cotizador'], header a[href*='cotizador'], a[href*='cotizador']"));
}
