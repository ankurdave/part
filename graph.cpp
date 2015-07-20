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

struct Edge {
    long srcId;
    long dstId;
    double attr;
};

class Main {
public:
    int num_edges, num_vertices;

    std::vector<Edge> edges;
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

            Edge e;
            e.srcId = src;
            e.dstId = dst;
            e.attr = 1.0;
            edges.push_back(e);
        }

        num_vertices = voffset;
        vertex_preagg = std::vector<double>(num_vertices, 0.0);
    }

    void scan() {
        printf("Scanning...\n");


        clock_t start_time = clock();

        for (int i = 0; i < num_edges; i++) {
            Edge& e = edges[i];
            unsigned char* src_bytes = reinterpret_cast<unsigned char*>(&e.srcId);
            unsigned char* dst_bytes = reinterpret_cast<unsigned char*>(&e.dstId);
            double src_attr = *vertices.search(src_bytes, 8);
            vertex_preagg[*vertex_offsets.search(dst_bytes, 8)] += src_attr * e.attr;
        }

        clock_t end_time = clock();
        printf("Scanned %d edges in %f seconds\n",
               num_edges, (end_time - start_time) / static_cast<double>(CLOCKS_PER_SEC));

    }
};

int main(int argc, char* argv[]) {
    (void)argc;
    Main m(atoi(argv[1]));
    m.scan();
    return 0;
}
