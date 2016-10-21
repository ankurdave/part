#include <iostream>
#include <chrono>
#include "art.cpp"
#include "discreteZipf.cpp"

const int m = 100000;
const int m_0 = 10000000;
const int T = 20;
const int failure_batch = 10;
const int key_len = 4;
#ifdef BATCHING
const bool batching_is_on = true;
#else
const bool batching_is_on = false;
#endif


// Simulation of fault recovery on one partition, and impact of adaptive batch size. Each partition
// updates keys based on each batch of input. The input is a series of words which are power-law
// distributed. For each word, there is a constant number of keys updated, which are uniformly
// distributed. When a failure occurs, the checkpoint must be loaded and 10 batches of updates must
// be applied. In one scenario it applies these separately, while in the other it applies them
// together in the same batch.

// clang++ -Wall -Wextra -O3 -c -std=c++11 -stdlib=libc++ art.cpp -o art.o && clang++ -DBATCHING
// -Wall -Wextra -O3 -std=c++11 -stdlib=libc++ -o ArtRecoveryBenchmark art.o
// ArtRecoveryBenchmark.cpp && ./ArtRecoveryBenchmark

unsigned char* gen_key() {
    unsigned char* str = new unsigned char[key_len];
    for (int j = 0; j < key_len; j++) {
        str[j] = static_cast<unsigned char>(rand() % 256);
    }
    return str;
}

int main() {
    ArtTree<long> map;
    for (int i = 0; i < m_0; i++) {
        map.insert(gen_key(), key_len, 1);
    }

    printf("# batching_is_on, batch_number, time\n");
    for (int iter = 0; iter < T; iter++) {
        auto begin = std::chrono::high_resolution_clock::now();

        auto map_old = map.snapshot();
        for (int i = 0; i < m; i++) {
            unsigned char* str = gen_key();
            long* result = map.search(str, key_len);
            if (result == NULL) {
                map.insert(str, key_len, 1);
            } else {
                delete[] str;
                (*result)++;
            }
        }
        map_old.destroy();

        if (iter == failure_batch) {
            if (batching_is_on) {
                for (int i = 0; i < m * 10; i++) {
                    unsigned char* str = gen_key();
                    long* result = map.search(str, key_len);
                    if (result == NULL) {
                        map.insert(str, key_len, 1);
                    } else {
                        delete[] str;
                        (*result)++;
                    }
                }
            } else {
                // Apply 10 batches sequentially
                for (int sub_iter = 0; sub_iter < 10; sub_iter++) {
                    auto sub_map_old = map.snapshot();
                    for (int i = 0; i < m; i++) {
                        unsigned char* str = gen_key();
                        long* result = map.search(str, key_len);
                        if (result == NULL) {
                            map.insert(str, key_len, 1);
                        } else {
                            delete[] str;
                            (*result)++;
                        }
                    }
                    sub_map_old.destroy();
                }
            }
        }
        auto end = std::chrono::high_resolution_clock::now();
        auto dur = end - begin;
        auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(dur).count();
        printf("%d,%d,%f\n", batching_is_on, iter, (float)ms);
    }
}
