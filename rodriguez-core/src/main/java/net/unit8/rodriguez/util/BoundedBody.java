package net.unit8.rodriguez.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Reads a request body fully into memory while enforcing a hard size cap, so a
 * chunked body with no {@code Content-Length} cannot exhaust the heap.
 *
 * <p>This is the single home for the overflow-safe read used by the AWS, GCS and
 * proxy mocks. They previously each carried a copy of this arithmetic — two threw
 * on overflow, one returned {@code null} — which was an easy trap for the next
 * endpoint to copy the wrong contract. Every caller now shares one behavior:
 * return the bytes, or throw {@link BodyTooLargeException}.
 */
public final class BoundedBody {

    /** Canonical default cap for an in-memory mock request body (64 MB). */
    public static final long DEFAULT_MAX_BODY_SIZE = 64L * 1024 * 1024;

    private BoundedBody() {
    }

    /**
     * Reads {@code in} fully into memory, rejecting anything larger than {@code limit}.
     *
     * <p>Reads at most {@code limit + 1} bytes so an oversized body is detected without
     * buffering more than one byte past the cap. {@code limit} is clamped to a Java
     * array's capacity first, so {@code limit + 1} cannot overflow to a negative int
     * (which would throw) nor silently saturate at {@link Integer#MAX_VALUE}.
     *
     * @param in    the request body stream
     * @param limit the maximum accepted body size in bytes
     * @return the body bytes (never larger than the effective limit)
     * @throws IOException           if an I/O error occurs while reading
     * @throws BodyTooLargeException if the body exceeds the effective limit
     */
    public static byte[] read(InputStream in, long limit) throws IOException {
        long effectiveLimit = Math.min(limit, (long) Integer.MAX_VALUE - 8);
        int cap = (int) (effectiveLimit + 1);
        byte[] data = in.readNBytes(cap);
        if (data.length > effectiveLimit) {
            throw new BodyTooLargeException(effectiveLimit);
        }
        return data;
    }
}
