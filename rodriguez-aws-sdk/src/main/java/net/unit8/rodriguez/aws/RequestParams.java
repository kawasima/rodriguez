package net.unit8.rodriguez.aws;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A multi-valued parameter map for AWS requests, supporting query string parsing and merging.
 */
public class RequestParams {
    private final Map<String, List<String>> params;

    /**
     * Constructs an empty {@code RequestParams} instance.
     */
    public RequestParams() {
        params = new LinkedHashMap<>();
    }

    /**
     * Constructs a {@code RequestParams} by parsing a URL-encoded query string.
     *
     * @param query the URL-encoded query string to parse
     */
    public RequestParams(String query) {
        params = parseQuery(query);
    }

    /**
     * Returns the first value associated with the given key.
     *
     * @param key the parameter key
     * @return the first value, or {@code null} if the key is not present
     */
    public String getFirst(String key) {
        return Optional.ofNullable(params.get(key))
                .map(list -> list.getFirst())
                .orElse(null);
    }

    /**
     * Returns all values associated with the given key.
     *
     * @param key the parameter key
     * @return the list of values, or {@code null} if the key is not present
     */
    public List<String> get(String key){
        return params.get(key);
    }

    /**
     * Returns a stream over the parameter entries.
     *
     * @return a stream of map entries
     */
    public Stream<Map.Entry<String, List<String>>> stream() {
        return params.entrySet().stream();
    }

    /**
     * Merges another {@code RequestParams} into this instance. Values for existing keys
     * are appended; new keys are added.
     *
     * @param other the other parameters to merge
     * @return this instance with merged parameters
     */
    public RequestParams merge(RequestParams other) {
        other.stream().forEach(entry -> {
            if (params.containsKey(entry.getKey())) {
                params.get(entry.getKey()).addAll(entry.getValue());
            } else {
                params.put(entry.getKey(), entry.getValue());
            }
        });
        return this;
    }

    /**
     * Sets a single-valued parameter, replacing any existing values for the given key.
     *
     * @param key   the parameter key
     * @param value the parameter value
     */
    public void put(String key, String value) {
        params.put(key, List.of(value));
    }

    /**
     * Checks whether this parameter map contains the given key.
     *
     * @param key the parameter key
     * @return {@code true} if the key is present, {@code false} otherwise
     */
    public boolean containsKey(String key) {
        return params.containsKey(key);
    }

    private Map<String, List<String>> parseQuery(String query) {
        if (query == null || query.isEmpty()) {
            return Collections.emptyMap();
        }
        return Arrays.stream(query.split("&"))
                .map(this::splitQueryParameter)
                .collect(Collectors.groupingBy(AbstractMap.SimpleImmutableEntry::getKey,
                        LinkedHashMap::new, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    private AbstractMap.SimpleImmutableEntry<String, String> splitQueryParameter(String it) {
        final int idx = it.indexOf("=");
        final String key = idx > 0 ? it.substring(0, idx) : it;
        final String value = idx > 0 && it.length() > idx + 1 ? it.substring(idx + 1) : null;
        return new AbstractMap.SimpleImmutableEntry<>(
                URLDecoder.decode(key, StandardCharsets.UTF_8),
                Optional.ofNullable(value)
                        .map(v -> URLDecoder.decode(v, StandardCharsets.UTF_8))
                        .orElse(null)
        );
    }

}
