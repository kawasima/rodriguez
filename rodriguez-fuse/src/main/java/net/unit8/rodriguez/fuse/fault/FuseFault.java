package net.unit8.rodriguez.fuse.fault;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Interface for FUSE filesystem fault injection behaviors.
 *
 * <p>Implementations simulate specific filesystem faults (e.g., I/O errors, permission denied)
 * by transforming the normal result of a FUSE operation into a fault result.
 * Uses Jackson polymorphic deserialization to allow JSON-based configuration.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "type")
public interface FuseFault {
    /**
     * Applies the fault to a FUSE operation result.
     *
     * @param normalResult the normal result value of the FUSE operation (e.g., bytes read/written)
     * @return the faulted result, typically a negative errno value indicating an error
     */
    int apply(int normalResult);
}
