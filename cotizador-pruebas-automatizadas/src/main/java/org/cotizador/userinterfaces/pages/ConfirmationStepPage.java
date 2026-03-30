package org.cotizador.userinterfaces.pages;

import net.serenitybdd.core.pages.PageObject;
import net.serenitybdd.screenplay.targets.Target;

import static org.openqa.selenium.By.cssSelector;

public class ConfirmationStepPage extends PageObject {

        public static final Target PAGE = Target.the("paso confirmacion")
                        .located(cssSelector("[data-testid='quote-status'], .status, button[data-testid='download-pdf'], a[download$='.pdf']"));

    public static final Target FINAL_CONFIRMATION_BUTTON = Target.the("boton de confirmacion final")
            .located(cssSelector("button[data-testid='confirm-quote'], button.confirm-quote"));

    public static final Target FINAL_STATUS = Target.the("estado final de cotizacion")
            .located(cssSelector("[data-testid='quote-status'], .status"));

    public static final Target TXT_ESTADO_FINAL = Target.the("texto estado final")
            .located(cssSelector("[data-testid='quote-status'], .status"));

    public static final Target DOWNLOAD_PDF_BUTTON = Target.the("boton descargar PDF")
            .located(cssSelector("button[data-testid='download-pdf'], a[download$='.pdf']"));

    public static final Target BTN_GENERAR_PDF = Target.the("boton generar PDF")
            .located(cssSelector("button[data-testid='download-pdf'], a[download$='.pdf']"));

    public static final Target DOWNLOADED_PDF_BY_NAME = Target.the("archivo PDF descargado {0}")
            .locatedBy("a[href$='.pdf'][href*='{0}'], [data-testid='downloaded-file'][title*='{0}']");
}
