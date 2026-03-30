package org.cotizador.userinterfaces.pages;

import net.serenitybdd.core.pages.PageObject;
import net.serenitybdd.screenplay.targets.Target;

import static org.openqa.selenium.By.cssSelector;

public class LocationsStepPage extends PageObject {

    public static final Target PAGE = Target.the("paso ubicaciones")
            .located(cssSelector("button[data-testid='add-location'], button.add-location"));

    public static final Target ADD_LOCATION_BUTTON = Target.the("boton agregar ubicacion")
            .located(cssSelector("button[data-testid='add-location'], button.add-location"));

    public static final Target BTN_AGREGAR_UBICACION = Target.the("boton agregar ubicacion")
            .located(cssSelector("button[data-testid='add-location'], button.add-location"));

    public static final Target LOCATION_ZIPCODE_INPUT = Target.the("codigo postal de ubicacion")
            .located(cssSelector("input[name='zipCode'], input[name='postalCode']"));

    public static final Target LOCATION_BUILDING_VALUE_INPUT = Target.the("valor edificio de ubicacion")
            .located(cssSelector("input[name='buildingValue'], input[name='valorEdificio']"));

    public static final Target LOCATION_CONTENTS_VALUE_INPUT = Target.the("valor contenidos de ubicacion")
            .located(cssSelector("input[name='contentsValue'], input[name='valorContenidos']"));

    public static final Target LOCATION_ROW_BY_INDEX = Target.the("fila de ubicacion {0}")
            .locatedBy("(//*[self::tr or self::div][.//input[@name='zipCode' or @name='postalCode']])[{0}]");

    public static final Target NEXT_BUTTON = Target.the("boton Siguiente")
            .located(cssSelector("button[data-testid='next-step'], button.next-step, button[type='submit']"));

    public static final Target BTN_SIGUIENTE = Target.the("boton siguiente ubicaciones")
            .located(cssSelector("button[data-testid='next-step'], button.next-step, button[type='submit']"));
}
