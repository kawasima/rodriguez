package net.unit8.rodriguez.fuse;

import net.unit8.rodriguez.fuse.fault.FuseFault;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * A rule that maps a path pattern and a set of FUSE operations to a specific fault.
 *
 * <p>When a FUSE operation occurs, fault rules are evaluated in order. The first rule
 * whose path pattern and operation set match the request determines the fault to apply.
 */
public class FaultRule {
    private Pattern pathPattern;
    private Set<FuseOperation> operations;
    private FuseFault fault;

    /**
     * Constructs a new {@code FaultRule} with no path pattern, operations, or fault.
     * Used for Jackson deserialization.
     */
    public FaultRule() {
    }

    /**
     * Constructs a new {@code FaultRule} with the specified path pattern, operations, and fault.
     *
     * @param pathPattern a regular expression to match against the file path, or {@code null} to match all paths
     * @param operations  the set of FUSE operations this rule applies to, or {@code null} to match all operations
     * @param fault       the fault to apply when this rule matches
     */
    public FaultRule(String pathPattern, Set<FuseOperation> operations, FuseFault fault) {
        this.pathPattern = pathPattern != null ? Pattern.compile(pathPattern) : null;
        this.operations = operations;
        this.fault = fault;
    }

    /**
     * Tests whether this rule matches the given path and operation.
     *
     * @param path      the file path to test
     * @param operation the FUSE operation to test
     * @return {@code true} if both the path pattern and operation set match (or are unset)
     */
    public boolean matches(String path, FuseOperation operation) {
        if (operations != null && !operations.contains(operation)) {
            return false;
        }
        if (pathPattern != null && !pathPattern.matcher(path).matches()) {
            return false;
        }
        return true;
    }

    /**
     * Returns the fault associated with this rule.
     *
     * @return the fault to apply when this rule matches
     */
    public FuseFault getFault() {
        return fault;
    }

    /**
     * Returns the path pattern for this rule.
     *
     * @return the compiled path pattern, or {@code null} if all paths match
     */
    public Pattern getPathPattern() {
        return pathPattern;
    }

    /**
     * Sets the path pattern for this rule.
     *
     * @param pathPattern a regular expression string to match against file paths
     */
    public void setPathPattern(String pathPattern) {
        this.pathPattern = Pattern.compile(pathPattern);
    }

    /**
     * Returns the set of FUSE operations this rule applies to.
     *
     * @return the set of operations, or {@code null} if all operations match
     */
    public Set<FuseOperation> getOperations() {
        return operations;
    }

    /**
     * Sets the set of FUSE operations this rule applies to.
     *
     * @param operations the set of operations to match
     */
    public void setOperations(Set<FuseOperation> operations) {
        this.operations = operations;
    }

    /**
     * Sets the fault to apply when this rule matches.
     *
     * @param fault the fault to set
     */
    public void setFault(FuseFault fault) {
        this.fault = fault;
    }
}
