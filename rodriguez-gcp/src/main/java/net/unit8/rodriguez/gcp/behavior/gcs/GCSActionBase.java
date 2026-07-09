package net.unit8.rodriguez.gcp.behavior.gcs;

import net.unit8.rodriguez.gcp.GCSException;
import net.unit8.rodriguez.util.ContainedStorage;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

/**
 * Abstract base class for GCS action handlers that provides filesystem-backed storage.
 *
 * <p>Each bucket is represented as a subdirectory under the GCS root directory.
 *
 * @param <T> the response type produced by this action
 */
public abstract class GCSActionBase<T> implements GCSMockAction<T> {
    private File gcsDirectory;

    private final ContainedStorage.ErrorFactory errors = new ContainedStorage.ErrorFactory() {
        @Override
        public RuntimeException storageUnavailable(String message) {
            return new GCSException(500, "GCS " + message);
        }

        @Override
        public RuntimeException invalidName(String kind, String name) {
            return new GCSException(400, "Invalid name (path traversal rejected): " + name);
        }
    };

    /** Creates a new action instance. */
    protected GCSActionBase() {
    }

    /**
     * Returns the list of bucket directories under the GCS root directory.
     *
     * @return list of directories representing buckets, or an empty list if none exist
     */
    public List<File> getBucketDirectories() {
        if (gcsDirectory == null) {
            return Collections.emptyList();
        }
        return Optional.ofNullable(gcsDirectory.listFiles(File::isDirectory))
                .map(Arrays::asList)
                .orElse(Collections.emptyList());
    }

    /**
     * Returns the GCS root directory.
     *
     * @return the root directory for GCS storage
     */
    public File getGcsDirectory() {
        return gcsDirectory;
    }

    /**
     * Sets the GCS root directory.
     *
     * @param directory the root directory for GCS storage
     */
    public void setGcsDirectory(File directory) {
        gcsDirectory = directory;
    }

    /**
     * Resolves a bucket name to its directory under the GCS root, rejecting any
     * name that would escape the root (path traversal via {@code ..}, absolute
     * paths, or embedded separators).
     *
     * @param bucketName the bucket name from the request
     * @return the validated bucket directory path, guaranteed to be contained under the GCS root
     * @throws GCSException if the name is missing or escapes the storage root
     */
    protected Path resolveBucketPath(String bucketName) {
        return new ContainedStorage(gcsDirectory, errors).resolveBucket(bucketName);
    }

    /**
     * Resolves a bucket/object name pair to the object's path under the GCS root,
     * rejecting any name that would escape the containing bucket directory.
     *
     * <p>Object names may legitimately contain {@code /} separators (GCS keys are
     * flat strings), so nested paths are permitted as long as the normalized result
     * stays inside the bucket directory.
     *
     * @param bucketName the bucket name from the request
     * @param objectName the object name from the request
     * @return the validated object path, guaranteed to be contained under the bucket directory
     * @throws GCSException if either name is missing or escapes its container
     */
    protected Path resolveObjectPath(String bucketName, String objectName) {
        return new ContainedStorage(gcsDirectory, errors).resolveObject(bucketName, objectName);
    }
}
