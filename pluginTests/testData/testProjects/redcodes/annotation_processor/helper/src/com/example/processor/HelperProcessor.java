package com.example.processor;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes("com.example.processor.GenerateHelper")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class HelperProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element instanceof TypeElement) {
                    TypeElement typeElement = (TypeElement) element;
                    String className = typeElement.getSimpleName().toString();
                    PackageElement pkg = (PackageElement) typeElement.getEnclosingElement();
                    String packageName = pkg.getQualifiedName().toString();
                    String helperName = className + "Helper";
                    try {
                        String qualifiedName = packageName.isEmpty() ? helperName : packageName + "." + helperName;
                        JavaFileObject file = processingEnv.getFiler().createSourceFile(qualifiedName, element);
                        try (Writer w = file.openWriter()) {
                            if (!packageName.isEmpty()) {
                                w.write("package " + packageName + ";\n\n");
                            }
                            w.write("public class " + helperName + " {\n");
                            w.write("    public static String help() { return \"" + className + "\"; }\n");
                            w.write("}\n");
                        }
                    } catch (IOException e) {
                        // Ignore duplicates in repeated rounds
                    }
                }
            }
        }
        return true;
    }
}
