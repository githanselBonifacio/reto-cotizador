package org.cotizador.questions;

import org.cotizador.userinterfaces.pages.ConfirmationStepPage;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.questions.Text;

import java.util.Locale;

public class QuoteStatus implements Question<String> {

    public static QuoteStatus value() {
        return new QuoteStatus();
    }

    @Override
    public String answeredBy(Actor actor) {
        return Text.of(ConfirmationStepPage.FINAL_STATUS)
                .answeredBy(actor)
                .trim()
                .toUpperCase(Locale.ROOT);
    }
}

