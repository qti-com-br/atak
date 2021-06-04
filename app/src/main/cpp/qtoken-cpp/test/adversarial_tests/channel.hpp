#ifndef CHANNEL_H
#define CHANNEL_H

#include <functional>

#include "../coders/encoder.hpp"
#include "../tools/chunker.hpp"

class Channel : public Encoder {
public:
    // In the channel base class the bit_op function does nothing
    // In derived classes we can set the bit_op function to more easily
    // describe how our channel effects bits
    Channel()
        : bit_op([](Bitlist& bits) {}){};

    ~Channel() = default;
    std::function<void(Bitlist&)> bit_op;

    // The channel chunk operator loops through chunks
    // converts the bytes to bits
    // passes the bits to the bit_op function
    // and finally converts the bits back to bytes
    ChunkOperator get_coder() override {
        // Class members cannot be captured so we make a local copy
        std::function<void(Bitlist&)> shard_op = bit_op;

        chunker_op = [this, shard_op](Writer& w) {
            std::vector<Bytelist>& chunks = w.first->get_padded_chunks();

            for (Bytelist& chunk : chunks) {
                Bitlist bits = bytes_to_bits<Bitlist>(chunk);
                shard_op(bits);
                chunk = bits_to_bytes(bits);
            }
        };
        ;

        return chunker_op;
    };

private:
    ChunkOperator chunker_op;
};

#endif  // CHANNEL_H