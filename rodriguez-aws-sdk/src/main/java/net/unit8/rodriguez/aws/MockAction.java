package net.unit8.rodriguez.aws;

public interface MockAction<T> {
    T handle(AWSRequest params);
}
