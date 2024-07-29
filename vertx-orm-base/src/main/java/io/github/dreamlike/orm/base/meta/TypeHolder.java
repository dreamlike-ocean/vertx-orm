package io.github.dreamlike.orm.base.meta;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class TypeHolder<T>{

    public Type getType() {
        Class<? extends TypeHolder> currentClass = this.getClass();
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
