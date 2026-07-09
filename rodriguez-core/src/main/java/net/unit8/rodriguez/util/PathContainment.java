package net.unit8.rodriguez.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Shared path-traversal containment used by the filesystem-backed mocks
 * (S3, GCS) and the FUSE filesystem.
 *
 * <p>Resolving an untrusted name under a trusted directory is done in two layers:
 * <ol>
 *   <li>a <em>lexical</em> check that normalizes the resolved path and confirms it
 *       stays within the lexical base, rejecting {@code ..} / absolute escapes and
 *       (unless explicitly allowed) the base directory itself; and</li>
 *   <li>a <em>real-path</em> check that resolves the nearest existing ancestor of
 *       the target to its canonical form and confirms it is still contained within
 *       the real boundary, defeating a symlink inside the base that points outside
 *       it. Not-yet-existing leaves resolve normally because only the existing
 *       prefix is checked.</li>
 * </ol>
 *
 * <p>The real-path check fails <em>closed</em>: if the canonical form cannot be
 * resolved (e.g. an {@link IOException}), the path is rejected. Keeping this logic
 * in one place means the three modules cannot silently diverge (they previously did,
 * with GCS failing open on {@code IOException} while S3 and FUSE failed closed).
 */
public final class PathContainment {
    private PathContainment() {
    }

    /**
     * Resolves {@code child} under {@code lexicalBase} and verifies containment.
     *
     * @param lexicalBase     the directory the resolved path must stay within lexically
     * @param realBoundary    the already-canonicalized ({@link Path#toRealPath}) boundary
     *                        the resolved path must stay within after symlink resolution;
     *                        callers on a hot path canonicalize this once and reuse it
     * @param child           the untrusted name to resolve (may contain {@code /} separators)
     * @param allowBaseItself whether a {@code child} that resolves to {@code lexicalBase}
     *                        itself is permitted (true for a filesystem root, false for a
     *                        bucket/object name that must be a strict descendant)
     * @return the contained path, or empty if {@code child} is missing, malformed, or escapes
     */
    public static Optional<Path> resolveWithin(Path lexicalBase, Path realBoundary,
                                               String child, boolean allowBaseItself) {
        if (child == null) {
            return Optional.empty();
        }
        Path resolved;
        try {
            resolved = lexicalBase.resolve(child).normalize();
        } catch (InvalidPathException e) {
            return Optional.empty();
        }
        if (!resolved.startsWith(lexicalBase)) {
            return Optional.empty();
        }
        if (!allowBaseItself && resolved.equals(lexicalBase)) {
            return Optional.empty();
        }
        if (!realPathContained(resolved, realBoundary)) {
            return Optional.empty();
        }
        return Optional.of(resolved);
    }

    private static boolean realPathContained(Path resolved, Path realBoundary) {
        try {
            Path existing = resolved;
            while (existing != null && !Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
                existing = existing.getParent();
            }
            if (existing == null) {
                return false;
            }
            return existing.toRealPath().startsWith(realBoundary);
        } catch (IOException e) {
            return false;
        }
    }
}
