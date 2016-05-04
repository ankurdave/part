#include <algorithm>
#include <cassert>
#include <chrono>
#include <iostream>
#include <string>
#ifdef HT
#include <unordered_map>
#endif
#ifdef VECTOR
#include <vector>
#endif
#ifdef ART
#include "art.cpp"
#endif
#ifdef RB
#include <map>
#endif
#ifdef BTREE
#include "btree_map.h"
#endif
#include "getRSS.c"
#ifdef ZIPF
#include "discreteZipf.cpp"
#endif
// clang++ -DART -Wall -Wextra -Wno-invalid-offsetof -O3 -std=c++11 -stdlib=libc++ -o ArtMicrobenchmark -g ArtMicrobenchmark.cpp && ./ArtMicrobenchmark

// Preprocessor macros - define using -D
// data structure: {ART, HT, RB, BTREE}
// batch size (int): BATCH_SIZE
// value size: {BIG_VAL,}
// key length (int in [4, 32]): KEY_LEN
// key distribution: {RANDOM, SEQUENTIAL, ZIPF}
// Zipf distribution skew, if ZIPF is defined (double in [1.0, 3.0]): ZIPF_ALPHA

#if defined(HT) || defined(RB) || defined(BTREE)
#define STDMAP
#endif

#ifdef BIG_VAL
#define VAL_TYPE unsigned char*
const int value_len = 1024;
#endif
#ifndef BIG_VAL
#define VAL_TYPE int
const int value_len = 4;
#endif

const int m_0 = 10000000;
const int T =   1000000;
const long max_duration = (long)5 * 1000 * 1000 * 1000;
long cur_seq_key = 0;
long cur_seq_string_key = 0;
#ifdef ZIPF
discreteZipf z(ZIPF_ALPHA, 256);
#endif

unsigned char* gen_key() {
    unsigned char* str = new unsigned char[KEY_LEN];
#ifdef RANDOM
    for (int j = 0; j < KEY_LEN; j++) {
        str[j] = static_cast<unsigned char>(rand() % 256);
    }
#endif
#ifdef SEQUENTIAL
    for (int j = 0; j < KEY_LEN; j++) {
        str[j] = reinterpret_cast<const unsigned char*>(&cur_seq_key)[j];
    }
    cur_seq_key++;
#endif
#ifdef ZIPF
    for (int j = 0; j < KEY_LEN; j++) {
        str[j] = static_cast<unsigned char>(z.next());
    }
#endif
    return str;
}

std::string gen_string_key() {
    std::string str(KEY_LEN, 0);
#ifdef RANDOM
    for (int j = 0; j < KEY_LEN; j++) {
        str[j] = static_cast<char>(rand() % 256);
    }
#endif
#ifdef SEQUENTIAL
    for (int j = 0; j < KEY_LEN; j++) {
        str[j] = reinterpret_cast<const char*>(&cur_seq_string_key)[j];
    }
    cur_seq_string_key++;
#endif
#ifdef ZIPF
    for (int j = 0; j < KEY_LEN; j++) {
        str[j] = static_cast<char>(z.next());
    }
#endif
    return str;
}

VAL_TYPE gen_value() {
#ifdef BIG_VAL
    unsigned char* value = new unsigned char[value_len];
    for (int j = 0; j < value_len; j++) {
        value[j] = static_cast<unsigned char>(rand() % 256);
    }
    return value;
#endif
#ifndef BIG_VAL
    return rand();
#endif
}

#ifdef BIG_VAL
#define increment_value(value) { \
    for (int j = value_len - 1; j >= 0; j--) { \
        if (value[j] < 255) { \
            value[j]++; \
            break; \
        } \
    } \
}
#endif
#ifndef BIG_VAL
#define increment_value(value) value++;
#endif

class Test {
public:
    static int n;
    static long sum;
    static void iter_test(const unsigned char* key, uint32_t key_len, VAL_TYPE value) {
        (void)key_len;
        n++;
        sum += key[0] +
#ifdef BIG_VAL
            value[0]
#endif
#ifndef BIG_VAL
            value
#endif
            ;
    }
};

void scan(ArtTree<VAL_TYPE> art, std::string label) {
    int n = 0;
    auto begin = std::chrono::high_resolution_clock::now();
    art.iter(&Test::iter_test);
    n = Test::n;
    auto end = std::chrono::high_resolution_clock::now();
    auto dur = end - begin;
    auto ns = std::chrono::duration_cast<std::chrono::nanoseconds>(dur).count();
    auto rate = n / ((double)ns / (1000 * 1000 * 1000));
    std::cout << "{'measurement': 'scan', 'datastructure': '" << label
              << "', 'y': " << rate << ", 'valsize': "
              << value_len << "}," << std::endl;
}

inline int max(int a, int b) { return (a > b) ? a : b; }

int Test::n = 0;
long Test::sum = 0;

