package co.com.sura.cotizador.tasks;

import co.com.sura.cotizador.interactions.SeleccionarPrimeraOpcion;
import co.com.sura.cotizador.questions.TextoVisible;
import co.com.sura.cotizador.utils.DatosCotizacionFaker;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.actions.Open;
import net.serenitybdd.screenplay.questions.Visibility;
import net.serenitybdd.screenplay.waits.WaitUntil;
import org.cotizador.userinterfaces.pages.CalculationSummaryStepPage;
import org.cotizador.userinterfaces.pages.ConfirmationStepPage;
import org.cotizador.userinterfaces.pages.GeneralInfoStepPage;
import org.cotizador.userinterfaces.pages.HomePage;
import org.cotizador.userinterfaces.pages.LocationDialogPage;
import org.cotizador.userinterfaces.pages.LocationsStepPage;
import org.cotizador.userinterfaces.pages.TechnicalInfoStepPage;
import org.cotizador.userinterfaces.pages.TermsAndConditionsStepPage;

import static net.serenitybdd.screenplay.GivenWhenThen.seeThat;
import static net.serenitybdd.screenplay.Tasks.instrumented;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.containsText;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isEnabled;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isNotVisible;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;
import static org.hamcrest.Matchers.is;

public class CrearCotizacionCompleta implements Task {

    private final DatosCotizacionFaker faker = new DatosCotizacionFaker();
    private final String nombre = faker.nombreCompleto();
    private final String rfc = faker.rfcValido();

    private HomePage homePage;
    private String estadoEsperado = "CALCULATED";
    private String factorComercialOpcional;

    public static CrearCotizacionCompleta deInicioAFin() {
        return instrumented(CrearCotizacionCompleta.class);
    }

    public CrearCotizacionCompleta conValidacionEstadoFinal(String estadoEsperado) {
        this.estadoEsperado = estadoEsperado;
        return this;
    }

    public CrearCotizacionCompleta conFactorComercialOpcional(String valor) {
        this.factorComercialOpcional = valor;
        return this;
    }

