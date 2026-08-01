package com.haadlit_sp.appCoreLogic.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;


/**
 * Thin HTTP client for the local llama-server's OpenAI-compatible chat endpoint. The response
 * format is constrained by a JSON schema, which llama-server compiles to a grammar — the reply
 * cannot be malformed JSON or stray from the schema. Fail-soft: any error returns empty and is
 * reported to the runtime so it can decide the server is down.
 */
public class LlmClient {

    private static final Logger LOG = System.getLogger(LlmClient.class.getName());

    private final LlmRuntime runtime;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    public LlmClient(LlmRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * One schema-constrained completion. Blocking — call on the browser worker only.
     *
     * @return the model's raw JSON reply, or empty on any failure (timeout, refusal, server down)
     */
    public Optional<String> complete(String system, String user, JsonObject schema, Duration timeout) {
        if (!runtime.isReady()) {
            return Optional.empty();
        }
        JsonObject body = new JsonObject();
        JsonArray messages = new JsonArray();
        messages.add(message("system", system));
        messages.add(message("user", user));
        body.add("messages", messages);
        JsonObject jsonSchema = new JsonObject();
        jsonSchema.addProperty("name", "screener_answer");
        jsonSchema.add("schema", schema);
        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_schema");
        responseFormat.add("json_schema", jsonSchema);
        body.add("response_format", responseFormat);
        body.addProperty("temperature", 0);
        body.addProperty("max_tokens", 200);

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(runtime.baseUrl() + "/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response =
                    http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOG.log(Level.WARNING, "Local AI returned HTTP {0}", response.statusCode());
                return Optional.empty();
            }
            String content = JsonParser.parseString(response.body()).getAsJsonObject()
                    .getAsJsonArray("choices").get(0).getAsJsonObject()
                    .getAsJsonObject("message").get("content").getAsString();
            return Optional.of(content);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Local AI request failed", e);
            runtime.noteFailure();
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Local AI reply was not in the expected shape", e);
            return Optional.empty();
        }
    }

    private static JsonObject message(String role, String content) {
        JsonObject m = new JsonObject();
        m.addProperty("role", role);
        m.addProperty("content", content);
        return m;
    }
}
