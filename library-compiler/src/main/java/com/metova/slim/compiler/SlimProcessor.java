package com.metova.slim.compiler;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes({
        "com.metova.slim.annotation.Callback",
        "com.metova.slim.annotation.Extra",
        "com.metova.slim.annotation.Layout"
})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class SlimProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // TODO: Process things
        return true;
    }
}
