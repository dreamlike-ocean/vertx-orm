package io.github.dreamlike.backend;


import io.github.dreamlike.backend.exception.CtorNonMatchException;
import io.github.dreamlike.backend.exception.MapperFormatException;

import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.util.*;

public class CompileMapperRecorder {
    private List<String> recorder;

    private Set<String> hasRecord;

    private final CompileTimeProcessor currentProcessor;

    public CompileMapperRecorder(CompileTimeProcessor currentProcessor) {
        this.currentProcessor = currentProcessor;
        recorder = new ArrayList<>();
        hasRecord = new HashSet<>();
    }


    public void record(Element reactiveMapperInterface) {
        ElementKind kind = reactiveMapperInterface.getKind();
        if (!kind.isInterface()) {
            return;
        }

        var methodElements = ElementFilter.methodsIn(reactiveMapperInterface.getEnclosedElements());

        for (var methodElement : methodElements) {
            TypeMirror returnType = methodElement.getReturnType();
            recordTypeMeta(reactiveMapperInterface, methodElement, returnType);
        }
    }

    private void recordTypeMeta(Element reactiveMapperInterface, ExecutableElement element, TypeMirror typeMirror) {
        //todo递归处理嵌套
        Messager messager = currentProcessor.env.getMessager();

        TypeKind typeKind = typeMirror.getKind();
        if (typeKind != TypeKind.DECLARED) {
            messager.printMessage(Diagnostic.Kind.NOTE, typeKind.name() + " isnt declared type");
            return;
        }

        TypeElement currentType = (TypeElement) currentProcessor.env.getTypeUtils().asElement(typeMirror);
        var name = currentType.getQualifiedName().toString();

        //先判断下常用集合类
        if (List.class.getName().equals(name) || Set.class.getName().equals(name) || Collection.class.getName().equals(name)) {
            //然后把签名上的泛型记录下来
            List<? extends TypeMirror> typeArguments = ((DeclaredType) typeMirror).getTypeArguments();
            if (typeArguments.isEmpty()) {
                throw new MapperFormatException(reactiveMapperInterface.toString() + "." + element.toString() + " has error signature! About generic Type!");
            }
            currentType = (TypeElement) currentProcessor.env.getTypeUtils().asElement(typeArguments.getFirst());
            name = currentType.getQualifiedName().toString();
        }

        if (!hasRecord.add(name)) {
            return;
        }

        String string = generatorCtorStatement(currentType);
    }

    public String generatorCtorStatement(TypeElement currentType) {
        List<ExecutableElement> executableElements = ElementFilter.constructorsIn(currentType.getEnclosedElements());

        if (executableElements.isEmpty()) {
            throw new CtorNonMatchException(currentType + " has no ctor!");
        }

        if (executableElements.size() != 1) {
            throw new CtorNonMatchException(currentType + " too many ctor!");
        }

        return null;
    }


}
