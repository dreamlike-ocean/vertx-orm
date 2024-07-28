import entity.BaseEntity;
import entity.DBEntity;
import entity.DBRecord;
import io.github.dreamlike.orm.base.TypeHandler;
import io.github.dreamlike.orm.base.meta.MetaInfo;
import io.vertx.sqlclient.Row;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class MetaTest {

    @Test
    public void test() {
        TypeHandler<String> handler = new TypeHandler<>() {
            @Override
            public String map(Row row) {
                return "";
            }
        };

        Type type = handler.getType();
        Assertions.assertEquals(String.class, type);

        TypeHandler<List<String>> handler1 = new TypeHandler<>() {
            @Override
            public List<String> map(Row row) {
                return null;
            }
        };

        Type type1 = handler1.getType();
        Assertions.assertInstanceOf(ParameterizedType.class, type1);
        ParameterizedType parameterizedType = (ParameterizedType) type1;
        Assertions.assertEquals(List.class, parameterizedType.getRawType());
        Assertions.assertEquals(String.class, parameterizedType.getActualTypeArguments()[0]);
    }

    @Test
    public void testSimpleMeta() {
        MetaInfo<BaseEntity> entityMetaInfo = MetaInfo.runtimeParse(BaseEntity.class);
        Function<Row, BaseEntity> ctor = entityMetaInfo.ctor();
        BaseEntity baseEntity = ctor.apply(null);
        Assertions.assertNotNull(baseEntity);
        List<MetaInfo.MetaField<BaseEntity, ?>> fields = entityMetaInfo.fields();
        Assertions.assertEquals(1, fields.size());
        MetaInfo.MetaField<BaseEntity, ?> metaField = fields.get(0);
        String name = metaField.dbField();
        Assertions.assertEquals("needExist", name);
        ((BiConsumer<BaseEntity, String>) metaField.setter()).accept(baseEntity, "123");
        Assertions.assertEquals("123", baseEntity.getNeedExist());
        Assertions.assertEquals(baseEntity.getNeedExist(), metaField.getter().apply(baseEntity));
    }

    @Test
    public void testComplexMeta() {
        MetaInfo<DBEntity> entityMetaInfo = MetaInfo.runtimeParse(DBEntity.class);
        Function<Row, DBEntity> ctor = entityMetaInfo.ctor();
        DBEntity dbEntity = ctor.apply(null);
        Assertions.assertNotNull(dbEntity);

        List<MetaInfo.MetaField<DBEntity, ?>> fields = entityMetaInfo.fields();
        Assertions.assertEquals(2, fields.size());

        var dbEntityMetaFieldOp = fields.stream().filter(mf -> mf.dbField().equals("ext_id")).findAny();
        Assertions.assertTrue(dbEntityMetaFieldOp.isPresent());
        var dbEntityMetaField = dbEntityMetaFieldOp.get();
        var setter = ((BiConsumer<DBEntity, Integer>) dbEntityMetaField.setter());
        setter.accept(dbEntity, 100);

        Assertions.assertEquals(100L, dbEntity.getId());
        Assertions.assertEquals(dbEntity.getId(), dbEntityMetaField.getter().apply(dbEntity));

    }

    @Test
    public void testRecord() {
        MetaInfo<DBRecord> entityMetaInfo = MetaInfo.runtimeParse(DBRecord.class);
        Row row = Mockito.mock(Row.class);
        String testString = UUID.randomUUID().toString();
        long testId = 1024L;
        Mockito.when(row.getColumnIndex(Mockito.eq("customer_name")))
                .thenReturn(0);
        Mockito.when(row.getString(Mockito.eq(0)))
                .thenReturn(testString);
        Mockito.when(row.getColumnIndex(Mockito.eq("id")))
                .thenReturn(1);
        Mockito.when(row.getLong(Mockito.eq(1)))
                .thenReturn(testId);

        DBRecord record = entityMetaInfo.ctor().apply(row);
        Assertions.assertEquals(testString, record.name());
        Assertions.assertEquals(testId, record.id());

        List<MetaInfo.MetaField<DBRecord, ?>> fields = entityMetaInfo.fields();
        Assertions.assertEquals(2, fields.size());
        Optional<MetaInfo.MetaField<DBRecord, ?>> customerNameMetaOp = fields.stream().filter(m -> m.dbField().equals("customer_name")).findAny();
        Assertions.assertTrue(customerNameMetaOp.isPresent());
        var customerNameMeta = customerNameMetaOp.get();
        BiConsumer<DBRecord, String> setter = (BiConsumer<DBRecord, String>) customerNameMeta.setter();

        Assertions.assertDoesNotThrow(() -> {
            setter.accept(record, testString);
        });
    }

}
