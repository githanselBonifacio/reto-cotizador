package org.cotizador.userinterfaces.pages;

import net.serenitybdd.core.pages.PageObject;
import net.serenitybdd.screenplay.targets.Target;

import static org.openqa.selenium.By.cssSelector;

public class TechnicalInfoStepPage extends PageObject {

    public static final Target PAGE = Target.the("paso informacion tecnica")
            .located(cssSelector("button[data-testid='add-technical-info'], button.add-technical-info"));

    public static final Target FIRST_COVERAGE_CHECKBOX = Target.the("primera cobertura")
            .located(cssSelector("mat-checkbox input[type='checkbox'], input[type='checkbox'][name*='coverage']"));

    public static final Target ADD_TECHNICAL_ROW_BUTTON = Target.the("boton agregar dato tecnico")
            .located(cssSelector("button[data-testid='add-technical-info'], button.add-technical-info"));

    public static final Target BTN_AGREGAR_CAMPO_LAYOUT = Target.the("boton agregar campo layout")
            .located(cssSelector("button[data-testid='add-technical-info'], button.add-technical-info"));

    public static final Target TECHNICAL_KEY_INPUT = Target.the("llave tecnica")
            .located(cssSelector("input[name='technicalKey'], input[name='claveTecnica']"));

    public static final Target TECHNICAL_VALUE_INPUT = Target.the("valor tecnico")
            .located(cssSelector("input[name='technicalValue'], input[name='valorTecnico']"));

    public static final Target TECHNICAL_KEY_INPUT_BY_ROW = Target.the("llave tecnica en fila {0}")
            .locatedBy("(//input[@name='technicalKey' or @name='claveTecnica'])[{0}]");

    public static final Target TECHNICAL_VALUE_INPUT_BY_ROW = Target.the("valor tecnico en fila {0}")
            .locatedBy("(//input[@name='technicalValue' or @name='valorTecnico'])[{0}]");

    public static final Target LAYOUT_TYPE_SELECT_BY_ROW = Target.the("selector tipo layout en fila {0}")
            .locatedBy("(//mat-select[contains(@formcontrolname,'type') or contains(@name,'type')])[{0}]");

    public static final Target SAVE_TECHNICAL_BUTTON = Target.the("boton Guardar tecnico")
            .located(cssSelector("button[data-testid='save-technical'], button.save-technical"));

    public static final Target BTN_GUARDAR_TECNICO = Target.the("boton Guardar tecnico")
            .located(cssSelector("button[data-testid='save-technical'], button.save-technical"));

    public static final Target NEXT_BUTTON = Target.the("boton Siguiente")
            .located(cssSelector("button[data-testid='next-step'], button.next-step, button[type='submit']"));

    public static final Target BTN_SIGUIENTE = Target.the("boton siguiente informacion tecnica")
            .located(cssSelector("button[data-testid='next-step'], button.next-step, button[type='submit']"));
}
