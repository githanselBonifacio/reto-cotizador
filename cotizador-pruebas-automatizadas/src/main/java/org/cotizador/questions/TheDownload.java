package org.cotizador.questions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.stream.Stream;

public class TheDownload implements Question<Boolean> {

    private final String expectedPdfName;

    public TheDownload(String expectedPdfName) {
        this.expectedPdfName = expectedPdfName;
    }

    public static TheDownload named(String expectedPdfName) {
        return new TheDownload(expectedPdfName);
    }

    @Override
    public Boolean answeredBy(Actor actor) {
        Path downloadDir = Paths.get(
                System.getProperty(
                        "qa.download.dir",
                        Paths.get(System.getProperty("user.home"), "Downloads").toString()
                )
        );

        long timeoutSeconds = Long.parseLong(System.getProperty("qa.download.timeout.seconds", "45"));
        Instant timeoutAt = Instant.now().plus(Duration.ofSeconds(timeoutSeconds));

        while (Instant.now().isBefore(timeoutAt)) {
            try (Stream<Path> files = Files.list(downloadDir)) {
                boolean exists = files
                        .filter(Files::isRegularFile)
                        .anyMatch(this::isExpectedDownloadedPdf);
                if (exists) {
                    return true;
                }
            } catch (IOException ignored) {
                return false;
            }

            waitHalfSecond();
        }
        return false;
    }

    private boolean isExpectedDownloadedPdf(Path file) {
        String fileName = file.getFileName().toString();
        String normalized = fileName.toLowerCase(Locale.ROOT);

        if (!normalized.endsWith(".pdf")) {
            return false;
        }

        if (!isExpectedName(fileName)) {
            return false;
        }

        try {
            return Files.size(file) > 0;
        } catch (IOException ignored) {
            return false;
        }
    }

    private boolean isExpectedName(String fileName) {
        if (expectedPdfName == null || expectedPdfName.isBlank()) {
            return true;
        }

        String expectedLower = expectedPdfName.toLowerCase(Locale.ROOT);
        String fileNameLower = fileName.toLowerCase(Locale.ROOT);

        if (fileNameLower.equals(expectedLower)) {
            return true;
        }

        String expectedWithoutExtension = expectedLower.replace(".pdf", "");
        return fileNameLower.contains(expectedWithoutExtension);
    }

    private void waitHalfSecond() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}

