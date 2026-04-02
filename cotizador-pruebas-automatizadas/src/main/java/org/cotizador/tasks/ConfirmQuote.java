package org.cotizador.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.matchers.WebElementStateMatchers;
import net.serenitybdd.screenplay.waits.WaitUntil;
import org.cotizador.userinterfaces.pages.ConfirmationStepPage;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class ConfirmQuote implements Task {
    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                WaitUntil.the(ConfirmationStepPage.FINAL_STATUS, WebElementStateMatchers.isVisible()).forNoMoreThan(15).seconds(),
                Click.on(ConfirmationStepPage.DOWNLOAD_PDF_BUTTON)
        );

    }
    public static ConfirmQuote confirmQuote(){
        return instrumented(ConfirmQuote.class);
    }
}
