package net.unit8.rodriguez.aws.behavior.s3;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
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

/**
 * Handles the S3 ListObjects API operation by listing files in the specified bucket directory.
 */
public class ListObjectsAction extends S3ActionBase<ListObjectsAction.ListBucketResult> {

    /**
     * Constructs a ListObjectsAction.
     */
    public ListObjectsAction() {
    }

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

    /**
     * XML-serializable result for the ListObjects S3 API response.
     */
    @JacksonXmlRootElement(localName = "ListBucketResult")
    public static class ListBucketResult implements Serializable {

        /**
         * Constructs an empty ListBucketResult.
         */
        public ListBucketResult() {
        }

        /** Whether the result is truncated. */
        public boolean IsTruncated = false;
        /** The marker for pagination. */
        public String Marker;
        /** The next marker for pagination. */
        public String NextMarker;
        /** The list of object contents. */
        @JacksonXmlElementWrapper(useWrapping = false)
        public List<Contents> Contents;
        /** The name of the bucket. */
        public String Name;
    }

    /**
     * Represents a single object in the ListObjects response.
     */
    public static class Contents implements Serializable {

        /**
         * Constructs an empty Contents.
         */
        public Contents() {
        }

        /** The ETag of the object. */
        public String ETag;
        /** The key (name) of the object. */
        public String Key;
        /** The last modified timestamp of the object. */
        public LocalDateTime LastModified;
        /** The size of the object in bytes. */
        public int Size;
        /** The storage class of the object. */
        public String StorageClass;
    }
}
