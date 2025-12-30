package org.rowland.jpony.library;

import org.rowland.jpony.annotations.Capability;
import org.rowland.jpony.annotations.ViewpointAdapt;

import static org.rowland.jpony.annotations.CapabilityType.*;

public class RefHolder {
    private StringHolder stringHolder;

    public @Capability(Ref) RefHolder() {

    }

    public @ViewpointAdapt @Capability(Ref) StringHolder get(@Capability(Box) RefHolder this) {
        if (stringHolder != null) {
            return stringHolder;
        }
        throw new IllegalStateException("get from IsoHolder that does not hold a value");
    }

    public void push(@Capability(Ref) RefHolder this, @Capability(Ref) StringHolder stringHolder) {
        if (stringHolder == null) {
            this.stringHolder = stringHolder;
            return;
        }
        throw new IllegalStateException("push to IsoHolder that already holds a value");
    }

    public @Capability(Ref) StringHolder pop(@Capability(Ref) RefHolder this) {
        if (stringHolder != null) {
            var rtrnVal = stringHolder;
            this.stringHolder = null;
            return rtrnVal;
        }
        throw new IllegalStateException("pop from IsoHolder that does not hold a value");
    }
}
