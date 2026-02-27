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

public class ListBucketsAction extends S3ActionBase<ListBucketsAction.ListAllMyBucketsResult> {
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

    @JacksonXmlRootElement(localName = "ListAllMyBucketsResult")
    public static class ListAllMyBucketsResult implements Serializable {
        @JacksonXmlElementWrapper(localName = "Buckets")
        @JacksonXmlProperty(localName = "Bucket")
        public List<Bucket> Buckets;
    }

    public static class Bucket implements Serializable {
        public String Name;
        public LocalDateTime CreationDate;
    }
}
