#ifndef CHUNKER_H
#define CHUNKER_H

#include <algorithm>
#include <fstream>
#include <iostream>
#include <string>
#include <vector>

#include "../tools/types.hpp"

#include "../globals/logger.hpp"

namespace Qtoken {

/**
 * Chunker handles file chunking operations. Its purpose is to store a byte
 * vector which can undergo several arbitrary transforms.
 *
 * Chunks are byte vectors that can be written to files. They support
 * encoding operations.
 */
class Chunker {
public:
    Chunker() = default;
    Chunker(std::istream& fs, int chunk_size) {
        Bytelist stream_vec((std::istreambuf_iterator<char>(fs)),
                            std::istreambuf_iterator<char>());
        create_chunker(stream_vec, chunk_size);
    }
    Chunker(Bytelist& char_vec, int chunk_size) {
        Log::message("WHYYYYY", "kjsafhgkahs");

        create_chunker(char_vec, chunk_size);
    }
    Chunker(const std::vector<Bytelist>& chunks, int file_size)
        : chunks(chunks)
        , file_size(file_size){};
    ~Chunker(){};

    inline ull size() { return chunks.size(); };
    inline Bytelist& get(int i) { return chunks.at(i); };
    inline int get_chunk_size() { return chunks.at(0).size(); };
    ull get_file_size() { return file_size; };

    int get_tail_size();
    void rebuild(std::string output_file);
    std::vector<Bytelist>& get_padded_chunks() { return chunks; }
    std::vector<Bytelist> get_chunks();
    std::vector<Chunker> split(int num_shards);
    void write_chunks(const std::string& dir);
    void resize_chunks(int new_size);
    Bytelist join();

    bool operator==(Chunker& rhs);

private:
    std::vector<Bytelist> chunks;
    int get_byte_count(std::vector<Bytelist>& shards);
    ull file_size;
    void create_chunker(Bytelist& char_vec, int chunk_size);
};

}  // namespace Qtoken

#endif  // CHUNKER_H
