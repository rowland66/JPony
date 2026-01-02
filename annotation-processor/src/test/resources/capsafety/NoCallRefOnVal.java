import org.rowland.jpony.JPony;
import org.rowland.jpony.annotations.Capability;
import org.rowland.jpony.annotations.CapabilityType;
import org.rowland.jpony.annotations.JPClass;

@JPClass
class Y {}

@JPClass
class X {
    void mutateme(@Capability(CapabilityType.Ref) X this) {
    }
}

@JPClass
public class NoCallRefOnVal {
    public NoCallRefOnVal() {
        @Capability(CapabilityType.Val) X x = JPony.recover(() -> new X());
        x.mutateme();
    }
}
