package net.unit8.rodriguez.gcp.behavior.gcs;

import net.unit8.rodriguez.gcp.GCSRequest;

import java.io.File;

/**
 * Handles {@code GET /storage/v1/b/{bucket}/o/{object}?alt=media} to download a GCS object.
 *
 * <p>Returns the file on disk so that {@link net.unit8.rodriguez.gcp.behavior.GCSMock}
 * can stream its binary content to the client.
 */
public class GetObjectAction extends GCSActionBase<File> {
    /** Creates a new GetObjectAction. */
    public GetObjectAction() {
    }

    @Override
    public File handle(GCSRequest request) {
        String bucketName = request.getQueryParam("_bucketName");
        String objectName = request.getQueryParam("_objectName");
        return getGcsDirectory().toPath().resolve(bucketName).resolve(objectName).toFile();
    }
}
