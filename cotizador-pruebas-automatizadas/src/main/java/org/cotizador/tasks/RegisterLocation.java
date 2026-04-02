package org.cotizador.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.matchers.WebElementStateMatchers;
import net.serenitybdd.screenplay.waits.WaitUntil;
import org.awaitility.Awaitility;
import org.cotizador.userinterfaces.pages.LocationDialogPage;
import org.cotizador.userinterfaces.pages.LocationsStepPage;

import java.util.List;
import java.util.Map;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class RegisterLocation implements Task {


    @Override
    public <T extends Actor> void performAs(T actor) {
       List< Map<String, String>> locations = actor.recall("ubicacion");

       for(Map<String, String> location : locations) {
           actor.attemptsTo(
                   WaitUntil.the(LocationsStepPage.ADD_LOCATION_BUTTON, WebElementStateMatchers.isEnabled())
                           .forNoMoreThan(15).seconds(),
                   Click.on(LocationsStepPage.ADD_LOCATION_BUTTON),
                   WaitUntil.the(LocationDialogPage.LOCATION_NAME_INPUT, WebElementStateMatchers.isVisible())
                           .forNoMoreThan(15).seconds(),

                   Enter.theValue("Ubicacion Principal").into(LocationDialogPage.LOCATION_NAME_INPUT),
                   Enter.theValue(location.get("zipCode")).into(LocationDialogPage.ZIP_CODE_INPUT),
                   Enter.theValue("Calle Falsa 123").into(LocationDialogPage.ADDRESS_INPUT),
                   Click.on(LocationDialogPage.GIRO_MAT_SELECT),
                   Click.on(LocationDialogPage.GIRO_OPTION.of(location.get("giro"))),
                   Enter.theValue("ABCDE").into(LocationDialogPage.FIRE_KEY_INPUT),
                   Enter.theValue("100000").into(LocationDialogPage.BUILDING_VALUE_INPUT),
                   Enter.theValue("50000").into(LocationDialogPage.CONTENTS_VALUE_INPUT),


                   WaitUntil.the(LocationDialogPage.SAVE_LOCATION_BUTTON, WebElementStateMatchers.isEnabled())
                           .forNoMoreThan(15).seconds(),
                   Click.on(LocationDialogPage.SAVE_LOCATION_BUTTON)

           );

       }
       actor.attemptsTo(
               WaitUntil.the(LocationDialogPage.BTN_NEXT, WebElementStateMatchers.isEnabled())
                       .forNoMoreThan(15).seconds(),
               Click.on(LocationDialogPage.BTN_NEXT)
       );

    }

    public static RegisterLocation withValidData() {
        return instrumented(RegisterLocation.class);
    }
}
