package org.cotizador.userinterfaces.pages;

import net.serenitybdd.core.pages.PageObject;
import net.serenitybdd.screenplay.targets.Target;

import static org.openqa.selenium.By.cssSelector;

public class CalculationSummaryStepPage extends PageObject {

        public static final Target PAGE = Target.the("paso resumen de calculo")
                        .located(cssSelector("[data-testid='total-premium'], .total-premium, button[data-testid='calculate-premium'], button.calculate-premium"));

    public static final Target TOTAL_PREMIUM = Target.the("total de prima")
            .located(cssSelector("[data-testid='total-premium'], .total-premium"));

    public static final Target CALCULATE_BUTTON = Target.the("boton calcular")
            .located(cssSelector("button[data-testid='calculate-premium'], button.calculate-premium"));

    public static final Target BTN_RECALCULAR = Target.the("boton recalcular")
            .located(cssSelector("button[data-testid='calculate-premium'], button.calculate-premium"));

    public static final Target BTN_CONFIRMAR_COTIZACION = Target.the("boton confirmar cotizacion")
            .located(cssSelector("button[data-testid='confirm-quote'], button.confirm-quote, button[data-testid='next-step'], button.next-step"));

    public static final Target NEXT_BUTTON = Target.the("boton Siguiente")
            .located(cssSelector("button[data-testid='next-step'], button.next-step, button[type='submit']"));
}
