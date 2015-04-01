package com.ankurdave.part;

abstract class ChildPtr {
    public abstract Node get();
    public abstract void set(Node n);

    public void change(Node n) {
        if (get() != null) {
            get().decrement_refcount();
        }
        n.refcount++;
        set(n);
    }

    public void change_no_decrement(Node n) {
        n.refcount++;
        set(n);
    }
}
