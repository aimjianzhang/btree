package org.aim.btree;

import java.util.Arrays;
import java.util.Comparator;

public class BTree<E> {
    static final int DEFAULT_DEGREE = 2;

    /**
     * 总的关键字个数
     */
    int size;

    /**
     * 根节点
     */
    Node<E> root;

    /**
     * 最小度数
     */
    static int degree = DEFAULT_DEGREE;

    static final int FULL_NODE_SIZE = 2 * degree - 1;

    final Comparator comparator;

    static class Node<E> {
        private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};
        private static final Node[] DEFAULTCAPACITY_EMPTY_CHILDREN = {};
        private static final int MAX_SIZE = Integer.MAX_VALUE - 8;
        private static int DEFAULT_CAPACITY = (degree * 2 - 1) / 2 > 8 ? 8 : (degree * 2 - 1) / 2;

        /**
         * 当前节点关键字数量
         */
        int size;
        /**
         * 关键字列表
         */
        Object[] items;
        /**
         * 孩子节点
         */
        Node<E>[] children;
        /**
         * 是否叶节点
         */
        boolean leaf;

        public Node() {
            this.leaf = true;
            this.size = 0;
            this.items = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
            this.children = DEFAULTCAPACITY_EMPTY_CHILDREN;
        }

        public Node(int initialCapacity) {
            this.leaf = true;
            this.size = 0;
            this.items = new Object[initialCapacity];
            this.children = new Node[initialCapacity + 1];
        }

        public void ensureCapacityInternal(int minCapacity) {
            ensureExplicitCapacity(calculateCapacity(items, minCapacity));
        }

        private void ensureExplicitCapacity(int minCapacity) {
            if (minCapacity - items.length > 0)
                grow(minCapacity);
        }

        private void grow(int minCapacity) {
            int oldCapacity = items.length;
            int newCapacity = oldCapacity + (oldCapacity >> 1);
            if (newCapacity - minCapacity < 0)
                newCapacity = minCapacity;
            if (newCapacity - MAX_SIZE > 0)
                newCapacity = hugeCapacity(minCapacity);
            if (newCapacity > FULL_NODE_SIZE) {
                newCapacity = FULL_NODE_SIZE;
            }
            items = Arrays.copyOf(items, newCapacity);
            children = Arrays.copyOf(children, newCapacity + 1);
        }

        private int hugeCapacity(int minCapacity) {
            if (minCapacity < 0) // overflow
                throw new OutOfMemoryError();
            return (minCapacity > MAX_SIZE) ?
                    Integer.MAX_VALUE :
                    MAX_SIZE;
        }

        private int calculateCapacity(Object[] elementData, int minCapacity) {
            if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
                return Math.max(DEFAULT_CAPACITY, minCapacity);
            }
            return minCapacity;
        }
    }

    public BTree(Comparator comparator) {
        this.size = 0;
        this.root = new Node();
        this.comparator = comparator;
    }

    public BTree(int degree, Comparator comparator) {
        this(comparator);
        this.degree = degree;
    }

    /**
     * 插入关键字
     *
     * @param item
     * @return
     */
    public boolean insert(E item) {
        Node<E> r = root;
        if (root.size == FULL_NODE_SIZE) {
            Node<E> newRoot = new Node<>(1);
            newRoot.leaf = false;
            newRoot.size = 0;
            newRoot.children[0] = r;
            root = newRoot;
            splitChild(newRoot, 0);
            return insertNonFull(newRoot, item);
        }
        return insertNonFull(r, item);
    }

    public E search(E item) {
        if (root.size == 0) {
            return null;
        }
        return search(root, item);
    }

    private E search(Node<E> node, E item){
        int i = 0;
        while (i < node.size && comparator.compare(item, node.items[i]) > 0){
            i++;
        }
        if(i < node.size && comparator.compare(item, node.items[i]) == 0){
            return (E) node.items[i];
        }
        else if(node.leaf){
            return null;
        } else {
            return search(node.children[i], item);
        }
    }

    /**
     * 向非满节点插入关键字
     *
     * @param node
     * @param item
     * @return
     */
    private boolean insertNonFull(Node<E> node, E item) {
        // 动态扩容数组长度
        node.ensureCapacityInternal(node.size + 1);
        int i = node.size;
        if (node.leaf) {
            while (i > 0 && comparator.compare(item, node.items[i - 1]) < 0) {
                node.items[i] = node.items[i - 1];
                i--;
            }
            node.items[i] = item;
            node.size++;
            return true;
        }
        while (i > 0 && comparator.compare(item, node.items[i - 1]) < 0) {
            i--;
        }
        if (node.children[i].size == FULL_NODE_SIZE) {
            splitChild(node, i);
            if (comparator.compare(item, node.items[i]) > 0) {
                i++;
            }
        }
        insertNonFull(node.children[i], item);
        return true;
    }

    /**
     * 分裂满节点
     *
     * @param parent
     * @param i
     */
    private void splitChild(Node<E> parent, int i) {
        Node<E> right = new Node<>(degree - 1);
        Node<E> left = parent.children[i];
        right.leaf = left.leaf;
        right.size = degree - 1;
        for (int j = 0; j < degree - 1; j++) {
            right.items[j] = left.items[j + degree];
        }

        if (!left.leaf) {
            for (int j = 0; j < degree; j++) {
                right.children[j] = left.children[j + degree];
            }
        }

        left.size = degree - 1;
        for (int j = parent.size; j > i; j--) {
            parent.children[j] = parent.children[j];
        }
        parent.children[i + 1] = right;
        for (int j = parent.size - 1; j > i; j--) {
            parent.items[j] = parent.items[j - 1];
        }
        parent.items[i] = left.items[degree];
        parent.size++;
    }
}
