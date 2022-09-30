package net.unit8.rodriguez.aws.behavior.s3;

import net.unit8.rodriguez.aws.MockAction;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public abstract class S3ActionBase<T> implements MockAction<T> {
    private File s3Directory;

    public List<File> getBucketDirectories() {
        if (s3Directory == null) {
            return Collections.emptyList();
        }
        return Optional.ofNullable(s3Directory.listFiles(File::isDirectory))
                .map(Arrays::asList)
                .orElse(Collections.emptyList());
    }

    public File getS3Directory() {
        return s3Directory;
    }

    public void setS3Directory(File directory) {
        s3Directory = directory;
    }
}
