package org.rowland.jpony.library;

import org.rowland.jpony.annotations.Capability;
import org.rowland.jpony.annotations.ViewpointAdapt;

import java.util.*;

import static org.rowland.jpony.annotations.CapabilityType.*;

public class PonyArrayList<E> implements List<E> {
    private ArrayList<E> innerList;

    @Capability(Ref)
    public PonyArrayList(int initialCapacity) {
        innerList = new ArrayList<>(initialCapacity);
    }

    @Capability(Ref)
    public PonyArrayList() {
        innerList = new ArrayList<>();
    }

    @Capability(Ref)
    public PonyArrayList(@Capability(Box) Collection<? extends E> c) {
        innerList = new ArrayList<>(c);
    }

    @Override
    public int size(@Capability(Box) PonyArrayList<E> this) {
        return innerList.size();
    }

    @Override
    public boolean isEmpty(@Capability(Box) PonyArrayList<E> this) {
        return innerList.isEmpty();
    }

    @Override
    public boolean contains(@Capability(Box) PonyArrayList<E> this, @Capability(value = Box) Object o) {
        return innerList.contains(o);
    }

    @Override
    public @Capability(Val) Iterator<E> iterator(@Capability(Box) PonyArrayList<E> this) {
        return innerList.iterator();
    }

    @Override
    public @Capability(Ref) Object[] toArray(@Capability(Box) PonyArrayList<E> this) {
        return innerList.toArray();
    }

    @Override
    public <T> @Capability(Ref) T[] toArray(@Capability(Box) PonyArrayList<E> this, T[] a) {
        return innerList.toArray(a);
    }

    @Override
    public boolean add(@Capability(Ref) PonyArrayList<E> this, E e) {
        return innerList.add(e);
    }

    @Override
    public boolean remove(@Capability(Ref) PonyArrayList<E> this, @Capability(Box) Object o) {
        return innerList.remove(o);
    }

    @Override
    public boolean containsAll(@Capability(Box) PonyArrayList<E> this, @Capability(Box) Collection<?> c) {
        return innerList.containsAll(c);
    }

    @Override
    public boolean addAll(@Capability(Ref) PonyArrayList<E> this, @Capability(Box) Collection<? extends E> c) {
        return innerList.addAll(c);
    }

    @Override
    public boolean addAll(@Capability(Ref) PonyArrayList<E> this, int index, @Capability(Box) Collection<? extends E> c) {
        return innerList.addAll(index, c);
    }

    @Override
    public boolean removeAll(@Capability(Ref) PonyArrayList<E> this, Collection<?> c) {
        return innerList.removeAll(c);
    }

    @Override
    public boolean retainAll(@Capability(Ref) PonyArrayList<E> this, Collection<?> c) {
        return innerList.retainAll(c);
    }

    @Override
    public void clear(@Capability(Ref) PonyArrayList<E> this) {
        innerList.clear();
    }

    @Override
    public @ViewpointAdapt E get(@Capability(Ref) PonyArrayList<E> this, int index) {
        return innerList.get(index);
    }

    @Override
    public E set(@Capability(Ref) PonyArrayList<E> this, @Capability(Box) int index, E element) {
        return innerList.set(index, element);
    }

    @Override
    public void add(@Capability(Ref) PonyArrayList<E> this, @Capability(Box) int index, E element) {
        innerList.add(index, element);
    }

    @Override
    public @ViewpointAdapt E remove(@Capability(Ref) PonyArrayList<E> this,@Capability(Box) int index) {
        return innerList.remove(index);
    }

    @Override
    public int indexOf(@Capability(Box) PonyArrayList<E> this, @Capability(Box) Object o) {
        return innerList.indexOf(o);
    }

    @Override
    public int lastIndexOf(@Capability(Box) PonyArrayList<E> this, @Capability(Box) Object o) {
        return innerList.lastIndexOf(o);
    }

    @Override
    public ListIterator<E> listIterator(@Capability(Box) PonyArrayList<E> this) {
        return innerList.listIterator();
    }

    @Override
    public ListIterator<E> listIterator(@Capability(Box) PonyArrayList<E> this, int index) {
        return innerList.listIterator(index);
    }

    @Override
    public List<E> subList(@Capability(Box) PonyArrayList<E> this, int fromIndex, int toIndex) {
        return innerList.subList(fromIndex, toIndex);
    }
}
