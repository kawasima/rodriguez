package net.unit8.rodriguez.aws.behavior.s3;

import net.unit8.rodriguez.aws.AWSRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that bucket/object name resolution stays within the storage root and
 * rejects path-traversal, absolute-path and invalid-name escapes.
 */
class S3PathTraversalTest {

    /** Test-only action that exposes the protected containment helpers. */
    static class ExposingAction extends S3ActionBase<Void> {
        @Override
        public Void handle(AWSRequest params) {
            return null;
        }

        Path bucket(String bucketName) {
            return resolveBucketPath(bucketName);
        }

        Path object(String bucketName, String objectName) {
            return resolveObjectPath(bucketName, objectName);
        }
    }

    private File root;
    private ExposingAction action;

    @BeforeEach
    void setUp() throws Exception {
        root = Files.createTempDirectory("rodriguez-traversal").toFile();
        action = new ExposingAction();
        action.setS3Directory(root);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (root != null) {
            Files.walk(root.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @Test
    void rejectsParentBucketName() {
        assertThatThrownBy(() -> action.bucket(".."))
                .isInstanceOf(S3AccessDeniedException.class);
    }

    @Test
    void rejectsEmptyBucketName() {
        assertThatThrownBy(() -> action.bucket(""))
                .isInstanceOf(S3AccessDeniedException.class);
    }

    @Test
    void rejectsTraversalObjectName() {
        assertThatThrownBy(() -> action.object("my-bucket", "../../etc/passwd"))
                .isInstanceOf(S3AccessDeniedException.class);
    }

    @Test
    void rejectsAbsoluteObjectName() {
        assertThatThrownBy(() -> action.object("my-bucket", "/etc/passwd"))
                .isInstanceOf(S3AccessDeniedException.class);
    }

    @Test
    void rejectsSymlinkEscapingRoot() throws Exception {
        Path outside = Files.createTempDirectory("rodriguez-outside");
        Files.writeString(outside.resolve("secret.txt"), "secret");
        Path link = root.toPath().resolve("bucket");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (UnsupportedOperationException | java.io.IOException e) {
            org.junit.jupiter.api.Assumptions.abort("Symbolic links are not supported on this platform");
            return;
        }
        try {
            // "bucket" passes the lexical check but its real path escapes the root.
            assertThatThrownBy(() -> action.object("bucket", "secret.txt"))
                    .isInstanceOf(S3AccessDeniedException.class);
        } finally {
            Files.walk(outside)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @Test
    void allowsNormalNames() {
        // Use Path#startsWith (pure lexical prefix check) rather than AssertJ's
        // PathAssert#startsWith, which calls toRealPath() and would require the
        // files to actually exist on disk.
        Path expectedRoot = root.toPath().toAbsolutePath().normalize();
        assertThat(action.object("my-bucket", "key").startsWith(expectedRoot)).isTrue();
        assertThat(action.object("my-bucket", "nested/key").startsWith(expectedRoot)).isTrue();
        assertThat(action.bucket("my-bucket").startsWith(expectedRoot)).isTrue();
    }
}
