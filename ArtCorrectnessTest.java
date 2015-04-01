package com.ankurdave.part;

import java.util.Random;
import java.util.ArrayList;

public class ArtCorrectnessTest {
    private static int m_0 = 100;
    private static int key_len = 10;
    private static Random r = new Random();

    private static void print_bytes(final byte[] b) {
        for (int i = 0; i < b.length; i++) {
            System.out.print((int)b[i] + " ");
        }
    }

    private static byte[][] key_list() {
        byte[][] a = new byte[m_0][key_len];
        for (int i = 0; i < m_0; i++) {
            for (int j = 0; j < key_len; j++) {
                a[i][j] = (byte)r.nextInt(256);
            }
        }
        return a;
    }

    private static class Test implements IterCallback {
        private int n = 0;
        @Override public void apply(final byte[] key, Object value) {
            n++;
        }
    }

    public static void main(String[] args) {
        byte[][] a = key_list();
        byte[][] b = key_list();

        ArtTree a_map = new ArtTree();
        for (int i = 0; i < a.length; i++)
            a_map.insert(a[i], 1);
        ArtTree b_map = new ArtTree();
        for (int i = 0; i < a.length; i++)
            b_map.insert(b[i], 2);

        for (int i = 0; i < a.length; i++) {
            assert(a_map.search(a[i]) == 1);
            Integer b_res = (Integer)b_map.search(a[i]);
            assert(b_res == null || b_res == 2);
        }

        for (int i = 0; i < b.length; i++) {
            Integer a_res = (Integer)a_map.search(b[i]);
            assert(a_res == null || a_res == 1);
            assert(b_map.search(b[i]) == 2);
        }

        System.out.println("Leaf: " + Leaf.count + ", "
                           + "Node4: " + ArtNode4.count + ", "
                           + "Node16: " + ArtNode16.count + ", "
                           + "Node48: " + ArtNode48.count + ", "
                           + "Node256: " + ArtNode256.count);

        ArtTree union_map = a_map.snapshot();
        for (int i = 0; i < b.length; i++)
            union_map.insert(b[i], 3);

        System.out.println("Leaf: " + Leaf.count + ", "
                           + "Node4: " + ArtNode4.count + ", "
                           + "Node16: " + ArtNode16.count + ", "
                           + "Node48: " + ArtNode48.count + ", "
                           + "Node256: " + ArtNode256.count);

        for (int i = 0; i < a.length; i++) {
            assert(a_map.search(a[i]) == 1);
            Integer b_res = (Integer)b_map.search(a[i]);
            assert(b_res == null || b_res == 2);
            assert(union_map.search(a[i]) == 1);
        }

        for (int i = 0; i < b.length; i++) {
            Integer a_res = (Integer)a_map.search(b[i]);
            assert(a_res == null || a_res == 1);
            assert(b_map.search(b[i]) == 2);
            assert(union_map.search(b[i]) == 3);
        }

        // Test iteration
        Test t = new Test();
        union_map.iter(t);
        assert(t.n == m_0 * 2);

        System.out.println("union_map had size " + union_map.destroy());

        System.out.println("Leaf: " + Leaf.count + ", "
                           + "Node4: " + ArtNode4.count + ", "
                           + "Node16: " + ArtNode16.count + ", "
                           + "Node48: " + ArtNode48.count + ", "
                           + "Node256: " + ArtNode256.count);

        for (int i = 0; i < a.length; i++) {
            assert(a_map.search(a[i]) == 1);
            Integer b_res = (Integer)b_map.search(a[i]);
            assert(b_res == null || b_res == 2);
        }

        for (int i = 0; i < b.length; i++) {
            Integer a_res = (Integer)a_map.search(b[i]);
            assert(a_res == null || a_res == 1);
            assert(b_map.search(b[i]) == 2);
        }

        System.out.println("a_map had size " + a_map.destroy());
        System.out.println("Leaf: " + Leaf.count + ", "
                           + "Node4: " + ArtNode4.count + ", "
                           + "Node16: " + ArtNode16.count + ", "
                           + "Node48: " + ArtNode48.count + ", "
                           + "Node256: " + ArtNode256.count);

        System.out.println("b_map had size " + b_map.destroy());
        System.out.println("Leaf: " + Leaf.count + ", "
                           + "Node4: " + ArtNode4.count + ", "
                           + "Node16: " + ArtNode16.count + ", "
                           + "Node48: " + ArtNode48.count + ", "
                           + "Node256: " + ArtNode256.count);
    }
}
