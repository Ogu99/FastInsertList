import java.io.Serializable;
import java.util.AbstractSequentialList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * This class implements a doubly-linked ArrayList like structure. This means, every
 * element inside the list is no longer represented by a single node. Every single node
 * contains an internal array holding each individual value of the list.
 * So the absolute amount of nodes will be significantly lower as in a {@link LinkedList}.
 * <p>
 * The advantage of this approach is, that contrary to an {@link ArrayList} which needs to resize
 * the internal data array, this process falls off in this implementation, thus runtime is saved.
 * Instead, when the data array of a node should be full, a new node with an empty array is initialized
 * and this new node will be linked to the current structure like in a {@code LinkedList}.
 * Each array will also grow in size dependent on the current size of the list. On top of that, every node
 * represents a specific index range inside the list, to preserve the indeces for each individual
 * element. The implementation is:
 * <pre>
 *      Node(int runtimeSize, int startIndex) {
 *          this.elements = (E[]) new Object[(runtimeSize >>> 1) + 10];
 *          this.startIndex = startIndex;
 *          this.endIndex = startIndex + elements.length - 1;
 *      }
 * </pre>
 * <p>
 * Also to be mentioned is the fact, that accessing and removing data from this list is faster 
 * than accessing or removing data from a {@code LinkedList} or {@code ArrayList}.
 * The best case time complexity for the opertions <i>add, remove, get</i> for this list are:
 * <ul>
 *     <li><i>add</i> - O(1)
 *     <li><i>get</i> - O(1)
 *     <li><i>remove</i> - O(1)
 * </ul>
 * 
 * and the worst cases are (n = amount of nodes, a = length of the array inside the node):
 * <ul>
 *     <li><i>add</i> - O(n*a)
 *     <li><i>get</i> - O(n)
 *     <li><i>remove</i> - O(n*a)
 * </ul>
 * 
 * @author Ogu99
 * @version 1.0
 * @param <E> - The type of the elements inside this list.
 */
public class FastInsertList<E> extends AbstractSequentialList<E> implements List<E>, Deque<E>, Cloneable, Serializable {
    private static final long serialVersionUID = 2799870846144770308L;
    private transient Node<E> head; //The head (first element) of the list.
    private transient Node<E> tail; //The tail (last element) of the list.
    private transient int size;     //The current size of the list.

    /**
     * Creates a new and empty list.
     */
    public FastInsertList() {
        head = tail = new Node<>(0, 0);
        size = 0;
    }

    private void requireNotEmpty() {
        if (isEmpty()) {
            throw new IllegalStateException("this deque is currently empty!");
        }
    }
    
