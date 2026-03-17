package net.unit8.rodriguez.proxy.store;

import net.unit8.rodriguez.proxy.model.FaultRule;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe in-memory store for fault injection rules.
 *
 * <p>Rules are matched in insertion order (first match wins).
 * When a rule's remaining count reaches zero, it is automatically removed.
 */
public class FaultRuleStore {
    private final ConcurrentHashMap<String, FaultRule> rules = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<String> ruleOrder = new CopyOnWriteArrayList<>();
    private final List<FaultRuleListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Listener for fault rule lifecycle events.
     */
    public interface FaultRuleListener {
        /**
         * Called when a new rule is added.
         *
         * @param rule the added rule
         */
        void onRuleAdded(FaultRule rule);

        /**
         * Called when a rule is consumed (remaining count decremented).
         *
         * @param rule      the consumed rule
         * @param remaining the remaining count after consumption
         */
        void onRuleConsumed(FaultRule rule, int remaining);

        /**
         * Called when a rule is removed (either consumed or manually deleted).
         *
         * @param rule the removed rule
         */
        void onRuleRemoved(FaultRule rule);
    }

    /**
     * Registers a listener for rule lifecycle events.
     *
     * @param listener the listener to add
     */
    public void addListener(FaultRuleListener listener) {
        listeners.add(listener);
    }

    /**
     * Adds a new fault rule to the store.
     *
     * @param rule the rule to add
     */
    public void addRule(FaultRule rule) {
        rules.put(rule.getId(), rule);
        ruleOrder.add(rule.getId());
        listeners.forEach(l -> l.onRuleAdded(rule));
    }

    /**
     * Removes a fault rule by ID.
     *
     * @param id the rule ID to remove
     */
    public void removeRule(String id) {
        FaultRule removed = rules.remove(id);
        if (removed != null) {
            ruleOrder.remove(id);
            listeners.forEach(l -> l.onRuleRemoved(removed));
        }
    }

    /**
     * Increments the remaining count of a rule by ID.
     *
     * @param id the rule ID
     * @return the updated rule, or empty if not found
     */
    public Optional<FaultRule> incrementRule(String id) {
        FaultRule rule = rules.get(id);
        if (rule == null) return Optional.empty();
        int remaining = rule.incrementAndGet();
        listeners.forEach(l -> l.onRuleConsumed(rule, remaining));
        return Optional.of(rule);
    }

    /**
     * Finds the first matching rule for the given path, decrements its counter,
     * and auto-removes it if the counter reaches zero.
     *
     * @param path the request path to match
     * @return the matched rule, or empty if no rule matches
     */
    public Optional<FaultRule> findAndConsume(String path) {
        for (String id : ruleOrder) {
            FaultRule rule = rules.get(id);
            if (rule == null) continue;
            if (rule.isExpired()) {
                rules.remove(id);
                ruleOrder.remove(id);
                listeners.forEach(l -> l.onRuleRemoved(rule));
                continue;
            }
            if (rule.matches(path)) {
                int remaining = rule.decrementAndGet();
                if (remaining <= 0) {
                    rules.remove(id);
                    ruleOrder.remove(id);
                    listeners.forEach(l -> l.onRuleRemoved(rule));
                } else {
                    listeners.forEach(l -> l.onRuleConsumed(rule, remaining));
                }
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }

    /**
     * Removes all fault rules from the store.
     */
    public void clearAll() {
        List<FaultRule> removed = new ArrayList<>(rules.values());
        rules.clear();
        ruleOrder.clear();
        removed.forEach(rule -> listeners.forEach(l -> l.onRuleRemoved(rule)));
    }

    /**
     * Returns all active rules in insertion order.
     *
     * @return unmodifiable list of active rules
     */
    public List<FaultRule> listRules() {
        List<FaultRule> result = new ArrayList<>();
        for (String id : ruleOrder) {
            FaultRule rule = rules.get(id);
            if (rule != null) {
                result.add(rule);
            }
        }
        return Collections.unmodifiableList(result);
    }
}
