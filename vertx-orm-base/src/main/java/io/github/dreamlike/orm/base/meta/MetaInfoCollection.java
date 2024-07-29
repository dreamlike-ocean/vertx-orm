package io.github.dreamlike.orm.base.meta;

public interface MetaInfoCollection {

    <T> MetaInfo<T> getMetaInfo(Class<T> clazz);

}
