package io.github.dreamlike.backend.test.mapper;

import io.github.dreamlike.backend.test.entity.DBEntity;
import io.github.dreamlike.backend.test.entity.DBRecord;
import io.github.dreamlike.orm.base.annotation.RowMapper;
import io.vertx.sqlclient.Row;

import java.util.List;

@RowMapper
public interface TestMapper {

    public List<DBEntity> selectAll(List<Row> dreamlikeRows);

    DBRecord selectById(List<Row> rows);
}
