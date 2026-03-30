package org.cotizador.userinterfaces.pages;

import net.serenitybdd.core.pages.PageObject;
import net.serenitybdd.screenplay.targets.Target;

import static org.openqa.selenium.By.cssSelector;

public class LocationDialogPage extends PageObject {

    public static final Target DIALOG_CONTAINER = Target.the("dialogo de ubicacion")
            .located(cssSelector("div[role='dialog'], .location-dialog"));

    public static final Target PAGE = Target.the("pagina del dialogo de ubicacion")
            .located(cssSelector("div[role='dialog'], .location-dialog"));

    public static final Target LOCATION_NAME_INPUT = Target.the("nombre de ubicacion")
            .located(cssSelector("div[role='dialog'] input[name='locationName'], div[role='dialog'] input[name='nombreUbicacion']"));

    public static final Target ZIP_CODE_INPUT = Target.the("campo codigo postal en dialogo")
            .located(cssSelector("div[role='dialog'] input[name='zipCode'], div[role='dialog'] input[name='postalCode']"));

    public static final Target BUILDING_VALUE_INPUT = Target.the("campo valor edificio en dialogo")
            .located(cssSelector("div[role='dialog'] input[name='buildingValue'], div[role='dialog'] input[name='valorEdificio']"));

    public static final Target CONTENTS_VALUE_INPUT = Target.the("campo valor contenidos en dialogo")
            .located(cssSelector("div[role='dialog'] input[name='contentsValue'], div[role='dialog'] input[name='valorContenidos']"));

    public static final Target ADDRESS_INPUT = Target.the("direccion de ubicacion")
            .located(cssSelector("div[role='dialog'] input[name='address'], div[role='dialog'] input[name='direccion']"));

    public static final Target FIRE_KEY_INPUT = Target.the("fire key de ubicacion")
            .located(cssSelector("div[role='dialog'] input[name='fireKey'], div[role='dialog'] input[name='claveIncendio']"));

    public static final Target GIRO_MAT_SELECT = Target.the("selector de giro")
            .located(cssSelector("div[role='dialog'] mat-select[formcontrolname='giro'], div[role='dialog'] mat-select[name='giro'], div[role='dialog'] mat-select[data-testid='giro-select']"));

    public static final Target INPUT_BY_NAME = Target.the("campo dinamico de ubicacion {0}")
            .locatedBy("div[role='dialog'] input[name='{0}']");

    public static final Target SAVE_LOCATION_BUTTON = Target.the("boton guardar ubicacion")
            .located(cssSelector("button[data-testid='save-location'], .location-dialog button[type='submit']"));

    public static final Target BTN_GUARDAR_UBICACION = Target.the("boton guardar ubicacion")
            .located(cssSelector("button[data-testid='save-location'], .location-dialog button[type='submit']"));
}
