package org.rowland.jpony.annotationprocessor;

import com.palantir.javapoet.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import java.io.IOException;
import java.util.List;

final class ActorFactoryGenerator extends GeneratorBase {
    private DeclaredType behaviorInterface;
    ActorFactoryGenerator(ProcessingEnvironment processingEnvironment, TypeElement actorType, DeclaredType behaviorInterface, DeclaredType factoryInterface) {
        super(processingEnvironment, actorType, factoryInterface);
        this.behaviorInterface = behaviorInterface;
    }

    void generate() throws IOException {
        ClassName factoryClassName = ClassName.get(
                ((PackageElement) actorElement.getEnclosingElement()).getQualifiedName().toString(),
                processingEnvironment.getTypeUtils().asElement(factoryInterface).getSimpleName() + "Impl");

        TypeSpec.Builder factoryBuilder = TypeSpec
                .classBuilder(processingEnvironment.getTypeUtils().asElement(factoryInterface).getSimpleName() + "Impl")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(factoryInterface);

        getInitMethods().stream()
                .map(this::createCreateMethodSpec)
                .forEach(factoryBuilder::addMethod);

        TypeSpec wrapper = factoryBuilder.build();

        JavaFile file = JavaFile.builder(((PackageElement) actorElement.getEnclosingElement()).getQualifiedName().toString(), wrapper).build();

        file.writeTo(processingEnvironment.getFiler());
    }

    private MethodSpec createCreateMethodSpec(ExecutableElement method) {
        ClassName wrapperClassName = ClassName.get(
                ((PackageElement) actorElement.getEnclosingElement()).getQualifiedName().toString(),
                actorElement.getSimpleName() + "Wrapper");

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.get(behaviorInterface));

        method.getParameters().stream()
                .map(p ->
                        ParameterSpec.builder(TypeName.get(p.asType()), p.getSimpleName().toString()).build())
                .forEach(methodBuilder::addParameter);

        List<CodeBlock> initCallParams = method.getParameters().stream()
                .map(p -> p.getSimpleName().toString())
                .map(paramName -> CodeBlock.of("$L", paramName))
                .toList();

        CodeBlock.Builder codeBuilder = CodeBlock.builder()
                .addStatement("$1T $2L = new $1T()", wrapperClassName, "wrapper")
                .addStatement("$L.create($L)", "wrapper", CodeBlock.join(initCallParams, ", "))
                .addStatement("return $L", "wrapper");

        methodBuilder.addCode(codeBuilder.build());

        return methodBuilder.build();
    }

}
