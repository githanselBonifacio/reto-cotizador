package org.cotizador.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.matchers.WebElementStateMatchers;
import net.serenitybdd.screenplay.waits.WaitUntil;
import org.cotizador.userinterfaces.pages.TechnicalInfoStepPage;

import java.util.List;
import java.util.Map;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class RegisterTechnicalInformation implements Task {
    @Override
    public <T extends Actor> void performAs(T actor) {
        List<Map<String, String>> technicalInfo = actor.recall("informacionTecnica");
        String opcionCobertura = "Fire Damage (GAR001)";
        actor.attemptsTo(
                WaitUntil.the(TechnicalInfoStepPage.COVERAGE_MAT_SELECT, WebElementStateMatchers.isEnabled())
                        .forNoMoreThan(15).seconds(),
                Click.on(TechnicalInfoStepPage.COVERAGE_MAT_SELECT),
                Click.on(TechnicalInfoStepPage.COVERAGE_MAT_OPTION.of(opcionCobertura)),
                Click.on(TechnicalInfoStepPage.COVERAGE_MAT_SELECT)
        );
        // Ingresar los campos técnicos dinámicamente según la tabla del feature
        for (int i = 0; i < technicalInfo.size(); i++) {
            Map<String, String> campo = technicalInfo.get(i);
            String numberRow = String.valueOf(i);
            String valor = campo.get("value");
            String type = campo.get("type");
            String key = campo.get("key");
            // Si no es el primer campo, agregar un nuevo campo layout
            if (i > 0) {
                actor.attemptsTo(
                        Click.on(TechnicalInfoStepPage.BTN_AGREGAR_CAMPO_LAYOUT)
                );
            }
            // El input se indexa desde 0
            String rowIndex = String.valueOf(i );
            actor.attemptsTo(
                    Enter.theValue(key).into(TechnicalInfoStepPage.TECHNICAL_KEY_INPUT.of(numberRow)),
                    Click.on(TechnicalInfoStepPage.TYPE_VALUE_MAT.of(numberRow)),
                    Click.on(TechnicalInfoStepPage.TYPE_VALUE_OPTION.of(type)),
                    Enter.theValue(valor).into(TechnicalInfoStepPage.TECHNICAL_VALUE_INPUT.of(rowIndex))
            );
        }

        actor.attemptsTo(
                Click.on(TechnicalInfoStepPage.SAVE_TECHNICAL_BUTTON),
                Click.on(TechnicalInfoStepPage.NEXT_BUTTON)
        );
    }

    public static RegisterTechnicalInformation withValidData() {
        return instrumented(RegisterTechnicalInformation.class);
    }
}
