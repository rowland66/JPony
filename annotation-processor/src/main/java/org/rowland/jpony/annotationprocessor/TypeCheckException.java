package org.rowland.jpony.annotationprocessor;

public class TypeCheckException extends RuntimeException {
    public TypeCheckException() {
        super();
    }

    public TypeCheckException(String message) {
        super(message);
    }
}
