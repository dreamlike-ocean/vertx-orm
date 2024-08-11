package io.github.dreamlike.backend.recorder;


import io.github.dreamlike.backend.CompileTimeProcessor;
import io.github.dreamlike.orm.base.constant.Constant;

import javax.lang.model.element.*;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CompileTimeMetaInfoRecorder {
    private final CompileTimeProcessor currentProcessor;

    private final List<Element> mappers;

    private MetaRecorder metaRecords;

    private RowMapperRecorder rowMapperRecords;

    public CompileTimeMetaInfoRecorder(CompileTimeProcessor currentProcessor) {
        this.currentProcessor = currentProcessor;
        this.mappers = new ArrayList<>();
        this.metaRecords = new MetaRecorder(currentProcessor);
        this.rowMapperRecords = new RowMapperRecorder(currentProcessor, metaRecords);
    }


    public void record(Element rowMapperInterface) {
        ElementKind kind = rowMapperInterface.getKind();
        if (!kind.isInterface()) {
            return;
        }

        mappers.add(rowMapperInterface);
        metaRecords.recordMeta(rowMapperInterface);
        rowMapperRecords.record(rowMapperInterface);
    }


    public void toJavaFile() {
        toMetaCollection();
        toImplCode();
    }


    private void toMetaCollection() {
        String values = String.join("\n", metaRecords.records.values());
        String file = String.format(
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
        try (OutputStream outputStream = currentProcessor.env.getFiler().createSourceFile(Constant.COMPILE_TIME_META_INFO_RECORD_CLASS_NAME).openOutputStream()) {
            outputStream.write(file.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void toImplCode() {
        List<RowMapperRecorder.ImplCode> implCodes = rowMapperRecords.implCodes;
        for (RowMapperRecorder.ImplCode implCode : implCodes) {
            try (OutputStream outputStream = currentProcessor.env.getFiler().createSourceFile(implCode.name()).openOutputStream()) {
                outputStream.write(implCode.code().getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
