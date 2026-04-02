package org.cotizador.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.waits.WaitUntil;
import net.serenitybdd.screenplay.matchers.WebElementStateMatchers;
import org.cotizador.userinterfaces.pages.GeneralInfoStepPage;

import java.util.Map;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class RegisterGeneralData implements Task {

    @Override
    public <T extends Actor> void performAs(T actor) {
        Map<String, String> datosCotizacion = actor.recall("datosCotizacion");
        actor.attemptsTo(
            WaitUntil.the(GeneralInfoStepPage.NOMBRE_INPUT, WebElementStateMatchers.isEnabled())
                .forNoMoreThan(15).seconds(),
                Enter.theValue(datosCotizacion.get("nombre")).into(GeneralInfoStepPage.NOMBRE_INPUT),
                Enter.theValue(datosCotizacion.get("rfc")).into(GeneralInfoStepPage.RFC_INPUT),
                Click.on(GeneralInfoStepPage.AGENTE_MAT_SELECT),
                Click.on(GeneralInfoStepPage.AGENTE_OPTION.of(datosCotizacion.get("codigoAgente"))),
                Click.on(GeneralInfoStepPage.SAVE_BUTTON),
                WaitUntil.the(GeneralInfoStepPage.NEXT_BUTTON, WebElementStateMatchers.isEnabled())
                    .forNoMoreThan(15).seconds(),
                Click.on(GeneralInfoStepPage.NEXT_BUTTON)
        );
    }

    public static RegisterGeneralData withValidData() {
        return instrumented(RegisterGeneralData.class);
    }
}
