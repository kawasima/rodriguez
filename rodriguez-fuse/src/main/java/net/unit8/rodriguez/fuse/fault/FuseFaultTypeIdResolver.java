package net.unit8.rodriguez.fuse.fault;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;

import java.io.IOException;

/**
 * Resolves {@link FuseFault} type ids from short names.
 */
public class FuseFaultTypeIdResolver extends TypeIdResolverBase {
    private static final String FUSE_FAULT_PACKAGE = "net.unit8.rodriguez.fuse.fault.";

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
        try {
            Class<?> clazz = Class.forName(FUSE_FAULT_PACKAGE + id);
            if (!FuseFault.class.isAssignableFrom(clazz)) {
                throw new IOException("Resolved class is not a FuseFault: " + clazz.getName());
            }
            return context.constructType(clazz);
        } catch (ClassNotFoundException e) {
            throw new IOException("Unknown fuse fault type: " + id, e);
        }
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CUSTOM;
    }
}
