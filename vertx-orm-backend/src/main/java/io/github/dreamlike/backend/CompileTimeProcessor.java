package io.github.dreamlike.backend;

import io.github.dreamlike.orm.base.annotation.ReactiveMapper;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@SupportedAnnotationTypes("io.github.dreamlike.orm.base.annotation.ReactiveMapper")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class CompileTimeProcessor extends AbstractProcessor  {
    private ProcessingEnvironment env;


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        this.env = processingEnv;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        Set<? extends Element> mapperClass = roundEnv.getElementsAnnotatedWith(ReactiveMapper.class);

        return false;
    }
}
