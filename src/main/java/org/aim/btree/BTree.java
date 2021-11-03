package org.aim.btree;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

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

        public void mergeNode(Object middleItem, Node<E> node) {
            this.ensureExplicitCapacity(FULL_NODE_SIZE);
            this.items[size] = middleItem;
            this.size++;
            for (int i = 0; i < node.size; i++) {
                this.items[size + i] = node.items[i];
                this.children[size + i] = node.children[i];
            }
            this.size = this.size + node.size;
            this.children[this.size] = node.children[node.size];
        }

        public Node<E> getLastChild() {
            return children[size];
        }

        public void add(Object item, Node<E> node, int index, boolean before) {
            ensureCapacityInternal(size + 1);
            int i = children[index].size;
            for (; i > index; i--) {
                items[i] = items[i - 1];
                children[i + 1] = children[i];
            }
            children[i + 1] = children[i];
            items[index] = item;
            size++;
            if (before) {
                children[index] = node;
                return;
            }
            children[index + 1] = node;
        }

        public E getLastItem() {
            return (E) items[size - 1];
        }

        public void removeLast() {
            items[size - 1] = null;
            children[size] = null;
            size--;
        }

        public Node<E> getFirstChild() {
            return children[0];
        }

        public E getFirstItem() {
            return (E) items[0];
        }

        public void removeFirst() {
            int i = 0;
            for (; i < size - 1; i++) {
                items[i] = items[i + 1];
                children[i] = children[i + 1];
            }
            children[i] = children[i + 1];
            items[i] = null;
            children[i + 1] = null;
            size--;
        }

        public void remove(int index) {
            int i = index;
            for (; i < size - 1; i++) {
                items[i] = items[i + 1];
                children[i + 1] = children[i + 2];
            }
            items[i] = null;
            children[i + 1] = null;
            size--;
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
    public boolean add(E item) {
        if (Objects.isNull(item)) {
            throw new NullPointerException();
        }
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

    /**
     * 查询指定元素
     *
     * @param item 要搜索元素
     * @return
     */
    public E search(E item) {
        if (Objects.isNull(item)) {
            throw new NullPointerException();
        }
        if (root.size == 0) {
            return null;
        }
        return search(root, item);
    }

    /**
     * 移除指定元素
     *
     * @param item
     * @return
     */
    public boolean remove(E item) {
        if (Objects.isNull(item)) {
            return false;
        }
        if (root.size == 0) {
            return false;
        }
        return remove(root, item);
    }

    private boolean remove(Node<E> node, E item) {
        int i = 0;
        // 要删除元素在当前节点
        while (i < node.size && comparator.compare(item, node.items[i]) > 0) {
            i++;
        }
        if (i < node.size && comparator.compare(item, node.items[i]) == 0) {
            remove(i, node);
        }
        if (node.leaf) {
            return false;
        }
        // 不在当前节点

        // 孩子节点至少包含degree个元素
        if (node.children[i].size >= degree) {
            return remove(node.children[i], item);
        }
        // 孩子节点包含元素等于degree - 1，相邻的兄弟节点至少包好degree个元素
        if (i > 0 && node.children[i - 1].size >= degree) {
            // 执行将当前节点i - 1位置的元素移动到，i位置孩子中，将i - 1位置孩子的最大元素移动至当前节点i - 1位置
            node.children[i].add(node.items[i - 1], node.children[i - 1].getLastChild(), 0, true);
            node.items[i - 1] = node.children[i - 1].getLastItem();
            node.children[i - 1].removeLast();
            return remove(node.children[i], item);
        }
        if (i > 0 && node.children[i + 1].size >= degree) {
            // 执行将当前节点i + 1位置的元素追加到，i位置孩子末尾，将i + 1位置孩子最小元素移动至当前节点i + 1位置，并将其前一个孩子移动至
            // 追加元素的右孩子
            node.children[i].add(node.items[i + 1], node.children[i + 1].getFirstChild(), node.size, false);
            node.items[i + 1] = node.children[i + 1].getFirstItem();
            node.children[i + 1].removeFirst();
            return remove(node.children[i], item);
        }
        // 当左右孩子包含元素个数都小于degree时，将第i位置的孩子与第i+1位置位置的孩子合并，并将i位置的元素下移到合并之后的孩子中作为中间元素
        node.children[i].mergeNode(node.items[i], node.children[i + 1]);
        node.remove(i);
        return remove(node.children[i], item);
    }

    private void remove(int index, Node<E> node) {
        // 如果是叶子节点直接移除
        if (node.leaf) {
            removeItem(node, index);
            return;
        }
        // 如果是内部节点，并且前一个子节点至少包含degree个元素，则找到前驱，并将前驱元素从原节点移除，并替换需要删除的元素
        if (node.children[index].size >= degree) {
            // 查找前驱并移除
            E preItem = removePre(node.children[index], node.items[index]);
            node.items[index] = preItem;
            return;
        }
        // 如果是内部节点，前一个子节点元素少于degree个元素，则查看后一个子节点至少包含degree个元素，则找到后续，并将后续元素从原节点移除，并替换需要删除的元素
        if (node.children[index + 1].size >= degree) {
            // 查找后续并移除
            E follow = removeFollow(node.children[index + 1], node.items[index]);
            node.items[index] = follow;
            return;
        }
        // 如果上述情况都不满足，则表明前一个节点和后一个节点元素个数都少于degree个，执行元素合并操作，并将要删除节点下移，继续执行删除操作
        int leftNodeSize = node.children[index].size;
        node.children[index].mergeNode(node.items[index], node.children[index + 1]);
        removeItemAndRightChild(node, index);
        remove(leftNodeSize, node.children[index]);
    }

    private void removeItemAndRightChild(Node<E> node, int index) {
        while (index < node.size - 1) {
            node.items[index] = node.items[index + 1];
            node.children[index + 1] = node.children[index + 2];
            index++;
        }
        node.items[node.size - 1] = null;
        node.children[node.size] = null;
        node.size--;
    }

    /**
     * 查找后续元素并移除
     *
     * @param node
     * @param item
     * @return
     */
    private E removeFollow(Node<E> node, Object item) {
        int i = 0;
        while (i < node.size && comparator.compare(item, node.items[i]) > 0) {
            i++;
        }
        if (!node.leaf) {
            removePre(node.children[i], item);
        }
        E follow = (E) node.items[i];
        for (; i < node.size - 1; i++) {
            node.items[i] = node.items[i + 1];
        }
        node.items[i + 1] = null;
        return follow;
    }

    /**
     * 查找item元素的前驱并移除
     *
     * @param node
     * @param item
     * @return
     */
    private E removePre(Node<E> node, Object item) {
        int i = 0;
        while (i < node.size && comparator.compare(item, node.items[i]) > 0) {
            i++;
        }
        if (!node.leaf) {
            removePre(node.children[i], item);
        }
        i--;
        E pre = (E) node.items[i];
        for (; i < node.size - 1; i++) {
            node.items[i] = node.items[i + 1];
        }
        node.items[i + 1] = null;
        return pre;
    }

    private void removeItem(Node<E> node, int index) {
        while (index < node.size - 1) {
            node.items[index] = node.items[index + 1];
            index++;
        }
        node.items[node.size - 1] = null;
        node.size--;
    }

    private E search(Node<E> node, E item) {
        int i = 0;
        while (i < node.size && comparator.compare(item, node.items[i]) > 0) {
            i++;
        }
        if (i < node.size && comparator.compare(item, node.items[i]) == 0) {
            return (E) node.items[i];
        } else if (node.leaf) {
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
            left.items[j + degree] = null;
        }

        if (!left.leaf) {
            for (int j = 0; j < degree; j++) {
                right.children[j] = left.children[j + degree];
                left.children[j + degree] = null;
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
        // 需要从left中剪切出来
        parent.items[i] = left.items[degree - 1];
        left.items[degree - 1] = null;
        parent.size++;
    }
}
