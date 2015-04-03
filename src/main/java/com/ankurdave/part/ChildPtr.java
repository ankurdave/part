package com.ankurdave.part;

abstract class ChildPtr {
    public abstract Node get();
    public abstract void set(Node n);

    public void change(Node n) {
        // First increment the refcount of the new node, in case it would
        // otherwise have been deleted by the decrement of the old node
        n.refcount++;
        if (get() != null) {
            get().decrement_refcount();
        }
        set(n);
    }

    public void change_no_decrement(Node n) {
        n.refcount++;
        set(n);
    }
}
