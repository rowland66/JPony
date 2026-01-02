package org.rowland.jpony;

import org.rowland.jpony.annotations.CapabilityType;

import java.util.function.Supplier;

public class JPony {

    public static <T> T consume(T value) {
        return value;
    }

    public static <T> T recover(Supplier<T> supplier) {
        return supplier.get();
    }
}
