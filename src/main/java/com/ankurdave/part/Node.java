package com.ankurdave.part;

import java.io.Serializable;

abstract class Node implements Serializable {
    static final int MAX_PREFIX_LEN = 8;

    public Node() {
        refcount = 0;
    }

    public Node(final Node other) {
        refcount = 0;
    }

    public abstract  Node n_clone();
    public static Node n_clone(Node n) {
        if (n == null) return null;
        else return n.n_clone();
    }

    public abstract Leaf minimum();
    public static Leaf minimum(Node n) {
        if (n == null) return null;
        else return n.minimum();
    }

    public abstract void insert(ChildPtr ref, final byte[] key, Object value, int depth,
                       boolean force_clone) throws UnsupportedOperationException;
    public static void insert(Node n, ChildPtr ref, final byte[] key, Object value, int depth,
                              boolean force_clone) {
        // If we are at a NULL node, inject a leaf
        if (n == null) {
            ref.change(new Leaf(key, value));
        } else {
            n.insert(ref, key, value, depth, force_clone);
        }
    }

    public abstract boolean delete(ChildPtr ref, final byte[] key, int depth,
                                   boolean force_clone);

    public abstract void iter(IterCallback cb);
    public static void iter(Node n, IterCallback cb) {
        if (n == null) return;
        else n.iter(cb);
    }

    public abstract int decrement_refcount();

    public abstract boolean exhausted(int i);
    public static boolean exhausted(Node n, int i) {
        if (n == null) return true;
        else return n.exhausted(i);
    }


    protected static int to_uint(byte b) {
        return ((int)b) & 0xFF;
    }

    int refcount;
}
