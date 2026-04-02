package org.cotizador.userinterfaces.pages;

import net.serenitybdd.core.pages.PageObject;
import net.serenitybdd.screenplay.targets.Target;
import org.openqa.selenium.By;

import static org.openqa.selenium.By.cssSelector;

public class TechnicalInfoStepPage extends PageObject {


    public static final Target COVERAGE_MAT_SELECT = Target.the("primera cobertura")
            .located(By.id("qc-select-coberturas"));

    public static final Target COVERAGE_MAT_OPTION = Target.the("boton agregar dato tecnico")
            .locatedBy("//mat-option//span[contains(text(),'{0}')]");

    public static final Target BTN_AGREGAR_CAMPO_LAYOUT = Target.the("boton agregar campo layout")
            .located(By.id("qc-btn-layout-add"));

    public static final Target TECHNICAL_KEY_INPUT = Target.the("llave tecnica")
            .locatedBy("//*[@id='qc-input-layout-key-{0}']");


    public static final Target TYPE_VALUE_MAT = Target.the("campo valor tecnico")
            .locatedBy("//*[@id='qc-select-layout-type-{0}']");

    public static final Target TYPE_VALUE_OPTION = Target.the("campo valor tecnico")
            .locatedBy("//mat-option/span[text()='{0}']");

    public static final Target TECHNICAL_VALUE_INPUT = Target.the("campo valor tecnico por fila")
            .locatedBy("//*[@id='qc-input-layout-value-{0}']");

    public static final Target SAVE_TECHNICAL_BUTTON = Target.the("boton Guardar tecnico")
            .located(By.id("qc-btn-technical-save"));


    public static final Target NEXT_BUTTON = Target.the("boton Siguiente")
            .located(By.id("qc-btn-technical-next"));

}
