import org.rowland.jpony.annotations.Capability;
import org.rowland.jpony.annotations.CapabilityType;
import org.rowland.jpony.annotations.JPClass;

@JPClass
class Y {}

@JPClass
class X {
    @Capability(CapabilityType.Ref) Y y = new Y();
}

@JPClass
public class NoWriteToBox {
    public NoWriteToBox() {
        @Capability(CapabilityType.Box) X x = new X();
        x.y = new Y();
    }
}