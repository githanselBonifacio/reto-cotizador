package org.cotizador.userinterfaces.pages;

import net.serenitybdd.core.pages.PageObject;
import net.serenitybdd.screenplay.targets.Target;
import org.openqa.selenium.By;

import static org.openqa.selenium.By.cssSelector;

public class TermsAndConditionsStepPage extends PageObject {


    public static final Target ACCEPT_PRIVACY_CHECKBOX = Target.the("checkbox aceptar privacidad")
            .located(By.id("qc-checkbox-accepted-terms-input"));

    public static final Target BTN_CONTINUAR_A_CALCULO = Target.the("boton Siguiente")
            .located(By.id("qc-btn-terms-next"));

}
