package org.rowland.jpony.checker;

import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;
import java.util.*;
import java.util.stream.Collectors;

class TypeEnvironment {

    private final Map<Name, EnvironmentValue> objectEnv;
    private MethodScope methodEnv;

    static class EnvironmentValue {
        final PonyType ponyType;
        final TypeMirror javaType;

        EnvironmentValue(PonyType ponyType, TypeMirror javaType) {
            this.ponyType = ponyType;
            this.javaType = javaType;
        }

        EnvironmentValue(TypeMirror javaType) {
            this.ponyType = PonyType.ConsumedVariable.INSTANCE;
            this.javaType = javaType;
        }

        PonyType getPonyType() {
            return ponyType;
        }

        TypeMirror getJavaType() {
            return javaType;
        }
    }

    TypeEnvironment() {
        this.objectEnv = new HashMap<>();
        this.methodEnv = new MethodScope(null);
    }

    PonyType getVariableType(Name variable) {
        return Optional.ofNullable(getVariableEnvVal(variable, true))
                .map(ev -> ev.ponyType)
                .orElse(null);
    }

    TypeMirror getVariableJavaType(Name variable) {
        return Optional.ofNullable(getVariableEnvVal(variable, true))
                .map(ev -> ev.javaType)
                .orElse(null);
    }

    PonyType getFieldType(Name variable) {
        return Optional.ofNullable(objectEnv.get(variable))
                .map(e -> e.ponyType)
                .orElse(null);
    }

    TypeMirror getFieldJavaType(Name variable) {
        return Optional.ofNullable(objectEnv.get(variable))
                .map(e -> e.javaType)
                .orElse(null);
    }

    private EnvironmentValue getVariableEnvVal(Name variable, boolean checkInstance) {
        MethodScope currentScope = methodEnv;
        EnvironmentValue rtrn = currentScope.scopeVariables.get(variable);
        if (rtrn != null && currentScope.consumedVariables.contains(variable)) {
            rtrn = new EnvironmentValue(rtrn.javaType);
        }
        while (rtrn == null && currentScope.parent != null) {
            currentScope = currentScope.parent;
            rtrn = currentScope.scopeVariables.get(variable);
            if (currentScope.consumedVariables.contains(variable)) {
                rtrn = new EnvironmentValue(rtrn.javaType);
            }
        }
        if (rtrn == null && checkInstance) {
            return Optional.ofNullable(objectEnv.get(variable))
                    .map(ev ->
                            new EnvironmentValue(
                                    ev.ponyType,
                                    ev.javaType))
                    .orElse(null);
        }
        return rtrn;
    }

    void declareObjectField(Name field, PonyType type, TypeMirror javaType) {
        objectEnv.put(field, new EnvironmentValue(type, javaType));
    }

    void declareMethodVariable(Name variable, PonyType type, TypeMirror javaType) {
        methodEnv.scopeVariables.put(variable, new EnvironmentValue(type, javaType));
    }

    // mark a variable consumed in the scope where it was defined.
    void consumeVariableInScope(Name variable) {
        MethodScope currentScope = methodEnv;
        boolean found = currentScope.scopeVariables.containsKey(variable);
        while (!found && currentScope.parent != null) {
            currentScope = currentScope.parent;
            found = currentScope.scopeVariables.containsKey(variable);
        }
        if (found) {
            currentScope.consumedVariables.add(variable);
        }
    }

    // get a variables type and mark it consumed in the current scope (consumeVariableInScope() is called later)
    EnvironmentValue consumeVariable(Name variable) {
        EnvironmentValue rtrn = Optional.ofNullable(getVariableEnvVal(variable, false))
                .orElse(null);
        if (rtrn != null) {
            methodEnv.consumedVariables.add(variable);
        }
        return rtrn;
    }

    EnvironmentValue consumeField(Name field) {
        EnvironmentValue rtrn = objectEnv.get(field);
        if (rtrn != null) {
        methodEnv.consumedField = field;
        }
        return rtrn;
    }

    boolean isAnyConsumed() {
        MethodScope currentScope = methodEnv;
        boolean rtrn = !currentScope.consumedVariables.isEmpty();
        while (!rtrn && currentScope.parent != null) {
            currentScope = currentScope.parent;
            rtrn = !currentScope.consumedVariables.isEmpty();
        }
        return rtrn;
    }

    Name getFieldConsumed() {
        return methodEnv.consumedField;
    }

    void clearFieldConsumed() {
        methodEnv.consumedField = null;
    }

    private Set<Name> getScopeConsumedVariables() {
        return methodEnv.consumedVariables.stream()
                .filter(nm -> !methodEnv.scopeVariables.containsKey(nm))
                .collect(Collectors.toSet());
    }

    PonyType getInstanceType(Name variable) {
        return Optional.ofNullable(objectEnv.get(variable))
                        .map(ev -> ev.ponyType)
                        .orElse(null);
    }

    void setReturnType(EnvironmentValue rtrnType) {
        methodEnv.returnType = rtrnType;
    }

    PonyType getReturnType() {
        if (methodEnv.returnType != null) {
            return methodEnv.returnType.ponyType;
        }
        return null;
    }

    TypeMirror getReturnJavaType() {
        if (methodEnv.returnType != null) {
            return methodEnv.returnType.javaType;
        }
        return null;
    }

    void pushRecoveryScope() {
        this.methodEnv = new MethodScope(this.methodEnv, true);
    }

    void pushScope() {
        this.methodEnv = new MethodScope(this.methodEnv);
    }

    Set<Name> popScope() {
        assert this.methodEnv.parent != null;
        Set<Name> rtrn = getScopeConsumedVariables();
        this.methodEnv = this.methodEnv.parent;
        return rtrn;
    }

    boolean isVariableDefinedInCurrentScope(Name variable) {
        return methodEnv.scopeVariables.containsKey(variable);
    }

    boolean isInRecovery() {
        return methodEnv.isRecoveryScope;
    }

    private static class MethodScope {
        private final MethodScope parent;
        private final Map<Name, EnvironmentValue> scopeVariables;
        private final Set<Name> consumedVariables;
        private final boolean isRecoveryScope;
        private EnvironmentValue returnType;
        private Name consumedField;

        private MethodScope(MethodScope parent) {
            this(parent, false);
        }

        private MethodScope(MethodScope parent, boolean isRecoveryScope) {
            this.parent = parent;
            this.isRecoveryScope = isRecoveryScope;
            scopeVariables = new HashMap<>();
            consumedVariables = new HashSet<>();
        }
    }
}
