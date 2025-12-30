package org.rowland.jpony.example.myactor;

import org.rowland.jpony.Actor;
import org.rowland.jpony.library.StringHolder;
import org.rowland.jpony.annotations.Capability;
import org.rowland.jpony.annotations.CapabilityType;

public interface MyActorBehaviors extends Actor {
    void calc1(@Capability(CapabilityType.Val) StringHolder ref1, @Capability(CapabilityType.Iso) StringHolder ref2);
    void calc2(@Capability(CapabilityType.Val) StringHolder ref1);
    void setMyself(@Capability(CapabilityType.Tag) MyActorBehaviors myself);
}
