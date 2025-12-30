package org.rowland.jpony.annotations;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target(value = {ElementType.TYPE_USE, ElementType.FIELD, ElementType.PARAMETER, ElementType.CONSTRUCTOR})
public @interface Capability {
    CapabilityType value = CapabilityType.Ref;

    CapabilityType value();
}
