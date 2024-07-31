package io.github.dreamlike.backend;

import io.github.dreamlike.orm.base.helper.Pair;

import java.util.Map;

public record CompileMetaInfo(String ctorStatement, Map<String /*dbName*/, FieldInjector> fieldInjectors) {

    public record FieldInjector(String setterReference, String getterReference) {
    }

}
