package net.unit8.rodriguez.aws.behavior.s3;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import net.unit8.rodriguez.aws.AWSRequest;

import java.io.File;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ListObjectsAction extends S3ActionBase<ListObjectsAction.ListBucketResult> {
    @Override
    public ListBucketResult handle(AWSRequest request) {
        ListBucketResult result = new ListBucketResult();
        result.Name = request.getParams().getFirst("BucketName");
        result.Contents = Optional.ofNullable(getS3Directory())
                .map(dir -> new File(dir, result.Name))
                .filter(File::isDirectory)
                .map(File::listFiles)
                .map(files -> Arrays.stream(files)
                        .map(f -> {
                            Contents contents = new Contents();
                            contents.Key = f.getName();
                            contents.LastModified = LocalDateTime.ofInstant(Instant.ofEpochMilli(f.lastModified()), ZoneOffset.UTC);
                            contents.Size = (int) f.length();
                            return contents;
                        })
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
        return result;
    }

    public static class ListBucketResult implements Serializable {
        public boolean IsTruncated = false;
        public String Marker;
        public String NextMarker;
        @JacksonXmlElementWrapper(useWrapping = false)
        public List<Contents> Contents;
        public String Name;
    }

    public static class Contents implements Serializable {
        public String ETag;
        public String Key;
        public LocalDateTime LastModified;
        public int Size;
        public String StorageClass;
    }
}
