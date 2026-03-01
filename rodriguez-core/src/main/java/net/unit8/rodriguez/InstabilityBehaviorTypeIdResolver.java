package net.unit8.rodriguez;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;

import java.io.IOException;
import java.util.List;

/**
 * Resolves {@link InstabilityBehavior} type ids from short names.
 *
 * <p>Example: {@code "RefuseConnection"} resolves to
 * {@code net.unit8.rodriguez.behavior.RefuseConnection}.
 */
public class InstabilityBehaviorTypeIdResolver extends TypeIdResolverBase {
    private static final List<String> PACKAGE_PREFIXES = List.of(
            "net.unit8.rodriguez.behavior.",
            "net.unit8.rodriguez.aws.behavior.",
            "net.unit8.rodriguez.gcp.behavior.",
            "net.unit8.rodriguez.jdbc.behavior."
    );

    @Override
    public String idFromValue(Object value) {
        return value.getClass().getSimpleName();
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        return suggestedType.getSimpleName();
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) throws IOException {
        Class<?> clazz = resolveClass(id);
        if (!InstabilityBehavior.class.isAssignableFrom(clazz)) {
            throw new IOException("Resolved class is not an InstabilityBehavior: " + clazz.getName());
        }
        return context.constructType(clazz);
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CUSTOM;
    }

    private Class<?> resolveClass(String id) throws IOException {
        for (String prefix : PACKAGE_PREFIXES) {
            String candidate = prefix + id;
            try {
                return Class.forName(candidate);
            } catch (ClassNotFoundException ignored) {
                // Try next known behavior package.
            }
        }
        throw new IOException("Unknown behavior type: " + id);
    }
}
