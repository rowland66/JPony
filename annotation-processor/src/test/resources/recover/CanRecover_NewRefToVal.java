import org.rowland.jpony.JPony;
import org.rowland.jpony.annotations.Capability;
import org.rowland.jpony.annotations.CapabilityType;
import org.rowland.jpony.annotations.JPClass;

@JPClass
class Class {
    @Capability(CapabilityType.Ref)
    Class() {
        super();
    }
}

@JPClass
public class CanRecover_NewRefToVal {
    @Capability(CapabilityType.Val) Class get() {
        return JPony.recover(() -> new Class());
    }
}