    @Override
    @Step("Crear cotizacion completa de inicio a fin")
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Open.browserOn(homePage),
                WaitUntil.the(HomePage.PAGE, isVisible()).forNoMoreThan(30).seconds()
        );

        if (Visibility.of(HomePage.BTN_CREAR_NUEVA_COTIZACION).answeredBy(actor)) {
            actor.attemptsTo(Click.on(HomePage.BTN_CREAR_NUEVA_COTIZACION));
        } else {
            actor.attemptsTo(Click.on(HomePage.NAVBAR_COTIZADOR_LINK));
        }

        actor.attemptsTo(
                WaitUntil.the(GeneralInfoStepPage.PAGE, isVisible()).forNoMoreThan(30).seconds(),
                Enter.theValue(nombre).into(GeneralInfoStepPage.NOMBRE_INPUT),
                Enter.theValue(rfc).into(GeneralInfoStepPage.RFC_INPUT)
        );

        if (Visibility.of(GeneralInfoStepPage.AGENTE_MAT_SELECT).answeredBy(actor)) {
            actor.attemptsTo(SeleccionarPrimeraOpcion.en(GeneralInfoStepPage.AGENTE_MAT_SELECT));
        } else {
            actor.attemptsTo(Enter.theValue(faker.layoutValueNumero()).into(GeneralInfoStepPage.CODIGO_AGENTE_INPUT));
        }

        if (Visibility.of(GeneralInfoStepPage.SAVE_BUTTON).answeredBy(actor)) {
            actor.attemptsTo(
                    WaitUntil.the(GeneralInfoStepPage.SAVE_BUTTON, isEnabled()).forNoMoreThan(30).seconds(),
                    Click.on(GeneralInfoStepPage.SAVE_BUTTON)
            );
        }

        actor.attemptsTo(
                WaitUntil.the(GeneralInfoStepPage.NEXT_BUTTON, isEnabled()).forNoMoreThan(30).seconds(),
                Click.on(GeneralInfoStepPage.NEXT_BUTTON),
                WaitUntil.the(LocationsStepPage.PAGE, isVisible()).forNoMoreThan(30).seconds(),
                Click.on(LocationsStepPage.BTN_AGREGAR_UBICACION),
                WaitUntil.the(LocationDialogPage.PAGE, isVisible()).forNoMoreThan(30).seconds(),
                Enter.theValue("UBI " + faker.layoutValueTexto()).into(LocationDialogPage.LOCATION_NAME_INPUT),
                Enter.theValue(faker.codigoPostal5Digitos()).into(LocationDialogPage.ZIP_CODE_INPUT),
                Enter.theValue(faker.direccion()).into(LocationDialogPage.ADDRESS_INPUT),
                Enter.theValue(faker.fireKey()).into(LocationDialogPage.FIRE_KEY_INPUT),
                Enter.theValue(faker.buildingValue()).into(LocationDialogPage.BUILDING_VALUE_INPUT),
                Enter.theValue(faker.contentsValue()).into(LocationDialogPage.CONTENTS_VALUE_INPUT)
        );

        if (Visibility.of(LocationDialogPage.GIRO_MAT_SELECT).answeredBy(actor)) {
            actor.attemptsTo(SeleccionarPrimeraOpcion.en(LocationDialogPage.GIRO_MAT_SELECT));
        }

        actor.attemptsTo(
                WaitUntil.the(LocationDialogPage.BTN_GUARDAR_UBICACION, isEnabled()).forNoMoreThan(30).seconds(),
                Click.on(LocationDialogPage.BTN_GUARDAR_UBICACION),
                WaitUntil.the(LocationDialogPage.DIALOG_CONTAINER, isNotVisible()).forNoMoreThan(30).seconds(),
                WaitUntil.the(LocationsStepPage.BTN_SIGUIENTE, isEnabled()).forNoMoreThan(30).seconds(),
                Click.on(LocationsStepPage.BTN_SIGUIENTE),
                WaitUntil.the(TechnicalInfoStepPage.PAGE, isVisible()).forNoMoreThan(30).seconds()
        );

        if (Visibility.of(TechnicalInfoStepPage.FIRST_COVERAGE_CHECKBOX).answeredBy(actor)) {
            actor.attemptsTo(Click.on(TechnicalInfoStepPage.FIRST_COVERAGE_CHECKBOX));
        }

        actor.attemptsTo(
                Click.on(TechnicalInfoStepPage.BTN_AGREGAR_CAMPO_LAYOUT),
                WaitUntil.the(TechnicalInfoStepPage.TECHNICAL_KEY_INPUT_BY_ROW.of("1"), isVisible()).forNoMoreThan(30).seconds(),
                Enter.theValue(faker.layoutKey()).into(TechnicalInfoStepPage.TECHNICAL_KEY_INPUT_BY_ROW.of("1"))
        );

        if (Visibility.of(TechnicalInfoStepPage.LAYOUT_TYPE_SELECT_BY_ROW.of("1")).answeredBy(actor)) {
            actor.attemptsTo(SeleccionarPrimeraOpcion.en(TechnicalInfoStepPage.LAYOUT_TYPE_SELECT_BY_ROW.of("1")));
        }

        String valorLayout = factorComercialOpcional == null || factorComercialOpcional.isBlank()
                ? faker.layoutValueTexto()
                : factorComercialOpcional;

        actor.attemptsTo(
                Enter.theValue(valorLayout).into(TechnicalInfoStepPage.TECHNICAL_VALUE_INPUT_BY_ROW.of("1")),
                WaitUntil.the(TechnicalInfoStepPage.BTN_GUARDAR_TECNICO, isEnabled()).forNoMoreThan(30).seconds(),
                Click.on(TechnicalInfoStepPage.BTN_GUARDAR_TECNICO),
                WaitUntil.the(TechnicalInfoStepPage.BTN_SIGUIENTE, isEnabled()).forNoMoreThan(30).seconds(),
                Click.on(TechnicalInfoStepPage.BTN_SIGUIENTE),
                WaitUntil.the(TermsAndConditionsStepPage.PAGE, isVisible()).forNoMoreThan(30).seconds()
        );

        if (!TermsAndConditionsStepPage.CHK_ACEPTAR_TERMINOS.resolveFor(actor).isSelected()) {
            actor.attemptsTo(Click.on(TermsAndConditionsStepPage.CHK_ACEPTAR_TERMINOS));
        }

        actor.attemptsTo(
                WaitUntil.the(TermsAndConditionsStepPage.BTN_CONTINUAR_A_CALCULO, isEnabled()).forNoMoreThan(30).seconds(),
                Click.on(TermsAndConditionsStepPage.BTN_CONTINUAR_A_CALCULO),
                WaitUntil.the(CalculationSummaryStepPage.PAGE, isVisible()).forNoMoreThan(30).seconds()
        );

        if (CalculationSummaryStepPage.BTN_RECALCULAR.resolveFor(actor).isCurrentlyEnabled()) {
            actor.attemptsTo(Click.on(CalculationSummaryStepPage.BTN_RECALCULAR));
        }

        actor.attemptsTo(
                WaitUntil.the(CalculationSummaryStepPage.BTN_CONFIRMAR_COTIZACION, isEnabled()).forNoMoreThan(30).seconds(),
                Click.on(CalculationSummaryStepPage.BTN_CONFIRMAR_COTIZACION),
                WaitUntil.the(ConfirmationStepPage.PAGE, isVisible()).forNoMoreThan(30).seconds(),
                WaitUntil.the(ConfirmationStepPage.TXT_ESTADO_FINAL, containsText(estadoEsperado)).forNoMoreThan(30).seconds(),
                WaitUntil.the(ConfirmationStepPage.BTN_GENERAR_PDF, isEnabled()).forNoMoreThan(30).seconds(),
                Click.on(ConfirmationStepPage.BTN_GENERAR_PDF)
        );

        actor.should(seeThat(TextoVisible.enPantalla(estadoEsperado), is(true)));
    }
}
