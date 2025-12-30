package org.rowland.jpony.library;

import org.rowland.jpony.annotations.Capability;

import static org.rowland.jpony.annotations.CapabilityType.*;

public class StringHolder {
    private String value;

    @Capability(Ref)
    public StringHolder(@Capability(Val) String value) {
        this.value = value;
    }

    public @Capability(Val) String get(@Capability(Box) StringHolder this) {
        return this.value;
    }


    public void push(@Capability(Ref) StringHolder this, @Capability(Val) String value) {
        if(this.value == null) {
            this.value = value;
        }
        throw new IllegalStateException("push to holder that already holds a value");
    }

    public @Capability(Val) String pop(@Capability(Ref) StringHolder this) {
        if (this.value != null) {
            this.value = null;
            return this.value;
        }
        throw new IllegalStateException("pop from holder that does not hold a value");
    }

    @Override
    public boolean equals(@Capability(Box) StringHolder this, @Capability(Box) Object obj) {
        if (obj instanceof StringHolder sh) {
            return value.equals(sh.value);
        }
        return false;
    }
}
