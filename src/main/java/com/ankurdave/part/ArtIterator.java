package com.ankurdave.part;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

import scala.Tuple2;

class ArtIterator implements Iterator<Tuple2<byte[], Object>> {
    private Deque<Node> elemStack = new ArrayDeque<Node>();
    private Deque<Integer> idxStack = new ArrayDeque<Integer>();

    public ArtIterator(Node root) {
        if (root != null) {
            elemStack.push(root);
            idxStack.push(0);
            maybeAdvance();
        }
    }

    @Override public boolean hasNext() {
        return !elemStack.isEmpty();
    }

    @Override public Tuple2<byte[], Object> next() {
        if (hasNext()) {
            Leaf leaf = (Leaf)elemStack.peek();
            byte[] key = leaf.key;
            Object value = leaf.value;

            // Mark the leaf as consumed
            idxStack.push(idxStack.pop() + 1);

            maybeAdvance();
            return new Tuple2<byte[], Object>(key, value);
        } else {
            throw new NoSuchElementException("end of iterator");
        }
    }

    @Override public void remove() {
        throw new UnsupportedOperationException();
    }

    // Postcondition: if the stack is nonempty, the top of the stack must contain a leaf
    private void maybeAdvance() {
        // Pop exhausted nodes
        while (!elemStack.isEmpty() && elemStack.peek().exhausted(idxStack.peek())) {
            elemStack.pop();
            idxStack.pop();

            if (!elemStack.isEmpty()) {
                // Move on by advancing the exhausted node's parent
                idxStack.push(idxStack.pop() + 1);
            }
        }

        if (!elemStack.isEmpty()) {
            // Descend to the next leaf node element
            while (true) {
                if (elemStack.peek() instanceof Leaf) {
                    // Done - reached the next element
                    break;
                } else {
                    // Advance to the next child of this node
                    ArtNode cur = (ArtNode)elemStack.peek();
                    idxStack.push(cur.nextChildAtOrAfter(idxStack.pop()));
                    Node child = cur.childAt(idxStack.peek());

                    // Push it onto the stack
                    elemStack.push(child);
                    idxStack.push(0);
                }
            }
        }
    }
}
