package org.rowland.jpony.checker;

import com.sun.source.tree.*;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;
import org.rowland.jpony.JPony;
import org.rowland.jpony.annotationprocessor.TypeCheckException;
import org.rowland.jpony.annotations.Capability;
import org.rowland.jpony.annotations.CapabilityType;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.*;
import java.util.stream.Collectors;

public class TypeChecker {
    private static final List<TypeKind> primitiveTypes = List.of( TypeKind.BOOLEAN
                                                                , TypeKind.INT
                                                                , TypeKind.LONG
                                                                , TypeKind.FLOAT
                                                                , TypeKind.DOUBLE
                                                                );

    private final ProcessingEnvironment processingEnvironment;
    private final List<Name> factoryNames;
    private final Trees trees;
    private final Name thisName;
    private final Name capabilityAnnotationName;
    private final TypeMirror capabilityAnnotationType;
    private final TypeMirror actorType;
    private final TypeMirror jPonyType;
    private final SourcePositions sourcePositions;

    private CompilationUnitTree clazzCompilationUnitTree;
    private NameResolver nameResolver;

    public TypeChecker(ProcessingEnvironment processingEnvironment, List<Name> factoryNames) {
        this.processingEnvironment = processingEnvironment;
        this.factoryNames = factoryNames;
        capabilityAnnotationName = processingEnvironment.getElementUtils().getName("Capability");
        capabilityAnnotationType = processingEnvironment
                .getElementUtils()
                .getTypeElement(Capability.class.getCanonicalName())
                .asType();
        thisName = processingEnvironment.getElementUtils().getName("this");
        trees = Trees.instance(processingEnvironment);
        actorType = processingEnvironment.getElementUtils().getTypeElement(org.rowland.jpony.Actor.class.getCanonicalName()).asType();
        jPonyType = processingEnvironment.getElementUtils().getTypeElement(JPony.class.getCanonicalName()).asType();
        sourcePositions = trees.getSourcePositions();
    }

    public boolean typeCheck(Element clazz) {
        clazzCompilationUnitTree = trees.getPath(clazz).getCompilationUnit();
        nameResolver = new NameResolver(
                processingEnvironment,
                this.factoryNames,
                ((PackageElement) clazz.getEnclosingElement()),
                clazzCompilationUnitTree.getImports());

        for (Element enclosedElement : clazz.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.FIELD) {

            }
        }

