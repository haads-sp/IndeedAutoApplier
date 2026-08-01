package com.haadlit_sp.appCoreLogic.llm;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


/**
 * Runs llama.cpp's {@code llama-server} as a managed background process on localhost and talks to
 * it over HTTP. Mirrors how the app launches Chrome: spawn via ProcessBuilder, own the lifecycle,
 * die cleanly on shutdown. An already-running server left by a previous session is adopted, not
 * duplicated. Logging is disabled and output discarded so prompt text (which contains the resume)
 * never reaches disk.
 */
public class LlamaServerRuntime implements LlmRuntime {

    private static final Logger LOG = System.getLogger(LlamaServerRuntime.class.getName());
    private static final int PORT_FIRST = 8791;
    private static final int PORT_LAST = 8799;
    private static final Duration PROBE_TIMEOUT = Duration.ofMillis(800);
    private static final long READY_TIMEOUT_MS = 120_000;   // model load; AV scans make it slow

    private final LlmAssetInstaller installer = new LlmAssetInstaller();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(500))
            .build();

    private volatile boolean ready = false;
    private volatile int port = PORT_FIRST;
    private volatile boolean respawnUsed = false;
    private Process process;   // guarded by synchronized (ensureReady/shutdown)

    @Override
    public synchronized boolean ensureReady(Consumer<String> status) {
        if (ready && (process == null || process.isAlive())) {
            return true;
        }
        if (!installer.ensureInstalled(status)) {
            return false;   // installer already reported the failure
        }
        for (int candidate = PORT_FIRST; candidate <= PORT_LAST; candidate++) {
            Probe probe = probe(candidate);
            if (probe == Probe.FREE) {
                return spawnAndWait(candidate, status);
            }
            if (probe == Probe.OURS) {
                LOG.log(Level.INFO, "Adopting llama-server already on port {0}", candidate);
                this.port = candidate;
                return waitUntilHealthy(candidate, status);
            }
            // Some other service — try the next port.
        }
        status.accept("AI features unavailable — no free local port. Continuing without AI.");
        return false;
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public String baseUrl() {
        return "http://127.0.0.1:" + port;
    }

    @Override
    public void noteFailure() {
        ready = false;
        if (respawnUsed) {
            return;   // one respawn per session; stay degraded
        }
        respawnUsed = true;
        Thread retry = new Thread(() -> ensureReady(msg -> {}), "llm-respawn");
        retry.setDaemon(true);
        retry.start();
    }

    @Override
    public synchronized void shutdown() {
        ready = false;
        if (process == null) {
            return;   // adopted or never started — leave it alone
        }
        process.destroy();
        try {
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
        process = null;
    }

    private boolean spawnAndWait(int candidate, Consumer<String> status) {
        status.accept("Starting local AI…");
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    LlmAssets.serverExe().toString(),
                    "-m", LlmAssets.modelFile().toString(),
                    "--host", "127.0.0.1",
                    "--port", String.valueOf(candidate),
                    "-c", "8192",
                    "--jinja",
                    "--reasoning-budget", "0",   // Qwen3: answer directly, no thinking tokens
                    "--no-webui",
                    "--log-disable");            // prompts contain the resume; keep them off disk
            builder.redirectErrorStream(true);
            builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            process = builder.start();
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "llm-shutdown"));
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not start llama-server", e);
            status.accept("AI features unavailable — the local AI could not start. Continuing without AI.");
            return false;
        }
        this.port = candidate;
        return waitUntilHealthy(candidate, status);
    }

    private boolean waitUntilHealthy(int candidate, Consumer<String> status) {
        long deadline = System.currentTimeMillis() + READY_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (process != null && !process.isAlive()) {
                status.accept("AI features unavailable — the local AI stopped unexpectedly. "
                        + "Continuing without AI.");
                return false;
            }
            Integer health = health(candidate);
            if (health != null && health == 200) {
                ready = true;
                status.accept("Local AI ready.");
                return true;
            }
            status.accept("Starting local AI — loading the model…");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        status.accept("AI features unavailable — the local AI did not become ready. Continuing without AI.");
        return false;
    }

    private enum Probe { FREE, OURS, OTHER }

    /** What lives on this port: nothing, a llama-server with our model, or something else. */
    private Probe probe(int candidate) {
        Integer health = health(candidate);
        if (health == null) {
            return Probe.FREE;
        }
        try {
            HttpResponse<String> props = http.send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + candidate + "/props"))
                            .timeout(PROBE_TIMEOUT).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (props.statusCode() == 200
                    && props.body().contains(LlmAssets.MODEL_FILE_NAME)) {
                return Probe.OURS;
            }
        } catch (IOException | InterruptedException ignored) {
            // fall through: responded to /health but can't confirm the model — not ours
        }
        return Probe.OTHER;
    }

    /** The server's /health status code, or null when nothing answers (port free). */
    private Integer health(int candidate) {
        try {
            HttpResponse<String> response = http.send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + candidate + "/health"))
                            .timeout(PROBE_TIMEOUT).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            return response.statusCode();
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
