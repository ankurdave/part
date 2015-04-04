package com.ankurdave.part;

abstract class ArtNode extends Node {
    public ArtNode() {
        super();
    }

    public ArtNode(final ArtNode other) {
        super(other);
        this.num_children = other.num_children;
        this.partial_len = other.partial_len;
        System.arraycopy(other.partial, 0,
                         partial, 0,
                         Math.min(Node.MAX_PREFIX_LEN, partial_len));
    }

    /**
     * Returns the number of prefix characters shared between
     * the key and node.
     */
    public int check_prefix(final byte[] key, int depth) {
        int max_cmp = Math.min(Math.min(partial_len, Node.MAX_PREFIX_LEN), key.length - depth);
        int idx;
        for (idx = 0; idx < max_cmp; idx++) {
            if (partial[idx] != key[depth + idx])
                return idx;
        }
        return idx;
    }

    /**
     * Calculates the index at which the prefixes mismatch
     */
    public int prefix_mismatch(final byte[] key, int depth) {
        int max_cmp = Math.min(Math.min(Node.MAX_PREFIX_LEN, partial_len), key.length - depth);
        int idx;
        for (idx = 0; idx < max_cmp; idx++) {
            if (partial[idx] != key[depth + idx])
                return idx;
        }

        // If the prefix is short we can avoid finding a leaf
        if (partial_len > Node.MAX_PREFIX_LEN) {
            // Prefix is longer than what we've checked, find a leaf
            final Leaf l = this.minimum();
            max_cmp = Math.min(l.key.length, key.length) - depth;
            for (; idx < max_cmp; idx++) {
                if (l.key[idx + depth] != key[depth + idx])
                    return idx;
            }
        }
        return idx;
    }

    public abstract ChildPtr find_child(byte c);

    public abstract void add_child(ChildPtr ref, byte c, Node child);

    public abstract void remove_child(ChildPtr ref, byte c);

    // Precondition: isLastChild(i) == false
    public abstract int nextChildAtOrAfter(int i);

    public abstract Node childAt(int i);

    @Override public boolean insert(ChildPtr ref, final byte[] key, Object value,
                                    int depth, boolean force_clone) {
        boolean do_clone = force_clone || this.refcount > 1;

        // Check if given node has a prefix
        if (partial_len > 0) {
            // Determine if the prefixes differ, since we need to split
            int prefix_diff = prefix_mismatch(key, depth);
            if (prefix_diff >= partial_len) {
                depth += partial_len;
            } else {
                // Create a new node
                ArtNode4 result = new ArtNode4();
                Node ref_old = ref.get();
                ref.change_no_decrement(result); // don't decrement yet, because doing so might destroy self
                result.partial_len = prefix_diff;
                System.arraycopy(partial, 0,
                                 result.partial, 0,
                                 Math.min(Node.MAX_PREFIX_LEN, prefix_diff));

                // Adjust the prefix of the old node
                ArtNode this_writable = do_clone ? (ArtNode)this.n_clone() : this;
                if (partial_len <= Node.MAX_PREFIX_LEN) {
                    result.add_child(ref, this_writable.partial[prefix_diff], this_writable);
                    this_writable.partial_len -= (prefix_diff + 1);
                    System.arraycopy(this_writable.partial, prefix_diff + 1,
                                     this_writable.partial, 0,
                                     Math.min(Node.MAX_PREFIX_LEN, this_writable.partial_len));
                } else {
                    this_writable.partial_len -= (prefix_diff+1);
                    final Leaf l = this.minimum();
                    result.add_child(ref, l.key[depth + prefix_diff], this_writable);
                    System.arraycopy(l.key, depth + prefix_diff + 1,
                                     this_writable.partial, 0,
                                     Math.min(Node.MAX_PREFIX_LEN, this_writable.partial_len));
                }

                // Insert the new leaf
                Leaf l = new Leaf(key, value);
                result.add_child(ref, key[depth + prefix_diff], l);

                ref_old.decrement_refcount();

                return true;
            }
        }

        // Clone self if necessary
        ArtNode this_writable = do_clone ? (ArtNode)this.n_clone() : this;
        if (do_clone) {
            ref.change(this_writable);
        }
        // Do the insert, either in a child (if a matching child already exists) or in self
        ChildPtr child = this_writable.find_child(key[depth]);
        if (child != null) {
            return Node.insert(child.get(), child, key, value, depth+1, force_clone);
        } else {
            // No child, node goes within us
            Leaf l = new Leaf(key, value);
            this_writable.add_child(ref, key[depth], l);
            // If `this` was full and `do_clone` is true, we will clone a full node
            // and then immediately delete the clone in favor of a larger node.
            // TODO: avoid this
            return true;
        }
    }

    @Override public boolean delete(ChildPtr ref, final byte[] key, int depth,
                                    boolean force_clone) {
        // Bail if the prefix does not match
        if (partial_len > 0) {
            int prefix_len = check_prefix(key, depth);
            if (prefix_len != Math.min(MAX_PREFIX_LEN, partial_len)) {
                return false;
            }
            depth += partial_len;
        }

        boolean do_clone = force_clone || this.refcount > 1;

        // Clone self if necessary. Note: this allocation will be wasted if the
        // key does not exist in the child's subtree
        ArtNode this_writable = do_clone ? (ArtNode)this.n_clone() : this;

        // Find child node
        ChildPtr child = this_writable.find_child(key[depth]);
        if (child == null) return false; // when translating to C++, make sure to delete this_writable

        if (do_clone) {
            ref.change(this_writable);
        }

        boolean child_is_leaf = child.get() instanceof Leaf;
        boolean do_delete = child.get().delete(child, key, depth + 1, do_clone);

        if (do_delete && child_is_leaf) {
            // The leaf to delete is our child, so we must remove it
            this_writable.remove_child(ref, key[depth]);
        }

        return do_delete;
    }

    int num_children = 0;
    int partial_len = 0;
    final byte[] partial = new byte[Node.MAX_PREFIX_LEN];
}
