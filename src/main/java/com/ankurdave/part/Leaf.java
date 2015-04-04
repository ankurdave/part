package com.ankurdave.part;

class Leaf extends Node {
    public static int count;

    public Leaf(final byte[] key, Object value) {
        super();
        this.key = key;
        this.value = value;
        count++;
    }

    public Leaf(final Leaf other) {
        super(other);
        this.key = other.key;
        this.value = other.value;
        count++;
    }

    @Override public Node n_clone() {
        return new Leaf(this);
    }

    public boolean matches(final byte[] key) {
        if (this.key.length != key.length) return false;
        for (int i = 0; i < key.length; i++) {
            if (this.key[i] != key[i]) {
                return false;
            }
        }
        return true;
    }

    @Override public Leaf minimum() {
        return this;
    }

    public int longest_common_prefix(Leaf other, int depth) {
        int max_cmp = Math.min(key.length, other.key.length) - depth;
        int idx;
        for (idx = 0; idx < max_cmp; idx++) {
            if (key[depth + idx] != other.key[depth + idx]) {
                return idx;
            }
        }
        return idx;
    }

    @Override public boolean insert(ChildPtr ref, final byte[] key, Object value,
                                    int depth, boolean force_clone) throws UnsupportedOperationException {
        boolean clone = force_clone || this.refcount > 1;
        if (matches(key)) {
            if (clone) {
                // Updating an existing value, but need to create a new leaf to
                // reflect the change
                ref.change(new Leaf(key, value));
            } else {
                // Updating an existing value, and safe to make the change in
                // place
                this.value = value;
            }
            return false;
        } else {
            // New value

            // Create a new leaf
            Leaf l2 = new Leaf(key, value);

            // Determine longest prefix
            int longest_prefix = longest_common_prefix(l2, depth);
            if (depth + longest_prefix >= this.key.length ||
                depth + longest_prefix >= key.length) {
                throw new UnsupportedOperationException("keys cannot be prefixes of other keys");
            }

            // Split the current leaf into a node4
            ArtNode4 result = new ArtNode4();
            result.partial_len = longest_prefix;
            Node ref_old = ref.get();
            ref.change_no_decrement(result);

            System.arraycopy(key, depth,
                             result.partial, 0,
                             Math.min(Node.MAX_PREFIX_LEN, longest_prefix));
            // Add the leafs to the new node4
            result.add_child(ref, this.key[depth + longest_prefix], this);
            result.add_child(ref, l2.key[depth + longest_prefix], l2);

            ref_old.decrement_refcount();

            // TODO: avoid the increment to self immediately followed by decrement

            return true;
        }
    }

    @Override public boolean delete(ChildPtr ref, final byte[] key, int depth,
                                    boolean force_clone) {
        return matches(key);
    }

    @Override public boolean exhausted(int i) {
        return i > 0;
    }

    @Override public int decrement_refcount() {
        if (--this.refcount <= 0) {
            count--;
            // delete this;
            // Don't delete the actual key or value because they may be used
            // elsewhere
            return 32;
            // object size (8) + refcount (4) + pointer to key array (8) +
            // pointer to value (8) + padding (4)
        }
        return 0;
    }

    Object value;
    final byte[] key;
}
