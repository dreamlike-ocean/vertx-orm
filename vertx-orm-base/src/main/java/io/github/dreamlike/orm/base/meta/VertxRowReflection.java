package io.github.dreamlike.orm.base.meta;

import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.data.Numeric;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

class VertxRowReflection {

    private final static int NOT_FIND = -1;

    final static MethodHandle getIntegerMH;

    final static MethodHandle getIntMh;

    final static MethodHandle getLongMH;

    final static MethodHandle getLongPrimitiveMH;

    final static MethodHandle getDoubleMH;

    final static MethodHandle getDoublePrimitiveMH;

    final static MethodHandle getFloatMh;

    final static MethodHandle getFloatPrimitiveMh;

    final static MethodHandle getBooleanMH;

    final static MethodHandle getBooleanPrimitiveMH;

    final static MethodHandle getStringMH;

    final static MethodHandle getLocalDateMH;

    final static MethodHandle getLocalDateTimeMH;

    final static MethodHandle getByteArrayMH;

    final static Map<Class<?>, MethodHandle> mapperMH;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            getIntegerMH = lookup.findStatic(VertxRowReflection.class, "getInteger", MethodType.methodType(Integer.class, Row.class, String.class));
            getIntMh = lookup.findStatic(VertxRowReflection.class, "getInt", MethodType.methodType(int.class, Row.class, String.class));
            getLongMH = lookup.findStatic(VertxRowReflection.class, "getLong", MethodType.methodType(Long.class, Row.class, String.class));
            getLongPrimitiveMH = lookup.findStatic(VertxRowReflection.class, "getLongPrimitive", MethodType.methodType(long.class, Row.class, String.class));
            getDoubleMH = lookup.findStatic(VertxRowReflection.class, "getDouble", MethodType.methodType(Double.class, Row.class, String.class));
            getDoublePrimitiveMH = lookup.findStatic(VertxRowReflection.class, "getDoublePrimitive", MethodType.methodType(double.class, Row.class, String.class));
            getFloatMh = lookup.findStatic(VertxRowReflection.class, "getFloat", MethodType.methodType(Float.class, Row.class, String.class));
            getFloatPrimitiveMh = lookup.findStatic(VertxRowReflection.class, "getFloatPrimitive", MethodType.methodType(float.class, Row.class, String.class));
            getBooleanMH = lookup.findStatic(VertxRowReflection.class, "getBoolean", MethodType.methodType(Boolean.class, Row.class, String.class));
            getBooleanPrimitiveMH = lookup.findStatic(VertxRowReflection.class, "getBooleanPrimitive", MethodType.methodType(boolean.class, Row.class, String.class));
            getStringMH = lookup.findStatic(VertxRowReflection.class, "getString", MethodType.methodType(String.class, Row.class, String.class));
            getLocalDateMH = lookup.findStatic(VertxRowReflection.class, "getLocalDate", MethodType.methodType(LocalDate.class, Row.class, String.class));
            getLocalDateTimeMH = lookup.findStatic(VertxRowReflection.class, "getLocalDateTime", MethodType.methodType(LocalDateTime.class, Row.class, String.class));
            getByteArrayMH = lookup.findStatic(VertxRowReflection.class, "getByteArray", MethodType.methodType(byte[].class, Row.class, String.class));

            Map<Class<?>, MethodHandle> mhHolder = new HashMap<>();

            mhHolder.put(Integer.class, getIntegerMH);
            mhHolder.put(int.class, getIntMh);
            mhHolder.put(Long.class, getLongMH);
            mhHolder.put(long.class, getLongPrimitiveMH);
            mhHolder.put(Double.class, getDoubleMH);
            mhHolder.put(double.class, getDoublePrimitiveMH);
            mhHolder.put(Float.class, getFloatMh);
            mhHolder.put(float.class, getFloatPrimitiveMh);
            mhHolder.put(Boolean.class, getBooleanMH);
            mhHolder.put(boolean.class, getBooleanPrimitiveMH);
            mhHolder.put(String.class, getStringMH);
            mhHolder.put(LocalDate.class, getLocalDateMH);
            mhHolder.put(LocalDateTime.class, getLocalDateTimeMH);
            mhHolder.put(byte[].class, getByteArrayMH);

