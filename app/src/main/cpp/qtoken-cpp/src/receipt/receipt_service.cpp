#include "../receipt/receipt_service.hpp"

using namespace Qtoken;

/**
 * Gathers crypto receipt shards into chunker and appends to internal buffer.
 * @param cr cryptor receipt to gather.
 */
void ReceiptService::push(const CryptoReceipt& cr) {
    Chunker chs = global_node->doGather(cr);

    for (auto& ch : chs.get_chunks()) {
        decoded_shards.push_back(std::move(ch));
    }
}

/**
 * Build a chunker from the buffer and return to user.
 * @return chunker of buffer contents.
 */
Chunker ReceiptService::build() {
    return Chunker(decoded_shards, get_buff_size());
}

/**
 * Returns size of shard buffer in bytes.
 * @return total bytes in received shards.
 */
ull ReceiptService::get_buff_size() {
    ull sum = 0;
    for (auto shard : decoded_shards) {
        sum += shard.size();
    }
    return sum;
}
