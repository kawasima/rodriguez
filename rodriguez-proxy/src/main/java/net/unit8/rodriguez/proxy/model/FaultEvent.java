package net.unit8.rodriguez.proxy.model;

/**
 * SSE event payload representing a fault rule lifecycle event.
 *
 * @param type        event type: "rule-added", "rule-consumed", or "rule-removed"
 * @param ruleId      the unique rule identifier
 * @param faultType   the fault behavior name
 * @param pathPattern the path pattern of the rule
 * @param remaining   the remaining request count after this event
 */
public record FaultEvent(
        String type,
        String ruleId,
        String faultType,
        String pathPattern,
        int remaining
) {
}
