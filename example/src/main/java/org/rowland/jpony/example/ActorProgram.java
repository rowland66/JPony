package org.rowland.jpony.example;

import org.rowland.jpony.ActorRegistry;
import org.rowland.jpony.library.StringHolder;
import org.rowland.jpony.example.myactor.MyActorBehaviors;
import org.rowland.jpony.example.myactor.MyActorFactory;

public class ActorProgram {

    public static void main(String[] args) {
        try {
            ActorRegistry actorRegistry = ActorRegistry.getInstance();
            MyActorFactory myActorFactory = actorRegistry.getActorFactory(MyActorFactory.class);
            MyActorBehaviors testActor = myActorFactory.create(Integer.valueOf(2), new StringHolder("Hello"), Boolean.valueOf(true));

            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } catch (ActorRegistry.LookupException e) {
            throw new RuntimeException(e);
        }
    }
}