            mapperMH = Map.copyOf(mhHolder);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    public static MethodHandle selectMapperMH(Class<?> clazz, String dbFieldName) {
        MethodHandle methodHandle = mapperMH.get(clazz);
        if (methodHandle == null) {
            throw new UnsupportedOperationException(clazz + " dont be support!");
        }

        return MethodHandles.insertArguments(methodHandle, 1, dbFieldName);
    }


    public static Integer getInteger(Row row, String dbFieldName) {
        int index = row.getColumnIndex(dbFieldName);
        if (index == NOT_FIND) {
            return null;
        }
        return row.getInteger(index);
    }

    public static int getInt(Row row, String dbFieldName) {
        int index = row.getColumnIndex(dbFieldName);
        if (index == NOT_FIND) {
            return 0;
        }
        return row.getInteger(index);
    }

    public static Long getLong(Row row, String dbFieldName) {
        int index = row.getColumnIndex(dbFieldName);
        if (index == NOT_FIND) {
            return null;
        }
        return row.getLong(index);
    }

    public static long getLongPrimitive(Row row, String dbFieldName) {
        int index = row.getColumnIndex(dbFieldName);
        if (index == NOT_FIND) {
            return 0L;
        }
        return row.getLong(index);
    }

    public static Double getDouble(Row row, String dbFieldName) {
        int index = row.getColumnIndex(dbFieldName);
        if (index == NOT_FIND) {
            return null;
        }
        return row.getDouble(index);
    }

    public static double getDoublePrimitive(Row row, String dbFieldName) {
        int index = row.getColumnIndex(dbFieldName);
        if (index == NOT_FIND) {
            return 0.0;
        }
        return row.getDouble(index);
    }

    public static Float getFloat(Row row, String dbFieldName) {
        int index = row.getColumnIndex(dbFieldName);
        if (index == NOT_FIND) {
            return null;
        }
        return row.getFloat(index);
    }

    public static float getFloatPrimitive(Row row, String dbFieldName) {
        int index = row.getColumnIndex(dbFieldName);
        if (index == NOT_FIND) {
            return 0.0f;
        }
        return row.getFloat(index);
    }

    public static Boolean getBoolean(Row row, String dbFieldName) {
        int index = row.getColumnIndex(dbFieldName);
        if (index == NOT_FIND) {
            return null;
        }
        Numeric numeric = row.getNumeric(index);
        if (numeric == null) {
            return null;
        }

        return numeric.intValue() != 0;
    }

    public static boolean getBooleanPrimitive(Row row, String dbFieldName) {
        int index = row.getColumnIndex(dbFieldName);
        if (index == NOT_FIND) {
            return false;
        }
        Numeric numeric = row.getNumeric(index);
        if (numeric == null) {
            return false;
        }

        return numeric.intValue() != 0;
    }

    public static String getString(Row row, String dbFieldName) {
        int index = row.getColumnIndex(dbFieldName);
        if (index == NOT_FIND) {
            return null;
        }
        return row.getString(index);
    }

    public static LocalDate getLocalDate(Row row, String dbFieldName) {
        int index = row.getColumnIndex(dbFieldName);
        if (index == NOT_FIND) {
            return null;
        }
        return row.getLocalDate(index);
    }

    public static LocalDateTime getLocalDateTime(Row row, String dbFieldName) {
        int index = row.getColumnIndex(dbFieldName);
        if (index == NOT_FIND) {
            return null;
        }
        return row.getLocalDateTime(index);
    }

    public static byte[] getByteArray(Row row, String dbFieldName) {
        int index = row.getColumnIndex(dbFieldName);
        if (index == NOT_FIND) {
            return null;
        }
        return row.getBuffer(index).getBytes();
    }
}
