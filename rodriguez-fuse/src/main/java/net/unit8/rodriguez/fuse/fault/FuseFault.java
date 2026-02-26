package net.unit8.rodriguez.fuse.fault;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "type")
public interface FuseFault {
    int apply(int normalResult);
}
