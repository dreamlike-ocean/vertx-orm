package io.github.dreamlike.orm.base.meta;


import io.github.dreamlike.orm.base.db.VertxRowReflection;
import io.vertx.sqlclient.Row;
import jakarta.persistence.Column;
import jakarta.persistence.Transient;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public record MetaInfo<T>(Class<T> ownerClass,Function<Row, T> ctor, List<MetaField<T, ?>> fields) {

    public record MetaField<Owner, FType>(String dbField, Field field, BiConsumer<Owner, FType> setter,
                                          Function<Owner, FType> getter) {
    }

    /**
     * 只支持单一构造函数
     *
     * @param tClass 目标类
     * @param <RT>   目标类型
     * @return MetaInfo
     */
    public static <RT> MetaInfo<RT> runtimeParse(Class<RT> tClass) {
        try {
            Function<Row, RT> ctor = parseCtor(tClass);
            List<MetaField<RT, ?>> metaFields;
            if (tClass.isRecord()) {
                metaFields = parseRecordMetaField(tClass);
            } else {
                metaFields = MetaHelper.recursiveFields(tClass)
                        .stream()
                        .filter(f -> f.getAnnotation(Transient.class) == null)
                        .map(f -> parseMetaField(tClass, f))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toUnmodifiableList());
            }
            return new MetaInfo<>(tClass, ctor, metaFields);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }


    private static <T> Function<Row, T> parseCtor(Class<T> t) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        //无参数构造函数的快速路径
        try {
            Constructor<T> noArgCtor = t.getConstructor();
            Supplier<T> ctor = MetaHelper.mhBinder(lookup.unreflectConstructor(noArgCtor));
            return (_) -> ctor.get();
        } catch (Throwable e) {
        }

        Constructor<?>[] constructors = t.getConstructors();
        if (constructors.length > 1) {
            throw new RuntimeException("Only support single constructor");
        }
        Constructor<?> constructor = constructors[0];
        //(arg1, arg2, arg3) -> T
        MethodHandle ctorMH = lookup.unreflectConstructor(constructor);

        //(Row, arg1, arg2, arg3) -> T
        ctorMH = MethodHandles.dropArguments(ctorMH, 0, Row.class);

        Parameter[] parameters = constructor.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            int mhIndex = i + 1;
            Parameter parameter = parameters[i];
            MethodHandle methodHandle = VertxRowReflection.selectMapperMH(
                    parameter.getType(),
                    Optional.ofNullable(parameter.getAnnotation(Column.class))
                            .map(Column::name)
                            .filter(Predicate.not(String::isBlank))
                            .orElseGet(parameter::getName)
            );
            //(Row, arg1, arg2, arg3) -> (Row, ((row) -> T)(), arg2, arg3)
            ctorMH = MethodHandles.filterArguments(ctorMH, mhIndex, methodHandle);
        }
        int[] orders = new int[ctorMH.type().parameterCount()];

        Arrays.fill(orders, 0);
        MethodHandle methodHandle = MethodHandles.permuteArguments(ctorMH, MethodType.methodType(t, Row.class), orders);
        return MetaHelper.getterMhBinder(methodHandle);
    }

    private static <T> MetaInfo.MetaField<T, ?> parseMetaField(Class<T> clazz, Field field) {
        String attributeName = field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
        String setterMethodName = "set" + attributeName;
        String getterMethodName = "get" + attributeName;
        try {
            Class<?> type = field.getType();
            MethodHandle setterMH = MethodHandles.lookup().findVirtual(clazz, setterMethodName, MethodType.methodType(void.class, type));
            MethodHandle getterMH = MethodHandles.lookup().findVirtual(clazz, getterMethodName, MethodType.methodType(type));
            String dbFieldName = Optional.ofNullable(field.getAnnotation(Column.class))
                    .map(Column::name)
                    .filter(Predicate.not(String::isBlank))
                    .orElseGet(field::getName);
            setterMH = type.isPrimitive() ? MethodHandles.filterArguments(setterMH, 1, MetaHelper.unboxSafeMh(type)) : setterMH;
            getterMH = type.isPrimitive() ? MethodHandles.filterReturnValue(getterMH, MetaHelper.boxMH(type)) : getterMH;
            return new MetaInfo.MetaField<>(
                    dbFieldName,
                    field,
                    MetaHelper.setterMhBinder(setterMH),
                    MetaHelper.getterMhBinder(getterMH)
            );

        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private static <T> List<MetaInfo.MetaField<T, ?>> parseRecordMetaField(Class<T> clazz) {
        ArrayList<MetaInfo.MetaField<T, ?>> list = new ArrayList<>();
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            for (RecordComponent recordComponent : clazz.getRecordComponents()) {
                Method accessor = recordComponent.getAccessor();
                //record的注解放在访问器方法上了。。。。
                String dbFieldName = Optional.ofNullable(accessor.getAnnotation(Column.class))
                        .map(Column::name)
                        .filter(Predicate.not(String::isBlank))
                        .orElseGet(recordComponent::getName);
                Field field = clazz.getDeclaredField(recordComponent.getName());
                MethodHandle getter = lookup.unreflect(accessor);
                Class<?> type = recordComponent.getType();
                getter = type.isPrimitive() ? MethodHandles.filterReturnValue(getter, MetaHelper.boxMH(type)) : getter;
                list.add(new MetaInfo.MetaField<>(
                        dbFieldName,
                        field,
                        MetaHelper.setterMhBinder(
                                MethodHandles.empty(
                                        MethodType
                                                .methodType(void.class, clazz, type)
                                                .wrap() // boxed
                                                .changeReturnType(void.class)
                                )),
                        MetaHelper.getterMhBinder(getter)
                ));
            }

            return Collections.unmodifiableList(list);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
}
