package co.com.sura.cotizador.utils;

import com.github.javafaker.Faker;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class DatosCotizacionFaker {

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final Faker faker = new Faker(new Locale("es", "MX"));

    public String nombreCompleto() {
        return faker.name().fullName();
    }

    public String rfcValido() {
        int prefixLength = ThreadLocalRandom.current().nextBoolean() ? 3 : 4;
        String lettersPrefix = randomLetters(prefixLength);
        String dateSection = faker.number().digits(6);
        String suffix = randomAlphanumeric(3);
        return (lettersPrefix + dateSection + suffix).toUpperCase(Locale.ROOT);
    }

    public String codigoPostal5Digitos() {
        return faker.number().digits(5);
    }

    public String direccion() {
        return faker.address().streetAddress();
    }

    public String fireKey() {
        return "FIRE-" + faker.bothify("??##").toUpperCase(Locale.ROOT);
    }

    public String buildingValue() {
        return String.valueOf(faker.number().numberBetween(500000, 9000000));
    }

    public String contentsValue() {
        return String.valueOf(faker.number().numberBetween(200000, 5000000));
    }

    public String layoutKey() {
        return "LAYOUT_" + faker.bothify("??##").toUpperCase(Locale.ROOT);
    }

    public String layoutValueTexto() {
        return faker.lorem().word() + "-" + faker.number().digits(3);
    }

    public String layoutValueNumero() {
        return String.valueOf(faker.number().numberBetween(1, 9999));
    }

    private String randomLetters(int size) {
        StringBuilder value = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            value.append((char) ('A' + ThreadLocalRandom.current().nextInt(26)));
        }
        return value.toString();
    }

    private String randomAlphanumeric(int size) {
        StringBuilder value = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            value.append(ALPHANUMERIC.charAt(ThreadLocalRandom.current().nextInt(ALPHANUMERIC.length())));
        }
        return value.toString();
    }
}
