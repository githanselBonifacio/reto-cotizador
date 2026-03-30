package org.cotizador.userinterfaces.pages;

import net.serenitybdd.core.pages.PageObject;
import net.serenitybdd.screenplay.targets.Target;

import static org.openqa.selenium.By.cssSelector;

public class TermsAndConditionsStepPage extends PageObject {

    public static final Target PAGE = Target.the("paso terminos y condiciones")
            .located(cssSelector("input[name='acceptTerms'], input[type='checkbox'][id='acceptTerms']"));

    public static final Target ACCEPT_TERMS_CHECKBOX = Target.the("checkbox aceptar terminos")
            .located(cssSelector("input[name='acceptTerms'], input[type='checkbox'][id='acceptTerms']"));

    public static final Target CHK_ACEPTAR_TERMINOS = Target.the("checkbox aceptar terminos")
            .located(cssSelector("input[name='acceptTerms'], input[type='checkbox'][id='acceptTerms']"));

    public static final Target ACCEPT_PRIVACY_CHECKBOX = Target.the("checkbox aceptar privacidad")
            .located(cssSelector("input[name='acceptPrivacy'], input[type='checkbox'][id='acceptPrivacy']"));

    public static final Target NEXT_BUTTON = Target.the("boton Siguiente")
            .located(cssSelector("button[data-testid='next-step'], button.next-step, button[type='submit']"));

    public static final Target BTN_CONTINUAR_A_CALCULO = Target.the("boton continuar a calculo")
            .located(cssSelector("button[data-testid='next-step'], button.next-step, button[type='submit']"));
}
