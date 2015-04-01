package com.ankurdave.part;

public class ArtTree extends ChildPtr {
    public ArtTree() {
        root = null;
    }

    public ArtTree(final ArtTree other) {
        root = other.root;
    }

    public ArtTree snapshot() {
        ArtTree b = new ArtTree();
        if (root != null) {
            b.root = Node.n_clone(root);
            b.root.refcount++;
        }
        return b;
    }

    @Override public Node get() {
        return root;
    }

    @Override public void set(Node n) {
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

                // Recursively search
                ChildPtr child = an.find_child(key[depth]);
                n = (child != null) ? child.get() : null;
                depth++;
            }
        }
        return null;
    }

    public void insert(final byte[] key, Object value) {
        Node.insert(root, this, key, value, 0, false);
    }

    public void iter(IterCallback cb) {
        Node.iter(root, cb);
    }

    public int destroy() {
        return Node.decrement_refcount(root);
    }

    private Node root;
}
