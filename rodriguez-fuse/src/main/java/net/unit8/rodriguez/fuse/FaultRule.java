package net.unit8.rodriguez.fuse;

import net.unit8.rodriguez.fuse.fault.FuseFault;

import java.util.Set;
import java.util.regex.Pattern;

public class FaultRule {
    private Pattern pathPattern;
    private Set<FuseOperation> operations;
    private FuseFault fault;

    public FaultRule() {
    }

    public FaultRule(String pathPattern, Set<FuseOperation> operations, FuseFault fault) {
        this.pathPattern = pathPattern != null ? Pattern.compile(pathPattern) : null;
        this.operations = operations;
        this.fault = fault;
    }

    public boolean matches(String path, FuseOperation operation) {
        if (operations != null && !operations.contains(operation)) {
            return false;
        }
        if (pathPattern != null && !pathPattern.matcher(path).matches()) {
            return false;
        }
        return true;
    }

    public FuseFault getFault() {
        return fault;
    }

    public Pattern getPathPattern() {
        return pathPattern;
    }

    public void setPathPattern(String pathPattern) {
        this.pathPattern = Pattern.compile(pathPattern);
    }

    public Set<FuseOperation> getOperations() {
        return operations;
    }

    public void setOperations(Set<FuseOperation> operations) {
        this.operations = operations;
    }

    public void setFault(FuseFault fault) {
        this.fault = fault;
    }
}
