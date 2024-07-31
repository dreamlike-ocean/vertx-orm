package io.github.dreamlike.backend;


import io.github.dreamlike.backend.exception.CtorNonMatchException;
import io.github.dreamlike.backend.exception.MapperFormatException;
import io.github.dreamlike.orm.base.helper.Pair;
import jakarta.persistence.Column;
import jakarta.persistence.Transient;

import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.util.*;

public class CompileTimeMetaInfoRecorder {
    Map<String /*className */, String /*MetaInfo init code */> records;

    Map<String, CompileMetaInfo> metaInfoMap;

    private final CompileTimeProcessor currentProcessor;

    public CompileTimeMetaInfoRecorder(CompileTimeProcessor currentProcessor) {
        this.currentProcessor = currentProcessor;
        this.records = new HashMap<>();
        this.metaInfoMap = new HashMap<>();
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

    public String toJavaFile() {
        //todo
        String values = String.join("\n", records.values());
        return String.format(
                """
                         package io.github.dreamlike.backend;
                         import io.github.dreamlike.orm.base.meta.*;
                         import io.github.dreamlike.orm.base.db.VertxRowReflection;
                         import java.util.ArrayList;
                         import java.util.List;
                         import java.util.Map;
                         import java.util.function.Function;
                         import java.util.stream.Collectors;
                        public class CompileTimeMetaInfoCollection implements MetaInfoCollection {
                                                                     
                             private final TypeHandleCache typeHandleCache;
                             
                             private final Map<Class<?>, MetaInfo<?>> metaInfoMap;
                             
                             public CompileTimeMetaInfoCollection(TypeHandleCache typeHandleCache) {
                                 this.typeHandleCache = typeHandleCache;
                                 List<MetaInfo<?>> metaInfos = new ArrayList<>();
                                 init(typeHandleCache, metaInfos);
                                 this.metaInfoMap = metaInfos.stream()
                                         .collect(Collectors.toMap(MetaInfo::ownerClass, Function.identity(), (a, b) -> a));
                             }
                             
                             @Override
                             public <T> MetaInfo<T> getMetaInfo(Class<T> clazz) {
                                 return (MetaInfo<T>) metaInfoMap.get(clazz);
                             }
                           
                                                     
                             private void init(TypeHandleCache typeHandleCache, List<MetaInfo<?>> metaInfos) {
                                    %s
                            
                              }
                        }                        
                         
                         """.trim(),
                values
        );
    }


    private void recordTypeMeta(Element reactiveMapperInterface, ExecutableElement element, TypeMirror typeMirror) {

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

        if (records.containsKey(name)) {
            return;
        }

        String newStatement = generatorNewStatement(currentType);

        String ctorStatement = String.format("(row) -> %s", newStatement);

        String ownerClass = String.format("%s.class", currentType.getQualifiedName().toString());
        Pair<List<String>, Map<String, CompileMetaInfo.FieldInjector>> fieldInfo = generatorFieldsStatement(currentType);
        List<String> fieldsStatement = fieldInfo.l();
        Map<String, CompileMetaInfo.FieldInjector> fieldInjectors = fieldInfo.r();

        String initStatement = String.format(
                """
                        {
                        try{
                        var ownerClass = %s;
                        var info = new MetaInfo<>(ownerClass, %s, List.of(%s));
                        metaInfos.add(info);
                        }catch (Throwable t) {
                                         throw new RuntimeException(t);
                        }
                        }
                        """.trim(),
                ownerClass,
                ctorStatement,
                String.join(",", fieldsStatement)
        );
        records.putIfAbsent(name, initStatement);
        metaInfoMap.putIfAbsent(name, new CompileMetaInfo(newStatement, fieldInjectors));
    }

    //生成 -> new io.github.dreamlike.backend.test.entity.DBEntity()
    public String generatorNewStatement(TypeElement currentType) {
        //默认当前能获取到变量名为typeHandleCache的参数
        List<ExecutableElement> executableElements = ElementFilter.constructorsIn(currentType.getEnclosedElements());

        if (executableElements.isEmpty()) {
            throw new CtorNonMatchException(currentType + " has no ctor!");
        }

        if (executableElements.size() != 1) {
            throw new CtorNonMatchException(currentType + " too many ctor!");
        }

        ExecutableElement ctorElement = executableElements.getFirst();

        StringJoiner joiner = new StringJoiner(",", "(", ")");
        for (VariableElement parameter : ctorElement.getParameters()) {
            TypeMirror typeMirror = parameter.asType();
            Element element = currentProcessor.env.getTypeUtils().asElement(typeMirror);
            TypeKind parameterType = typeMirror.getKind();
            final String dbName = String.format("""
                    "%s"
                    """.trim(), parameter.getSimpleName());
            String statement = switch (parameterType) {
                case BOOLEAN -> String.format("VertxRowReflection.getBooleanPrimitive(row, %s)", dbName);
                case BYTE -> String.format("VertxRowReflection.getBytePrimitive(row, %s)", dbName);
                case SHORT -> String.format("VertxRowReflection.getShortPrimitive(row, %s)", dbName);
                case INT -> String.format("VertxRowReflection.getInt(row, %s)", dbName);
                case LONG -> String.format("VertxRowReflection.getLongPrimitive(row, %s)", dbName);
                case FLOAT -> String.format("VertxRowReflection.getFloatPrimitive(row, %s)", dbName);
                case DOUBLE -> String.format("VertxRowReflection.getDoublePrimitive(row, %s)", dbName);
                //这里不判断具体的类型了。。反正有编译器保证
                case ARRAY -> String.format("VertxRowReflection.getByteArray(row, %s)", dbName);
                case DECLARED -> switch (((TypeElement) element).getQualifiedName().toString()) {
                    case "java.lang.Boolean" -> String.format("VertxRowReflection.getBoolean(row, %s)", dbName);
                    case "java.lang.String" -> String.format("VertxRowReflection.getString(row, %s)", dbName);
                    case "java.lang.Byte" -> String.format("VertxRowReflection.getByte(row, %s)", dbName);
                    case "java.lang.Short" -> String.format("VertxRowReflection.getShort(row, %s)", dbName);
                    case "java.lang.Integer" -> String.format("VertxRowReflection.getInteger(row, %s)", dbName);
                    case "java.lang.Long" -> String.format("VertxRowReflection.getLong(row, %s)", dbName);
                    case "java.lang.Float" -> String.format("VertxRowReflection.getFloat(row, %s)", dbName);
                    case "java.lang.Double" -> String.format("VertxRowReflection.getDouble(row, %s)", dbName);
                    case "java.time.LocalDate" -> String.format("VertxRowReflection.getLocalDate(row, %s)", dbName);
                    case "java.time.LocalDateTime" ->
                            String.format("VertxRowReflection.getLocalDateTime(row, %s)", dbName);
                    default ->
                            String.format("(%s)typeHandleCache.get(new io.github.dreamlike.orm.base.meta.TypeHolder<%s>(){}.getType()).map(row)", ((TypeElement) element).getQualifiedName().toString(), ((TypeElement) element).getQualifiedName().toString());
                };
                default -> throw new UnsupportedOperationException(typeMirror + " cant be supported");
            };
            joiner.add(statement);
        }

        return String.format("new %s%s", currentType.getQualifiedName(), joiner);
    }

    public Pair<List<String>,  Map<String /*dbName*/, CompileMetaInfo.FieldInjector>> generatorFieldsStatement(TypeElement ownerType) {
        List<VariableElement> allFieldElement = new ArrayList<>();
        String ownerClassName = ownerType.getQualifiedName().toString();

        Map<String /*dbName*/, CompileMetaInfo.FieldInjector> fieldInjectors = new HashMap<>();

        boolean isRecord = ownerType.getKind() == ElementKind.RECORD;

        TypeElement node = ownerType;

        //不想动脑了 就这样
        while (!node.getQualifiedName().toString().equals(Object.class.getName())) {
            allFieldElement.addAll(ElementFilter.fieldsIn(node.getEnclosedElements()));
            node = (TypeElement) currentProcessor.env.getTypeUtils().asElement(node.getSuperclass());
        }

        allFieldElement = allFieldElement.stream()
                .filter(variableElement -> variableElement.getAnnotation(Transient.class) == null)
                .toList();
        ArrayList<String> fieldMetaInfoStatement = new ArrayList<>();

        for (VariableElement variableElement : allFieldElement) {
            String fieldName = variableElement.getSimpleName().toString();
            String attributeName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

            String setterMethodReference = isRecord ? "(o,t) -> {}" : String.format("%s::set%s", ownerClassName, attributeName);
            String getterMethodReference = isRecord ? String.format("%s::%s", ownerClassName, fieldName) : String.format("%s::get%s", ownerClassName, attributeName);


            String dbFieldName = Optional.ofNullable(variableElement.getAnnotation(Column.class))
                    .map(Column::name)
                    .orElse(fieldName);
            String fieldSearch = String.format("""
                    ownerClass.getDeclaredField("%s")
                    """.trim(), fieldName);

            fieldMetaInfoStatement.add(
                    String.format("""
                                    new MetaInfo.MetaField<>(%s,%s,%s,%s)
                                    """.trim(),
                            String.format("\"%s\"", dbFieldName),
                            fieldSearch,
                            setterMethodReference,
                            getterMethodReference
                    )

            );

            fieldInjectors.put(dbFieldName, new CompileMetaInfo.FieldInjector(setterMethodReference, getterMethodReference));
        }

        return new Pair<>(fieldMetaInfoStatement, fieldInjectors);
    }

}
