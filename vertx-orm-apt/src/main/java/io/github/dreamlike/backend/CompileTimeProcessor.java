package io.github.dreamlike.backend;

import io.github.dreamlike.orm.base.annotation.ReactiveMapper;
import io.github.dreamlike.orm.base.constant.Constant;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@SupportedAnnotationTypes("io.github.dreamlike.orm.base.annotation.ReactiveMapper")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class CompileTimeProcessor extends AbstractProcessor {
    ProcessingEnvironment env;

    private CompileTimeMetaInfoRecorder recorder;


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        this.env = processingEnv;
        this.recorder = new CompileTimeMetaInfoRecorder(this);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            try (OutputStream outputStream = env.getFiler().createSourceFile(Constant.COMPILE_TIME_META_INFO_RECORD_CLASS_NAME).openOutputStream()) {
                outputStream.write(recorder.toJavaFile().getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        Set<? extends Element> mapperClass = roundEnv.getElementsAnnotatedWith(ReactiveMapper.class);
        for (Element element : mapperClass) {
            recorder.record(element);
        }
        return false;
    }
}
