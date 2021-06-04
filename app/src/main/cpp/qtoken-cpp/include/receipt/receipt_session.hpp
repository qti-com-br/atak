#ifndef RECEIPT_SESSION_H
#define RECEIPT_SESSION_H

#include <iostream>
#include <iterator>
#include <sstream>
#include <chrono>

#include <Poco/Net/StreamSocket.h>

#include "globals/logger.hpp"
#include "globals/strings.hpp"
#include "receipt/crypto_receipt.hpp"
#include "tools/types.hpp"

namespace Qtoken {

/**
 * ReceiptSession establishes a TCP session with a peer and exposes functinality
 * for sending one or more receipts alone or in a stream.
 */
class ReceiptSession {
public:
    explicit ReceiptSession(Addr addr);
    ReceiptSession(ReceiptSession&) = delete;
    ~ReceiptSession();

    void start_session();
    void send_receipts(std::vector<CryptoReceipt> crs);
    void send_receipt(CryptoReceipt cr);

private:
    Addr addr;
    Poco::Net::StreamSocket ss;
    Bytelist session_token;
};

}  // namespace Qtoken

#endif  // RECEIPT_SESSION_H
