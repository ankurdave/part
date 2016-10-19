#include <algorithm>
#include <cassert>
#include <chrono>
#include <iostream>
#include <string>
#include <vector>
#include "getRSS.c"

#ifdef HT
#include <unordered_map>
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

#ifdef ZIPF
#include "discreteZipf.cpp"
#endif

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
#ifndef VECTOR
const int T =   1000000;
const long max_duration = (long)5 * 1000 * 1000 * 1000;
#endif
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

unsigned char* string_to_art_key(std::string& str) {
    unsigned char* result = new unsigned char[KEY_LEN];
    const unsigned char* c_str = reinterpret_cast<const unsigned char*>(str.c_str());
    for (int j = 0; j < KEY_LEN; j++) {
        result[j] = c_str[j];
    }
    return result;
}

unsigned char* gen_existing_key(std::vector<std::string>& vector_k) {
#ifdef RANDOM
    return string_to_art_key(vector_k[rand() % vector_k.size()]);
#endif
#ifdef SEQUENTIAL
    unsigned char* str = new unsigned char[KEY_LEN];
    for (int j = 0; j < KEY_LEN; j++) {
        str[j] = reinterpret_cast<const unsigned char*>(&cur_seq_key)[j];
    }
    cur_seq_key++;
    return str;
#endif
#ifdef ZIPF
    unsigned char* str = new unsigned char[KEY_LEN];
    for (int j = 0; j < KEY_LEN; j++) {
        str[j] = static_cast<unsigned char>(z.next());
    }
    return str;
#endif
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

std::string gen_existing_string_key(std::vector<std::string>& vector_k) {
#ifdef RANDOM
    return vector_k[rand() % vector_k.size()];
#endif
#ifdef SEQUENTIAL
    std::string str(KEY_LEN, 0);
    for (int j = 0; j < KEY_LEN; j++) {
        str[j] = reinterpret_cast<const char*>(&cur_seq_string_key)[j];
    }
    cur_seq_string_key++;
    return str;
#endif
#ifdef ZIPF
    std::string str(KEY_LEN, 0);
    for (int j = 0; j < KEY_LEN; j++) {
        str[j] = static_cast<char>(z.next());
    }
    return str;
#endif
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
    static volatile long sum;
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

inline int max(int a, int b) { return (a > b) ? a : b; }

int Test::n = 0;
volatile long Test::sum = 0;

int main() {
    volatile long sum = 0;

    std::vector<std::string> vector_k(m_0);
    std::vector<VAL_TYPE> vector_v(m_0);

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
    auto label = "vector";
#endif
    for (int i = 0; i < m_0; i++) {
        vector_k[i] = gen_string_key();
        vector_v[i] = gen_value();
#ifdef ART
        art.insert(string_to_art_key(vector_k[i]), KEY_LEN, gen_value());
#endif
#ifdef STDMAP
        map.insert(std::make_pair(vector_k[i], gen_value()));
#endif
    }

    std::cout << "{'measurement': 'memory', 'datastructure': '" << label
              << "', 'y': " << getCurrentRSS() << ", 'valsize': "
              << value_len << "}," << std::endl;

#ifdef ART_REORDER_LEAVES
    art.reorder_leaves();
#endif

    {
        int n = 0;
        auto begin = std::chrono::high_resolution_clock::now();
#ifdef ART
        art.iter(&Test::iter_test);
        n = Test::n;
#endif
#ifdef STDMAP
        for (auto it = map.begin(); it != map.end(); it++) {
            n++;
            sum += it->first[0] +
#ifdef BIG_VAL
                it->second[0]
#endif
#ifndef BIG_VAL
                it->second
#endif
                ;
        }
#endif
#ifdef VECTOR
        for (std::vector<std::string>::size_type i = 0; i < vector_k.size(); i++) {
            n++;
            sum += vector_k[i][0] +
#ifdef BIG_VAL
                vector_v[i][0]
#endif
#ifndef BIG_VAL
                vector_v[i]
#endif
                ;
        }
#endif
        auto end = std::chrono::high_resolution_clock::now();
        auto dur = end - begin;
        auto ns = std::chrono::duration_cast<std::chrono::nanoseconds>(dur).count();
        auto rate = n / ((double)ns / (1000 * 1000 * 1000));
        std::cout << "{'measurement': 'scan', 'datastructure': '" << label
                  << "', 'y': " << rate << ", 'valsize': "
                  << value_len << "}," << std::endl;
    }

#ifndef VECTOR
    {
        auto begin = std::chrono::high_resolution_clock::now();
        int num_successes = 0;
        int num_failures = 0;
        for (int iter = 0; iter < T; iter++) {
#ifdef ART
            unsigned char* str = gen_existing_key(vector_k);
            VAL_TYPE* result = art.search(str, KEY_LEN);
            if (result) {
#ifdef BIG_VAL
                sum += (*result)[0];
#endif
#ifndef BIG_VAL
                sum += *result;
#endif
                num_successes++;
            } else {
                num_failures++;
            }
            delete[] str;
#endif
#ifdef STDMAP
            std::string str = gen_existing_string_key(vector_k);
            auto it = map.find(str);
            if (it != map.end()) {
#ifdef BIG_VAL
                sum += it->second[0];
#endif
#ifndef BIG_VAL
                sum += it->second;
#endif
                num_successes++;
            } else {
                num_failures++;
            }
#endif
        }
        auto end = std::chrono::high_resolution_clock::now();
        auto dur = end - begin;
        auto ns = std::chrono::duration_cast<std::chrono::nanoseconds>(dur).count();
        auto rate = T / ((double)ns / (1000 * 1000 * 1000));
        std::cout << "{'measurement': 'read', 'datastructure': '" << label
                  << "', 'y': " << rate << ", 'valsize': "
                  << value_len << ", 'num_successes': " << num_successes
                  << ", 'num_failures': " << num_failures << "}," << std::endl;
    }
#endif

#ifndef VECTOR
    {
            int insertions = 0, updates = 0;
            long ns = 0;
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
                art2.destroy();
#endif
                num_trials++;
            }
            auto rate = num_trials * BATCH_SIZE / ((double)ns / (1000 * 1000 * 1000));
            std::cout << "{'measurement': 'insert', 'datastructure': '" << label_clone
                      << "', 'x': " << BATCH_SIZE << ", 'y': " << rate << ", 'valsize': "
                      << value_len << ", 'inplace': False}," << std::endl;
    }
    {
            int insertions = 0, updates = 0;
            long ns = 0;
            int num_trials = 0;
            while (ns < max_duration) {
                auto begin = std::chrono::high_resolution_clock::now();
                for (int i = 0; i < T; i++) {
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
            auto rate = num_trials * T / ((double)ns / (1000 * 1000 * 1000));
            std::cout << "{'measurement': 'insert', 'datastructure': '" << label
                      << "', 'y': " << rate << ", 'valsize': "
                      << value_len << ", 'inplace': True}," << std::endl;
    }
#endif
}
