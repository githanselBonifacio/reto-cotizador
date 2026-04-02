package org.cotizador.userinterfaces.pages;

import net.serenitybdd.core.pages.PageObject;
import net.serenitybdd.screenplay.targets.Target;
import org.openqa.selenium.By;

import static org.openqa.selenium.By.cssSelector;

public class ConfirmationStepPage extends PageObject {



    public static final Target FINAL_STATUS = Target.the("estado final de cotizacion")
            .located(cssSelector("body > app-root > main > app-quote-creation > section > mat-card.mat-mdc-card.mdc-card.folio-card > div:nth-child(3) > h3"));



    public static final Target DOWNLOAD_PDF_BUTTON = Target.the("boton descargar PDF")
            .located(By.id("qc-btn-generate-pdf"));


}
