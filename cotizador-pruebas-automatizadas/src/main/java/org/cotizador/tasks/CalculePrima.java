package org.cotizador.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.matchers.WebElementStateMatchers;
import net.serenitybdd.screenplay.waits.WaitUntil;
import org.cotizador.userinterfaces.pages.CalculationSummaryStepPage;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class CalculePrima implements Task {
    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                WaitUntil.the(CalculationSummaryStepPage.CALCULATE_BUTTON, WebElementStateMatchers.isEnabled()).forNoMoreThan(15).seconds(),
                Click.on(CalculationSummaryStepPage.CALCULATE_BUTTON),
                WaitUntil.the(CalculationSummaryStepPage.BTN_CONFIRMAR_COTIZACION,WebElementStateMatchers.isEnabled()).forNoMoreThan(15).seconds(),
                Click.on(CalculationSummaryStepPage.BTN_CONFIRMAR_COTIZACION),
                WaitUntil.the(CalculationSummaryStepPage.FLAT_CARD_CONFIRM,WebElementStateMatchers.isVisible()).forNoMoreThan(15).seconds()
        );
    }

    public static CalculePrima conData(){
        return instrumented(CalculePrima.class);
    }
}
