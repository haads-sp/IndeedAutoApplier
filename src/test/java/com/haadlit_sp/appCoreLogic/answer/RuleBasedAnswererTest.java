package com.haadlit_sp.appCoreLogic.answer;

import com.haadlit_sp.appCoreLogic.model.Answer;
import com.haadlit_sp.appCoreLogic.model.ProfileFacts;
import com.haadlit_sp.appCoreLogic.model.ScreenerQuestion;
import com.haadlit_sp.appCoreLogic.model.ScreenerQuestion.QuestionType;
import com.haadlit_sp.appCoreLogic.store.QaBankStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleBasedAnswererTest {

    @TempDir
    Path dir;

    private final RuleBasedAnswerer answerer = new RuleBasedAnswerer();
    private final ProfileFacts facts = new ProfileFacts("Pat Doe", "pat@x.com", "4165550100",
            List.of("reception"), 3, "Diploma", List.of(), "Canadian citizen", "resume text");

    private QaBankStore bank() {
        return new QaBankStore(dir.resolve("qa-bank.tsv"));
    }

    private static ScreenerQuestion number(String text) {
        return new ScreenerQuestion("q1", text, QuestionType.NUMBER, List.of(), true);
    }

    @Test
    void genericYearsOfExperienceIsAnsweredFromTheResumeTotal() {
        for (String text : List.of(
                "How many years of experience do you have?",
                "How many years of work experience do you have?",
                "Years of relevant experience?")) {
            Optional<Answer> answer = answerer.answer(number(text), facts, bank());
            assertTrue(answer.isPresent(), text);
            assertEquals("3", answer.get().primary(), text);
            assertEquals(Answer.Source.RULE, answer.get().source(), text);
        }
    }

    @Test
    void skillSpecificYearsQuestionsAreNotAnsweredWithTheTotal() {
        for (String text : List.of(
                "How many years of AZ driving experience do you have?",
                "How many years of dental receptionist experience do you have?",
                "How many years of experience with forklifts do you have?",
                "How many years of experience in customer service do you have?")) {
            assertTrue(answerer.answer(number(text), facts, bank()).isEmpty(), text);
        }
    }

    @Test
    void freeTextPayQuestionsGetNegotiable() {
        var q = new ScreenerQuestion("q1", "What are your minimum rate/salary expectations?",
                QuestionType.TEXT, List.of(), true);
        Optional<Answer> answer = answerer.answer(q, facts, bank());
        assertTrue(answer.isPresent());
        assertEquals("Negotiable", answer.get().primary());
    }

    @Test
    void numericPayQuestionsStillPause() {
        var q = new ScreenerQuestion("q1", "What is your expected hourly pay?",
                QuestionType.NUMBER, List.of(), true);
        assertTrue(answerer.answer(q, facts, bank()).isEmpty());
    }

    @Test
    void howDidYouHearPrefersIndeedThenJobBoardThenOther() {
        var withIndeed = new ScreenerQuestion("q1", "How did you hear about this job?",
                QuestionType.SINGLE_CHOICE, List.of("A friend", "Indeed", "Other"), true);
        assertEquals("Indeed", answerer.answer(withIndeed, facts, bank()).orElseThrow().primary());

        var withBoard = new ScreenerQuestion("q1", "How did you hear about this job?",
                QuestionType.SINGLE_CHOICE, List.of("A friend", "Job board", "Other"), true);
        assertEquals("Job board", answerer.answer(withBoard, facts, bank()).orElseThrow().primary());

        var otherOnly = new ScreenerQuestion("q1", "How did you hear about this position?",
                QuestionType.SINGLE_CHOICE, List.of("A friend", "Career fair", "Other"), true);
        assertEquals("Other", answerer.answer(otherOnly, facts, bank()).orElseThrow().primary());
    }

    @Test
    void howDidYouHearAsFreeTextSaysIndeed() {
        var q = new ScreenerQuestion("q1", "How did you hear about this job?",
                QuestionType.TEXT, List.of(), true);
        assertEquals("Indeed", answerer.answer(q, facts, bank()).orElseThrow().primary());
    }

    @Test
    void openEndedAvailabilityGetsAFlexibleAnswer() {
        var q = new ScreenerQuestion("q1", "Please detail your days and hours of working "
                + "availability below.", QuestionType.TEXT, List.of(), true);
        assertTrue(answerer.answer(q, facts, bank()).orElseThrow().primary().startsWith("Flexible"));
    }

    @Test
    void skillSpecificYearsWithRangeOptionsAlsoDeclines() {
        var q = new ScreenerQuestion("q1", "How many years of AZ driving experience do you have?",
                QuestionType.SINGLE_CHOICE,
                List.of("None", "Under 1 year", "1-2 years", "3-5 years", "5+ years"), true);
        assertTrue(answerer.answer(q, facts, bank()).isEmpty());
    }
}