    /**
     * Returns the node at the index i.
     * 
     * @param i - The index of the node.
     * @return the node.
     */
    private Node<E> getNode(int i) {
        //If the element is in the first half of this list start at the head.
        if (i <= (size >>> 1)) {
            for (Node<E> x = head; x != null; x = x.next) {
                if (i >= x.startIndex && i <= x.endIndex)
                    return x;
            }
        } else {
            ///Otherwise we start at the end and move backwards to the head.
            for (Node<E> x = tail; x != null; x = x.prev) {
                if (i >= x.startIndex && i <= x.endIndex)
                    return x;
            }
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean add(E e) {
    	if (e == null)
    	    return false;
    	
        Node<E> last = tail;
        if (last.canInsert()) {
            last.add(e);
        } else {
            Node<E> insert = new Node<>(size, last.endIndex + 1);
            insert.prev = last;
            insert.add(e);

            tail = insert;
            last.next = insert;
        }

        modCount++;
        size++;
        return true;
    }

    @Override
    public void add(int i, E e) {
        if (e == null)
            throw new NullPointerException();
        if (i <  0 || i > size)
            throw new IndexOutOfBoundsException("the index " + i + " is out of bounds!");

        if (i == size) {
            add(e);
        } else {
            Node<E> target = getNode(i);

            @SuppressWarnings("unchecked")
			E[] newArr = (E[]) new Object[target.elements.length + 1];
            System.arraycopy(target.elements, 0, newArr, 0, i - target.startIndex);

            newArr[i - target.startIndex] = e;
            System.arraycopy(target.elements, i - target.startIndex, newArr, i - target.startIndex + 1, newArr.length - (i - target.startIndex) - 1);

            target.elements = newArr;
            target.pointer++;
            modCount++;
            size++;
        }
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        if (c == null)
            return false;

        for (E e : c)
            add(e);

        return true;
    }

    @Override
    public E set(int i, E e) {
        if (i < 0 || i >= size() || e == null)
            return null;

        Node<E> target = getNode(i);
        E old = target.elements[i - target.startIndex];
        target.elements[i - target.startIndex] = e;

        return old;
    }

    @Override
    public E get(int i) {
        if (i >= size || i < 0)
            return null;
        /*
         * Equally fast to this one would be:
         * return toArray()[i];
         */
        Node<E> get = getNode(i);
        return get.elements[i - get.startIndex];
    }

    @Override
    public int indexOf(Object o) {
        if (o == null)
            return -1;

        if (o.equals(head.elements[0])) {
            return 0;
        }

        int i = 0;
        for (Node<E> x = head; x != null; x = x.next) {
            for (int j = 0; j < x.elements.length; j++) {
                if (o.equals(x.elements[j])) {
                    return i;
                }
                i++;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        if (o == null)
            return -1;

        int i = size - 1;
        for (Node<E> x = tail; x != null; x = x.prev) {
            for (E element : x.elements) {
                if (o.equals(element))
                    return i;
                i--;
            }
        }
        return -1;
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    @Override
    public E remove(int i) {
        if (i < 0 || i >= size)
            return null;

        if (size == 1) {
            head = tail = new Node<>(0, 0);
            return head.elements[0];
        }

        Node<E> target = getNode(i);
        E remove = target.elements[i - target.startIndex];

        @SuppressWarnings("unchecked")
        E[] newArr = (E[]) new Object[target.elements.length - 1];
        System.arraycopy(target.elements, 0, newArr, 0, i - target.startIndex);
        System.arraycopy(target.elements, i - target.startIndex + 1, newArr, i - target.startIndex,
                newArr.length - (i - target.startIndex));

        //This whole node is empty, so remove it.
        if (newArr[0] == null) {
            target.prev.next = target.next;
        } else {
            target.elements = newArr;
            target.endIndex--;
        }

        for (Node<E> x = target.next; x != null; x = x.next) {
            x.startIndex--;
            x.endIndex--;
        }

        size--;
        modCount++;
        return remove;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (c == null)
            return false;

        if (c.isEmpty()) {
            return false;
        }

        boolean modified = false;
        for (Object e : c) {
            modified |= remove(e);
        }

        return modified;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if (c == null)
            return false;
    
        if (c.isEmpty()) {
            return false;
        }

        boolean modified = false;
        for (Object o : this) {
            if (!c.contains(o))
                modified |= remove(o);
        }

        return modified;
    }

    @Override
    public boolean remove(Object o) {
        int index = indexOf(o);
        return index != -1 ? remove(index) != null : false;
    }

    @Override
    public void clear() {
        head = tail = new Node<>(0, 0);
        size = 0;
        modCount++;
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return super.subList(fromIndex, toIndex);
    }

    @Override
    public Object[] toArray() {
        Object[] array = new Object[size];
        int index = 0;
        for (Node<E> x = head; x != null; x = x.next) {
            for (int i = 0; i < x.elements.length; i++) {
                if (x.elements[i] != null) {
                    array[index] = x.elements[i];
                    index++;
                }
            }
        }

        return array;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] a) {
        return (T[]) Arrays.copyOf(toArray(), size, a.getClass());
    }


    /**
     * Inserts the given element at the front of the list. So this method is equal
     * to {@link #add(int, Object)} with {@code add(0, e)}.
     * 
     */
    @Override
    public void addFirst(E e) {
        add(0, e);
    }

    /**
     * Insert the given element at the end of the list. So this method is equal
     * to {@link #add(Object)}.
     */
    @Override
    public void addLast(E e) {
        add(e);
    }

    /**
     * Inserts the given element at the front of the list. This method is equal
     * to {@link #addFirst(Object)} with the difference that {@code true} is returned
     * when the element was successfully added to the list (which is always the case
     * except for {@code null}).
     */
    @Override
    public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }

    /**
     * Inserts the given element at the end of the list. This method is equal
     * to {@link #addLast(Object)} with the difference that {@code true} is returned
     * when the element was successfully added to the list (which is always the case
     * except for {@code null}).
     */
    @Override
    public boolean offerLast(E e) {
        addLast(e);
        return true;
    }

    @Override
    public E removeFirst() {
    	requireNotEmpty();
    
        E first = getFirst();
        remove(first);
        return first;
    }

    @Override
    public E removeLast() {
    	requireNotEmpty();
    	
        E last = getLast();
        remove(last);
        return last;
    }

    @Override
    public E pollFirst() {
        if (isEmpty())
            return null;
       
        return removeFirst();
    }

    @Override
    public E pollLast() {
        if (isEmpty())
            return null;
       
        return removeFirst();
    }

    @Override
    public E getFirst() {
        return head.elements[0];
    }

    @Override
    public E getLast() {
        requireNotEmpty();
        
        E[] elements = tail.elements;
        E current = null;
        for (E e : elements) {
            if (e == null)
            	break;
            current = e;
        }
        
        return current;
    }

    @Override
    public E peekFirst() { 
        if (isEmpty())
            return null;
	
        return getFirst();
    }

    @Override
    public E peekLast() {
        if (isEmpty())
            return null;
        
        return getLast();
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        return remove(o);
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        return remove(lastIndexOf(o)) != null;
    }

    /**
     * Inserts the given object at the end of this list. So this method is equal to
     * {@link #add(Object)}.
     */
    @Override
    public boolean offer(E e) {
        return add(e);
    }

    @Override
    public E remove() {
        return removeFirst();
    }

    @Override
    public E poll() {
        return pollLast();
    }

    @Override
    public E element() {
        return getFirst();
    }

    @Override
    public E peek() {
        return pollFirst();
    }

    @Override
    public void push(E e) {
        addFirst(e);
    }

    @Override
    public E pop() {
        E old = getFirst();
        removeFirst();
        return old;
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }
	
    @Override
    public Iterator<E> descendingIterator() {
	return new DescendingItr();
    }
    
    @Override
    public ListIterator<E> listIterator(int index) {
        return new ListItr(index);
    }
	
    @Override
    public String toString() {
        if (isEmpty())
            return "[]";

        StringBuilder sb = new StringBuilder();
        for (Node<E> x = head; x != null; x = x.next) {
            sb.append(x.toString() + ", ");
        }
        return "[" + sb.toString().trim().substring(0, sb.length() - 2) + "]";
    }

    /**
     * Implementation of an {@code Iterator} to iterate over the list.
     */
    private class Itr implements Iterator<E> {
        private ListItr itr = new ListItr(0);
        public boolean hasNext() {
            return itr.hasNext();
        }
        public E next() {
            return itr.next();
        }
        public void remove() {
            itr.remove();
        }
    }
    
    /**
     * Implementation of a {@code descending Iterator} to iterate backwards
     * through the list.
     */
    private class DescendingItr implements Iterator<E> {
        private ListItr itr = new ListItr(size());
        public boolean hasNext() {
            return itr.hasPrevious();
        }
        public E next() {
            return itr.previous();
        }
        public void remove() {
            itr.remove();
        }
    }

    /**
     * An optimized version of AbstractList.ListItr.
     */
    @SuppressWarnings("unchecked")
    private class OptimizedListItr implements Iterator<E> {
        int cursor;
        int lastRet = -1;
        int expectedModCount = modCount;
        
        OptimizedListItr() { }
        
        @Override
        public boolean hasNext() {
            return cursor != size;
        }

	@Override
        public E next() {
            checkForComodification();
            int i = cursor;
            if (i >= size)
                throw new NoSuchElementException();
            Object[] elementData = toArray();
            if (i >= elementData.length)
                throw new ConcurrentModificationException();
            cursor = i + 1;
            return (E) elementData[lastRet = i];
        }
    
        @Override
        public void remove() {
           if (lastRet < 0)
                throw new IllegalStateException();
            checkForComodification();

            try {
                FastInsertList.this.remove(lastRet);
                cursor = lastRet;
                lastRet = -1;
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }
        
	@Override
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            final int size = FastInsertList.this.size;
            int i = cursor;
            if (i < size) {
                final Object[] es = toArray();
                if (i >= es.length)
                    throw new ConcurrentModificationException();
                for (; i < size && modCount == expectedModCount; i++)
                    action.accept((E) es[i]);
                // update once at end to reduce heap write traffic
                cursor = i;
                lastRet = i - 1;
                checkForComodification();
            }
        }
        
        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }

    /**
     * An optimized version of AbstractList.ListItr.
     */
    @SuppressWarnings("unchecked")
    private class ListItr extends OptimizedListItr implements ListIterator<E> {
        ListItr(int index) {
            super();
            cursor = index;
        }
        
        @Override
        public boolean hasPrevious() {
            return cursor != 0;
        }

	@Override
	public E previous() {
            checkForComodification();
            int i = cursor - 1;
            if (i < 0)
                throw new NoSuchElementException();
            Object[] elementData = toArray();
            if (i >= elementData.length)
                throw new ConcurrentModificationException();
            cursor = i;
            return (E) elementData[lastRet = i];
	}

        @Override
        public int nextIndex() {
            return cursor;
        }

        @Override
        public int previousIndex() {
            return cursor - 1;
        }

        @Override
        public void set(E e) {
            if (lastRet < 0)
                throw new IllegalStateException();
            checkForComodification();

            try {
                FastInsertList.this.set(lastRet, e);
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public void add(E e) {
            checkForComodification();

            try {
                int i = cursor;
                FastInsertList.this.add(i, e);
                cursor = i + 1;
                lastRet = -1;
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }

    }
    
    /**
     * Internal representation of an element inside the list.
     */
    static class Node<E> {
        Node<E> prev, next;
        E[] elements;

        int pointer = 0;
        int startIndex, endIndex;

        @SuppressWarnings("unchecked")
		Node(int runtimeSize, int startIndex) {
            this.elements = (E[]) new Object[(runtimeSize >>> 1) + 10];
            this.startIndex = startIndex;
            this.endIndex = startIndex + elements.length - 1;
        }

        boolean canInsert() {
            return pointer < elements.length;
        }

        void add(E e) {
            elements[pointer++] = e;
        }

        @Override
        public String toString() {

            StringBuilder sb = new StringBuilder();
            for (E e : elements) {
                if (e != null)
                    sb.append(e + ", ");
            }

            String s = sb.toString().trim();
            return s.isBlank() ? "" : s.substring(0, s.length() - 1);
        }
    }
}
