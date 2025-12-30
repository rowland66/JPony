package org.rowland.jpony.test;

import org.checkerframework.checker.units.qual.C;
import org.rowland.jpony.annotations.Capability;
import org.rowland.jpony.annotations.CapabilityType;
import org.rowland.jpony.annotations.JPClass;

@JPClass
public class FirstSimple {
    private @Capability(CapabilityType.Val) String foo = "Foo";
    private @Capability(CapabilityType.Ref) String bar = "Bar";

    public @Capability(CapabilityType.Ref) String getFoo(@Capability(CapabilityType.Box) FirstSimple this) {
        return this.foo;
    }
}