package org.cotizador.userinterfaces.pages;

import net.serenitybdd.core.pages.PageObject;
import net.serenitybdd.screenplay.targets.Target;
import org.openqa.selenium.By;

import static org.openqa.selenium.By.cssSelector;

public class GeneralInfoStepPage extends PageObject {

        public static final Target PAGE = Target.the("paso informacion general")
                        .located(cssSelector("input[name='nombre'], input[name='name']"));

    public static final Target NOMBRE_INPUT = Target.the("campo nombre")
            .located(By.id("qc-input-nombre"));

    public static final Target RFC_INPUT = Target.the("campo RFC")
            .located(By.id("qc-input-rfc"));

    public static final Target AGENTE_MAT_SELECT = Target.the("selector de agente")
            .located(cssSelector("mat-select[formcontrolname='codigoAgente'], mat-select[name='codigoAgente'], mat-select[data-testid='agent-select']"));

    public static final Target AGENTE_OPTION = Target.the("opción de agente")
        .locatedBy("//mat-option//span[contains(text(),'{0}')]");

    public static final Target SAVE_BUTTON = Target.the("boton guardar informacion general")
            .located(By.id("qc-btn-general-save"));

    public static final Target NEXT_BUTTON = Target.the("boton Siguiente")
            .located(By.id("qc-btn-general-next"));
}
