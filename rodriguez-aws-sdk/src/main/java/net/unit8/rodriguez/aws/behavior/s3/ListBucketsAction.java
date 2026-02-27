package net.unit8.rodriguez.aws.behavior.s3;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import net.unit8.rodriguez.aws.AWSRequest;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles the S3 ListBuckets API operation by listing subdirectories in the S3 storage directory.
 */
public class ListBucketsAction extends S3ActionBase<ListBucketsAction.ListAllMyBucketsResult> {

    /**
     * Constructs a ListBucketsAction.
     */
    public ListBucketsAction() {
    }

    @Override
    public ListAllMyBucketsResult handle(AWSRequest request) {
        ListAllMyBucketsResult result = new ListAllMyBucketsResult();
        result.Buckets = getBucketDirectories().stream()
                .map(f -> {
                    Bucket bucket = new Bucket();
                    bucket.Name = f.getName();
                    bucket.CreationDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(f.lastModified()), ZoneOffset.UTC);
                    return bucket;
                })
                .collect(Collectors.toList());
        return result;
    }

    /**
     * XML-serializable result for the ListBuckets S3 API response.
     */
    @JacksonXmlRootElement(localName = "ListAllMyBucketsResult")
    public static class ListAllMyBucketsResult implements Serializable {

        /**
         * Constructs an empty ListAllMyBucketsResult.
         */
        public ListAllMyBucketsResult() {
        }

        /** The list of buckets. */
        @JacksonXmlElementWrapper(localName = "Buckets")
        @JacksonXmlProperty(localName = "Bucket")
        public List<Bucket> Buckets;
    }

    /**
     * Represents a single S3 bucket in the ListBuckets response.
     */
    public static class Bucket implements Serializable {

        /**
         * Constructs an empty Bucket.
         */
        public Bucket() {
        }

        /** The name of the bucket. */
        public String Name;
        /** The creation date of the bucket. */
        public LocalDateTime CreationDate;
    }
}
