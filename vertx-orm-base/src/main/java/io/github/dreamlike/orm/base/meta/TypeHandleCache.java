package io.github.dreamlike.orm.base.meta;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TypeHandleCache {
    private final Map<Type, TypeHandler<?>> cache;

    public TypeHandleCache() {
        cache = new ConcurrentHashMap<>();
    }



    public TypeHandler<?> get(Type type) {
        //todo
        return null;
    }
}