        for (Element enclosedElement : clazz.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.METHOD || enclosedElement.getKind() == ElementKind.CONSTRUCTOR) {
                TypeEnvironment typeEnvironment = new TypeEnvironment();
                populateObjectEnvironment(clazz, typeEnvironment);
                typeCheckMethod(typeEnvironment, (ExecutableElement) enclosedElement);
            }
        }

        return true;
    }

    private void printError(String msg, Tree errorTree) {
        long startPos = sourcePositions.getStartPosition(clazzCompilationUnitTree, errorTree);
        long lineNumber = clazzCompilationUnitTree.getLineMap().getLineNumber(startPos);
        long columnNumber = clazzCompilationUnitTree.getLineMap().getColumnNumber(startPos);
        processingEnvironment.getMessager().printError("at ("+lineNumber+","+columnNumber+") "+msg);
    }

    private void populateObjectEnvironment(Element clazz, TypeEnvironment typeEnvironment) {
        for (Element enclosedElement : clazz.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.FIELD) {
                VariableElement variableElement = ((VariableElement) enclosedElement);
                Name variableName = variableElement.getSimpleName();
                PonyType capability = mapCapabilityAnnotationToPonyCapability(enclosedElement.asType());
                typeEnvironment.declareObjectField(variableName, capability, variableElement.asType());
            }
        }
    }

    private void typeCheckMethod(TypeEnvironment typeEnvironment, ExecutableElement method) {

        PonyType.Capability defaultMethodCapability = PonyType.Capability.Box;
        if (method.getKind() == ElementKind.CONSTRUCTOR) {
            defaultMethodCapability = PonyType.Capability.Ref;
        }
        typeEnvironment.declareMethodVariable(
                thisName,
                mapReceiverCapAnnotationToPonyCapability(method, defaultMethodCapability),
                method.getEnclosingElement().asType());

        PonyType methodRtrnType = mapCapabilityAnnotationToPonyCapability(method.getReturnType());
        typeEnvironment.setReturnType(new TypeEnvironment.EnvironmentValue(
                methodRtrnType,
                method.getReturnType()));

        method.getParameters()
                .forEach(ve -> typeEnvironment.declareMethodVariable(
                        ve.getSimpleName(),
                        mapCapabilityAnnotationToPonyCapability(ve.asType()),
                        ve.asType()));

        MethodTree methodTree = trees.getTree(method);

        List<? extends StatementTree> bodyStatementTrees = methodTree.getBody().getStatements();
        try {
            for (StatementTree statementTree : bodyStatementTrees) {
                checkStatement(typeEnvironment, statementTree);
            }
            if (typeEnvironment.getFieldConsumed() != null) {
                printError("consumed field is not assigned a new value", bodyStatementTrees.getLast());
            }
        } catch (TypeCheckException e) {
            // Abort checking statements in the method, error will already have been printed
        }

        if (method.getReturnType().getKind() == TypeKind.VOID) {
            return;
        }
    }

    private TypeEnvironment.EnvironmentValue typeCheckLambda(TypeEnvironment typeEnvironment, LambdaExpressionTree lamda) {
        typeEnvironment.pushRecoveryScope();
        try {
            if (lamda.getBody().getKind() == Tree.Kind.BLOCK) {
                List<? extends StatementTree> bodyStatementTrees = ((BlockTree) lamda.getBody()).getStatements();
                for (StatementTree statementTree : bodyStatementTrees) {
                    checkStatement(typeEnvironment, statementTree);
                }
                if (typeEnvironment.getFieldConsumed() != null) {
                    printError("consumed field is not assigned a new value", bodyStatementTrees.getLast());
                }
            } else {
                ExpressionTree expressionTree = ((ExpressionTree) lamda.getBody());
                TypeEnvironment.EnvironmentValue rtrnType = getExpressionType(typeEnvironment, expressionTree);
                if (typeEnvironment.getFieldConsumed() != null) {
                    printError("consumed field is not assigned a new value", expressionTree);
                }
                typeEnvironment.setReturnType(new TypeEnvironment.EnvironmentValue(
                        new PonyType(rtrnType.getPonyType().capability),
                        rtrnType.getJavaType()));
            }
            PonyType rtrnType = typeEnvironment.getReturnType();
            if (rtrnType == null) {
                printError("recover lambda does not return a value", lamda);
                return null;
            } else {
                TypeMirror rtrnJavaType = typeEnvironment.getReturnJavaType();
                return new TypeEnvironment.EnvironmentValue(
                        rtrnType,
                        rtrnJavaType
                );
            }
        } finally {
            typeEnvironment.popScope();
        }
    }

    private void checkStatement(TypeEnvironment typeEnvironment, StatementTree statementTree) {
        if (typeEnvironment.getFieldConsumed() != null) {
            validateConsumedFieldAssignedValue(typeEnvironment, statementTree);
        }

        if (statementTree.getKind() == Tree.Kind.VARIABLE) {
            Name variable = ((VariableTree) statementTree).getName();
            TypeEnvironment.EnvironmentValue lhsType = getVariableTreeDeclaredType((VariableTree) statementTree);
            PonyType rhsType = getExpressionType(typeEnvironment, ((VariableTree) statementTree).getInitializer()).ponyType;
            rhsType = rhsType.asAlias();
            if (lhsType.getPonyType() == null) {
                lhsType = new TypeEnvironment.EnvironmentValue(
                        new PonyType(rhsType.capability),
                        lhsType.getJavaType());
            }
            evaluateAssignment(lhsType.ponyType, rhsType, ((VariableTree) statementTree).getInitializer());
            typeEnvironment.declareMethodVariable(
                    variable,
                    lhsType.ponyType,
                    lhsType.javaType);
        } else if (statementTree.getKind() == Tree.Kind.EXPRESSION_STATEMENT) {
            ExpressionTree expressionTree = ((ExpressionStatementTree) statementTree).getExpression();
            // Ignore calls to super for now
            if (expressionTree.getKind() == Tree.Kind.METHOD_INVOCATION
                    && ((MethodInvocationTree) expressionTree).getMethodSelect().getKind() == Tree.Kind.IDENTIFIER) {
                if (((IdentifierTree) ((MethodInvocationTree) expressionTree).getMethodSelect()).getName().contentEquals("super")) {
                    return;
                }
            }
            getExpressionType(typeEnvironment, expressionTree);
        } else if (statementTree.getKind() == Tree.Kind.RETURN) {
            ReturnTree returnTree = (ReturnTree) statementTree;
            TypeEnvironment.EnvironmentValue rtrnType =
                    getExpressionType(typeEnvironment, returnTree.getExpression()); //TODO: compare to method return type
            if (typeEnvironment.getReturnType() != null) {
                if (!typeEnvironment.getReturnType().equals(rtrnType.ponyType)) {
                    printError("incorrect or inconsistent return type", returnTree.getExpression());
                    throw new TypeCheckException();
                }
            }
            typeEnvironment.setReturnType(rtrnType);
        } else if (statementTree.getKind() == Tree.Kind.IF) {
            StatementTree thenStatement = ((IfTree) statementTree).getThenStatement();
            Set<Name> thenConsumedParentScopeVariables = Collections.emptySet();
            Set<Name> elseConsumedParentScopeVariables = Collections.emptySet();
            if (thenStatement.getKind() == Tree.Kind.BLOCK) {
                typeEnvironment.pushScope();
                ((BlockTree) thenStatement).getStatements()
                        .forEach(st -> checkStatement(typeEnvironment, st));
                thenConsumedParentScopeVariables = typeEnvironment.popScope();
            } else {
                typeEnvironment.pushScope();
                checkStatement(typeEnvironment, thenStatement);
                thenConsumedParentScopeVariables = typeEnvironment.popScope();
            }
            StatementTree elseStatement = ((IfTree) statementTree).getElseStatement();
            if (elseStatement != null) {
                if (elseStatement.getKind() == Tree.Kind.BLOCK) {
                    typeEnvironment.pushScope();
                    ((BlockTree) elseStatement).getStatements()
                            .forEach(st -> checkStatement(typeEnvironment, st));
                    elseConsumedParentScopeVariables = typeEnvironment.popScope();
                } else {
                    typeEnvironment.pushScope();
                    checkStatement(typeEnvironment, thenStatement);
                    elseConsumedParentScopeVariables = typeEnvironment.popScope();
                }
            }
            thenConsumedParentScopeVariables.forEach(typeEnvironment::consumeVariableInScope);
            elseConsumedParentScopeVariables.forEach(typeEnvironment::consumeVariableInScope);
        }
    }

    private void validateConsumedFieldAssignedValue(TypeEnvironment typeEnvironment, StatementTree statementTree) {
        if (statementTree.getKind() == Tree.Kind.EXPRESSION_STATEMENT &&
                ((ExpressionStatementTree) statementTree).getExpression().getKind() == Tree.Kind.ASSIGNMENT) {
            ExpressionTree variableExpression =
                    ((AssignmentTree) ((ExpressionStatementTree) statementTree).getExpression()).getVariable();
            Name fieldName = null;
            if (variableExpression.getKind() == Tree.Kind.IDENTIFIER) {
                fieldName = ((IdentifierTree) variableExpression).getName();
            } else if (variableExpression.getKind() == Tree.Kind.MEMBER_SELECT) {
                ExpressionTree selectExpression = ((MemberSelectTree) variableExpression).getExpression();
                if (selectExpression.getKind() == Tree.Kind.IDENTIFIER &&
                        ((IdentifierTree) selectExpression).getName().contentEquals("this")) {
                    fieldName = ((MemberSelectTree) variableExpression).getIdentifier();
                }
            }
            if (fieldName == null || !fieldName.equals(typeEnvironment.getFieldConsumed())) {
                printError("consumed field is not assigned a new value", statementTree);
            }
        } else {
            printError("consumed field is not assigned a new value", statementTree);
        }
        typeEnvironment.clearFieldConsumed();
    }

    private PonyType mapCapabilityAnnotationToPonyCapability(TypeMirror annotatedType) {
        return annotatedType.getAnnotationMirrors().stream()
                .filter(at ->
                        processingEnvironment.getTypeUtils().isSameType(at.getAnnotationType(), capabilityAnnotationType))
                .map(this::getCapablityAnnotationsValue)
                .map(PonyType::getPonyTypeForCapability)
                .findAny()
                .orElse(getDefaultPonyType(annotatedType));
    }

    private PonyType getDefaultPonyType(TypeMirror javaType) {
        if (javaType.getKind() == TypeKind.DECLARED) {
            if (processingEnvironment.getTypeUtils().isSameType(
                    javaType,
                    processingEnvironment.getElementUtils().getTypeElement(String.class.getCanonicalName()).asType())) {
                return new PonyType(PonyType.Capability.Val);
            } else if (processingEnvironment.getTypeUtils().isSameType(
                    javaType,
                    processingEnvironment.getElementUtils().getTypeElement(Boolean.class.getCanonicalName()).asType())) {
                return new PonyType(PonyType.Capability.Val);
            }
        } else if (primitiveTypes.contains(javaType.getKind())) {
            return new PonyType(PonyType.Capability.Val);
        }        return new PonyType(PonyType.Capability.Ref);
    }

    private PonyType mapReceiverCapAnnotationToPonyCapability(ExecutableElement method, PonyType.Capability defaultCapability) {
        return ((ExecutableType) method.asType()).getReceiverType().getAnnotationMirrors().stream()
                .filter(at ->
                        processingEnvironment.getTypeUtils().isSameType(at.getAnnotationType(), capabilityAnnotationType))
                .map(this::getCapablityAnnotationsValue)
                .map(PonyType::getPonyTypeForCapability)
                .findAny()
                .orElse(new PonyType(defaultCapability));
    }

    private CapabilityType getCapablityAnnotationsValue(AnnotationMirror annotationMirror) {
        Optional<? extends ExecutableElement> valueElement = annotationMirror.getElementValues().keySet().stream()
                .filter(ee -> ee.getSimpleName().contentEquals("value"))
                .findAny();
        return valueElement
                .map(key -> annotationMirror.getElementValues().get(key))
                .map(av -> ((VariableElement) av.getValue()).getSimpleName())
                .map(n -> CapabilityType.valueOf(n.toString()))
                .orElse(null);
    }

    private TypeEnvironment.EnvironmentValue getVariableTreeDeclaredType(VariableTree variableTree) {
        String className = nameResolver.resolve(variableTree.getType());
        if (className == null) {
            printError("undefined java type: "+((IdentifierTree) variableTree.getType()).getName(), variableTree);
            throw new TypeCheckException();
        }
        TypeMirror javaType = Optional.ofNullable(processingEnvironment.getElementUtils().getTypeElement(className))
                .map(TypeElement::asType)
                .orElse(null);

        var annotationTrees = variableTree.getModifiers().getAnnotations();
        PonyType ponyType = annotationTrees.stream()
                .reduce(
                        null,
                        (cap, at) ->
                            at.getAnnotationType().accept(new SimpleTreeVisitor<>() {
                                @Override
                                public PonyType visitIdentifier(IdentifierTree node, Object o) {
                                    if (capabilityAnnotationName.equals(node.getName())) {
                                        if (at.getArguments().getFirst().getKind() == Tree.Kind.IDENTIFIER) {
                                            Name n = ((IdentifierTree) at.getArguments().getFirst()).getName();
                                            return new PonyType(PonyType.Capability.valueOf(n.toString()));
                                        } else if (at.getArguments().getFirst().getKind() == Tree.Kind.MEMBER_SELECT) {
                                            MemberSelectTree mst = ((MemberSelectTree) at.getArguments().getFirst());
                                            if (mst.getExpression().getKind() == Tree.Kind.IDENTIFIER
                                                && ((IdentifierTree) mst.getExpression()).getName().contentEquals(CapabilityType.class.getSimpleName())) {
                                                Name n = mst.getIdentifier();
                                                return new PonyType(PonyType.Capability.valueOf(n.toString()));
                                            }
                                        }
                                    }
                                    return cap;
                                }
                            }, null),
                        (t1, t2) -> t1);
        return new TypeEnvironment.EnvironmentValue(ponyType, javaType);
    }

    private TypeEnvironment.EnvironmentValue getExpressionType(
            TypeEnvironment env,
            ExpressionTree expressionTree) {
        return switch (expressionTree) {
            case LiteralTree lt -> new TypeEnvironment.EnvironmentValue(
                    new PonyType(PonyType.Capability.Val).asEphemeral(),
                    getLiteralType(lt.getValue()));
            case MethodInvocationTree mit -> getMethodInvocationType(env, mit);
            case IdentifierTree it -> getIdentifierType(env, it);
            case ConditionalExpressionTree cet -> getConditionalType(env, cet);
            case NewClassTree nct -> getConstructorCreateType(nct.getIdentifier(), nct.getArguments());
            case AssignmentTree at -> getAssignmentType(env, at.getVariable(), at.getExpression());
            case MemberSelectTree mst -> getMemberType(env, mst);
            default -> {
                printError("unrecognized expression type", expressionTree);
                throw new TypeCheckException();
            }
        };
    }

    private Optional<TypeEnvironment.EnvironmentValue> getExpressionWriteType(
            TypeEnvironment env,
            ExpressionTree expressionTree) {
        return switch (expressionTree) {
            case IdentifierTree it -> getIdentifierWriteType(env, it);
            case MemberSelectTree mst -> getMemberWriteType(env, mst);
            default -> {
                printError("unrecognized expression type", expressionTree);
                throw new TypeCheckException();
            }
        };
    }

    private TypeEnvironment.EnvironmentValue getAssignmentType(
            TypeEnvironment typeEnvironment,
            ExpressionTree variable,
            ExpressionTree expression) {

        return getExpressionWriteType(typeEnvironment, variable)
                .map(lhsType -> {
                    TypeEnvironment.EnvironmentValue rhsType = getExpressionType(typeEnvironment, expression);
                    evaluateAssignment(lhsType.ponyType, rhsType.ponyType, expression);
                    return lhsType;
                }).orElseThrow(() -> {
                    printError("left hand side variable is not writable", variable);
                    return new TypeCheckException();
                });
    }

    private void evaluateAssignment(PonyType lhsType, PonyType rhsType, Tree rhsTree) {
        if (rhsType instanceof PonyType.ConsumedVariable) {
            printError("Illegal use of consumed type", rhsTree);
        } else {
            if (lhsType != null && !rhsType.isSubtypeOf(lhsType)) {
                printError("right side must be a subtype of left side", rhsTree);
            }
        }
    }

    private TypeEnvironment.EnvironmentValue getIdentifierType(TypeEnvironment typeEnvironment, IdentifierTree it) {
        Name variableName = it.getName();
        PonyType variableType = typeEnvironment.getVariableType(variableName);
        if (variableType != null) {
            PonyType receiverType = typeEnvironment.getVariableType(thisName);
            if (typeEnvironment.isInRecovery()) {
                 if (!variableType.isSendable()){
                    if (!typeEnvironment.isVariableDefinedInCurrentScope(variableName)) {
                        printError("illegal access of non sendable variable from recover block", it);
                    }
                }
            }
            return new TypeEnvironment.EnvironmentValue(
                    variableType.viewpointAdapt(receiverType),
                    typeEnvironment.getVariableJavaType(it.getName()));
        }
        String classname = nameResolver.resolve(it.getName());
        if (classname != null) {
            if (nameResolver.isFactory(classname)) {
                return new TypeEnvironment.EnvironmentValue(
                        new PonyType(PonyType.Capability.Tag),
                        null);
            }
            return new TypeEnvironment.EnvironmentValue(
                    null,
                    processingEnvironment.getElementUtils().getTypeElement(classname).asType());
        }
        printError("unable to resolve identifier", it);
        throw new TypeCheckException();
    }

    private Optional<TypeEnvironment.EnvironmentValue> getIdentifierWriteType(TypeEnvironment typeEnvironment, IdentifierTree it) {
        Name variableName = it.getName();
        PonyType variableType = typeEnvironment.getVariableType(variableName);
        if (variableType != null) {
            PonyType receiverType = typeEnvironment.getVariableType(thisName);
            TypeMirror receiverJavaType = typeEnvironment.getVariableJavaType(variableName);
            return receiverType.getWriteType(variableType)
                    .map(pt -> new TypeEnvironment.EnvironmentValue(pt, receiverJavaType));
        }
        printError("unable to resolve identifier", it);
        throw new TypeCheckException();
    }

    private TypeEnvironment.EnvironmentValue getMemberType(TypeEnvironment env, MemberSelectTree memberSelectTree) {
        TypeEnvironment.EnvironmentValue receiver = getExpressionType(env, memberSelectTree.getExpression());

        TypeElement receiverClassElement = (TypeElement) processingEnvironment.getTypeUtils().asElement(receiver.getJavaType());
        Optional<VariableElement> maybeMember =  processingEnvironment.getElementUtils().getAllMembers(receiverClassElement).stream()
                .filter(e -> e.getKind() == ElementKind.FIELD)
                .filter(e -> !e.getModifiers().contains(Modifier.STATIC))
                .filter(e -> e.getSimpleName().equals(memberSelectTree.getIdentifier()))
                .map(e -> (VariableElement) e)
                .findFirst();

        if (maybeMember.isEmpty()) {
            printError("failed to identify field", memberSelectTree);
            throw new TypeCheckException();
        }

        PonyType ponyType =
                mapCapabilityAnnotationToPonyCapability(maybeMember.get().asType()).viewpointAdapt(receiver.getPonyType());

        Optional<TypeEnvironment.EnvironmentValue> maybeResult = maybeMember
                .map(ve -> new TypeEnvironment.EnvironmentValue(ponyType, ve.asType()));

        return maybeResult.get();
    }

    private Optional<TypeEnvironment.EnvironmentValue> getMemberWriteType(TypeEnvironment env, MemberSelectTree memberSelectTree) {
        return getExpressionWriteType(env, memberSelectTree.getExpression())
                .flatMap(receiver -> {
                    TypeElement receiverClassElement = (TypeElement) processingEnvironment.getTypeUtils().asElement(receiver.getJavaType());
                    Optional<VariableElement> maybeMember = processingEnvironment.getElementUtils().getAllMembers(receiverClassElement).stream()
                            .filter(e -> e.getKind() == ElementKind.FIELD)
                            .filter(e -> !e.getModifiers().contains(Modifier.STATIC))
                            .filter(e -> e.getSimpleName().equals(memberSelectTree.getIdentifier()))
                            .map(e -> (VariableElement) e)
                            .findFirst();

                    if (maybeMember.isEmpty()) {
                        printError("failed to identify field", memberSelectTree);
                        throw new TypeCheckException();
                    }

                    return maybeMember.flatMap(member ->
                            receiver.getPonyType().getWriteType(mapCapabilityAnnotationToPonyCapability(member.asType()))
                                            .map(pt -> new TypeEnvironment.EnvironmentValue(pt, member.asType())));

                });
    }

    private TypeEnvironment.EnvironmentValue getMethodInvocationType(TypeEnvironment env, MethodInvocationTree methodInvocationTree) {
        Optional<TypeEnvironment.EnvironmentValue> maybeResult = switch (methodInvocationTree.getMethodSelect()) {
            case IdentifierTree it -> getMethodReturnType(
                    env,
                    methodInvocationTree,
                    env.getVariableType(thisName).asAlias(),
                    (DeclaredType) env.getVariableJavaType(thisName),
                    it.getName(),
                    methodInvocationTree.getArguments());
            case MemberSelectTree mst ->
                getMethodReturnType(
                        env,
                        methodInvocationTree,
                        Optional.ofNullable(getExpressionType(env, mst.getExpression()).ponyType)
                                .map(PonyType::asAlias)
                                .orElse(null),
                        (DeclaredType) getExpressionType(env, mst.getExpression()).javaType,
                        mst.getIdentifier(),
                        methodInvocationTree.getArguments());
            default -> Optional.empty();
        };
        if (maybeResult.isEmpty()) {
            printError("failed to identify method", methodInvocationTree);
            throw new TypeCheckException();
        }
        return maybeResult.get();
    }

    private TypeEnvironment.EnvironmentValue getConstructorCreateType(ExpressionTree expression, List<? extends ExpressionTree> args) {
        String className;
        if (expression.getKind() == Tree.Kind.IDENTIFIER) {
            className = nameResolver.resolve(((IdentifierTree) expression).getName());
        } else if (expression.getKind() == Tree.Kind.MEMBER_SELECT) {
            className = nameResolver.resolve(expression);
        } else if (expression.getKind() == Tree.Kind.PARAMETERIZED_TYPE) {
            Tree typeTree = ((ParameterizedTypeTree) expression).getType();
            className = nameResolver.resolve(typeTree);
        } else {
            printError("unrecognized instance creation expression", expression);
            throw new TypeCheckException();
        }

        TypeElement targetType = processingEnvironment.getElementUtils().getTypeElement(className);
        if (targetType == null) {
            printError("undefined class in instance creation expression", expression);
            throw new TypeCheckException();
        }
        Optional<ExecutableElement> maybeMethodElement = processingEnvironment.getElementUtils().getAllMembers(targetType).stream()
                .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
                .map(e -> (ExecutableElement) e)
                .filter(e -> e.getParameters().size() == args.size())
                .findAny();

        if (maybeMethodElement.isEmpty()) {
            printError("undefined constructor with necessary agruments", expression);
            throw new TypeCheckException();
        }

        return maybeMethodElement
                .flatMap(ee -> Optional.ofNullable(ee.getAnnotation(Capability.class)))
                .map(Capability::value)
                .map(PonyType::getPonyTypeForCapability)
                .map(pt -> new PonyType(pt.capability).asEphemeral())
                .map(pt -> new TypeEnvironment.EnvironmentValue(pt, targetType.asType()))
                .orElseGet(() -> new TypeEnvironment.EnvironmentValue(
                        new PonyType(PonyType.Capability.Ref).asEphemeral(),
                        targetType.asType()));
    }

    private Optional<ExecutableElement> getMethodExecutableElement(
            TypeEnvironment typeEnvironment,
            MethodInvocationTree methodInvocationTree,
            PonyType receiverType,
            DeclaredType receiverJavaType,
            Name methodName,
            List<? extends ExpressionTree> args) {
        TypeElement receiverClassElement = (TypeElement) processingEnvironment.getTypeUtils().asElement(receiverJavaType);
        Optional<ExecutableElement> maybeMethod =  processingEnvironment.getElementUtils().getAllMembers(receiverClassElement).stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .filter(e -> !e.getModifiers().contains(Modifier.STATIC))
                .filter(e -> e.getSimpleName().equals(methodName))
                .filter(e -> ((ExecutableElement) e).getParameters().size() == args.size())
                .map(e -> (ExecutableElement) e)
                .findFirst();

        if (maybeMethod.isEmpty()) {
            return maybeMethod;
        }

        ExecutableElement method = maybeMethod.get();

        PonyType targetType = (
                processingEnvironment.getTypeUtils().isSubtype(receiverJavaType, actorType) ?
                        new PonyType(PonyType.Capability.Tag) :
                        mapReceiverCapAnnotationToPonyCapability(method, PonyType.Capability.Box));

        if (!receiverType.isSubtypeOf(targetType)) {
            printError("receiver is not a subtype of target type", methodInvocationTree);
            throw new TypeCheckException();
        }

        List<PonyType> argTypes = args.stream()
                .map(et -> getExpressionType(typeEnvironment, et))
                .map(TypeEnvironment.EnvironmentValue::getPonyType)
                .map(PonyType::asAlias)
                .toList();

        Optional<List<PonyType>> maybeParamTypes = maybeMethod
                .map(ExecutableElement::getParameters)
                .map(paramList -> paramList.stream()
                        .map(VariableElement::asType)
                        .map(this::mapCapabilityAnnotationToPonyCapability)
                        .collect(Collectors.toList()));

        Iterator<PonyType> argTypesIterator = argTypes.iterator();
        Iterator<PonyType> paramTypesIterator = maybeParamTypes.get().iterator();
        Iterator<? extends ExpressionTree> argsIterator = args.iterator();
        boolean error = false;
        while (argTypesIterator.hasNext()) {
            if (!argTypesIterator.next().isSubtypeOf(paramTypesIterator.next())) {
                printError("argument is not a subtype of parameter", argsIterator.next());
                error = true;
            } else {
                argsIterator.next();
            }
        }

        if (error) {
            throw new TypeCheckException();
        }
        return maybeMethod;
    }

    private Optional<ExecutableElement> getStaticMethodExecutableElement(
            TypeEnvironment typeEnvironment,
            DeclaredType receiverJavaType,
            Name methodName,
            List<? extends ExpressionTree> args) {
        TypeElement receiverClassElement = (TypeElement) processingEnvironment.getTypeUtils().asElement(receiverJavaType);
        Optional<ExecutableElement> maybeMethod =  processingEnvironment.getElementUtils().getAllMembers(receiverClassElement).stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .filter(e -> e.getModifiers().contains(Modifier.STATIC))
                .filter(e -> e.getSimpleName().equals(methodName))
                .filter(e -> ((ExecutableElement) e).getParameters().size() == args.size())
                .map(e -> (ExecutableElement) e)
                .findFirst();

        if (maybeMethod.isEmpty()) {
            return maybeMethod;
        }

        ExecutableElement method = maybeMethod.get();

        List<PonyType> argTypes = args.stream()
                .map(et -> getExpressionType(typeEnvironment, et))
                .map(TypeEnvironment.EnvironmentValue::getPonyType)
                .map(PonyType::asAlias)
                .toList();

        Optional<List<PonyType>> maybeParamTypes = maybeMethod
                .map(ExecutableElement::getParameters)
                .map(paramList -> paramList.stream()
                        .map(VariableElement::asType)
                        .map(this::mapCapabilityAnnotationToPonyCapability)
                        .collect(Collectors.toList()));

        Iterator<PonyType> argTypesIterator = argTypes.iterator();
        Iterator<PonyType> paramTypesIterator = maybeParamTypes.get().iterator();
        Iterator<? extends ExpressionTree> argsIterator = args.iterator();
        boolean error = false;
        while (argTypesIterator.hasNext()) {
            if (!argTypesIterator.next().isSubtypeOf(paramTypesIterator.next())) {
                printError("argument is not a subtype of parameter", argsIterator.next());
                error = true;
            } else {
                argsIterator.next();
            }
        }

        if (error) {
            throw new TypeCheckException();
        }
        return maybeMethod;
    }

    private Optional<TypeEnvironment.EnvironmentValue> getMethodReturnType(
            TypeEnvironment env,
            MethodInvocationTree methodInvocationTree,
            PonyType receiverType,
            DeclaredType receiverJavaType,
            Name methodName, List<? extends ExpressionTree> args) {

        /* if (receiverJavaType == null) { // Need to fix this hack for identifying factory classes that don't exist get.
            return Optional.of(new TypeEnvironment.EnvironmentValue(
                    new PonyType(PonyType.Capability.Tag),
                    null));
        } else*/ if (processingEnvironment.getTypeUtils().isSameType(receiverJavaType, jPonyType)) {
            if (methodName.contentEquals("consume")) {
                if (args.size() == 1) {
                    if (args.getFirst().getKind() == Tree.Kind.IDENTIFIER) {
                        TypeEnvironment.EnvironmentValue consumeType =
                                env.consumeVariable(((IdentifierTree) args.getFirst()).getName());
                        if (consumeType != null) {
                            return Optional.of(new TypeEnvironment.EnvironmentValue(
                                    new PonyType(consumeType.ponyType.capability).asEphemeral(),
                                    consumeType.javaType));
                        } else {
                            printError("unknown variable", args.getFirst());
                        }
                    } else if (args.getFirst().getKind() == Tree.Kind.MEMBER_SELECT) {
                        ExpressionTree selectExpression = ((MemberSelectTree) args.getFirst()).getExpression();
                        if (selectExpression.getKind() == Tree.Kind.IDENTIFIER &&
                                ((IdentifierTree) selectExpression).getName().contentEquals("this")) {
                            TypeEnvironment.EnvironmentValue consumeType =
                                    env.consumeField(((MemberSelectTree) args.getFirst()).getIdentifier());
                            if (consumeType != null) {
                                return Optional.of(new TypeEnvironment.EnvironmentValue(
                                        new PonyType(consumeType.ponyType.capability).asEphemeral(),
                                        consumeType.javaType));
                            } else {
                                printError("unknown field", args.getFirst());
                            }
                        } else {
                            printError("JPony.comsume() can only be call on a local variable, parameter or instance variable", methodInvocationTree);
                        }
                    } else {
                        printError("JPony.comsume() can only be call on a local variable, parameter or instance variable", methodInvocationTree);
                    }
                } else {
                    printError("JPony.comsume() called with too many arguments", methodInvocationTree);
                }
            } else if (methodName.contentEquals("recover")) {
                if (args.size() == 1) {
                    if (args.getFirst().getKind() == Tree.Kind.LAMBDA_EXPRESSION) {
                        TypeEnvironment.EnvironmentValue recoverType = typeCheckLambda(env, ((LambdaExpressionTree) args.getFirst()));
                        if (recoverType.ponyType.isMutatable()) {
                            return Optional.of(new TypeEnvironment.EnvironmentValue(
                                    new PonyType(PonyType.Capability.Iso).asEphemeral(),
                                    recoverType.javaType));
                        } else {
                            return Optional.of(new TypeEnvironment.EnvironmentValue(
                                    new PonyType(PonyType.Capability.Val).asEphemeral(),
                                    recoverType.javaType));
                        }
                    }
                }
            }
        } else if (receiverType == null) { // null receiver type means static method
            return getStaticMethodExecutableElement(env, receiverJavaType, methodName, args)
                    .map(ExecutableElement::getReturnType)
                    .map(tm ->
                            new TypeEnvironment.EnvironmentValue(mapCapabilityAnnotationToPonyCapability(tm), tm));
        }

        return getMethodExecutableElement(env, methodInvocationTree, receiverType, receiverJavaType, methodName, args)
                .map(ExecutableElement::getReturnType)
                .map(tm -> new TypeEnvironment.EnvironmentValue(mapCapabilityAnnotationToPonyCapability(tm), tm));
    }

    private TypeEnvironment.EnvironmentValue getConditionalType(TypeEnvironment typeEnvironment, ConditionalExpressionTree expressionTree) {
        Set<Name> trueConsumedParentScopeVariables, falseConsumedParentScopeVariables;
        typeEnvironment.pushScope();
        TypeEnvironment.EnvironmentValue trueType = getExpressionType(typeEnvironment, expressionTree.getTrueExpression());
        trueConsumedParentScopeVariables = typeEnvironment.popScope();
        typeEnvironment.pushScope();
        TypeEnvironment.EnvironmentValue falseType = getExpressionType(typeEnvironment, expressionTree.getFalseExpression());
        falseConsumedParentScopeVariables = typeEnvironment.popScope();
        trueConsumedParentScopeVariables.forEach(typeEnvironment::consumeVariableInScope);
        falseConsumedParentScopeVariables.forEach(typeEnvironment::consumeVariableInScope);
        if (!trueType.equals(falseType)) {
            printError("conditional branches return different types", expressionTree);
        }
        return trueType;
    }

    private TypeMirror getLiteralType(Object o) {
        if (o == null) {
            return processingEnvironment.getTypeUtils().getNullType();
        }
        return processingEnvironment.getElementUtils().getTypeElement(o.getClass().getCanonicalName()).asType();
    }
}
