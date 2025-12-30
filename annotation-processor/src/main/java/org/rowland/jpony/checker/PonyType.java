package org.rowland.jpony.checker;

import org.rowland.jpony.annotations.CapabilityType;

sealed class PonyType permits PonyType.ConsumedVariable {
    enum Capability {
        Iso,
        Trn,
        Ref,
        Val,
        Box,
        Tag,
    }

    enum Modifier {
        Ephemeral,
        Alias
    }

    final Capability capability;
    final Modifier modifier;

    PonyType(Capability capability) {
        this(capability, false);
    }

    PonyType(Capability capability, boolean isEphemeral) {
        this.capability = capability;
        this.modifier = Modifier.Ephemeral;
    }

    public PonyType alias() {
        if (this.modifier == Modifier.Ephemeral) {
            return this;
        }
        if (this.capability == Capability.Iso) {
            return new PonyType(Capability.Tag);
        }
        if (this.capability == Capability.Trn) {
            return new PonyType(Capability.Box);
        }
        return this;
    }

    public boolean isSubtypeOf(PonyType t) {
        if (t.capability == Capability.Tag) {
                return true;
        }
        if (capability == Capability.Tag) {
            return false;
        }

        if (t.capability == Capability.Box && capability != Capability.Iso) {
            return true;
        }

        if (capability == Capability.Box) {
            return false;
        }

        if (capability == Capability.Val || capability == Capability.Ref) {
            return t.capability.equals(capability);
        }

        if (capability == Capability.Trn && (modifier == Modifier.Ephemeral)) {
            if (t.capability == Capability.Trn || t.capability == Capability.Ref || t.capability == Capability.Val) {
                return true;
            }
            return false;
        }

        if (capability == Capability.Iso && (modifier == Modifier.Ephemeral)) {
            return true;
        }

        return false;
    }

    public PonyType viewpointAdapt(PonyType receiverType) {

        if (receiverType.capability == PonyType.Capability.Iso) {
            return switch (this.capability) {
                case Iso -> this;
                case Trn -> new PonyType(PonyType.Capability.Tag);
                case Ref -> new PonyType(PonyType.Capability.Tag);
                case Val -> this;
                case Box -> new PonyType(PonyType.Capability.Tag);
                case Tag -> this;
            };
        }

        if (receiverType.capability == PonyType.Capability.Trn) {
            return switch (this.capability) {
                case Iso -> this;
                case Trn -> new PonyType(PonyType.Capability.Box);
                case Ref -> new PonyType(PonyType.Capability.Box);
                case Val -> this;
                case Box -> this;
                case Tag -> this;
            };
        }

        if (receiverType.capability == PonyType.Capability.Ref) {
            return this;
        }

        if (receiverType.capability == PonyType.Capability.Val) {
            if (this.capability == PonyType.Capability.Val || this.capability == PonyType.Capability.Tag) {
                return this;
            } else {
                return new PonyType(PonyType.Capability.Val);
            }
        }

        if (receiverType.capability == PonyType.Capability.Box) {
            return switch (this.capability) {
                case Iso -> new PonyType(PonyType.Capability.Tag);
                case Trn -> new PonyType(PonyType.Capability.Box);
                case Ref -> new PonyType(PonyType.Capability.Box);
                case Val -> this;
                case Box -> this;
                case Tag -> this;
            };
        }

        return this; // Tag
    }

    public static PonyType getPonyTypeForCapability(CapabilityType capability) {
        return switch (capability) {
            case Iso -> new PonyType(PonyType.Capability.Iso);
            case Trn -> new PonyType(PonyType.Capability.Trn);
            case Ref -> new PonyType(PonyType.Capability.Ref);
            case Val -> new PonyType(PonyType.Capability.Val);
            case Box -> new PonyType(PonyType.Capability.Box);
            case Tag -> new PonyType(PonyType.Capability.Tag);
            case Iso_ -> new PonyType(PonyType.Capability.Iso,true);
            case Trn_ -> new PonyType(PonyType.Capability.Trn,true);
            case Ref_ -> new PonyType(PonyType.Capability.Ref,true);
            case Val_ -> new PonyType(PonyType.Capability.Val,true);
            case Box_ -> new PonyType(PonyType.Capability.Box,true);
            case Tag_ -> new PonyType(PonyType.Capability.Tag,true);
        };
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PonyType pt) {
            return pt.capability == this.capability && pt.modifier == this.modifier;
        }
        return false;
    }

    @Override
    public String toString() {
        return this.capability.toString()+
                (this.modifier == Modifier.Ephemeral ? "^" : (this.modifier == Modifier.Alias ? "!" : ""));
    }

    final static class ConsumedVariable extends PonyType {
        final static PonyType INSTANCE = new ConsumedVariable();

        private ConsumedVariable() {
            super(null);
        }
    }
}
