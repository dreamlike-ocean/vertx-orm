package io.github.dreamlike.backend.common;

import io.github.dreamlike.backend.exception.MapperFormatException;
import io.github.dreamlike.orm.base.meta.TypeHandler;
import io.vertx.sqlclient.Row;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class AptHelper {

    private final ProcessingEnvironment env;

    public AptHelper(ProcessingEnvironment env) {
        this.env = env;
    }

    public <T> boolean isRowList(VariableElement variableElement) {
        TypeMirror mirror = variableElement.asType();
        TypeElement typeElement = (TypeElement) env.getTypeUtils().asElement(mirror);
        String baseName = typeElement.getQualifiedName().toString();
        if(!List.class.getName().equals(baseName)) {
            return false;
        }
        if (mirror instanceof DeclaredType) {
            return ((DeclaredType) mirror).getTypeArguments()
                    .stream()
                    .anyMatch(t -> t.toString().equals(Row.class.getTypeName()));
        }
        return false;
    }


    public static boolean isCollection(TypeElement currentType) {
        var name = currentType.getQualifiedName().toString();
       return List.class.getName().equals(name) || Set.class.getName().equals(name) || Collection.class.getName().equals(name);
    }

    public TypeElement removeCollectionGeneric(TypeMirror currentType) {
        TypeElement typeElement = (TypeElement) env.getTypeUtils().asElement(currentType);

        if (!isCollection(typeElement)) {
            return typeElement;
        }

        List<? extends TypeMirror> typeArguments = ((DeclaredType) currentType).getTypeArguments();
        if (typeArguments.isEmpty()) {
            return null;
        }
        TypeMirror argumentsFirst = typeArguments.getFirst();
        var res = (TypeElement) env.getTypeUtils().asElement(argumentsFirst);

        return res;
    }

    public static boolean isString(TypeMirror mirror) {
        if (mirror.getKind() != TypeKind.DECLARED) {
            return false;
        }

        Element element = ((DeclaredType) mirror).asElement();

        return ((TypeElement) element).getQualifiedName().toString().equals(String.class.getName());
    }


    public static String qualifiedName(TypeMirror typeMirror) {
        DeclaredType declaredType = (DeclaredType) typeMirror;
        TypeElement element = (TypeElement) declaredType.asElement();
        return element.getQualifiedName().toString();
    }



}
