#ifndef TEST_UTILS_H
#define TEST_UTILS_H

#include <gtest/gtest.h>
#include <stdio.h>
#include <cstdlib>
#include <fstream>

#include "../coders/decoder.hpp"
#include "../coders/encoder.hpp"
#include "../tools/chunker.hpp"

inline std::ifstream get_test_file(int file_size = 1024) {
    std::string cmd_stub =
        "/bin/dd if=/dev/urandom of=rand_bytes.dat status=none count=1 bs=";
    std::system((cmd_stub + std::to_string(file_size)).c_str());
    std::ifstream test_file("rand_bytes.dat", std::ios::in | std::ios::binary);
    return test_file;
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
    Bytelist stream_vec((std::istreambuf_iterator<char>(test_file)),
                        std::istreambuf_iterator<char>());

    Chunker* ch = new Chunker(stream_vec, chunk_size);

    // Close and delete random bytes
    test_file.close();
    std::system("rm rand_bytes.dat");

    return ch;
}

#endif  // TEST_UTILS_H
