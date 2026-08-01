package com.haadlit_sp.appCoreLogic.llm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * First-run setup for the local AI: downloads the inference engine and model weights into
 * {@code ~/.indeedapplier/llm/} with resume + sha256 verification, so it happens once and a future
 * installer can pre-bundle the same layout. Fail-soft: any unrecoverable problem returns false and
 * the app continues without AI.
 */
public class LlmAssetInstaller {

    private static final Logger LOG = System.getLogger(LlmAssetInstaller.class.getName());
    private static final int ATTEMPTS = 3;

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    /**
     * Ensure engine + model are present and plausible. Blocking (a first run downloads ~1.2 GB);
     * progress goes to {@code status} as plain language.
     */
    public boolean ensureInstalled(Consumer<String> status) {
        try {
            return engineInstalled(status) && modelInstalled(status);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean engineInstalled(Consumer<String> status) throws InterruptedException {
        if (Files.exists(LlmAssets.serverExe())) {
            return true;
        }
        Path zip = LlmAssets.binDir().resolve("engine.zip");
        if (!download(LlmAssets.ENGINE_URL, zip, LlmAssets.ENGINE_SHA256, LlmAssets.ENGINE_SIZE,
                "AI engine", status)) {
            return false;
        }
        try {
            unzipEngine(zip);
            Files.deleteIfExists(zip);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not unpack the AI engine", e);
            return false;
        }
        return Files.exists(LlmAssets.serverExe());
    }

    private boolean modelInstalled(Consumer<String> status) throws InterruptedException {
        Path model = LlmAssets.modelFile();
        try {
            // Fast path: correct size = installed (a full sha of >1 GB on every launch is too slow;
            // the sha is verified once, right after download).
            if (Files.exists(model) && Files.size(model) == LlmAssets.MODEL_SIZE) {
                return true;
            }
        } catch (IOException ignored) {
            // fall through to a fresh download
        }
        return download(LlmAssets.MODEL_URL, model, LlmAssets.MODEL_SHA256, LlmAssets.MODEL_SIZE,
                "AI model", status);
    }

    /** Download with resume (Range) into {@code target.part}, verify sha256, atomically move. */
    private boolean download(String url, Path target, String sha256, long expectedSize,
                             String label, Consumer<String> status) throws InterruptedException {
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            try {
                Files.createDirectories(target.getParent());
                Path part = target.resolveSibling(target.getFileName() + ".part");
                fetch(url, part, expectedSize, label, status);

                status.accept("Setting up AI features — verifying " + label + "…");
                String actual = sha256Of(part);
                if (!sha256.equalsIgnoreCase(actual)) {
                    LOG.log(Level.WARNING, "{0} sha256 mismatch (attempt {1}): {2}",
                            label, attempt, actual);
                    Files.deleteIfExists(part);
                    continue;
                }
                Files.move(part, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
                return true;
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Could not download " + label + " (attempt " + attempt + ")", e);
            }
        }
        status.accept("AI setup failed — could not download the " + label
                + ". Continuing without AI; it will retry next launch.");
        return false;
    }

    private void fetch(String url, Path part, long expectedSize, String label,
                       Consumer<String> status) throws IOException, InterruptedException {
        long have = Files.exists(part) ? Files.size(part) : 0;
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url));
        if (have > 0) {
            request.header("Range", "bytes=" + have + "-");
        }
        HttpResponse<InputStream> response =
                http.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() == 200 && have > 0) {
            have = 0;   // server ignored the range; start over
        } else if (response.statusCode() != 200 && response.statusCode() != 206) {
            throw new IOException(label + " download failed: HTTP " + response.statusCode());
        }
        long done = have;
        long lastReport = 0;
        try (InputStream in = response.body();
             OutputStream out = Files.newOutputStream(part,
                     have > 0 ? StandardOpenOption.APPEND : StandardOpenOption.CREATE,
                     have > 0 ? StandardOpenOption.WRITE : StandardOpenOption.TRUNCATE_EXISTING)) {
            byte[] buffer = new byte[64 * 1024];
            int n;
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
                done += n;
                long now = System.currentTimeMillis();
                if (now - lastReport > 1000) {
                    lastReport = now;
                    status.accept(String.format(
                            "Setting up AI features — downloading %s %d%% (%d MB / %d MB)",
                            label, done * 100 / expectedSize, done >> 20, expectedSize >> 20));
                }
            }
        }
    }

    /** The release zip is flat; we keep only the server exe and its dlls, by bare filename. */
    private static void unzipEngine(Path zip) throws IOException {
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
            for (ZipEntry entry; (entry = in.getNextEntry()) != null; ) {
                Path name = Path.of(entry.getName()).getFileName();
                String file = name.toString();
                if (!entry.isDirectory()
                        && (file.equals("llama-server.exe") || file.endsWith(".dll"))) {
                    Files.copy(in, LlmAssets.binDir().resolve(name),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static String sha256Of(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[1024 * 1024];
                for (int n; (n = in.read(buffer)) > 0; ) {
                    digest.update(buffer, 0, n);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException(e);   // SHA-256 always exists on the JDK
        }
    }
}
