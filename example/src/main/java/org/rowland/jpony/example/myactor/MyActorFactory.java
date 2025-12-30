package org.rowland.jpony.example.myactor;

import org.rowland.jpony.library.StringHolder;
import org.rowland.jpony.annotations.Capability;

import static org.rowland.jpony.annotations.CapabilityType.*;
import static org.rowland.jpony.annotations.CapabilityType.Val;

public interface MyActorFactory {
    @Capability(Tag) MyActorBehaviors create(@Capability(Val) Integer ref1, @Capability(Iso) StringHolder ref2, @Capability(Val) Boolean ref3);
}
