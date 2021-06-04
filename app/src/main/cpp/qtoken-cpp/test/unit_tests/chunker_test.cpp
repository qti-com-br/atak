#include <gtest/gtest.h>

#include "test_utils.hpp"
#include "../tools/chunker.hpp"

class ChunkerBuildingFixture
    : public ::testing::TestWithParam<std::tuple<int, int>> {};

// Test encoding on entire chunks built from randomly generated files
TEST_P(ChunkerBuildingFixture, BuildChunker) {
    int file_size = std::get<0>(GetParam());
    int chunk_size = std::get<1>(GetParam());

    Chunker* ch = alloc_rand_chunker(file_size, chunk_size);
    Chunker ch_cp(*ch);

    EXPECT_TRUE(*ch == ch_cp);

    delete ch;
}

// Test encoding on entire chunks built from randomly generated files
TEST_P(ChunkerBuildingFixture, BuildChunkerVector) {
    int file_size = std::get<0>(GetParam());
    int chunk_size = std::get<1>(GetParam());

    auto test_file = get_test_file(file_size);
    Bytelist test_file_vec((std::istreambuf_iterator<char>(test_file)),
                           std::istreambuf_iterator<char>());

    // build chunker
    Chunker ch(test_file_vec, chunk_size);

    // check bytes match
    int i = 0;
    for (auto chunk : ch.get_padded_chunks()) {
        for (auto c : chunk) {
            if (i >= test_file_vec.size())
                EXPECT_TRUE(c == 0);
            else
                EXPECT_TRUE(c == test_file_vec.at(i));
            i++;
        }
    }
}

INSTANTIATE_TEST_CASE_P(
    BuildChunker, ChunkerBuildingFixture,
    ::testing::Values(std::make_tuple(7, 5), std::make_tuple(7, 10),
                      std::make_tuple(1000, 200), std::make_tuple(1953, 555),
                      std::make_tuple(5111, 67), std::make_tuple(15625, 9625)));

TEST(ChunkerTest, HandlesEmptyFile) {
    // TODO
}
