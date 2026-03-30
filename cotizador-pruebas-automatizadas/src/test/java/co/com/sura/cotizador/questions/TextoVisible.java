package co.com.sura.cotizador.questions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;

import java.util.Locale;

public class TextoVisible implements Question<Boolean> {

    private final String textoEsperado;

    public TextoVisible(String textoEsperado) {
        this.textoEsperado = textoEsperado;
    }

    public static TextoVisible enPantalla(String textoEsperado) {
        return new TextoVisible(textoEsperado);
    }

    @Override
    public Boolean answeredBy(Actor actor) {
        String pageText = BrowseTheWeb.as(actor).getDriver().getPageSource();
        return pageText != null
                && pageText.toUpperCase(Locale.ROOT).contains(textoEsperado.toUpperCase(Locale.ROOT));
    }
}
