package com.ankurdave.part;

import scala.Tuple2;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ArtIterator implements Iterator<Tuple2<byte[], Object>> {
    private ArrayList<Node> elemStack = new ArrayList<Node>();
    private ArrayList<Integer> idxStack = new ArrayList<Integer>();
    private int pos = 0;
    private boolean _hasNext;

    public ArtIterator(Node root) {
        elemStack.add(root);
        idxStack.add(0);
        maybeAdvance();
    }

    @Override public boolean hasNext() {
        return _hasNext;
    }

    @Override public Tuple2<byte[], Object> next() {
        if (_hasNext) {
            Leaf leaf = (Leaf)elemStack.get(pos);
            byte[] key = leaf.key;
            Object value = leaf.value;
            maybeAdvance();
            return new Tuple2<byte[], Object>(key, value);
        } else {
            throw new NoSuchElementException("end of iterator");
        }
    }

    @Override public void remove() {
        throw new UnsupportedOperationException();
    }

    // Postcondition: if _hasNext is true, the top of the stack must contain a leaf
    private void maybeAdvance() {
        // Pop exhausted nodes
        while (pos >= 0 && elemStack.get(pos).exhausted(idxStack.get(pos) + 1)) {
            pos -= 1;
        }

        _hasNext = (pos >= 0);
        if (_hasNext) {
            // Descend to the next leaf node element
            while (true) {
                if (elemStack.get(pos) instanceof Leaf) {
                    // Done - reached the next element
                    break;
                } else {
                    // Advance to the next child of this node
                    ArtNode cur = (ArtNode)elemStack.get(pos);
                    idxStack.set(pos, cur.nextChildAtOrAfter(idxStack.get(pos) + 1));
                    Node child = cur.childAt(idxStack.get(pos));

                    // Push it onto the stack
                    pos += 1;
                    while (elemStack.size() <= pos) elemStack.add(null);
                    elemStack.set(pos, child);
                    while (idxStack.size() <= pos) idxStack.add(-1);
                    idxStack.set(pos, -1);
                }
            }
        }
    }
}
