import org.rowland.jpony.JPony;
import org.rowland.jpony.annotations.Capability;
import org.rowland.jpony.annotations.CapabilityType;
import org.rowland.jpony.annotations.JPClass;

@JPClass
class Class {
    @Capability(CapabilityType.Val)
    Class() {
        super();
    }
}

@JPClass
public class CantRecover_NewValToIso {
    @Capability(CapabilityType.Iso) Class get() {
        return JPony.recover(() -> new Class());
    }
}