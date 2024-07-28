package io.github.dreamlike.orm.base.db;

import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.data.Numeric;

import java.util.function.Consumer;

/**
 * 大概率jvm能够通过JIT折叠内联lambda进来以及去除装箱 所以这里不使用Consumer的基础类型版本
 */
public class RowMapperShortcut {
    private final static int NOT_FIND = -1;

    public static void setInt(Row row, String dbFieldName, Consumer<Integer> setter) {
        int index = row.getColumnIndex(dbFieldName);
        if (index == NOT_FIND) {
            return;
        }
        Integer dbValue = row.getInteger(index);
        if (dbValue == null) {
            return;
        }
        setter.accept(dbValue);
    }

    public static void setLong(Row row, String dbFieldName, Consumer<Long> setter) {
        int index = row.getColumnIndex(dbFieldName);
        if (index == NOT_FIND) {
            return;
        }
        Long dbValue = row.getLong(index);
        setter.accept(dbValue);
    }

    public static void setString(Row row, String dbFieldName, Consumer<String> setter) {
        int index = row.getColumnIndex(dbFieldName);
        if (index == NOT_FIND) {
            return;
        }
        String dbValue = row.getString(index);
        if (dbValue == null) {
            return;
        }
        setter.accept(dbValue);
    }

    public static void setBoolean(Row row, String dbFieldName, Consumer<Boolean> setter) {
        int index = row.getColumnIndex(dbFieldName);
        if (index == NOT_FIND) {
            return;
        }
        Numeric numeric = row.getNumeric(index);
        if (numeric == null) {
            return;
        }
        setter.accept(numeric.intValue() != 0);
    }

    public static void setFloat(Row row, String dbFieldName, Consumer<Float> setter) {
        int index = row.getColumnIndex(dbFieldName);
        if (index == NOT_FIND) {
            return;
        }

        Float dbValue = row.getFloat(index);

        if (dbValue == null) {
            return;
        }

        setter.accept(dbValue);
    }

    public static void setDouble(Row row, String dbFieldName, Consumer<Double> setter) {
        int index = row.getColumnIndex(dbFieldName);
        if (index == NOT_FIND) {
            return;
        }
        Double dbValue = row.getDouble(index);

        if (dbValue == null) {
            return;
        }

        setter.accept(dbValue);
    }


    public static void setLocalDate(Row row, String dbFieldName, Consumer<java.time.LocalDate> setter) {
        int index = row.getColumnIndex(dbFieldName);
        if (index == NOT_FIND) {
            return;
        }
        java.time.LocalDate dbValue = row.getLocalDate(index);
        setter.accept(dbValue);
    }

    public static void setLocalDateTime(Row row, String dbFieldName, Consumer<java.time.LocalDateTime> setter) {
        int index = row.getColumnIndex(dbFieldName);
        if (index == NOT_FIND) {
            return;
        }
        java.time.LocalDateTime dbValue = row.getLocalDateTime(index);
        setter.accept(dbValue);
    }

    public static void setByteArray(Row row , String dbFieldName, Consumer<byte[]> setter) {
        int index = row.getColumnIndex(dbFieldName);
        if (index == NOT_FIND) {
            return;
        }
        setter.accept(row.getBuffer(index).getBytes());
    }

}
