package org.rowland.jpony.annotationprocessor;

import com.palantir.javapoet.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class ActorWrapperGenerator extends GeneratorBase {
    private static final String ACTOR_IMPL_VAR = "actorImpl";
    private static final String MAILBOX_VAR = "mailbox";

    /**
    public static void main(String[] args) {
        GenerateActorWrapper generator = new GenerateActorWrapper();
        try {
            generator.execute(MyActor.class, MyActorBehaviors.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    */
    ActorWrapperGenerator(ProcessingEnvironment processingEnvironment, TypeElement actorType, DeclaredType factoryInterface) {
        super(processingEnvironment, actorType, factoryInterface);
    }

    void generate(TypeMirror behaviorInterface) throws IOException {
        ClassName wrapperClassName = ClassName.get(((PackageElement) actorElement.getEnclosingElement()).getQualifiedName().toString(), actorElement.getSimpleName()+"Wrapper");
        ClassName paramsClassName = wrapperClassName.nestedClass("Params");
        List<ClassName> paramsSubclassNames = Stream.concat(getInitMethods().stream(), getBehaviorMethods(behaviorInterface).stream())
                .map(m -> wrapperClassName.nestedClass(ActorWrapperGenerator.makeParamsClassName(m.getSimpleName().toString())))
                .toList();

        TypeSpec.Builder paramsBuilder = TypeSpec
                .classBuilder(paramsClassName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.ABSTRACT, Modifier.SEALED);

        paramsSubclassNames
                .forEach(paramsBuilder::addPermittedSubclass);

        TypeSpec params = paramsBuilder.build();

        TypeSpec.Builder wrapperBuilder = TypeSpec
                .classBuilder(actorElement.getSimpleName()+"Wrapper")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(behaviorInterface)
                .addField(TypeName.get(actorElement.asType()), ACTOR_IMPL_VAR, Modifier.PRIVATE, Modifier.FINAL)
                .addField(ParameterizedTypeName.get((ClassName) TypeName.get(BlockingDeque.class), paramsClassName), MAILBOX_VAR, Modifier.PRIVATE, Modifier.FINAL);

        Stream.concat(getInitMethods().stream(), getBehaviorMethods(behaviorInterface).stream())
                .map(ee -> ActorWrapperGenerator.createWrapperMethodSpec(wrapperClassName, ee))
                .forEach(wrapperBuilder::addMethod);

        ClassName messageProcessorClassName = wrapperClassName.nestedClass("MessageProcessor");
        TypeSpec messageProcessor = TypeSpec
                .classBuilder(messageProcessorClassName)
                .addModifiers(Modifier.PRIVATE)
                .addSuperinterface(Runnable.class)
                .addMethod(messageProcessorRun(wrapperClassName, paramsClassName, behaviorInterface))
                .build();

        wrapperBuilder.addType(messageProcessor);

        wrapperBuilder.addType(params);

        Stream.concat(getInitMethods().stream(), getBehaviorMethods(behaviorInterface).stream())
                .map(m -> ActorWrapperGenerator.createParamsSubclassTypeSpec(wrapperClassName, paramsClassName, m))
                .forEach(wrapperBuilder::addType);

        MethodSpec contructor = MethodSpec.constructorBuilder()
                .addStatement("this.$L = new $T()", ACTOR_IMPL_VAR, actorElement)
                .addStatement("this.$L = new $T<>()", MAILBOX_VAR, LinkedBlockingDeque.class)
                .addStatement("$T.startVirtualThread(new $T())", Thread.class, messageProcessorClassName)
                .build();

        wrapperBuilder.addMethod(contructor);

        TypeSpec wrapper = wrapperBuilder.build();

        JavaFile file = JavaFile.builder(((PackageElement) actorElement.getEnclosingElement()).getQualifiedName().toString(), wrapper).build();

        file.writeTo(processingEnvironment.getFiler());
    }

    private static TypeSpec createParamsSubclassTypeSpec(ClassName wrapperClassName, ClassName paramsClassName, ExecutableElement method) {
        ClassName subclassName = wrapperClassName.nestedClass(ActorWrapperGenerator.makeParamsClassName(method.getSimpleName().toString()));

        TypeSpec.Builder builder = TypeSpec
                .classBuilder(subclassName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .superclass(paramsClassName);

        method.getParameters()
                .forEach(p -> builder.addField(TypeName.get(p.asType()), p.getSimpleName().toString(), Modifier.PRIVATE));

        return builder.build();
    }

    private static MethodSpec createWrapperMethodSpec(ClassName wrapperClassName, ExecutableElement method) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID);

        method.getParameters()
                .forEach(p -> methodBuilder.addParameter(TypeName.get(p.asType()), p.getSimpleName().toString()));

        ClassName paramsSubclassName = wrapperClassName.nestedClass(ActorWrapperGenerator.makeParamsClassName(method.getSimpleName().toString()));

        CodeBlock.Builder codeBuilder = CodeBlock.builder()
                .addStatement("$1T $2L = new $1T()", paramsSubclassName, "message");

        method.getParameters()
                .forEach(p -> codeBuilder.addStatement("$1L.$2L = $3L", "message", p.getSimpleName().toString(), p.getSimpleName()).toString());

        codeBuilder.beginControlFlow("try");
        codeBuilder.addStatement("this.$1L.putLast($2L)", MAILBOX_VAR, "message");
        codeBuilder.endControlFlow();
        codeBuilder.beginControlFlow("catch ($T e)", InterruptedException.class);
        codeBuilder.addStatement("throw new $1T($2S)", IllegalStateException.class, "This exception should never be thrown since JPony mailboxes have no limits");
        codeBuilder.endControlFlow();

        methodBuilder.addCode(codeBuilder.build());

        return methodBuilder.build();
    }

    private static CodeBlock createWrapperMethodDispatcher(ClassName wrapperClassName, ClassName paramsClassName, ExecutableElement method) {
        TypeSpec params = ActorWrapperGenerator.createParamsSubclassTypeSpec(wrapperClassName, paramsClassName, method);
        String parameters = params.fieldSpecs().stream()
                .map(fs -> "m."+fs.name())
                .collect(Collectors.joining(","));
        return CodeBlock.builder()
                .addStatement("case $L m -> $L.$L($L)", makeParamsClassName(method.getSimpleName().toString()), ACTOR_IMPL_VAR, method.getSimpleName().toString(), parameters)
                .build();
    }

    private MethodSpec messageProcessorRun(ClassName wrapperClassName, ClassName paramsClassName, TypeMirror behaviorInterface) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("run")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID);

        methodBuilder.beginControlFlow("try");
        methodBuilder.beginControlFlow("while (true)");
        methodBuilder.addStatement("$1T $2L = $3T.this.mailbox.takeFirst()", paramsClassName, "message", wrapperClassName);
        methodBuilder.beginControlFlow("switch ($L)", "message");
        Stream.concat(getInitMethods().stream(), getBehaviorMethods(behaviorInterface).stream())
                .forEach(ee -> methodBuilder.addCode(createWrapperMethodDispatcher(wrapperClassName, paramsClassName, ee)));
        methodBuilder.endControlFlow();
        methodBuilder.endControlFlow();
        methodBuilder.endControlFlow();
        methodBuilder.beginControlFlow("catch ($T e)", InterruptedException.class);
        methodBuilder.addComment("Exit since interruption means shutdown");
        methodBuilder.endControlFlow();

        return methodBuilder.build();
    }

    private List<ExecutableElement> getBehaviorMethods(TypeMirror behaviorType) {
        TypeElement element = (TypeElement) this.processingEnvironment.getTypeUtils().asElement(behaviorType);
        TypeElement objectTypeElement = this.processingEnvironment.getElementUtils().getAllTypeElements("java.lang.Object").stream()
                .toList()
                .getFirst();
        List<ExecutableElement> objectMethods = ElementFilter.methodsIn(this.processingEnvironment.getElementUtils().getAllMembers(objectTypeElement));
        List<ExecutableElement> interfaceMethods = ElementFilter.methodsIn(this.processingEnvironment.getElementUtils().getAllMembers(element));
        return interfaceMethods.stream()
                .filter(ee -> !objectMethods.contains(ee))
                .toList();
        /*
        List<ExecutableElement> rtrnList = new LinkedList<>();
        behaviorType.accept(new SimpleTypeVisitor14<Void, List<ExecutableElement>>() {
            @Override
            public Void visitDeclared(DeclaredType t, List<ExecutableElement> rtrnList) {
                if (t.getKind() == TypeKind.DECLARED) {
                    if (t.asElement().getKind() == ElementKind.INTERFACE) {
                        (TypeElement) t.asElement())
                        t.asElement().accept(new SimpleElementVisitor14<Void, List<ExecutableElement>>() {

                            @Override
                            public Void visitExecutable(ExecutableElement e, List<ExecutableElement> rtrnList) {
                                rtrnList.add(e);
                                return null;
                            }
                        }, rtrnList);
                    }
                }
                return null;
            }
        }, rtrnList);
        return rtrnList; */
    }
 }
