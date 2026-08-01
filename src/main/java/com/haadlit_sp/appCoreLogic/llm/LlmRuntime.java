package com.haadlit_sp.appCoreLogic.llm;

import java.util.function.Consumer;


/**
 * The local inference engine behind AI-enhanced answering. Implementations own the whole
 * lifecycle: getting the assets onto disk, starting the server process, health, and shutdown.
 * Everything is fail-soft — when the runtime can't come up the app keeps working in rule-based
 * standard behavior; callers just see {@link #isReady()} stay false.
 */
public interface LlmRuntime {

    /**
     * Install assets if missing, start (or adopt) the local server, and wait until it answers.
     * Blocking — call on a worker, never the EDT. Progress and failures are reported as
     * plain-language lines through {@code status}.
     *
     * @return true when the server is up and serving; false = AI unavailable this session
     */
    boolean ensureReady(Consumer<String> status);

    /** Cheap check used per-answer; true only while the server is believed up. */
    boolean isReady();

    /** Base URL of the running server, e.g. {@code http://127.0.0.1:8791}. Only valid when ready. */
    String baseUrl();

    /**
     * Tell the runtime a request to the server failed (connection refused, timeout). It may
     * attempt one background respawn per session; until that succeeds {@link #isReady()} is false.
     */
    void noteFailure();

    /** Stop the server process if this runtime started one. Safe to call when never started. */
    void shutdown();
}
