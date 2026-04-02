package org.cotizador.userinterfaces.pages;

import net.serenitybdd.core.pages.PageObject;
import net.serenitybdd.screenplay.targets.Target;
import org.openqa.selenium.By;

import static org.openqa.selenium.By.cssSelector;

public class LocationDialogPage extends PageObject {

    public static final Target DIALOG_CONTAINER = Target.the("dialogo de ubicacion")
            .located(cssSelector("div[role='dialog'], .location-dialog"));

    public static final Target PAGE = Target.the("pagina del dialogo de ubicacion")
            .located(By.cssSelector("div[role='dialog'], .location-dialog"));

    public static final Target LOCATION_NAME_INPUT = Target.the("nombre de ubicacion")
            .located(By.id("loc-input-location-name"));

    public static final Target ZIP_CODE_INPUT = Target.the("campo codigo postal en dialogo")
            .located(By.id("loc-input-zip-code"));

    public static final Target BUILDING_VALUE_INPUT = Target.the("campo valor edificio en dialogo")
            .located(By.id("loc-input-building-value"));

    public static final Target CONTENTS_VALUE_INPUT = Target.the("campo valor contenidos en dialogo")
            .located(By.id("loc-input-contents-value"));

    public static final Target ADDRESS_INPUT = Target.the("direccion de ubicacion")
            .located(By.id("loc-input-address"));

    public static final Target FIRE_KEY_INPUT = Target.the("fire key de ubicacion")
            .located(By.id("loc-input-fire-key"));

    public static final Target GIRO_MAT_SELECT = Target.the("selector de giro")
            .located(By.id("loc-select-giro"));

    public static final Target GIRO_OPTION = Target.the("opción de giro")
            .locatedBy("//mat-option//span[contains(text(),'{0}')]");

    public static final Target INPUT_BY_NAME = Target.the("campo dinamico de ubicacion {0}")
            .locatedBy("div[role='dialog'] input[name='{0}']");

    public static final Target SAVE_LOCATION_BUTTON = Target.the("boton guardar ubicacion")
            .located(By.id("loc-btn-save"));

    public static final Target BTN_NEXT = Target.the("boton paso siguiente")
            .located(By.id("qc-btn-locations-next"));
}
