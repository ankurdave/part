package com.ankurdave.part;

class ArtNode256 extends ArtNode {
    public static int count;

    public ArtNode256() {
        super();
        count++;
    }

    public ArtNode256(final ArtNode256 other) {
        super(other);
        for (int i = 0; i < 256; i++) {
            children[i] = other.children[i];
            if (children[i] != null) {
                children[i].refcount++;
            }
        }
        count++;
    }

    public ArtNode256(final ArtNode48 other) {
        this();
        // ArtNode
        this.num_children = other.num_children;
        this.partial_len = other.partial_len;
        System.arraycopy(other.partial, 0, this.partial, 0, Math.min(MAX_PREFIX_LEN, this.partial_len));
        // ArtNode256 from ArtNode48
        for (int i = 0; i < 256; i++) {
            if (other.keys[i] != 0) {
                children[i] = other.children[to_uint(other.keys[i]) - 1];
                children[i].refcount++;
            }
        }
    }

    @Override public Node n_clone() {
        return new ArtNode256(this);
    }

    @Override public ChildPtr find_child(byte c) {
        if (children[to_uint(c)] != null) return new ArrayChildPtr(children, to_uint(c));
        return null;
    }

    @Override public Leaf minimum() {
        int idx = 0;
        while (children[idx] == null) idx++;
        return Node.minimum(children[idx]);
     }

    @Override public void add_child(ChildPtr ref, byte c, Node child) {
        assert(refcount <= 1);

        this.num_children++;
        this.children[to_uint(c)] = child;
        child.refcount++;
    }

    @Override public void remove_child(ChildPtr ref, byte c) {
        assert(refcount <= 1);

        children[to_uint(c)].decrement_refcount();
        children[to_uint(c)] = null;
        num_children--;

        if (num_children == 37) {
            ArtNode48 result = new ArtNode48(this);
            ref.change(result);
        }
    }

    @Override public boolean exhausted(int c) {
        for (int i = c; i < 256; i++) {
            if (children[i] != null) {
                return false;
            }
        }
        return true;
    }

    @Override public int nextChildAtOrAfter(int c) {
        int pos = c;
        for (; pos < 256; pos++) {
            if (children[pos] != null) {
                break;
            }
        }
        return pos;
    }

    @Override public Node childAt(int pos) {
        return children[pos];
    }

    @Override public int decrement_refcount() {
        if (--this.refcount <= 0) {
            int freed = 0;
            for (int i = 0; i < 256; i++) {
                if (children[i] != null) {
                    freed += children[i].decrement_refcount();
                }
            }
            count--;
            // delete this;
            return freed + 2120;
            // object size (8) + refcount (4) +
            // num_children int (4) + partial_len int (4) +
            // pointer to partial array (8) + partial array size (8+4+1*MAX_PREFIX_LEN)
            // pointer to children array (8) + children array size (8+4+8*256) +
            // padding (4)
        }
        return 0;
    }

    Node[] children = new Node[256];
}
