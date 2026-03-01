package net.unit8.rodriguez.proxy;

import net.unit8.rodriguez.proxy.model.FaultRule;
import net.unit8.rodriguez.proxy.store.FaultRuleStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FaultRuleStoreTest {
    private FaultRuleStore store;
    private List<String> events;

    @BeforeEach
    void setUp() {
        store = new FaultRuleStore();
        events = new ArrayList<>();
        store.addListener(new FaultRuleStore.FaultRuleListener() {
            @Override
            public void onRuleAdded(FaultRule rule) {
                events.add("added:" + rule.getFaultType());
            }

            @Override
            public void onRuleConsumed(FaultRule rule, int remaining) {
                events.add("consumed:" + rule.getFaultType() + ":" + remaining);
            }

            @Override
            public void onRuleRemoved(FaultRule rule) {
                events.add("removed:" + rule.getFaultType());
            }
        });
    }

    @Test
    void shouldAddAndListRules() {
        store.addRule(new FaultRule("/api/.*", "SlowResponse", 10205, 3));
        store.addRule(new FaultRule("/web/.*", "BrokenJson", 10208, 1));

        assertThat(store.listRules()).hasSize(2);
        assertThat(events).containsExactly("added:SlowResponse", "added:BrokenJson");
    }

    @Test
    void shouldMatchAndConsumeRule() {
        store.addRule(new FaultRule("/api/.*", "SlowResponse", 10205, 2));

        Optional<FaultRule> matched = store.findAndConsume("/api/users");
        assertThat(matched).isPresent();
        assertThat(matched.get().getFaultType()).isEqualTo("SlowResponse");
        assertThat(matched.get().getFaultPort()).isEqualTo(10205);
        assertThat(events).contains("consumed:SlowResponse:1");
    }

    @Test
    void shouldAutoRemoveWhenCountReachesZero() {
        store.addRule(new FaultRule("/api/.*", "SlowResponse", 10205, 1));

        Optional<FaultRule> matched = store.findAndConsume("/api/users");
        assertThat(matched).isPresent();
        assertThat(store.listRules()).isEmpty();
        assertThat(events).contains("removed:SlowResponse");
    }

    @Test
    void shouldReturnEmptyWhenNoMatch() {
        store.addRule(new FaultRule("/api/.*", "SlowResponse", 10205, 3));

        Optional<FaultRule> matched = store.findAndConsume("/web/dashboard");
        assertThat(matched).isEmpty();
    }

    @Test
    void shouldMatchFirstRuleInInsertionOrder() {
        store.addRule(new FaultRule("/api/.*", "SlowResponse", 10205, 3));
        store.addRule(new FaultRule("/api/users.*", "BrokenJson", 10208, 3));

        Optional<FaultRule> matched = store.findAndConsume("/api/users/1");
        assertThat(matched).isPresent();
        assertThat(matched.get().getFaultType()).isEqualTo("SlowResponse");
    }

    @Test
    void shouldRemoveRuleById() {
        FaultRule rule = new FaultRule("/api/.*", "SlowResponse", 10205, 3);
        store.addRule(rule);

        store.removeRule(rule.getId());
        assertThat(store.listRules()).isEmpty();
        assertThat(events).contains("removed:SlowResponse");
    }

    @Test
    void shouldDecrementMultipleTimes() {
        store.addRule(new FaultRule("/api/.*", "SlowResponse", 10205, 3));

        store.findAndConsume("/api/a");
        store.findAndConsume("/api/b");
        assertThat(store.listRules()).hasSize(1);
        assertThat(store.listRules().getFirst().getRemaining()).isEqualTo(1);

        store.findAndConsume("/api/c");
        assertThat(store.listRules()).isEmpty();
    }
}
