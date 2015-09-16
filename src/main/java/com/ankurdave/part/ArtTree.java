package com.ankurdave.part;

import java.io.Serializable;
import java.util.Iterator;

import scala.Tuple2;

public class ArtTree extends ChildPtr implements Serializable {
    public ArtTree() { }

    public ArtTree(final ArtTree other) {
        root = other.root;
        num_elements = other.num_elements;
    }

    public ArtTree snapshot() {
        ArtTree b = new ArtTree();
        if (root != null) {
            b.root = Node.n_clone(root);
            b.root.refcount++;
        }
        b.num_elements = num_elements;
        return b;
    }

    @Override Node get() {
        return root;
    }

    @Override void set(Node n) {
        root = n;
    }

    public Object search(final byte[] key) {
        Node n = root;
        int prefix_len, depth = 0;
        while (n != null) {
            if (n instanceof Leaf) {
                Leaf l = (Leaf)n;
                // Check if the expanded path matches
                if (l.matches(key)) {
                    return l.value;
                } else {
                    return null;
                }
            } else {
                ArtNode an = (ArtNode)(n);

                // Bail if the prefix does not match
                if (an.partial_len > 0) {
                    prefix_len = an.check_prefix(key, depth);
                    if (prefix_len != Math.min(Node.MAX_PREFIX_LEN, an.partial_len)) {
                        return null;
                    }
                    depth += an.partial_len;
                }

                if (depth >= key.length) return null;

                // Recursively search
                ChildPtr child = an.find_child(key[depth]);
                n = (child != null) ? child.get() : null;
                depth++;
            }
        }
        return null;
    }

    public void insert(final byte[] key, Object value) throws UnsupportedOperationException {
        if (Node.insert(root, this, key, value, 0, false)) num_elements++;
    }

    public void delete(final byte[] key) {
        if (root != null) {
            boolean child_is_leaf = root instanceof Leaf;
            boolean do_delete = root.delete(this, key, 0, false);
            if (do_delete) {
                num_elements--;
                if (child_is_leaf) {
                    // The leaf to delete is the root, so we must remove it
                    root = null;
                }
            }
        }
    }

    public Iterator<Tuple2<byte[], Object>> iterator() {
        return new ArtIterator(root);
    }

    public Iterator<Tuple2<byte[], Object>> prefixIterator(final byte[] prefix) {
        // Find the root node for the prefix
        Node n = root;
        int prefix_len, depth = 0;
        while (n != null) {
            if (n instanceof Leaf) {
                Leaf l = (Leaf)n;
                // Check if the expanded path matches
                if (l.prefix_matches(prefix)) {
                    return new ArtIterator(l);
                } else {
                    return new ArtIterator(null);
                }
            } else {
                if (depth == prefix.length) {
                    // If we have reached appropriate depth, return the iterator
                    if (n.minimum().prefix_matches(prefix)) {
                        return new ArtIterator(n);
                    } else {
                        return new ArtIterator(null);
                    }
                } else {
                    ArtNode an = (ArtNode)(n);

                    // Bail if the prefix does not match
                    if (an.partial_len > 0) {
                        prefix_len = an.prefix_mismatch(prefix, depth);
                        if (prefix_len == 0) {
                            // No match, return empty
                            return new ArtIterator(null);
                        } else if (depth + prefix_len == prefix.length) {
                            // Prefix match, return iterator
                            return new ArtIterator(n);
                        } else {
                            // Full match, go deeper
                            depth += an.partial_len;
                        }
                    }

                    // Recursively search
                    ChildPtr child = an.find_child(prefix[depth]);
                    n = (child != null) ? child.get() : null;
                    depth++;
                }
            }
        }
        return new ArtIterator(null);
    }

    public long size() {
        return num_elements;
    }

    public int destroy() {
        if (root != null) {
            int result = root.decrement_refcount();
            root = null;
            return result;
        } else {
            return 0;
        }
    }

    Node root = null;
    long num_elements = 0;
}
