package com.metova.slim.compiler;

import com.metova.slim.annotation.Callback;
import com.metova.slim.annotation.Extra;
import com.metova.slim.annotation.Layout;
import com.metova.slim.internal.ProcessorValues;
import com.metova.slim.internal.SlimBinder;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.tools.JavaFileObject;

import static javax.lang.model.element.ElementKind.INTERFACE;
import static javax.tools.Diagnostic.Kind.ERROR;

public class SlimProcessor extends AbstractProcessor {

    private static Set<String> arrayToSet(String[] array) {
        assert array != null;

        Set<String> set = new HashSet<>(array.length);
        Collections.addAll(set, array);

        return set;
    }

    private static String getEnclosingClassName(Element type) {
        return type.getEnclosingElement().getSimpleName().toString();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Callback.class)) {
            String packageName = getPackageName(element);
            String enclosingClassName = getEnclosingClassName(element);

            TypeMirror elementType = element.asType();
            if (elementType.getKind() == TypeKind.TYPEVAR) {
                TypeVariable typeVariable = (TypeVariable) elementType;
                elementType = typeVariable.getUpperBound();
            }

            if (!(elementType instanceof DeclaredType) || ((DeclaredType) elementType).asElement().getKind() != INTERFACE) {
                error(element, "Callback must be an interface");
                continue;
            }

            JavaBuilder builder = new JavaBuilder(packageName, enclosingClassName + ProcessorValues.SUFFIX, new String[]{SlimBinder.class.getCanonicalName()});
            builder.addMethod("bind", new String[]{"Object source"}, null,
                    "\t\t" + enclosingClassName + " fragSource = (" + enclosingClassName + ") source;\n"
                            + "\t\tfragSource.mCallback = (DemoFragment.DemoCallback) fragSource.getActivity();");

            String javaSource = builder.build();

            final String sourceName = packageName + "." + enclosingClassName + ProcessorValues.SUFFIX;
            try {
                JavaFileObject jfo = processingEnv.getFiler().createSourceFile(sourceName, element);
                Writer writer = jfo.openWriter();
                writer.write(javaSource);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                error(element, "Unable to write process binder for type " + element + ": " + e.getMessage());
            }
        }

        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return arrayToSet(new String[]{
                Callback.class.getCanonicalName(),
                Extra.class.getCanonicalName(),
                Layout.class.getCanonicalName()
        });
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private String getPackageName(Element type) {
        return processingEnv.getElementUtils().getPackageOf(type).getQualifiedName().toString();
    }

    private void error(Element element, String message) {
        processingEnv.getMessager().printMessage(ERROR, message, element);
    }
}
