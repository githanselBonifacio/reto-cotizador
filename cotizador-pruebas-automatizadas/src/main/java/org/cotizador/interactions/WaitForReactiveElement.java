package org.cotizador.interactions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.targets.Target;
import net.serenitybdd.screenplay.waits.WaitUntil;
import net.serenitybdd.screenplay.matchers.WebElementStateMatchers;

public class WaitForReactiveElement implements Interaction {

    private final Target target;
    private final int seconds;

    public WaitForReactiveElement(Target target, int seconds) {
        this.target = target;
        this.seconds = seconds;
    }

    public static WaitForReactiveElement onPresenceOf(Target target) {
        return Tasks.instrumented(WaitForReactiveElement.class, target, 20);
    }

    public static WaitForReactiveElement onPresenceOf(Target target, int seconds) {
        return Tasks.instrumented(WaitForReactiveElement.class, target, seconds);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                WaitUntil.the(target, WebElementStateMatchers.isPresent())
                        .forNoMoreThan(seconds)
                        .seconds(),
                WaitUntil.the(target, WebElementStateMatchers.isVisible())
                        .forNoMoreThan(seconds)
                        .seconds()
        );
    }
}

