#!/bin/bash

set -eu

# clang++ -DART -DBATCH_SIZE=1000 -DKEY_LEN=4 -DRANDOM -Wall -Wextra -Wno-invalid-offsetof -O3 -std=c++11 -stdlib=libc++ -o ArtMicrobenchmark -g ArtMicrobenchmark.cpp && ./ArtMicrobenchmark

# clang++ -DART -Wall -Wextra -Wno-invalid-offsetof -O3 -std=c++11 -stdlib=libc++ -o ArtMicrobenchmark -g ArtMicrobenchmark.cpp && ./ArtMicrobenchmark

# Stacked benchmark
data_file=~/repos/indexedrdd-paper/osdi16/figures/stacked.txt
echo 'stacked_data=[' > $data_file
echo '# 1. No batching, no node compaction'
clang++ -DART -DBATCH_SIZE=1 -DKEY_LEN=4 -DRANDOM -Wall -Wextra -Wno-invalid-offsetof -Wno-unused-parameter -O3 -std=c++11 -stdlib=libc++ -o ArtMicrobenchmark -g ArtMicrobenchmark.cpp && ./ArtMicrobenchmark | tee -a $data_file

# 2. +Key space transformations (improves all)

echo '# 3. +Batching, batch size=10,000 (improves insert performance)'
clang++ -DART -DBATCH_SIZE=10000 -DKEY_LEN=4 -DRANDOM -Wall -Wextra -Wno-invalid-offsetof -Wno-unused-parameter -O3 -std=c++11 -stdlib=libc++ -o ArtMicrobenchmark -g ArtMicrobenchmark.cpp && ./ArtMicrobenchmark | tee -a $data_file

echo '# 4. +Node compaction (improves scan performance)'
clang++ -DART -DART_REORDER_LEAVES -DBATCH_SIZE=10000 -DKEY_LEN=4 -DRANDOM -Wall -Wextra -Wno-invalid-offsetof -Wno-unused-parameter -O3 -std=c++11 -stdlib=libc++ -o ArtMicrobenchmark -g ArtMicrobenchmark.cpp && ./ArtMicrobenchmark | tee -a $data_file

echo ']' >> $data_file
