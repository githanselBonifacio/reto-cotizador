package co.com.sura.cotizador.hooks;

import co.com.sura.cotizador.stages.StageConfig;
import io.cucumber.java.Before;

public class Hooks {
    @Before
    public void setTheStage() {
        StageConfig.setTheStage();
    }
}
