package net.unit8.rodriguez.proxy;

import net.unit8.rodriguez.proxy.model.FaultRule;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class FaultRuleTest {

    @Test
    void rejectsOverlyLongPathPattern() {
        String longPattern = "a".repeat(FaultRule.MAX_PATTERN_LENGTH + 1);
        assertThatThrownBy(() -> new FaultRule(longPattern, "SlowResponse", 10205, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum length");
    }

    @Test
    void acceptsPatternAtMaxLength() {
        String pattern = "a".repeat(FaultRule.MAX_PATTERN_LENGTH);
        FaultRule rule = new FaultRule(pattern, "SlowResponse", 10205, 1);
        assertThat(rule.getPathPattern()).hasSize(FaultRule.MAX_PATTERN_LENGTH);
    }

    @Test
    void doesNotMatchOversizedInputPath() {
        FaultRule rule = new FaultRule(".*", "SlowResponse", 10205, 1);
        String hugePath = "/" + "x".repeat(FaultRule.MAX_MATCH_INPUT_LENGTH);
        assertThat(rule.matches(hugePath)).isFalse();
        assertThat(rule.matches("/normal")).isTrue();
    }

    @Test
    void catastrophicBacktrackingPatternAbortsQuicklyAsNonMatch() {
        // (a+)+ against a long run of 'a' followed by a non-'a' forces exponential
        // backtracking in a naive engine. The step budget must abort it in bounded time.
        FaultRule rule = new FaultRule("(a+)+$", "SlowResponse", 10205, 1);
        String evil = "a".repeat(FaultRule.MAX_MATCH_INPUT_LENGTH - 1) + "!";
        boolean result = assertTimeoutPreemptively(
                Duration.ofSeconds(2), () -> rule.matches(evil));
        assertThat(result).isFalse();
    }

    @Test
    void stepBudgetDoesNotBreakNormalMatching() {
        FaultRule rule = new FaultRule("/api/users/\\d+", "SlowResponse", 10205, 1);
        assertThat(rule.matches("/api/users/12345")).isTrue();
        assertThat(rule.matches("/api/users/abc")).isFalse();
    }
}
