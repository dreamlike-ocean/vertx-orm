package io.github.dreamlike.orm.base.db;

import io.vertx.sqlclient.Row;

import java.util.List;

public interface RowMapperCollection {

    /**
     * 应对oneToMany这种场景 所以传入的是个List<Row>
     *
     * @param rows 当前结果集
     * @param tClass   目标类型
     * @return 映射出来的类型
     *
     * @param <T> 目标类型
     */
    <T> List<T> map(List<Row> rows, Class<T> tClass);

    //获取泛型的手段
    default <T> List<T> map(List<Row> rows, T...t) {
        return (List<T>) map(rows, t.getClass().getComponentType());
    }
}
