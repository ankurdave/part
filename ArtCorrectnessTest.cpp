#include <iostream>

#include <string>
#include <algorithm>
#include <vector>
#include <chrono>
#include <cassert>

#include "art.cpp"
// clang++ -O3 -c -std=c++11 -stdlib=libc++ art.cpp -o art.o && clang++ -O3 -std=c++11 -stdlib=libc++ -o ArtCorrectnessTest art.o ArtCorrectnessTest.cpp && ./ArtCorrectnessTest

int m_0 = 100;
int key_len = 10;

void print_bytes(const unsigned char* b) {
  for (int i = 0; i < key_len; i++) {
    std::cout << (int)b[i] << " ";
  }
}

std::vector<unsigned char*> key_list() {
  std::vector<unsigned char*> a;
  for (int i = 0; i < m_0; i++) {
    auto str = new unsigned char[key_len];
    for (int j = 0; j < key_len; j++) {
        str[j] = static_cast<unsigned char>(rand() % 256);
    }
    a.push_back(str);
  }
  return a;
}

class Test {
public:
  static int n;
  static void iter_test(const unsigned char* key, uint32_t key_len, long value) {
      (void)key;
      (void)key_len;
      (void)value;
      // print_bytes(key);
      // std::cout << std::endl;
      n++;
  }
};

int Test::n = 0;

int main()
{
  auto a = key_list();
  auto b = key_list();

  ArtTree<int> a_map;
  for (auto it = a.begin(); it != a.end(); it++) {
    a_map.insert(*it, key_len, 1);
  }
  ArtTree<int> b_map;
  for (auto it = b.begin(); it != b.end(); it++)
    b_map.insert(*it, key_len, 2);

  for (auto it = a.begin(); it != a.end(); it++) {
      assert(*a_map.search(*it, key_len) == 1);
      auto b_res = b_map.search(*it, key_len);
      assert(b_res == NULL || *b_res == 2);
  }

  for (auto it = b.begin(); it != b.end(); it++) {
    auto a_res = a_map.search(*it, key_len);
    assert(a_res == NULL || *a_res == 1);
    assert(*b_map.search(*it, key_len) == 2);
  }

  std::cout << "Leaf: " << Leaf<int>::count << ", "
            << "Node4: " << ArtNode4<int>::count << ", "
            << "Node16: " << ArtNode16<int>::count << ", "
            << "Node48: " << ArtNode48<int>::count << ", "
            << "Node256: " << ArtNode256<int>::count << std::endl;

  ArtTree<int> union_map = a_map.snapshot();
  for (auto it = b.begin(); it != b.end(); it++)
    union_map.insert(*it, key_len, 3);

  std::cout << "Leaf: " << Leaf<int>::count << ", "
            << "Node4: " << ArtNode4<int>::count << ", "
            << "Node16: " << ArtNode16<int>::count << ", "
            << "Node48: " << ArtNode48<int>::count << ", "
            << "Node256: " << ArtNode256<int>::count << std::endl;

  for (auto it = a.begin(); it != a.end(); it++) {
    assert(*a_map.search(*it, key_len) == 1);
    auto b_res = b_map.search(*it, key_len);
    assert(b_res == NULL || *b_res == 2);
    assert(*union_map.search(*it, key_len) == 1);
  }

  for (auto it = b.begin(); it != b.end(); it++) {
    auto a_res = a_map.search(*it, key_len);
    assert(a_res == NULL || *a_res == 1);
    assert(*b_map.search(*it, key_len) == 2);
    assert(*union_map.search(*it, key_len) == 3);
  }

  // Test iteration
  union_map.iter(&Test::iter_test);
  assert(Test::n == m_0 * 2);

  std::cout << "union_map had size " << union_map.destroy() << std::endl;

  std::cout << "Leaf: " << Leaf<int>::count << ", "
            << "Node4: " << ArtNode4<int>::count << ", "
            << "Node16: " << ArtNode16<int>::count << ", "
            << "Node48: " << ArtNode48<int>::count << ", "
            << "Node256: " << ArtNode256<int>::count << std::endl;

  for (auto it = a.begin(); it != a.end(); it++) {
    assert(*a_map.search(*it, key_len) == 1);
    auto b_res = b_map.search(*it, key_len);
    assert(b_res == NULL || *b_res == 2);
  }

  for (auto it = b.begin(); it != b.end(); it++) {
    auto a_res = a_map.search(*it, key_len);
    assert(a_res == NULL || *a_res == 1);
    assert(*b_map.search(*it, key_len) == 2);
  }

  // ArtTree<int> a2_map = a_map.snapshot();
  // auto it = b.begin();
  // a2_map.insert(*it, key_len, 4);
  // std::cout << "a_map checkpoint size: " << a_map.checkpoint() << "\n";
  // std::cout << "a2_map checkpoint size: " << a2_map.checkpoint() << "\n";
  // std::cout << "a2_map checkpoint size: " << a2_map.checkpoint() << "\n";
  // std::cout << "a2_map max depth: " << a2_map.max_depth() << "\n";

  std::cout << "a_map had size " << a_map.destroy() << std::endl;
  std::cout << "Leaf: " << Leaf<int>::count << ", "
            << "Node4: " << ArtNode4<int>::count << ", "
            << "Node16: " << ArtNode16<int>::count << ", "
            << "Node48: " << ArtNode48<int>::count << ", "
            << "Node256: " << ArtNode256<int>::count << std::endl;

  std::cout << "b_map had size " << b_map.destroy() << std::endl;
  std::cout << "Leaf: " << Leaf<int>::count << ", "
            << "Node4: " << ArtNode4<int>::count << ", "
            << "Node16: " << ArtNode16<int>::count << ", "
            << "Node48: " << ArtNode48<int>::count << ", "
            << "Node256: " << ArtNode256<int>::count << std::endl;

  for (auto it = a.begin(); it != a.end(); it++)
      delete[] *it;
  for (auto it = b.begin(); it != b.end(); it++)
      delete[] *it;
}
