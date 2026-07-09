package net.unit8.rodriguez.proxy;

import net.unit8.rodriguez.proxy.model.FaultRule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
}
