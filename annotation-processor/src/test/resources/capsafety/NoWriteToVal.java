import org.checkerframework.checker.units.qual.C;
import org.rowland.jpony.JPony;
import org.rowland.jpony.annotations.Capability;
import org.rowland.jpony.annotations.CapabilityType;
import org.rowland.jpony.annotations.JPClass;

@JPClass
class Y {

}

@JPClass
class X {
    @Capability(CapabilityType.Ref) Y y = new Y();
}

@JPClass
public class NoWriteToVal {
    public NoWriteToVal() {
        @Capability(CapabilityType.Val) X x = JPony.recover(() -> new X());
        x.y = new Y();
    }
}