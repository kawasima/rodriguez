package net.unit8.rodriguez.gcp.behavior.gcs;

import net.unit8.rodriguez.gcp.GCSRequest;

import java.io.File;
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
}
