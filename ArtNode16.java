package com.ankurdave.part;

class ArtNode16 extends ArtNode {
    public static int count;

    public ArtNode16() {
        super();
        count++;
    }

    public ArtNode16(final ArtNode16 other) {
        super(other);
        System.arraycopy(other.keys, 0, keys, 0, other.num_children);
        for (int i = 0; i < other.num_children; i++) {
            children[i] = other.children[i];
            children[i].refcount++;
        }
        count++;
    }

    public ArtNode16(final ArtNode4 other) {
        this();
        // ArtNode
        this.num_children = other.num_children;
        this.partial_len = other.partial_len;
        System.arraycopy(other.partial, 0,
                         this.partial, 0,
                         Math.min(MAX_PREFIX_LEN, this.partial_len));
        // ArtNode16 from ArtNode4
        System.arraycopy(other.keys, 0, keys, 0, this.num_children);
        for (int i = 0; i < this.num_children; i++) {
            children[i] = other.children[i];
            children[i].refcount++;
        }
    }

    @Override public Node n_clone() {
        return new ArtNode16(this);
    }

    @Override public ChildPtr find_child(byte c) {
        // TODO: avoid linear search using intrinsics if available
        for (int i = 0; i < this.num_children; i++) {
            if (keys[i] == to_uint(c)) {
                return new ArrayChildPtr(children, i);
            }
        }
        return null;
    }

    @Override public Leaf minimum() {
        return Node.minimum(children[0]);
    }

    @Override public void add_child(ChildPtr ref, byte c, Node child, boolean force_clone) {
        if (this.num_children < 16) {
            // TODO: avoid linear search using intrinsics if available
            int idx;
            for (idx = 0; idx < this.num_children; idx++) {
                if (to_uint(c) < to_uint(keys[idx])) break;
            }

            ArtNode16 target = force_clone ? new ArtNode16(this) : this;

            // Shift to make room
            System.arraycopy(target.keys, idx, target.keys, idx + 1, target.num_children - idx);
            System.arraycopy(target.children, idx, target.children, idx + 1, target.num_children - idx);

            // Insert element
            target.keys[idx] = c;
            target.children[idx] = child;
            child.refcount++;
            target.num_children++;

            if (force_clone) {
                // Update the parent pointer to the new node
                ref.change(target);
            }
        } else {
            // Copy the node16 into a new node48
            ArtNode48 result = new ArtNode48(this);
            // Update the parent pointer to the node48
            ref.change(result);
            // Insert the element into the node48 instead
            result.add_child(ref, c, child, false);
        }
    }

    @Override public void iter(IterCallback cb) {
        for (int i = 0; i < this.num_children; i++) {
            Node.iter(children[i], cb);
        }
    }

    @Override public int decrement_refcount() {
        if (--this.refcount <= 0) {
            int freed = 0;
            for (int i = 0; i < this.num_children; i++) {
                freed += Node.decrement_refcount(children[i]);
            }
            count--;
            // delete this;
            return freed + 232;
            // object size (8) + refcount (4) +
            // num_children int (4) + partial_len int (4) +
            // pointer to partial array (8) + partial array size (8+4+1*MAX_PREFIX_LEN)
            // pointer to key array (8) + key array size (8+4+1*16) +
            // pointer to children array (8) + children array size (8+4+8*16)
        }
        return 0;
    }

    byte[] keys = new byte[16];
    Node[] children = new Node[16];
}
