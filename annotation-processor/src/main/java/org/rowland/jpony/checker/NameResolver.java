package org.rowland.jpony.checker;

import com.sun.source.tree.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

class NameResolver {
    private final ProcessingEnvironment processingEnvironment;
    private final List<String> factoryNames;
    private final Name WILDCARD;
    private final String packagePrefix;
    private final Map<Name, String> packageNameMapping;
    private final List<String> wildcardImports;
    private final Map<Name, String> resolvedNameCache = new HashMap<>();

    NameResolver(
            ProcessingEnvironment processingEnvironment,
            List<Name> factoryNames,
            PackageElement packageElement,
            List<? extends ImportTree> imports) {
        this.processingEnvironment = processingEnvironment;
        this.factoryNames = factoryNames.stream().map(Object::toString).toList();
        this.WILDCARD = processingEnvironment.getElementUtils().getName("*");
        if (packageElement.isUnnamed()) {
            packagePrefix = "";
        } else {
            packagePrefix = packageElement.getQualifiedName().toString()+".";
        }
        this.packageNameMapping = imports.stream()
                .filter(it -> !it.isStatic())
                .filter(it ->
                        !((MemberSelectTree) it.getQualifiedIdentifier()).getIdentifier().equals(WILDCARD))
                .collect(Collectors.toMap(
                        it -> ((MemberSelectTree) it.getQualifiedIdentifier()).getIdentifier(),
                        it ->
                                buildClassName(((MemberSelectTree) it.getQualifiedIdentifier()).getExpression())));
        this.wildcardImports = imports.stream()
                .filter(it -> !it.isStatic())
                .filter(it ->
                        ((MemberSelectTree) it.getQualifiedIdentifier()).getIdentifier().equals(WILDCARD))
                .map(it -> buildClassName(((MemberSelectTree) it.getQualifiedIdentifier()).getExpression()))
                .collect(Collectors.toList());
    }

    String resolve(Tree typeTree) {
        if (typeTree.getKind() == Tree.Kind.IDENTIFIER) {
            return resolve(((IdentifierTree) typeTree).getName());
        } else if (typeTree.getKind() == Tree.Kind.MEMBER_SELECT) {
            return buildClassName((MemberSelectTree) typeTree);
        } else if (typeTree.getKind() == Tree.Kind.PARAMETERIZED_TYPE) {
            return resolve(((ParameterizedTypeTree) typeTree).getType());
        }
        assert(false);
        return null;
    }

    String resolve(Name simpleName) {
        if (packageNameMapping.containsKey(simpleName)) {
            return packageNameMapping.get(simpleName)+"."+simpleName.toString();
        }
        if (resolvedNameCache.containsKey(simpleName)) {
            return resolvedNameCache.get(simpleName);
        }
        if (processingEnvironment.getElementUtils().getTypeElement(packagePrefix+simpleName) != null) {
            String rtrnValue = packagePrefix+simpleName;
            resolvedNameCache.put(simpleName, rtrnValue);
            return rtrnValue;
        }

        if (this.factoryNames.contains(packagePrefix+simpleName)) {
            String rtrnValue = packagePrefix+simpleName;
            resolvedNameCache.put(simpleName, rtrnValue);
            return rtrnValue;
        }

        List<String> candidatePackages = wildcardImports.stream()
                .filter(wildcardPackage ->
                        processingEnvironment
                                .getElementUtils()
                                .getTypeElement(wildcardPackage+"."+simpleName.toString()) != null)
                .collect(Collectors.toList());

        if (candidatePackages.size() > 1) {

        }

        if (candidatePackages.size() == 1) {
            String rtrnValue = candidatePackages.getFirst()+"."+simpleName;
            resolvedNameCache.put(simpleName, rtrnValue);
            return rtrnValue;
        }

        if (processingEnvironment.getElementUtils().getTypeElement("java.lang."+simpleName.toString()) != null) {
            String rtrnValue = "java.lang."+simpleName;
            resolvedNameCache.put(simpleName, rtrnValue);
            return rtrnValue;
        }
        return null;
    }

    boolean isFactory(String factoryName) {
        return this.factoryNames.contains(factoryName);
    }

    private static String buildClassName(ExpressionTree expressionArg) {
        ExpressionTree expression = expressionArg;
        Stack<String> classNameBuilder = new Stack<>();
        while (expression.getKind() == Tree.Kind.MEMBER_SELECT) {
            classNameBuilder.push(((MemberSelectTree) expression).getIdentifier().toString());
            expression = ((MemberSelectTree) expression).getExpression();
        }
        classNameBuilder.push(((IdentifierTree) expression).getName().toString());
        StringBuilder rtrnBuilder = new StringBuilder();
        while (!classNameBuilder.empty()) {
            rtrnBuilder.append('.');
            rtrnBuilder.append(classNameBuilder.pop());
        }
        return rtrnBuilder.substring(1);
    }
}
