package org.rowland.jpony.library;

import org.rowland.jpony.annotations.Capability;
import org.rowland.jpony.annotations.CapabilityType;

public class ImmutablePoint {
    private int x;
    private int y;

    @Capability(CapabilityType.Val)
    public ImmutablePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX(@Capability(CapabilityType.Box) ImmutablePoint this) {
        return x;
    }

    public int getY(@Capability(CapabilityType.Box) ImmutablePoint this) {
        return y;
    }
}
