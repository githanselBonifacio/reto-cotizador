package org.cotizador.questions;

import net.serenitybdd.core.Serenity;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
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
        Instant checkStartedAt = Instant.now();
        Path downloadDir = Paths.get(
                System.getProperty(
                        "qa.download.dir",
                        Paths.get(System.getProperty("user.home"), "Downloads").toString()
                )
        );

        long timeoutSeconds = Long.parseLong(System.getProperty("qa.download.timeout.seconds", "45"));
        Instant timeoutAt = Instant.now().plus(Duration.ofSeconds(timeoutSeconds));

        while (Instant.now().isBefore(timeoutAt)) {
            Optional<Path> downloadedFile = findDownloadedPdf(downloadDir, checkStartedAt);
            if (downloadedFile.isPresent()) {
                attachDownloadedPdfToEvidence(downloadedFile.get());
                    return true;
            }

            if (!Files.exists(downloadDir)) {
                return false;
            }

            waitHalfSecond();
        }
        return false;
    }

    private Optional<Path> findDownloadedPdf(Path downloadDir, Instant checkStartedAt) {
        try (Stream<Path> files = Files.list(downloadDir)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(this::isExpectedDownloadedPdf)
                    .filter(file -> wasDownloadedAfterCheckStarted(file, checkStartedAt))
                    .findFirst();
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    private boolean wasDownloadedAfterCheckStarted(Path file, Instant checkStartedAt) {
        try {
            Instant lastModified = Files.getLastModifiedTime(file).toInstant();
            return lastModified.isAfter(checkStartedAt.minusSeconds(1));
        } catch (IOException ignored) {
            return false;
        }
    }

    private void attachDownloadedPdfToEvidence(Path file) {
        try {
            Serenity.recordReportData()
                    .asEvidence()
                    .withTitle("PDF descargado")
                    .downloadable()
                    .fromFile(file);
        } catch (IOException ignored) {
            Serenity.recordReportData()
                    .withTitle("Ruta del PDF descargado")
                    .andContents(file.toAbsolutePath().toString());
        }
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

