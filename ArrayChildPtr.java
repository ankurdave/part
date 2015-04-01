package com.ankurdave.part;

class ArrayChildPtr extends ChildPtr {
    public ArrayChildPtr(Node[] children, int i) {
        this.children = children;
        this.i = i;
    }

    @Override public Node get() {
        return children[i];
    }

    @Override public void set(Node n) {
        children[i] = n;
    }

    private Node[] children;
    private final int i;
}
