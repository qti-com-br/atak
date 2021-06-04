#include "receipt/receipt_session.hpp"

using namespace Qtoken;

/**
 * Launches a connection with peer addr.
 * @param addr peer to create secure channel with.
 */
ReceiptSession::ReceiptSession(Addr addr)
    : addr(addr)
    , session_token(UUID_LEN) {
    try {
        ss.connect(Poco::Net::SocketAddress(addr.ip, addr.port));
    } catch (...) {
        Log::message("root", "Share failed: Invalid Host");
        Log::message("root", "Receipt session started");
        return;
    }

    Log::message("root", "Receipt session started");
    ss.setReceiveTimeout(5000000);
}

/**
 * Starts a streaming session by requesting a session token from the receiver
 * node.
 */
void ReceiptSession::start_session() {
    ss.sendBytes(init_stream_session.data(), init_stream_session.size());
    ss.receiveBytes(session_token.data(), UUID_LEN);
    Log::message("root", "Session token obtained: " +
                std::string(session_token.begin(), session_token.end()));
}

/**
 * Loops through list of receipts sending each through the secure channel.
 * @param crs list of crypto receipts.
 */
void ReceiptSession::send_receipts(std::vector<CryptoReceipt> crs) {
    for (int i = 0; i < crs.size(); i++) {
        Log::message("root", "Stream frame");
        auto timeStart = std::chrono::high_resolution_clock::now();
        send_receipt(crs.at(i));        
        auto timeEnd = std::chrono::high_resolution_clock::now();
        long long duration = std::chrono::duration_cast<std::chrono::microseconds>(timeEnd - timeStart).count();
        Log::message("root", "Streamed receipt " + std::to_string(i) + " : " + std::to_string(duration) + " ms.");
    }
}

/**
 * Serializes, encrypts, and sends receipt through secure channel.
 * @param cr crytpo receipt to send.
 */
void ReceiptSession::send_receipt(CryptoReceipt cr) {
    // check if valid session token exists
    if (session_token.size() != UUID_LEN)
        return;

    std::stringstream cr_stream = cr.serialize();
    Bytelist receipt_bytes((std::istreambuf_iterator<char>(cr_stream)),
                           std::istreambuf_iterator<char>());

    // prepend session token as per protocol
    Bytelist cr_msg;
    cr_msg.reserve(session_token.size() + receipt_bytes.size());
    cr_msg.insert(cr_msg.end(), session_token.begin(), session_token.end());
    cr_msg.insert(cr_msg.end(), receipt_bytes.begin(), receipt_bytes.end());

    ss.sendBytes(cr_msg.data(), cr_msg.size());
    
    Bytelist resp(received_receipt.size());
    ss.receiveBytes(resp.data(), received_receipt.size());

    if(resp.size() != received_receipt.size()){
        Log::message("root", "Failed to send receipt");
    }
}

/**
 * Shuts down connection.
 */
ReceiptSession::~ReceiptSession() {
    // let receiver know we're done
    Bytelist end_stream_transmission;
    end_stream_transmission.reserve(session_token.size() +
                                    end_stream_session.size());
    end_stream_transmission.insert(end_stream_transmission.end(),
                                   session_token.begin(), session_token.end());
    end_stream_transmission.insert(end_stream_transmission.end(),
                                   end_stream_session.begin(),
                                   end_stream_session.end());
    Log::message("root", "killing session: " +
                std::string(session_token.begin(), session_token.end()));
    ss.sendBytes(end_stream_transmission.data(),
                 end_stream_transmission.size());
}