package com.ankurdave.part;

abstract class ChildPtr {
    abstract Node get();
    abstract void set(Node n);

    void change(Node n) {
        // First increment the refcount of the new node, in case it would
        // otherwise have been deleted by the decrement of the old node
        n.refcount++;
        if (get() != null) {
            get().decrement_refcount();
        }
        set(n);
    }

    void change_no_decrement(Node n) {
        n.refcount++;
        set(n);
    }
}
