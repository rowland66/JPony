package org.rowland.jpony.library;

import org.rowland.jpony.annotations.Capability;

import static org.rowland.jpony.annotations.CapabilityType.Ref;
import static org.rowland.jpony.annotations.CapabilityType.Box;

public class MutablePoint {
    private int x;
    private int y;

    @Capability(Ref)
    public MutablePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX(@Capability(Box) MutablePoint this) {
        return x;
    }

    public int getY(@Capability(Box) MutablePoint this) {
        return y;
    }

    public void setX(@Capability(Ref) MutablePoint this, int x) {
        this.x = x;
    }

    public void setY(@Capability(Ref) MutablePoint this, int y) {
        this.y = y;
    }
}
