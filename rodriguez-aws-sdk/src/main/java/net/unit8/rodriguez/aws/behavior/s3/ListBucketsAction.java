package net.unit8.rodriguez.aws.behavior.s3;

import net.unit8.rodriguez.aws.AWSRequest;
import net.unit8.rodriguez.aws.MockAction;
import net.unit8.rodriguez.aws.RequestParams;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

public class ListBucketsAction extends S3ActionBase<ListBucketsAction.ListBucketsOutput> {
    @Override
    public ListBucketsOutput handle(AWSRequest request) {
        ListBucketsOutput result = new ListBucketsOutput();
        result.buckets = getBucketDirectories().stream()
                .map(f -> {
                    Bucket bucket = new Bucket();
                    bucket.Name = f.getName();
                    bucket.CreationDate = LocalDateTime.ofEpochSecond(f.lastModified(), 0, ZoneOffset.UTC);
                    return bucket;
                })
                .collect(Collectors.toList());
        return result;
    }

    public static class ListBucketsOutput implements Serializable {
        public List<Bucket> buckets;
    }

    public static class Bucket implements Serializable {
        public String Name;
        public LocalDateTime CreationDate;
    }
}
