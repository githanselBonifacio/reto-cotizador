package org.cotizador.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.matchers.WebElementStateMatchers;
import net.serenitybdd.screenplay.waits.WaitUntil;
import org.cotizador.userinterfaces.pages.TermsAndConditionsStepPage;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class ConfirmConditions implements Task {

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                WaitUntil.the(TermsAndConditionsStepPage.ACCEPT_PRIVACY_CHECKBOX, WebElementStateMatchers.isEnabled())
                        .forNoMoreThan(15).seconds(),
                Click.on(TermsAndConditionsStepPage.ACCEPT_PRIVACY_CHECKBOX),
                Click.on(TermsAndConditionsStepPage.BTN_CONTINUAR_A_CALCULO)
        );
    }
    public static ConfirmConditions withValidData() {
        return instrumented(ConfirmConditions.class);
    }
}