int main() {
    long sum = 0;

#ifdef ART
    ArtTree<VAL_TYPE> art;
    auto label = "art";
    auto label_clone = "part";
#endif
#ifdef HT
    std::unordered_map<std::string, VAL_TYPE> map;
    auto label = "ht";
    auto label_clone = "pht";
#endif
#ifdef RB
    std::map<std::string, VAL_TYPE> map;
    auto label = "rb";
    auto label_clone = "prb";
#endif
#ifdef BTREE
    btree::btree_map<std::string, VAL_TYPE> map;
    auto label = "btree";
    auto label_clone = "pbtree";
#endif
#ifdef VECTOR
    std::vector<std::string> vector_k(m_0);
    std::vector<VAL_TYPE> vector_v(m_0);
    auto label = "vector";
    auto label_clone = "vector";
#endif
    for (int i = 0; i < m_0; i++) {
#ifdef ART
        art.insert(gen_key(), KEY_LEN, gen_value());
#endif
#ifdef STDMAP
        map.insert(std::make_pair(gen_string_key(), gen_value()));
#endif
#ifdef VECTOR
        vector_k[i] = gen_string_key();
        vector_v[i] = gen_value();
#endif
    }

    std::cout << "{'measurement': 'memory', 'datastructure': '" << label
              << "', 'y': " << getCurrentRSS() << ", 'valsize': "
              << value_len << "}," << std::endl;

#ifdef ART_REORDER_LEAVES
    art.reorder_leaves();
    // printf("Average stride: %f\n", art.avg_stride());
    // printf("size of leaf %d\n", sizeof(Leaf<VAL_TYPE>));
#endif

    scan(art, label);

#ifndef VECTOR
    {
        auto begin = std::chrono::high_resolution_clock::now();
        for (int iter = 0; iter < T; iter++) {
#ifdef ART
            unsigned char* str = gen_key();
            VAL_TYPE* result = art.search(str, KEY_LEN);
            if (result) {
#ifdef BIG_VAL
                sum += (*result)[0];
#endif
#ifndef BIG_VAL
                sum += *result;
#endif
            }
            delete[] str;
#endif
#ifdef STDMAP
            std::string str = gen_string_key();
            auto it = map.find(str);
            if (it != map.end()) {
#ifdef BIG_VAL
                sum += it->second[0];
#endif
#ifndef BIG_VAL
                sum += it->second;
#endif
            }
#endif
        }
        auto end = std::chrono::high_resolution_clock::now();
        auto dur = end - begin;
        auto ns = std::chrono::duration_cast<std::chrono::nanoseconds>(dur).count();
        auto rate = T / ((double)ns / (1000 * 1000 * 1000));
        std::cout << "{'measurement': 'read', 'datastructure': '" << label
                  << "', 'y': " << rate << ", 'valsize': "
                  << value_len << "}," << std::endl;
    }
#endif

#ifndef VECTOR
    {
            int insertions = 0, updates = 0;
            long ns = 0;
            long allocd = 0;
            int num_trials = 0;
            while (ns < max_duration) {
                auto begin = std::chrono::high_resolution_clock::now();
#ifdef ART
                auto art2 = art.snapshot();
#endif
#ifdef STDMAP
                auto map2 = map;
#endif
                for (int i = 0; i < BATCH_SIZE; i++) {
#ifdef ART
                    unsigned char* str = gen_key();
                    VAL_TYPE* result = art2.search(str, KEY_LEN);
                    if (result == NULL) {
                        art2.insert(str, KEY_LEN, gen_value());
                        insertions++;
                    } else {
                        delete[] str;
                        increment_value((*result));
                        updates++;
                    }
#endif
#ifdef STDMAP
                    std::string str = gen_string_key();
                    auto it = map2.find(str);
                    if (it == map2.end()) {
                        map2.insert(std::make_pair(str, gen_value()));
                        insertions++;
                    } else {
                        increment_value(it->second);
                        updates++;
                    }
#endif

                }
                ns += std::chrono::duration_cast<std::chrono::nanoseconds>(
                    std::chrono::high_resolution_clock::now() - begin).count();
#ifdef ART
                allocd += art2.destroy();
#endif
                num_trials++;
            }
            auto rate = num_trials * BATCH_SIZE / ((double)ns / (1000 * 1000 * 1000));
            std::cout << "{'measurement': 'insert', 'datastructure': '" << label_clone
                      << "', 'x': " << BATCH_SIZE << ", 'y': " << rate << ", 'valsize': "
                      << value_len << ", 'inplace': False}," << std::endl;
                      // << " (insert: " << insertions << ", update: " << updates
                      // << ", alloc'd bytes per elem: " << (double)allocd / (num_trials * m)
                      // << ")"
    }
    {
            int insertions = 0, updates = 0;
            long ns = 0;
            int num_trials = 0;
            while (ns < max_duration) {
                auto begin = std::chrono::high_resolution_clock::now();
                for (int i = 0; i < BATCH_SIZE; i++) {
#ifdef ART
                    unsigned char* str = gen_key();
                    VAL_TYPE* result = art.search(str, KEY_LEN);
                    if (result == NULL) {
                        art.insert(str, KEY_LEN, gen_value());
                        insertions++;
                    } else {
                        delete[] str;
                        increment_value((*result));
                        updates++;
                    }
#endif
#ifdef STDMAP
                    std::string str = gen_string_key();
                    auto it = map.find(str);
                    if (it == map.end()) {
                        map.insert(std::make_pair(str, gen_value()));
                        insertions++;
                    } else {
                        increment_value(it->second);
                        updates++;
                    }
#endif

                }
                ns += std::chrono::duration_cast<std::chrono::nanoseconds>(
                    std::chrono::high_resolution_clock::now() - begin).count();
                num_trials++;
            }
            auto rate = num_trials * BATCH_SIZE / ((double)ns / (1000 * 1000 * 1000));
            std::cout << "{'measurement': 'insert', 'datastructure': '" << label
                      << "', 'x': " << BATCH_SIZE << ", 'y': " << rate << ", 'valsize': "
                      << value_len << ", 'inplace': True}," << std::endl;
    }
#endif

// #ifdef ART
//     {
//         int art_size = art.destroy();
//         std::cout << "art size " << art_size << std::endl;
//     }
// #endif

#ifdef ART
    std::cout << "# " << Test::sum << std::endl;
#endif
    std::cout << "# " << sum << std::endl;
}
