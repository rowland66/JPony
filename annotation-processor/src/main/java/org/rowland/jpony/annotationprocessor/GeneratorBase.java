package org.rowland.jpony.annotationprocessor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;
import java.util.List;

abstract sealed class GeneratorBase permits ActorWrapperGenerator, ActorFactoryGenerator {
    ProcessingEnvironment processingEnvironment;
    TypeElement actorElement;
    DeclaredType factoryInterface;

    GeneratorBase(ProcessingEnvironment processingEnvironment, TypeElement actorElement, DeclaredType factoryInterface) {
        this.processingEnvironment = processingEnvironment;
        this.actorElement = actorElement;
        this.factoryInterface = factoryInterface;
    }

    static String makeParamsClassName(String methodName) {
        return methodName.substring(0,1).toUpperCase()+methodName.substring(1)+"Params";
    }

    protected List<ExecutableElement> getInitMethods() {
        TypeElement element = (TypeElement) this.processingEnvironment.getTypeUtils().asElement(factoryInterface);
        List<ExecutableElement> actorMethods = ElementFilter.methodsIn(this.processingEnvironment.getElementUtils().getAllMembers(element));
        return actorMethods.stream()
                .filter(ee -> !((TypeElement) ee.getEnclosingElement()).getQualifiedName().contentEquals("java.lang.Object"))
                .filter(ee -> !ee.isDefault())
                .toList();
    }


}
