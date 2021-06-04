#include "tools/chunker.hpp"

using namespace Qtoken;

void Chunker::create_chunker(Bytelist& char_vec, int chunk_size) {
    this->file_size = char_vec.size();
    if (chunk_size <= 0) {
        std::cerr << "Chunk size " << chunk_size << " is invalid" << std::endl;
        return;
    }
    ull total_chunks = 1 + ((file_size - 1) / chunk_size);
    auto beg = char_vec.begin();

    for (ull i = 0; i < total_chunks; i++) {
        Bytelist curr_chunk;
        // if last chunk, pad
        if (std::size_t(std::distance(beg, char_vec.end()) < chunk_size)) {
            Bytelist chunk(beg, char_vec.end());
            chunk.resize(chunk_size, 0);
            curr_chunk = std::move(chunk);
        } else {
            auto end = beg + chunk_size;
            Bytelist chunk(beg, end);
            curr_chunk = std::move(chunk);
        }

        std::advance(beg, chunk_size);

        chunks.push_back(curr_chunk);
    }
}

/**
 * Given a chunk size resize each chunk to match the new size
 * smaller chunker sizes will truncate to achieve the new size
 * larger chunk sizes will pad with zeroes
 * @param new_size new chunk size
 */
void Chunker::resize_chunks(int new_size) {
    for (auto& chunk : chunks) {
        chunk.resize(new_size, 0);
    }
}

/**
 * Rebuilds file from chunks and saves to specified directory
 * Note: tail chunk padding is truncated on rebuild
 * @param output_file save directory
 */
void Chunker::rebuild(std::string output_file) {
    int tail_size = get_tail_size();
    std::ofstream fs(output_file, std::ios::app | std::ios::binary);

    for (ull i = 0; i < chunks.size(); i++) {
        // Truncate tail padding
        if (i == chunks.size() - 1) {
            fs.write(reinterpret_cast<char*>(chunks[i].data()), tail_size);
        } else {
            fs.write(reinterpret_cast<char*>(chunks[i].data()),
                     chunks[i].size());
        }
    }

    fs.close();
}

/**
 * Retrieves chunks with padding removed.
 * @return unpadded chunks.
 */
std::vector<Bytelist> Chunker::get_chunks() {
    int tail_size = get_tail_size();
    std::vector<Bytelist> unpadded_chunks = chunks;
    unpadded_chunks.at(unpadded_chunks.size() - 1).resize(tail_size);
    return unpadded_chunks;
}

/**
 * Calculate tail size from file size, chunk size, and total chunks.
 * @return unpadded size of tail chunk.
 */
int Chunker::get_tail_size() {
    auto chunk_size = get_chunk_size();
    ull total_chunks = 1 + ((file_size - 1) / chunk_size);
    return (file_size - ((total_chunks - 1) * chunk_size));
}

/**
 * Destroys current chunker by splitting chunks into a list of chunkers.
 * Each resulting chunker contains at most num_shards chunks.
 * @param num_shards max number of chunks per chunker.
 * @return list of resulting chunkers.
 */
std::vector<Chunker> Chunker::split(int num_shards) {
    std::vector<Chunker> chunkers;
    int tail_size = get_tail_size();

    for (int i = 0; i < chunks.size(); i += num_shards) {
        std::vector<Bytelist> shards;
        // Move every set of num_shards chunks into a shard list
        for (int j = 0; j < num_shards; j++) {
            // Cut off the last shard before a multiple of num_shards
            if (i + j < chunks.size()) {
                shards.push_back(std::move(chunks.at(i + j)));
            }
        }

        // Construct a chunker from the shard list
        if (i + num_shards >= chunks.size()) {
            // Handle padding on final shard
            int last_ch_size =
                (get_chunk_size() * (shards.size() - 1)) + tail_size;
            chunkers.push_back(Chunker(shards, last_ch_size));
        } else {
            chunkers.push_back(Chunker(shards, get_byte_count(shards)));
        }
    }

    return chunkers;
}

/**
 * Counts bytes in provided shard list.
 * @param shards shard list.
 * @return total bytes in list.
 */
int Chunker::get_byte_count(std::vector<Bytelist>& shards) {
    int byte_count = 0;
    for (int i = 0; i < shards.size(); i++) {
        byte_count += shards[i].size();
    }
    return byte_count;
}

/**
 * Chunk wise comparison of two chunkers
 * @param rhs chunker to be compared to this
 * Note: Because it is chunk wise comparison equal files
 * with differing chunk sizes can return != as chunkers
 */
bool Chunker::operator==(Chunker& rhs) {
    if (file_size != rhs.get_file_size()) {
        return false;
    }
    if (get_chunk_size() != rhs.get_chunk_size()) {
        return false;
    }
    if (this->chunks != rhs.get_padded_chunks())
        return false;

    return true;
}
