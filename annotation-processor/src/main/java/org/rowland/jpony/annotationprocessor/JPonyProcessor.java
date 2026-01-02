package org.rowland.jpony.annotationprocessor;

import com.google.auto.service.AutoService;
import com.sun.source.util.Trees;
import org.rowland.jpony.annotations.JPClass;
import org.rowland.jpony.checker.TypeChecker;
import org.rowland.jpony.annotations.JPActor;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@AutoService(Processor.class)
@SupportedAnnotationTypes(value={"org.rowland.jpony.annotations.JPActor","org.rowland.jpony.annotations.JPClass"})
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class JPonyProcessor extends AbstractProcessor {
    Trees trees;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        trees = Trees.instance(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            List<Name> factoryNames = roundEnv.getElementsAnnotatedWith(JPActor.class).stream()
                    .map(e -> getFactoryName((TypeElement) e))
                    .toList();

            TypeChecker typeChecker = new TypeChecker(processingEnv, factoryNames);
            for (Element element : roundEnv.getRootElements()) {
                if (element.getAnnotation(JPActor.class) != null || element.getAnnotation(JPClass.class) != null) {
                    typeChecker.typeCheck(element);
                }
            }

            for (Element element : roundEnv.getElementsAnnotatedWith(JPActor.class)) {
                JPActor actorAnnotation = element.getAnnotation(JPActor.class);
                List<DeclaredType> behaviors = null;
                try {
                    Class<?>[] behaviorClasses = actorAnnotation.behaviors();
                    if (behaviorClasses == null || behaviorClasses.length == 0) {
                        processingEnv.getMessager().printMessage(
                                Diagnostic.Kind.ERROR,
                                "@Actor requires at least one behavior interface",
                                element);
                    }
                    throw new IllegalStateException("behavior interface is compiled");
                } catch (MirroredTypesException e) {
                    TypeMirror factoryMirror = e.getTypeMirrors().getFirst();
                    if (factoryMirror.getKind() == TypeKind.DECLARED  &&
                            ((DeclaredType) factoryMirror).asElement().getKind() == ElementKind.INTERFACE) {
                        behaviors = List.of((DeclaredType) factoryMirror);
                    } else {
                        processingEnv.getMessager().printMessage(
                                Diagnostic.Kind.ERROR,
                                "@Actor behaviors property must reference an interface",
                                element);
                        throw new IllegalStateException();
                    }
                }
                DeclaredType factory = null;
                try {
                    Class<?> factoryClass = actorAnnotation.factory();
                    if (factoryClass == null) {
                        processingEnv.getMessager().printMessage(
                                Diagnostic.Kind.ERROR,
                                "@Actor requires at least one factory interface",
                                element);
                    }
                    throw new IllegalStateException("factory interface is compiled");
                } catch (MirroredTypesException e) {
                    TypeMirror factoryMirror = e.getTypeMirrors().getFirst();
                    if (factoryMirror.getKind() == TypeKind.DECLARED  &&
                            ((DeclaredType) factoryMirror).asElement().getKind() == ElementKind.INTERFACE) {
                        factory = (DeclaredType) factoryMirror;
                    } else {
                        processingEnv.getMessager().printMessage(
                                Diagnostic.Kind.ERROR,
                                "@Actor factory property must reference an interface",
                                element);
                        throw new IllegalStateException();
                    }
                }

                ActorWrapperGenerator actorWrapperGenerator = new ActorWrapperGenerator(processingEnv, (TypeElement) element, factory);
                actorWrapperGenerator.generate(behaviors.getFirst());

                ActorFactoryGenerator actorFactoryGenerator = new ActorFactoryGenerator(processingEnv, (TypeElement) element, behaviors.getFirst(), factory);
                actorFactoryGenerator.generate();
            }

            return false; // Claim these annotations
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            processingEnv.getMessager().printError(e.getMessage());
            throw e;
        }
    }

    private Name getFactoryName(TypeElement typeElement) {
        Name packageName = processingEnv.getElementUtils().getPackageOf(typeElement).getQualifiedName();
        Name className = typeElement.getSimpleName();

        return processingEnv.getElementUtils().getName(packageName.toString()+"."+className.toString()+"Factory");
    }
}