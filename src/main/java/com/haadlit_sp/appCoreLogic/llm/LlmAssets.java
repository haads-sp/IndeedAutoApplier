package com.haadlit_sp.appCoreLogic.llm;

import com.haadlit_sp.appCoreLogic.store.AppPaths;

import java.nio.file.Path;


/**
 * The pinned local-AI assets: exact engine release, exact model file, their sha256s, and where
 * they live on disk. This is the ONE place a version bump (or a future installer that pre-bundles
 * the same layout) touches.
 *
 * <p>Engine: llama.cpp {@code llama-server} release b10221, win-cpu-x64 (MIT). Model:
 * Qwen3-1.7B Q4_K_M GGUF (Apache 2.0), quantised by unsloth. Pins verified 2026-08-01 —
 * sha256s computed from the downloaded release asset and HuggingFace's LFS metadata.
 */
final class LlmAssets {

    private LlmAssets() {}

    static final String ENGINE_URL =
            "https://github.com/ggml-org/llama.cpp/releases/download/b10221/llama-b10221-bin-win-cpu-x64.zip";
    static final String ENGINE_SHA256 =
            "61dd281e1c5be8a7ae0b403efd0feee9da126e40605c27df35f9d5bef49caaf9";
    static final long ENGINE_SIZE = 18_352_727L;

    static final String MODEL_FILE_NAME = "Qwen3-1.7B-Q4_K_M.gguf";
    static final String MODEL_URL =
            "https://huggingface.co/unsloth/Qwen3-1.7B-GGUF/resolve/main/" + MODEL_FILE_NAME;
    static final String MODEL_SHA256 =
            "b139949c5bd74937ad8ed8c8cf3d9ffb1e99c866c823204dc42c0d91fa181897";
    static final long MODEL_SIZE = 1_107_409_472L;

    static Path binDir() {
        return AppPaths.llmDir().resolve("bin");
    }

    static Path serverExe() {
        return binDir().resolve("llama-server.exe");
    }

    static Path modelFile() {
        return AppPaths.llmDir().resolve("models").resolve(MODEL_FILE_NAME);
    }
}
