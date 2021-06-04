#ifndef BENCH_UTILS_H
#define BENCH_UTILS_H

#include <stdio.h>
#include <cstdlib>
#include <fstream>

#include "../coders/decoder.hpp"
#include "../coders/encoder.hpp"
#include "../tools/chunker.hpp"

using namespace Qtoken;

inline Bytelist get_test_file(int file_size = 1024) {
    std::string cmd_stub =
        "/bin/dd if=/dev/urandom of=rand_bytes.dat status=none count=1 bs=";
    std::system((cmd_stub + std::to_string(file_size)).c_str());
    std::ifstream test_file("rand_bytes.dat", std::ios::in | std::ios::binary);

    Bytelist stream_vec((std::istreambuf_iterator<char>(test_file)),
                        std::istreambuf_iterator<char>());
    test_file.close();
    return stream_vec;
}

// TODO: This is a great place to implement a mock chunker so we
// don't have to deal with the files on disc and making std::system calls
/**
 * Create a chunker of provided size from a randomized
 * file.
 * @param file_size chunker file size in bytes
 * @param chunk_size chunk size in bytes
 * @return random chunker allocated to pointer
 */
inline Chunker* alloc_rand_chunker(int file_size = 1024, int chunk_size = 251) {
    // Create random test files
    auto test_file = get_test_file(file_size);
    Chunker* ch = new Chunker(test_file, chunk_size);

    // Close and delete random bytes
    std::system("rm rand_bytes.dat");

    return ch;
}

#endif  // BENCH_UTILS_H