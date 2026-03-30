Feature: Creacion de cotizacion y descarga de PDF

  Scenario: Complete insurance quote creation and PDF download
    Given el usuario inicia el proceso de cotizacion con:
      | nombre      | rfc           | codigoAgente | expectedStatus | expectedPdfName |
      | QA Automat  | RFC123456789  | AGT001       | CALCULATED     | cotizacion.pdf  |
    And registra las ubicaciones:
      | zipCode | buildingValue | contentsValue |
      | 110111  | 500000        | 120000        |
    And configura la informacion tecnica:
      | key              | value |
      | tipoConstruccion | A     |
      | nivelRiesgo      | Bajo  |
    When completa el flujo hasta calcular la prima y descargar el PDF
    Then el estado final de la cotizacion debe ser "CALCULATED"
    And se debe descargar el archivo PDF "cotizacion.pdf"
