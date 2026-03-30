package org.cotizador.userinterfaces.pages;

import net.serenitybdd.core.pages.PageObject;
import net.serenitybdd.screenplay.targets.Target;

import static org.openqa.selenium.By.cssSelector;

public class GeneralInfoStepPage extends PageObject {

        public static final Target PAGE = Target.the("paso informacion general")
                        .located(cssSelector("input[name='nombre'], input[name='name']"));

    public static final Target NOMBRE_INPUT = Target.the("campo nombre")
            .located(cssSelector("input[name='nombre'], input[name='name']"));

    public static final Target RFC_INPUT = Target.the("campo RFC")
            .located(cssSelector("input[name='rfc'], input[name='taxId']"));

    public static final Target CODIGO_AGENTE_INPUT = Target.the("campo codigo agente")
            .located(cssSelector("input[name='codigoAgente'], input[name='agentCode']"));

    public static final Target AGENTE_MAT_SELECT = Target.the("selector de agente")
            .located(cssSelector("mat-select[formcontrolname='codigoAgente'], mat-select[name='codigoAgente'], mat-select[data-testid='agent-select']"));

    public static final Target SAVE_BUTTON = Target.the("boton guardar informacion general")
            .located(cssSelector("button[data-testid='save-general-info'], button.save-general-info"));

    public static final Target NEXT_BUTTON = Target.the("boton Siguiente")
            .located(cssSelector("button[data-testid='next-step'], button.next-step, button[type='submit']"));
}
