package io.github.dreamlike.backend.recorder;

import io.github.dreamlike.backend.CompileTimeProcessor;
import io.github.dreamlike.backend.common.AptHelper;
import io.github.dreamlike.backend.common.CompileMetaInfo;
import io.github.dreamlike.backend.exception.MapperFormatException;
import io.github.dreamlike.backend.exception.TodoException;
import io.github.dreamlike.orm.base.constant.Constant;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RowMapperRecorder {

    public record ImplCode(String name, String code) {
    }


    private final CompileTimeProcessor currentProcessor;

    final List<ImplCode> implCodes = new ArrayList<>();

    private final MetaRecorder metaRecorder;

    public RowMapperRecorder(CompileTimeProcessor currentProcessor, MetaRecorder metaRecorder) {
        this.currentProcessor = currentProcessor;
        this.metaRecorder = metaRecorder;
    }

    public void record(Element rowMapperInterface) {
        var methodElements = ElementFilter.methodsIn(rowMapperInterface.getEnclosedElements());
        List<String> implBody = new ArrayList<>();
        for (ExecutableElement methodElement : methodElements) {
            String body = generateMethodImplBody(rowMapperInterface, methodElement);
            String returnType = methodElement.getReturnType().toString();
            String methodName = methodElement.getSimpleName().toString();
            String rowListParamName = Constant.ROWS_PARAM_NAME;

            String implMethodBody = String.format("""
                    @Override
                    public %s %s(List<io.vertx.sqlclient.Row> %s) {
                        %s
                    }
                    
                    """.trim(), returnType, methodName, rowListParamName, body);
            implBody.add(implMethodBody);
        }

        //todo 塞进implCodes里面
    }

    private String generateMethodImplBody(Element rowMapperInterface, ExecutableElement methodElement) {
        if (methodElement.getModifiers().contains(Modifier.DEFAULT)) {
            //default不管
            return "";
        }

        DeclaredType returnType = (DeclaredType) methodElement.getReturnType();
        TypeElement currentType = (TypeElement) currentProcessor.env.getTypeUtils().asElement(returnType);

        if (AptHelper.isCollection(currentType)) {
            currentType = currentProcessor.aptHelper.removeCollectionGeneric(returnType);
        }

        var name = currentType.getQualifiedName().toString();
        CompileMetaInfo compileMetaInfo = metaRecorder.metaInfoMap.get(name);

        if (compileMetaInfo == null) {
            throw new MapperFormatException(rowMapperInterface + " " + methodElement + " no meta!");
        }

        boolean isNeedMultiRowMapping = false;

        for (CompileMetaInfo.FieldInjector value : compileMetaInfo.fieldInjectors().values()) {
            if (value.mappingType() == CompileMetaInfo.MappingType.ONE_TO_MANY) {
                isNeedMultiRowMapping = true;
            }
        }

        if (isNeedMultiRowMapping) {
            throw new TodoException();
        } else {
            return generateSimple(rowMapperInterface, methodElement, compileMetaInfo);
        }

    }

    private String generateSimple(Element rowMapperInterface, ExecutableElement methodElement, CompileMetaInfo realReturnTypeMetaInfo) {

        DeclaredType returnType = (DeclaredType) methodElement.getReturnType();
        TypeElement realTypeElement = currentProcessor.aptHelper.removeCollectionGeneric(returnType);
        //结果集合容器
        String firstResultStatement = String.format("var resultSet = new ArrayList<%s>();", realTypeElement.getQualifiedName());
        List<String> singleRowMappingStatement = new ArrayList<>();

        String mappingEntityReferenceName = "_userEntity";
        singleRowMappingStatement.add(
                "var _userEntity = " + realReturnTypeMetaInfo.newStatement() + ";"
        );

        //非record才setter
        if (!realReturnTypeMetaInfo.isRecord()) {
            Map<String, CompileMetaInfo.FieldInjector> dbNameMetaMapping = realReturnTypeMetaInfo.fieldInjectors();
            Set<String> dbNames = dbNameMetaMapping.keySet();
            for (String dbName : dbNames) {
                String quoteDbName = String.format("\"%s\"".trim(), dbName);
                CompileMetaInfo.FieldInjector fieldInjector = dbNameMetaMapping.get(dbName);
                TypeMirror fieldType = fieldInjector.toType();
                switch (fieldInjector.mappingType()) {
                    case SIMPLE -> {
                        if (AptHelper.isString(fieldType)) {
                            singleRowMappingStatement.add(String.format("%s.%s(VertxRowReflection.getString(row, %s))", mappingEntityReferenceName, fieldInjector.setterName(), quoteDbName));
                            break;
                        }
                        switch (fieldType.getKind()) {
                            case INT ->
                                    singleRowMappingStatement.add(String.format("%s.%s(VertxRowReflection.getInt(row, %s))", mappingEntityReferenceName, fieldInjector.setterName(), quoteDbName));
                            case BOOLEAN ->
                                    singleRowMappingStatement.add(String.format(String.format("%s.%s(VertxRowReflection.getBooleanPrimitive(row, %s))", mappingEntityReferenceName, fieldInjector.setterName(), quoteDbName)));
                            case LONG ->
                                    singleRowMappingStatement.add(String.format("%s.%s(VertxRowReflection.getLongPrimitive(row, %s))", mappingEntityReferenceName, fieldInjector.setterName(), quoteDbName));
                            case FLOAT ->
                                    singleRowMappingStatement.add(String.format("%s.%s(VertxRowReflection.getFloatPrimitive(row, %s))", mappingEntityReferenceName, fieldInjector.setterName(), quoteDbName));
                            case DOUBLE ->
                                    singleRowMappingStatement.add(String.format("%s.%s(VertxRowReflection.getDoublePrimitive(row, %s))", mappingEntityReferenceName, fieldInjector.setterName(), quoteDbName));
                            case SHORT ->
                                    singleRowMappingStatement.add(String.format("%s.%s(VertxRowReflection.getShortPrimitive(row, %s))", mappingEntityReferenceName, fieldInjector.setterName(), quoteDbName));
                            case BYTE ->
                                    singleRowMappingStatement.add(String.format("%s.%s(VertxRowReflection.getBytePrimitive(row, %s))", mappingEntityReferenceName, fieldInjector.setterName(), quoteDbName));
                            default ->
                                    throw new MapperFormatException(rowMapperInterface + " " + methodElement + " " + fieldInjector.toType() + " not support!");
                        }
                    }

                    case CUSTOMER -> {
                        String name = AptHelper.qualifiedName(fieldType);
                        singleRowMappingStatement.add(String.format("%s.%s(typeHandleCache.get(new io.github.dreamlike.orm.base.meta.TypeHolder<%s>(){}.getType()).map(row)", mappingEntityReferenceName, fieldInjector.setterName(), name));
                    }

                    //todo one to one
                }
            }
        }


        return "";
    }
}
