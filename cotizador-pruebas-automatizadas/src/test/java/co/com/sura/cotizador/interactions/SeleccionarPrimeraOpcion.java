package co.com.sura.cotizador.interactions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.targets.Target;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isEnabled;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

public class SeleccionarPrimeraOpcion implements Interaction {

    private static final Target FIRST_VISIBLE_OPTION = Target.the("primera opcion visible del mat-select")
            .locatedBy("//div[contains(@class,'cdk-overlay-pane')]//mat-option[not(@aria-disabled='true')][1]");

    private final Target selectTarget;

    public SeleccionarPrimeraOpcion(Target selectTarget) {
        this.selectTarget = selectTarget;
    }

    public static SeleccionarPrimeraOpcion en(Target selectTarget) {
        return Tasks.instrumented(SeleccionarPrimeraOpcion.class, selectTarget);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                WaitUntil.the(selectTarget, isVisible()).forNoMoreThan(30).seconds(),
                WaitUntil.the(selectTarget, isEnabled()).forNoMoreThan(30).seconds(),
                Click.on(selectTarget),
                WaitUntil.the(FIRST_VISIBLE_OPTION, isVisible()).forNoMoreThan(30).seconds(),
                Click.on(FIRST_VISIBLE_OPTION)
        );
    }
}
