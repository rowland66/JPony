import org.rowland.jpony.JPony;
import org.rowland.jpony.annotations.Capability;
import org.rowland.jpony.annotations.CapabilityType;
import org.rowland.jpony.annotations.JPClass;

@JPClass
class A {}

@JPClass
class Extract {
    @Capability(CapabilityType.Trn) A a = new A();

    @Capability(CapabilityType.Trn_) A extract_trn(@Capability(CapabilityType.Ref) Extract this) {
        A rtrn = JPony.consume(this.a);
        a = JPony.recover(() -> new A());
        return JPony.consume(rtrn);
    }
}

@JPClass
public class CantReturnTrn_TrnAutoRecovery {
    public CantReturnTrn_TrnAutoRecovery() {
        final @Capability(CapabilityType.Trn) Extract bad = JPony.recover(() -> new Extract());
        final @Capability(CapabilityType.Trn) A a_trn = bad.extract_trn();
    }
}