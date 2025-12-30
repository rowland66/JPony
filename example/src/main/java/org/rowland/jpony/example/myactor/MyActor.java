package org.rowland.jpony.example.myactor;

import org.rowland.jpony.*;
import org.rowland.jpony.library.ImmutablePoint;
import org.rowland.jpony.library.StringHolder;
import org.rowland.jpony.library.RefHolder;
import org.rowland.jpony.library.MutablePoint;
import org.rowland.jpony.annotations.JPActor;
import org.rowland.jpony.annotations.Capability;

import static org.rowland.jpony.annotations.CapabilityType.*;

@JPActor(behaviors = MyActorBehaviors.class, factory = MyActorFactory.class)
class MyActor {
    private MyActorFactory myActorFactory;
    private @Capability(Tag) MyActorBehaviors myself;
    private @Capability(Val) Integer counter;
    private @Capability(Iso) MutablePoint myPoint;

    MyActor() {
        try {
            ActorRegistry registry = ActorRegistry.getInstance();
            myActorFactory = registry.getActorFactory(MyActorFactory.class);
        } catch (ActorRegistry.LookupException e) {
            throw new RuntimeException(e);
        }
    }

    void create(@Capability(Ref) MyActor this, @Capability(Val) Integer ref1, @Capability(Iso) StringHolder ref2, @Capability(Val) Boolean ref3) {
        String a = "Hello";
        @Capability(Iso) StringHolder alias1 = JPony.consume(ref2);
        counter = ref1;
        myPoint = JPony.recover(() -> new MutablePoint(ref1, ref1));
    }

    void setMyself(@Capability(Ref) MyActor this, @Capability(Tag) MyActorBehaviors myself) {
        this.myself = myself;
    }

    void calc1(@Capability(Ref) MyActor this, @Capability(Val) StringHolder ref1, @Capability(Iso) StringHolder ref2) {
        @Capability(Iso) MutablePoint temp = JPony.consume(this.myPoint);
        this.myPoint = JPony.recover(() -> new MutablePoint(temp.getX()+1, temp.getY()+1));

        @Capability(Val) Boolean result = internalCalc(ref1, JPony.consume(ref2));
        if (result) {
            updateCounter(5);
        }
        @Capability(Val)ImmutablePoint myPoint = new ImmutablePoint(5, 7);
        @Capability(Val) StringHolder c1 = JPony.recover(() -> new StringHolder("Chuck"));
        this.myself.calc2(c1);
    }

    void calc2(@Capability(Ref) MyActor this, @Capability(Val) StringHolder ref1) {
        @Capability(Iso) RefHolder myHolder = JPony.recover(() -> {
            @Capability(Ref) RefHolder l = new RefHolder();
            @Capability(Ref) StringHolder sh1 = new StringHolder("Rowalnd");
            l.push(sh1);
            return l;
        });
        @Capability(Tag) RefHolder goodAlias = myHolder;
        @Capability(Iso) StringHolder badSh = JPony.recover(() -> {
            @Capability(Ref) RefHolder l = JPony.consume(myHolder);
            return l.pop();
        });
        @Capability(Val) String myName = JPony.recover(() -> {
            @Capability(Ref) StringHolder sh = JPony.consume(badSh);
            return sh.pop();
        });
        @Capability(Ref) StringHolder sh1 = new StringHolder("Laura");
        @Capability(Iso) RefHolder anotherHolder = JPony.recover(() -> {
            @Capability(Ref) RefHolder l = JPony.consume(myHolder);
            @Capability(Ref) StringHolder sh = l.pop();
            sh.push("Laura");
            l.push(sh);
            return l;
        });
    }

    private @Capability(Val) Boolean internalCalc(@Capability(Box) MyActor this, @Capability(Box) StringHolder ref1, @Capability(Box) StringHolder ref2) {
        return Boolean.valueOf(ref1.equals(ref2));
    }

    private void updateCounter(@Capability(Ref) MyActor this, @Capability(Val) Integer newValue) {
        counter = newValue;
    }
}
