package org.rowland.jpony.checker;

import org.rowland.jpony.annotations.CapabilityType;

import java.util.Optional;

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
        this(capability, null);
    }

    private PonyType(Capability capability, Modifier modifier) {
        this.capability = capability;
        this.modifier = modifier;
    }

    PonyType asEphemeral() {
        return new PonyType(this.capability, Modifier.Ephemeral);
    }

    PonyType asAlias() {
        if (modifier == Modifier.Ephemeral) {
            return this;
        }
        return new PonyType(this.capability, Modifier.Alias);
    }

    boolean isEphemeral() {
        return modifier == Modifier.Ephemeral;
    }

    boolean isAlias() {
        return modifier == Modifier.Alias;
    }

    private static PonyType applyAliasing(PonyType pt) {
        if (pt.modifier == null) {
            return pt;
        }

        return switch (pt.modifier) {
            case Ephemeral -> switch (pt.capability) {
                case Iso, Trn -> pt;
                default -> new PonyType(pt.capability, null);
            };
            case Alias -> switch (pt.capability) {
                case Iso -> new PonyType(Capability.Tag, null);
                case Trn -> new PonyType(Capability.Box, null);
                default -> new PonyType(pt.capability, null);
            };
        };
    }

    public boolean isSubtypeOf(PonyType superType) {
        PonyType subType = applyAliasing(this);
        superType = applyAliasing(superType);

        if (superType.isEphemeral() && !subType.isEphemeral()) return false;

        if (superType.capability == Capability.Tag) return true;

        if (subType.isEphemeral()) {
            switch (subType.capability) {
                case Iso: return true;
                case Trn: switch (superType.capability) {
                    case Iso: return false;
                    default: return true;
                }
            }
        }

        switch (subType.capability) {
            case Iso: return (superType.capability == Capability.Iso);
            case Trn:
                switch (superType.capability) {
                    case Trn:
                    case Box:
                        return true;
                }
                break;
            case Ref:
                switch (superType.capability) {
                    case Ref:
                    case Box:
                        return true;
                }
                break;
            case Val:
                switch (superType.capability) {
                    case Val:
                    case Box:
                        return true;
                }
                break;
            case Box:
                switch (superType.capability) {
                    case Box:
                        return true;
                }
                break;
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

    public Optional<PonyType> getWriteType(PonyType pt) {
        if (capability == Capability.Tag || capability == Capability.Box || capability == Capability.Val) {
            return Optional.empty();
        }
        if (capability == Capability.Ref) {
            return Optional.of(pt);
        }
        if (pt.capability == Capability.Iso || pt.capability == Capability.Val || pt.capability == Capability.Tag) {
            return Optional.of(pt);
        }
        if (capability == Capability.Trn && pt.capability == Capability.Trn) {
            return Optional.of(pt);
        }
        return Optional.empty();
    }

    public boolean isSendable() {
        if (capability == Capability.Iso || capability == Capability.Val || capability == Capability.Tag) {
            return true;
        }
        return false;
    }

    public boolean isMutatable() {
        if (capability == Capability.Iso || capability == Capability.Trn || capability == Capability.Ref) {
            return true;
        }
        return false;
    }

    public static PonyType getPonyTypeForCapability(CapabilityType capability) {
        return switch (capability) {
            case Iso -> new PonyType(PonyType.Capability.Iso);
            case Trn -> new PonyType(PonyType.Capability.Trn);
            case Ref -> new PonyType(PonyType.Capability.Ref);
            case Val -> new PonyType(PonyType.Capability.Val);
            case Box -> new PonyType(PonyType.Capability.Box);
            case Tag -> new PonyType(PonyType.Capability.Tag);
            case Iso_ -> new PonyType(PonyType.Capability.Iso).asEphemeral();
            case Trn_ -> new PonyType(PonyType.Capability.Trn).asEphemeral();
            case Ref_ -> new PonyType(PonyType.Capability.Ref).asEphemeral();
            case Val_ -> new PonyType(PonyType.Capability.Val).asEphemeral();
            case Box_ -> new PonyType(PonyType.Capability.Box).asEphemeral();
            case Tag_ -> new PonyType(PonyType.Capability.Tag).asEphemeral();
            case Iso__ -> new PonyType(PonyType.Capability.Iso).asAlias();
            case Trn__ -> new PonyType(PonyType.Capability.Trn).asAlias();
            case Ref__ -> new PonyType(PonyType.Capability.Ref).asAlias();
            case Val__ -> new PonyType(PonyType.Capability.Val).asAlias();
            case Box__ -> new PonyType(PonyType.Capability.Box).asAlias();
            case Tag__ -> new PonyType(PonyType.Capability.Tag).asAlias();
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
