package org.rowland.jpony;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class ActorRegistry {
    private static volatile ActorRegistry instance;

    private final Map<Class<?>, Object> factoryMap = new HashMap<>();

    public static ActorRegistry getInstance() {
        if (ActorRegistry.instance != null) {
            return ActorRegistry.instance;
        }
        synchronized (ActorRegistry.class) {
            if (ActorRegistry.instance != null) {
                return ActorRegistry.instance;
            }
            ActorRegistry.instance = new ActorRegistry();
            return instance;
        }
    }

    public <T> T getActorFactory(Class<T> actorFactoryClazz) throws LookupException {
        if (factoryMap.containsKey(actorFactoryClazz)) {
            return (T) factoryMap.get(actorFactoryClazz);
        }
        synchronized (this) {
            if (factoryMap.containsKey(actorFactoryClazz)) {
                return (T) factoryMap.get(actorFactoryClazz);
            }
            try {
                Class<?> factoryImplClass = Class.forName(actorFactoryClazz.getCanonicalName() + "Impl");
                T factoryImpl = (T) factoryImplClass.getDeclaredConstructor(new Class<?>[]{}).newInstance(new Object[]{});
                factoryMap.put(actorFactoryClazz, factoryImpl);
                return factoryImpl;
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new LookupException(e);
            }
        }
    }

    public static class LookupException extends Exception {
        private LookupException(Exception cause) {
            super(cause);
        }
    }
}
