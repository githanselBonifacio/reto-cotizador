package co.com.sura.cotizador.stepsdefinitions;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Open;
import net.serenitybdd.screenplay.actors.OnStage;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.cotizador.userinterfaces.pages.HomePage;
import org.openqa.selenium.WebDriver;
import net.thucydides.core.annotations.Managed;

import java.util.Map;


public class CrearCotizacionStepdefinition {
    @Managed(driver = "chrome")
    WebDriver driver;
    Actor usuario;

    @Given("el usuario inicia el proceso de cotizacion con:")
    public void elUsuarioIniciaElProcesoDeCotizacionCon(DataTable dataTable) {
        usuario = OnStage.theActorCalled("Usuario");
        usuario.can(BrowseTheWeb.with(driver));
        usuario.wasAbleTo(Open.browserOn().the(new HomePage()));
        // Presionar el botón para iniciar la cotización
        usuario.attemptsTo(Click.on(HomePage.BTN_CREAR_NUEVA_COTIZACION));
        // Recuperar los datos de la tabla del feature
        Map<String, String> datos = dataTable.asMaps().get(0);
        // Puedes guardar estos datos en el contexto del actor si lo necesitas para los siguientes pasos
        usuario.remember("datosCotizacion", datos);
    }

    @And("registra las ubicaciones:")
    public void registraLasUbicaciones() {
    }

    @And("configura la informacion tecnica:")
    public void configuraLaInformacionTecnica() {
    }

    @When("completa el flujo hasta calcular la prima y descargar el PDF")
    public void completaElFlujoHastaCalcularLaPrimaYDescargarElPDF() {
    }

    @Then("el estado final de la cotizacion debe ser {string}")
    public void elEstadoFinalDeLaCotizacionDebeSer(String arg0) {
    }

    @And("se debe descargar el archivo PDF {string}")
    public void seDebeDescargarElArchivoPDF(String arg0) {
    }
}
