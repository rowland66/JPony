import org.rowland.jpony.JPony;
import org.rowland.jpony.annotations.Capability;
import org.rowland.jpony.annotations.CapabilityType;
import org.rowland.jpony.annotations.JPClass;

@JPClass
class Inner {}

@JPClass
class Wrap {
    Wrap(@Capability(CapabilityType.Box) Inner s) {
        super();
    }
}

@JPClass
public class CanSee_LetLocalRefAsTag {
    public @Capability(CapabilityType.Iso) Wrap get() {
        final @Capability(CapabilityType.Ref) Inner inner = new Inner();
        return JPony.recover(() -> new Wrap(inner));
    }
}
