Feature: Creacion de cotizacion y descarga de PDF

  Scenario: crear cotización y descargar PDF

    Given el usuario inicia el proceso de cotizacion con:
      | nombre      | rfc           | codigoAgente        | expectedStatus |
      | QA Automat  | RFC123456789  | AG001 - John Carter | PENDING        |
    And registra las ubicaciones:
      | zipCode | giro                          | buildingValue | contentsValue |
      | 01000   | BL002 - Catastrophe Coverage  | 500000        | 120000        |
    And configura la informacion tecnica:
      | key               | value | type  |
      | Tipo Construccion | A     | Texto |
      | Nivel Riesgo      | Bajo  | Texto |
    When completa el flujo hasta calcular la prima y descargar el PDF
    Then el estado final de la cotizacion debe ser "CALCULATED"
