package net.unit8.rodriguez.aws.behavior.s3;

import net.unit8.rodriguez.aws.MockAction;
import net.unit8.rodriguez.util.ContainedStorage;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Abstract base class for S3 mock actions providing shared access to the S3 storage directory.
 *
 * @param <T> the response type produced by this action
 */
public abstract class S3ActionBase<T> implements MockAction<T> {
    private File s3Directory;

    private final ContainedStorage.ErrorFactory errors = new ContainedStorage.ErrorFactory() {
        @Override
        public RuntimeException storageUnavailable(String message) {
            return new S3AccessDeniedException("S3 " + message);
        }

        @Override
        public RuntimeException invalidName(String kind, String name) {
            return new S3AccessDeniedException("Invalid or unsafe " + kind + " name: " + name);
        }
    };

    /**
     * Constructs an S3ActionBase with no directory set.
     */
    public S3ActionBase() {
    }

    /**
     * Returns the list of bucket directories under the S3 storage directory.
     *
     * @return the list of bucket directories, or an empty list if the directory is not set
     */
    public List<File> getBucketDirectories() {
        if (s3Directory == null) {
            return Collections.emptyList();
        }
        return Optional.ofNullable(s3Directory.listFiles(File::isDirectory))
                .map(Arrays::asList)
                .orElse(Collections.emptyList());
    }

    /**
     * Returns the S3 storage directory.
     *
     * @return the S3 directory, or {@code null} if not set
     */
    public File getS3Directory() {
        return s3Directory;
    }

    /**
     * Sets the filesystem directory used for S3 storage.
     *
     * @param directory the directory to use for S3 storage
     */
    public void setS3Directory(File directory) {
        s3Directory = directory;
    }

    /**
     * Safely resolves a bucket directory under the S3 storage root.
     *
     * @param bucketName the bucket name from the request
     * @return the resolved bucket path, guaranteed to be contained within the storage root
     * @throws S3AccessDeniedException if the storage root is unset, the name is
     *                                 invalid, or the resolved path escapes the root
     */
    protected Path resolveBucketPath(String bucketName) {
        return new ContainedStorage(s3Directory, errors).resolveBucket(bucketName);
    }

    /**
     * Safely resolves an object file under the given bucket within the S3 storage root.
     *
     * <p>The object name is contained within its <em>bucket</em> directory (not merely
     * the storage root), so an object key such as {@code ../other-bucket/key} cannot
     * cross into a sibling bucket.
     *
     * @param bucketName the bucket name from the request
     * @param objectName the object name from the request
     * @return the resolved object path, guaranteed to be contained within its bucket directory
     * @throws S3AccessDeniedException if the storage root is unset, a name is
     *                                 invalid, or the resolved path escapes its container
     */
    protected Path resolveObjectPath(String bucketName, String objectName) {
        return new ContainedStorage(s3Directory, errors).resolveObject(bucketName, objectName);
    }
}
