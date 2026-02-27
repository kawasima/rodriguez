package net.unit8.rodriguez.gcp.behavior.gcs;

import net.unit8.rodriguez.gcp.GCSRequest;

import java.io.File;

/**
 * Handles {@code DELETE /storage/v1/b/{bucket}/o/{object}} to delete a GCS object.
 */
public class DeleteObjectAction extends GCSActionBase<Void> {
    /** Creates a new DeleteObjectAction. */
    public DeleteObjectAction() {
    }

    @Override
    public Void handle(GCSRequest request) {
        String bucketName = request.getQueryParam("_bucketName");
        String objectName = request.getQueryParam("_objectName");
        File file = getGcsDirectory().toPath().resolve(bucketName).resolve(objectName).toFile();
        file.delete();
        return null;
    }
}
