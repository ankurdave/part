package com.ankurdave.part;

import java.util.Random;
import java.util.Iterator;
import scala.Tuple2;

public class ArtCorrectnessTest {
    private static int m_0 = 100;
    private static int key_len = 10;
    private static Random r = new Random();

    private static void print_bytes(final byte[] b) {
        for (int i = 0; i < b.length; i++) {
            System.out.print((int)b[i] + " ");
        }
        System.out.println();
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

    private static int to_i(Object o) {
        return ((Integer)o).intValue();
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
            assert(to_i(a_map.search(a[i])) == 1);
            Integer b_res = (Integer)b_map.search(a[i]);
            assert(b_res == null || to_i(b_res) == 2);
        }

        for (int i = 0; i < b.length; i++) {
            Integer a_res = (Integer)a_map.search(b[i]);
            assert(a_res == null || to_i(a_res) == 1);
            assert(to_i(b_map.search(b[i])) == 2);
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
            assert(to_i(a_map.search(a[i])) == 1);
            Integer b_res = (Integer)b_map.search(a[i]);
            assert(b_res == null || to_i(b_res) == 2);
            assert(to_i(union_map.search(a[i])) == 1);
        }

        for (int i = 0; i < b.length; i++) {
            Integer a_res = (Integer)a_map.search(b[i]);
            assert(a_res == null || to_i(a_res) == 1);
            assert(to_i(b_map.search(b[i])) == 2);
            assert(to_i(union_map.search(b[i])) == 3);
        }

        // Test iteration
        Test t = new Test();
        union_map.iter(t);
        assert(t.n == m_0 * 2);

        Iterator<Tuple2<byte[], Object>> it = union_map.iterator();
        int n2 = 0;
        while (it.hasNext()) {
            Tuple2<byte[], Object> elem = it.next();
            n2++;
        }
        assert(n2 == t.n);

        System.out.println("union_map had size " + union_map.destroy());

        System.out.println("Leaf: " + Leaf.count + ", "
                           + "Node4: " + ArtNode4.count + ", "
                           + "Node16: " + ArtNode16.count + ", "
                           + "Node48: " + ArtNode48.count + ", "
                           + "Node256: " + ArtNode256.count);

        for (int i = 0; i < a.length; i++) {
            assert(to_i(a_map.search(a[i])) == 1);
            Integer b_res = (Integer)b_map.search(a[i]);
            assert(b_res == null || to_i(b_res) == 2);
        }

        for (int i = 0; i < b.length; i++) {
            Integer a_res = (Integer)a_map.search(b[i]);
            assert(a_res == null || to_i(a_res) == 1);
            assert(to_i(b_map.search(b[i])) == 2);
        }

        ArtTree deletion_map = a_map.snapshot();
        // for (int i = 0; i < b.length; i++)
        //     deletion_map.insert(b[i], 3);
        for (int i = 0; i < a.length; i++)
            deletion_map.delete(a[i]);
        assert(deletion_map.root == null);

        for (int i = 0; i < a.length; i++) {
            assert(to_i(a_map.search(a[i])) == 1);
            Integer b_res = (Integer)b_map.search(a[i]);
            assert(b_res == null || to_i(b_res) == 2);
            assert(deletion_map.search(a[i]) == null);
        }

        for (int i = 0; i < b.length; i++) {
            Integer a_res = (Integer)a_map.search(b[i]);
            assert(a_res == null || to_i(a_res) == 1);
            assert(to_i(b_map.search(b[i])) == 2);
            assert(to_i(deletion_map.search(b[i])) == 3);
        }

        System.out.println("deletion_map had size " + deletion_map.destroy());

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
