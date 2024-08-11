package io.github.dreamlike.backend.common;

import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Map;

public record CompileMetaInfo(boolean isRecord, String newStatement,
                              Map<String /*dbName*/, FieldInjector> fieldInjectors) {

    public record FieldInjector(VariableElement origin, String setterName, String getterName, MappingType mappingType) {

        public TypeMirror toType() {
            return origin.asType();
        }

    }

    public enum MappingType {
        ONE_TO_ONE,
        ONE_TO_MANY,
        SIMPLE,
        CUSTOMER;

        public static MappingType getMappingType(VariableElement field) {
            TypeMirror mirror = field.asType();
            //原始类型直接变为SIMPLE
            if (mirror.getKind().isPrimitive() || AptHelper.isString(mirror)) {
                return MappingType.SIMPLE;
            }
            TypeElement typeElement = (TypeElement) ((DeclaredType) mirror).asElement();
            if (AptHelper.isCollection(typeElement)) {
                if (field.getAnnotation(OneToMany.class) != null) {
                    return MappingType.ONE_TO_MANY;
                } else {
                    return MappingType.CUSTOMER;
                }
            }

            return field.getAnnotation(OneToOne.class) == null ? MappingType.CUSTOMER : MappingType.ONE_TO_ONE;
        }
    }
}
