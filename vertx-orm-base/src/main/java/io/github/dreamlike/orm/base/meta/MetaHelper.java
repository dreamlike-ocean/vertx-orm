package io.github.dreamlike.orm.base.meta;

import jakarta.persistence.Column;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class MetaHelper {

    public static <T> Supplier<T> mhBinder(MethodHandle methodHandle) throws Throwable {
       try {
           CallSite callSite = LambdaMetafactory.metafactory(
                   MethodHandles.lookup(),
                   "get",
                   MethodType.methodType(Supplier.class),
                   MethodType.methodType(Object.class),
                   methodHandle,
                   methodHandle.type()
           );
           return (Supplier<T>) callSite.getTarget().invoke();
       } catch (LambdaConversionException lambdaConversionException) {
           return MethodHandleProxies.asInterfaceInstance(Supplier.class, methodHandle);
       }
    }

    public static <Owner, FType> BiConsumer<Owner, FType> setterMhBinder(MethodHandle methodHandle) throws Throwable {

       try {
           CallSite callSite = LambdaMetafactory.metafactory(
                   MethodHandles.lookup(),
                   "accept",
                   MethodType.methodType(BiConsumer.class),
                   MethodType.methodType(void.class, Object.class, Object.class),
                   methodHandle,
                   methodHandle.type()
           );
           return (BiConsumer<Owner, FType>) callSite.getTarget().invoke();
       } catch (LambdaConversionException lambdaConversionException) {
           //使用降级方案
           return MethodHandleProxies.asInterfaceInstance(BiConsumer.class, methodHandle);
       }
    }


    public static MethodHandle boxMH(Class primitiveType) {

        class Cache {
             static final MethodHandle INT_BOX_MH;
             static final MethodHandle LONG_BOX_MH;
             static final MethodHandle FLOAT_BOX_MH;
             static final MethodHandle DOUBLE_BOX_MH;
             static final MethodHandle BOOLEAN_BOX_MH;
            static  {
                try {
                    INT_BOX_MH = MethodHandles.publicLookup().findStatic(Integer.class, "valueOf", MethodType.methodType(Integer.class, int.class));
                    LONG_BOX_MH = MethodHandles.publicLookup().findStatic(Long.class, "valueOf", MethodType.methodType(Long.class, long.class));
                    FLOAT_BOX_MH = MethodHandles.publicLookup().findStatic(Float.class, "valueOf", MethodType.methodType(Float.class, float.class));
                    DOUBLE_BOX_MH = MethodHandles.publicLookup().findStatic(Double.class, "valueOf", MethodType.methodType(Double.class, double.class));
                    BOOLEAN_BOX_MH = MethodHandles.publicLookup().findStatic(Boolean.class, "valueOf", MethodType.methodType(Boolean.class, boolean.class));

                } catch (NoSuchMethodException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return switch (primitiveType.getName()) {
            case "int" -> Cache.INT_BOX_MH;
            case "long" -> Cache.LONG_BOX_MH;
            case "float" -> Cache.FLOAT_BOX_MH;
            case "double" -> Cache.DOUBLE_BOX_MH;
            case "boolean" -> Cache.BOOLEAN_BOX_MH;
            default -> throw new IllegalArgumentException("Unsupported primitive type: " + primitiveType.getName());
        };
    }

    public static MethodHandle unboxSafeMh(Class primitiveType) {
        class Cache {
            static final MethodHandle INT_UNBOX_MH;
            static final MethodHandle LONG_UNBOX_MH;
            static final MethodHandle FLOAT_UNBOX_MH;
            static final MethodHandle DOUBLE_UNBOX_MH;
            static final MethodHandle BOOLEAN_UNBOX_MH;
            static  {
                try {
                    INT_UNBOX_MH = MethodHandles.publicLookup().findVirtual(Integer.class, "intValue", MethodType.methodType(int.class));
                    LONG_UNBOX_MH = MethodHandles.publicLookup().findVirtual(Long.class, "longValue", MethodType.methodType(long.class));
                    FLOAT_UNBOX_MH = MethodHandles.publicLookup().findVirtual(Float.class, "floatValue", MethodType.methodType(float.class));
                    DOUBLE_UNBOX_MH = MethodHandles.publicLookup().findVirtual(Double.class, "doubleValue", MethodType.methodType(double.class));
                    BOOLEAN_UNBOX_MH = MethodHandles.publicLookup().findVirtual(Boolean.class, "booleanValue", MethodType.methodType(boolean.class));
                } catch (NoSuchMethodException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return switch (primitiveType.getName()) {
            case "int" -> Cache.INT_UNBOX_MH;
            case "long" -> Cache.LONG_UNBOX_MH;
            case "float" -> Cache.FLOAT_UNBOX_MH;
            case "double" -> Cache.DOUBLE_UNBOX_MH;
            case "boolean" -> Cache.BOOLEAN_UNBOX_MH;
            default -> throw new IllegalArgumentException("Unsupported primitivetype: " + primitiveType.getName());
        };
    }

    public static <Owner, FType> Function<Owner, FType> getterMhBinder(MethodHandle methodHandle) throws Throwable {

        try {
            CallSite callSite = LambdaMetafactory.metafactory(
                    MethodHandles.lookup(),
                    "apply",
                    MethodType.methodType(Function.class),
                    MethodType.methodType(Object.class, Object.class),
                    methodHandle,
                    methodHandle.type()
            );
            return (Function<Owner, FType>) callSite.getTarget().invoke();
        } catch (LambdaConversionException lambdaConversionException) {
            return MethodHandleProxies.asInterfaceInstance(Function.class, methodHandle);
        }
    }

    public static List<Field> recursiveFields(Class<?> clazz) {
        Class<?> classNode = clazz;
        ArrayList<Field> fields = new ArrayList<>();
        while (classNode != Object.class) {
            Collections.addAll(fields, classNode.getDeclaredFields());
            classNode = classNode.getSuperclass();
        }
        return fields;
    }
}
