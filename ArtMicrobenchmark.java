package com.ankurdave.part;

import java.util.Random;

public class ArtMicrobenchmark {

    private static final int m_0 = 10000000;
    private static final int T =   1000000;
    private static final int key_len = 4;
    private static final int value_len = 4;
    private static final long max_duration = (long)5 * 1000 * 1000 * 1000;
    private static Random r = new Random();

    private static byte[] gen_key() {
        byte[] str = new byte[key_len];
        for (int j = 0; j < key_len; j++) {
            str[j] = (byte)r.nextInt(256);
        }
        return str;
    }

    private static byte[] gen_value() {
        byte[] value = new byte[value_len];
        for (int j = 0; j < value_len; j++) {
            value[j] = (byte)r.nextInt(256);
        }
        return value;
    }

    private static void increment_value(byte[] value) {
        for (int j = value_len - 1; j >= 0; j--) {
            if (value[j] < 127) {
                value[j]++;
                break;
            }
        }
    }

    private static int to_uint(byte b) {
        return ((int)b) & 0xFF;
    }

    private static class Test implements IterCallback {
        private int n = 0;
        private long sum = 0;
        @Override public void apply(final byte[] key, Object value) {
            n++;
            sum += to_uint(key[0]) + to_uint(((byte[])value)[0]);
        }
    }

    public static void main(String[] args) {
        long sum = 0;

        ArtTree art = new ArtTree();
        String label = "jart";
        String label_clone = "jpart";

        for (int i = 0; i < m_0; i++) {
            art.insert(gen_key(), gen_value());
        }

        {
            Runtime runtime = Runtime.getRuntime();
            System.out.println("{'measurement': 'memory', 'datastructure': '" + label
                               + "', 'y': " + (runtime.totalMemory() - runtime.freeMemory())
                               + ", 'valsize': " + value_len + "},");
        }

        {
            long begin = System.nanoTime();
            for (int iter = 0; iter < T; iter++) {
                byte[] str = gen_key();
                byte[] result = (byte[])art.search(str);
                if (result != null) {
                    sum += to_uint(result[0]);
                }
            }
            long end = System.nanoTime();
            long ns = end - begin;
            double rate = T / ((double)ns / (1000 * 1000 * 1000));
            System.out.println("{'measurement': 'read', 'datastructure': '" + label
                               + "', 'y': " + rate + ", 'valsize': "
                               + value_len + "},");
            System.out.println(sum);
        }

        {
            int n = 0;
            Test t = new Test();
            long begin = System.nanoTime();
            art.iter(t);
            n = t.n;
            sum += t.sum;
            long end = System.nanoTime();
            long ns = end - begin;
            double rate = n / ((double)ns / (1000 * 1000 * 1000));
            System.out.println("{'measurement': 'scan', 'datastructure': '" + label
                               + "', 'y': " + rate + ", 'valsize': "
                               + value_len + "},");
        }

        {
            for (int m = 1; m <= m_0; m *= 10) {
                int insertions = 0, updates = 0;
                long ns = 0;
                long allocd = 0;
                int num_trials = 0;
                while (ns < max_duration) {
                    long begin = System.nanoTime();
                    ArtTree art2 = art.snapshot();
                    for (int i = 0; i < m; i++) {
                        byte[] str = gen_key();
                        byte[] result = (byte[])art2.search(str);
                        if (result == null) {
                            art2.insert(str, gen_value());
                            insertions++;
                        } else {
                            increment_value(result);
                            updates++;
                        }
                    }
                    ns += System.nanoTime() - begin;
                    allocd += art2.destroy();
                    num_trials++;
                }
                double rate = num_trials * m / ((double)ns / (1000 * 1000 * 1000));
                System.out.println("{'measurement': 'insert', 'datastructure': '" + label_clone
                                   + "', 'x': " + m + ", 'y': " + rate + ", 'valsize': "
                                   + value_len + ", 'inplace': False},");
                // << " (insert: " << insertions << ", update: " << updates
                // << ", alloc'd bytes per elem: " << (double)allocd / (num_trials * m)
                // << ")"
            }
        }
        {
            for (int m = 1; m <= m_0; m *= 10) {
                int insertions = 0, updates = 0;
                long ns = 0;
                int num_trials = 0;
                while (ns < max_duration) {
                    long begin = System.nanoTime();
                    for (int i = 0; i < m; i++) {
                        byte[] str = gen_key();
                        byte[] result = (byte[])art.search(str);
                        if (result == null) {
                            art.insert(str, gen_value());
                            insertions++;
                        } else {
                            increment_value(result);
                            updates++;
                        }
                    }
                    ns += System.nanoTime() - begin;
                    num_trials++;
                }
                double rate = num_trials * m / ((double)ns / (1000 * 1000 * 1000));
                System.out.println("{'measurement': 'insert', 'datastructure': '" + label
                                   + "', 'x': " + m + ", 'y': " + rate + ", 'valsize': "
                                   + value_len + ", 'inplace': True},");
            }
        }


        {
            for (int t = 0; t < 10000; t++) {
                long begin = System.nanoTime();
                ArtTree art2 = art.snapshot();
                for (int i = 0; i < 1000; i++) {
                    byte[] str = gen_key();
                    byte[] result = (byte[])art2.search(str);
                    if (result == null) {
                        art2.insert(str, gen_value());
                    } else {
                        increment_value(result);
                    }
                }
                art2.destroy();
                System.out.println("{'measurement': 'gc', 'datastructure': '" + label
                                   + "', 'x': " + t + ", 'y': " + (System.nanoTime() - begin)
                                   + ", 'valsize': " + value_len + ", 'inplace': False},");
            }
        }

//     {
//         int art_size = art.destroy();
//         std::cout << "art size " << art_size << std::endl;
//     }

        System.out.println(sum);
    }
}
