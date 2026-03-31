package co.com.sura.cotizador.runners;

import io.cucumber.junit.CucumberOptions;
import net.serenitybdd.cucumber.CucumberWithSerenity;
import org.junit.runner.RunWith;

@RunWith(CucumberWithSerenity.class)
@CucumberOptions(
    features = "src/test/resources/features/quote_creation.feature",
    glue = {"co.com.sura.cotizador"},
    snippets = CucumberOptions.SnippetType.CAMELCASE,
    plugin = {"pretty"},
    monochrome = true
)
public class QuoteE2ERunner {
}
