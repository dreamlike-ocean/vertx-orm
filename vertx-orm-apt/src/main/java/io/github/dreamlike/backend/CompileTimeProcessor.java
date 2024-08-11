package io.github.dreamlike.backend;

import io.github.dreamlike.backend.recorder.CompileTimeMetaInfoRecorder;
import io.github.dreamlike.backend.common.AptHelper;
import io.github.dreamlike.orm.base.annotation.RowMapper;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@SupportedAnnotationTypes("io.github.dreamlike.orm.base.annotation.RowMapper")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class CompileTimeProcessor extends AbstractProcessor {
    public ProcessingEnvironment env;

    private CompileTimeMetaInfoRecorder recorder;

    public AptHelper aptHelper;


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        this.env = processingEnv;
        this.aptHelper = new AptHelper(env);
        this.recorder = new CompileTimeMetaInfoRecorder(this);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
           recorder.toJavaFile();
        }

        Set<? extends Element> mapperClass = roundEnv.getElementsAnnotatedWith(RowMapper.class);
        for (Element element : mapperClass) {
            recorder.record(element);
        }
        return false;
    }
}
