package io.github.dreamlike.orm.base;

import io.vertx.sqlclient.Row;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public interface TypeHandler<T> {

    T map(Row row);

    default Type getType() {

        Class<? extends TypeHandler> currentClass = this.getClass();
        if (currentClass.isHidden()) {
            throw new UnsupportedOperationException("cant support lambda!");
        }

        for (Type genericInterface : currentClass.getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType pt) {
                if (pt.getRawType().equals(TypeHandler.class)) {
                    return pt.getActualTypeArguments()[0];
                }
            }
        }

        return null;
    }
}
