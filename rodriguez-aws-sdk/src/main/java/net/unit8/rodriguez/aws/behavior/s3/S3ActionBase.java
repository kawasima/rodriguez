package net.unit8.rodriguez.aws.behavior.s3;

import net.unit8.rodriguez.aws.MockAction;

import java.io.File;
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
}
