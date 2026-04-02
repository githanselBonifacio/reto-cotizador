package org.cotizador.userinterfaces.pages;

import net.serenitybdd.core.pages.PageObject;
import net.serenitybdd.screenplay.targets.Target;
import org.openqa.selenium.By;

import static org.openqa.selenium.By.cssSelector;

public class LocationsStepPage extends PageObject {

    public static final Target PAGE = Target.the("paso ubicaciones")
            .located(cssSelector("#mat-mdc-dialog-2 > div > div"));

    public static final Target ADD_LOCATION_BUTTON = Target.the("boton agregar ubicacion")
            .located(By.id("qc-btn-location-add"));

    public static final Target NEXT_BUTTON = Target.the("boton Siguiente")
            .located(cssSelector("button[data-testid='next-step'], button.next-step, button[type='submit']"));

    public static final Target BTN_SIGUIENTE = Target.the("boton siguiente ubicaciones")
            .located(cssSelector("button[data-testid='next-step'], button.next-step, button[type='submit']"));
}
