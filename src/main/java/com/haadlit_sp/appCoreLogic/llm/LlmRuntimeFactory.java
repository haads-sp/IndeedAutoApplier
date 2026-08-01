package com.haadlit_sp.appCoreLogic.llm;


/** Selects the local inference runtime. llama-server is the only variant so far. */
public final class LlmRuntimeFactory {

    private LlmRuntimeFactory() {}

    public static LlmRuntime create() {
        return new LlamaServerRuntime();
    }
}
