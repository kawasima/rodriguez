package net.unit8.rodriguez.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;

public class DummyCredentialsProvider implements AWSCredentialsProvider {

        @Override
        public AWSCredentials getCredentials() {
            return new AWSCredentials() {
                @Override
                public String getAWSAccessKeyId() {
                    return "DUMMY-ACCESS-KEY";
                }

                @Override
                public String getAWSSecretKey() {
                    return "DUMMY-SECRET-KEY";
                }
            };
        }

        @Override
        public void refresh() {

        }
}
