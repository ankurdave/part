#include <cstdlib>
#include <unordered_map>
#include <vector>
#include <ctime>
#include <functional>
#include <iostream>
#include "art.cpp"

// Usage:
// g++ -Wall -O3 graph.cpp -o graph
// clang++ -Wall -Wextra -O3 -std=c++11 -stdlib=libc++ graph.cpp -o graph
// ./graph 14167504 < ~/Downloads/uk-2007-05-coalesced-part-00137

class Main {
public:
    int num_edges, num_vertices;

    ArtTree<ArtTree<double>*> edges;
    ArtTree<double> vertices;
    ArtTree<int> vertex_offsets;

    std::vector<double> vertex_preagg;

    Main(int num_edges) : num_edges(num_edges), edges(), vertices(),
                          vertex_offsets() {
        printf("Loading...\n");

        long src, dst;
        int voffset = 0;
        for (int i = 0; i < num_edges; i++) {
            scanf("%ld\t%ld", &src, &dst);

            unsigned char* src_bytes = new unsigned char[8];
            for (int i = 0; i < 8; i++) src_bytes[i] = reinterpret_cast<unsigned char*>(&src)[i];
            unsigned char* dst_bytes = new unsigned char[8];
            for (int i = 0; i < 8; i++) dst_bytes[i] = reinterpret_cast<unsigned char*>(&dst)[i];
            // TODO: avoid leaking these

            if (vertex_offsets.search(src_bytes, 8) == NULL) {
                vertex_offsets.insert(src_bytes, 8, voffset++);
                vertices.insert(src_bytes, 8, 1.0);
            }

            if (vertex_offsets.search(dst_bytes, 8) == NULL) {
                vertex_offsets.insert(dst_bytes, 8, voffset++);
                vertices.insert(dst_bytes, 8, 1.0);
            }

            ArtTree<double>** cluster = edges.search(src_bytes, 8);
            if (cluster == NULL) {
                ArtTree<double>* new_cluster = new ArtTree<double>;
                new_cluster->insert(dst_bytes, 8, 1.0);
                edges.insert(src_bytes, 8, new_cluster);
            } else {
                (*cluster)->insert(dst_bytes, 8, 1.0);
            }
        }

        num_vertices = voffset;
        vertex_preagg = std::vector<double>(num_vertices, 0.0);
    }

    void scan() {
        printf("Scanning...\n");


        clock_t start_time = clock();

        edges.iter(std::bind(&Main::scan_outer, this,
                             std::placeholders::_1,
                             std::placeholders::_2,
                             std::placeholders::_3));

        clock_t end_time = clock();
        printf("Scanned %d edges in %f seconds\n",
               num_edges, (end_time - start_time) / static_cast<double>(CLOCKS_PER_SEC));

    }

    void scan_outer(const unsigned char* key, uint32_t key_len,
                    ArtTree<double>* inner) {
        long src_id = *reinterpret_cast<const long*>(key);
        inner->iter(std::bind(&Main::scan_inner, this,
                              src_id,
                              *vertices.search(key, key_len),
                              std::placeholders::_1,
                              std::placeholders::_2,
                              std::placeholders::_3));

    }

    void scan_inner(long src_id, double src_attr, const unsigned char* key,
                    uint32_t key_len, double edge_attr) {
        (void)src_id;
        vertex_preagg[*vertex_offsets.search(key, key_len)] += src_attr * edge_attr;
    }
};

int main(int argc, char* argv[]) {
    (void)argc;
    Main m(atoi(argv[1]));
    m.scan();
    return 0;
}
