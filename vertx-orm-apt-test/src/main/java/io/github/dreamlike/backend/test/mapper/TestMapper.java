package io.github.dreamlike.backend.test.mapper;

import io.github.dreamlike.backend.test.entity.DBEntity;
import io.github.dreamlike.backend.test.entity.DBRecord;
import io.github.dreamlike.orm.base.annotation.ReactiveMapper;

import java.util.List;

@ReactiveMapper
public interface TestMapper {

    public List<DBEntity> selectAll();

    DBRecord selectById(int id);


}
