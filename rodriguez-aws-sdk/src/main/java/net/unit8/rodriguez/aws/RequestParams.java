package net.unit8.rodriguez.aws;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RequestParams {
    private final Map<String, List<String>> params;

    public RequestParams() {
        params = new LinkedHashMap<>();
    }
    public RequestParams(String query) {
        params = parseQuery(query);
    }

    public String getFirst(String key) {
        return Optional.ofNullable(params.get(key))
                .map(list -> list.get(0))
                .orElse(null);
    }

    public List<String> get(String key){
        return params.get(key);
    }

    public Stream<Map.Entry<String, List<String>>> stream() {
        return params.entrySet().stream();
    }

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

    public void put(String key, String value) {
        params.put(key, List.of(value));
    }

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
