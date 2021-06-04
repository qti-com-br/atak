#ifndef RECEIPT_SERVICE_H
#define RECEIPT_SERVICE_H

#include "../globals/globals.hpp"
#include "../nodes/node.hpp"
#include "../receipt/crypto_receipt.hpp"
#include "../tools/chunker.hpp"
#include "../tools/types.hpp"

namespace Qtoken {

/**
 * Handles gathering of multiple receipts, appending shards to
 * its internal buffer. Intended to be extensible for handling
 * ordering(for not tcp connections), ttl, and thread safe access(multiple
 * users).
 */
class ReceiptService {
public:
    ReceiptService() {}
    ~ReceiptService(){};
    void push(const CryptoReceipt& cr);
    Chunker build();

private:
    ull get_buff_size();
    std::vector<Bytelist> decoded_shards;
};

}  // namespace Qtoken

#endif  // RECEIPT_SERVICE_H
