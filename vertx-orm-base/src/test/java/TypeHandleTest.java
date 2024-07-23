import io.github.dreamlike.orm.base.TypeHandler;
import io.vertx.sqlclient.Row;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

public class TypeHandleTest {

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
        Assertions.assertEquals(String.class,  parameterizedType.getActualTypeArguments()[0]);

    }
}
