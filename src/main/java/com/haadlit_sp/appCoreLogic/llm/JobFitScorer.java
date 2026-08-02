package com.haadlit_sp.appCoreLogic.llm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.haadlit_sp.appCoreLogic.model.ContactDetails;
import com.haadlit_sp.appCoreLogic.model.JobPosting;
import com.haadlit_sp.appCoreLogic.model.ProfileFacts;

import java.time.Duration;
import java.util.OptionalInt;


/**
 * Asks the local model how well the candidate fits one found posting, as a 0–100 score. Judged
 * from the search card only (title, company, location, snippet) — opening every posting for its
 * full description would hammer Indeed; the card excerpt is the honest basis we have. Local
 * inference costs only time, so every found job gets a score while the user reviews the list.
 */
public class JobFitScorer {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final LlmClient client;

    public JobFitScorer(LlmRuntime runtime) {
        this.client = new LlmClient(runtime);
    }

    private static final String SYSTEM = """
            You estimate how well a candidate fits a job posting, judging ONLY from the resume and \
            the posting excerpt provided. Reply with JSON {"score": N} where N is 0-100:
            - 90-100: the candidate's experience matches the role directly.
            - 60-89: adjacent field or most requirements met.
            - 30-59: some transferable skills, but a different line of work.
            - 0-29: little to no overlap.
            Judge honestly; do not inflate. Reply ONLY with the JSON.""";

    /** The fit score for this posting, or empty when the model is unavailable or answers junk. */
    public OptionalInt score(JobPosting posting, ProfileFacts facts, ContactDetails contact) {
        StringBuilder user = new StringBuilder(ScreenerPrompt.profileBlock(facts, contact));
        user.append("\nJOB POSTING\n")
                .append("Title: ").append(posting.title()).append('\n')
                .append("Company: ").append(posting.company()).append('\n');
        if (!posting.location().isBlank()) {
            user.append("Location: ").append(posting.location()).append('\n');
        }
        if (!posting.snippet().isBlank()) {
            user.append("Excerpt: ").append(posting.snippet()).append('\n');
        }
        user.append("\nHow well does the candidate fit this job? Reply {\"score\": 0-100}.\n");

        return client.complete(SYSTEM, user.toString(), schema(), TIMEOUT)
                .flatMap(JobFitScorer::parse)
                .map(OptionalInt::of)
                .orElse(OptionalInt.empty());
    }

    private static java.util.Optional<Integer> parse(String raw) {
        try {
            int score = JsonParser.parseString(raw).getAsJsonObject().get("score").getAsInt();
            return java.util.Optional.of(Math.max(0, Math.min(100, score)));
        } catch (RuntimeException e) {
            return java.util.Optional.empty();
        }
    }

    private static JsonObject schema() {
        JsonObject score = new JsonObject();
        score.addProperty("type", "integer");
        score.addProperty("minimum", 0);
        score.addProperty("maximum", 100);
        JsonObject properties = new JsonObject();
        properties.add("score", score);
        com.google.gson.JsonArray required = new com.google.gson.JsonArray();
        required.add("score");
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", properties);
        schema.add("required", required);
        schema.addProperty("additionalProperties", false);
        return schema;
    }
}
