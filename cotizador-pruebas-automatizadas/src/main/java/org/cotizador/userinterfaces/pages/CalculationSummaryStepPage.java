package org.cotizador.userinterfaces.pages;

import net.serenitybdd.core.pages.PageObject;
import net.serenitybdd.screenplay.targets.Target;
import org.openqa.selenium.By;

import static org.openqa.selenium.By.cssSelector;

public class CalculationSummaryStepPage extends PageObject {


    public static final Target CALCULATE_BUTTON = Target.the("boton calcular")
            .located(By.id("qc-btn-calculate"));

    public static final Target BTN_CONFIRMAR_COTIZACION = Target.the("boton confirmar cotizacion")
            .located(By.id("qc-btn-calculation-next"));

    public static final Target FLAT_CARD_CONFIRM= Target.the("flat de confirmacion")
            .located(cssSelector("body > app-root > main > app-quote-creation > section > mat-card.mat-mdc-card.mdc-card.lock-banner"));

}
