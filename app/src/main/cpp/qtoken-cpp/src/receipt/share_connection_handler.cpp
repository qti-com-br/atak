#include "../receipt/share_connection_handler.hpp"

using namespace Qtoken;

/**
 * Constructs server for ParallelSocketAcceptor construction.
 * @param socket poco stream socket for waiting on server port.
 * @param reactor poco socket reactor for handling incoming connections.
 */
ShareConnectionHandler::ShareConnectionHandler(
    Poco::Net::StreamSocket& socket, Poco::Net::SocketReactor& reactor)
    : _socket(socket)
    , _reactor(reactor) {
    _reactor.addEventHandler(_socket,
                             Poco::NObserver<ShareConnectionHandler,
                                             Poco::Net::ReadableNotification>(
                                 *this, &ShareConnectionHandler::onReadable));
    _reactor.addEventHandler(_socket,
                             Poco::NObserver<ShareConnectionHandler,
                                             Poco::Net::ShutdownNotification>(
                                 *this, &ShareConnectionHandler::onShutdown));

    // _socket.setReceiveTimeout(5000000);
}

/**
 * Removes handlers and deletes reactor.
 */
ShareConnectionHandler::~ShareConnectionHandler() noexcept(false) {
    _reactor.removeEventHandler(
        _socket, Poco::NObserver<ShareConnectionHandler,
                                 Poco::Net::ReadableNotification>(
                     *this, &ShareConnectionHandler::onReadable));
    _reactor.removeEventHandler(
        _socket, Poco::NObserver<ShareConnectionHandler,
                                 Poco::Net::ShutdownNotification>(
                     *this, &ShareConnectionHandler::onShutdown));
}

/**
 * Handler deployed on readable bytes.
 * Determines message type and calls appropriate handler.
 * @param pNf poco on readable notification.
 */
void ShareConnectionHandler::onReadable(
    const Poco::AutoPtr<Poco::Net::ReadableNotification>& pNf) {
    received_dir = Config::get("files.receipts.received");
    Bytelist buff_vec(USHRT_MAX);
    _socket.setReceiveBufferSize(USHRT_MAX);

    if (_socket.receiveBytes(buff_vec.data(), USHRT_MAX)) {
        switch (parse_message(buff_vec)) {
        case START_STREAM_SESSION: {
            Log::message("cr-server", "Start stream session request received");
            handle_start_session();
            Log::message("root", "handled start");
            break;
        }
        case RECEIPT_STREAM: {
            Log::message("cr-server", "Stream receipt received");
            handle_cr(parse_session_token(buff_vec), buff_vec);
            break;
        }
        case END_STREAM_SESSION: {
            Log::message("cr-server", "End stream session request received");
            auto session_token = parse_session_token(buff_vec);
            handle_end_session(session_token);
            _reactor.stop();
            break;
        }
        case UNKNOWN: {
            Log::message("cr-server", "Unknown cr server request");
            _socket.sendBytes(cr_server_unknown.data(),
                              cr_server_unknown.size());
            break;
        }
        }
    }
}

/**
 * Create a new stream session on the global node.
 */
void ShareConnectionHandler::handle_start_session() {
    UUID id;
    std::string session_token(id.ToString());
    Log::message("root", "adding stream session");
    global_node->add_stream_session(session_token);
    Log::message("root", "added stream session");
    _socket.sendBytes(session_token.data(), session_token.size());
    Log::message("root", "sent session ID");
}

/**
 * Decrypt receipt with public key and push to receipt_service.
 * @param buff message buffer with incoming receipt bytes.
 */
void ShareConnectionHandler::handle_cr(std::string session_token,
                                       const Bytelist& buff) {
    Bytelist receipt_bytes(buff.begin() + UUID_LEN, buff.end());
    CryptoReceipt cr(receipt_bytes);
    Log::message("root", "updating stream session");
    global_node->update_stream_session(cr);
    Log::message("root", "sent receipt ack");
    _socket.sendBytes(received_receipt.data(), received_receipt.size());
}

/**
 * End an existing stream session on the global node.
 * @param session_token ID of the session to end
 */
void ShareConnectionHandler::handle_end_session(std::string session_token) {
#ifdef __ANDROID__
    auto ip = _socket.peerAddress().toString();
    ip.substr(0, ip.find(":"));
    Log::message("root", "ending sessioyn");
    global_node->end_stream_session(ip);
#else
    global_node->end_stream_session();
#endif
}

/**
 * Determines incoming message type.
 * Assumes non pki bytes are receipt bytes.
 * @param msg raw message bytes (will always be USHRT_MAX length).
 */
MessageType ShareConnectionHandler::parse_message(const Bytelist& buff_vec) {
    // only want to check max protocol msg len bytes, which is session_token
    auto msg = parse_session_token(buff_vec);
    std::string msg_c_str(msg.c_str());

    if (msg_c_str == init_stream_session) {
        return MessageType::START_STREAM_SESSION;
    } else if (global_node->is_active_stream_session()) {
        // if active stream, check if stream end message was sent
        std::string check_end(
            buff_vec.begin() + UUID_LEN,
            buff_vec.begin() + UUID_LEN + end_stream_session.size());
        if (check_end == end_stream_session)
            return MessageType::END_STREAM_SESSION;
        return MessageType::RECEIPT_STREAM;
    } else {
        return MessageType::UNKNOWN;
    }
}

/**
 * Extract session ID from message bytes
 * @return parsed session_token (n.b. this could be invalid; cross-checked with
 * node's active sessions)
 */
std::string ShareConnectionHandler::parse_session_token(
    const Bytelist& buff_vec) {
    std::string msg(buff_vec.begin(), buff_vec.begin() + UUID_LEN);
    return msg;
}

/**
 * Handles receipt server shutdown.
 * @param pNf poco on shutdown notification.
 */
void ShareConnectionHandler::onShutdown(
    const Poco::AutoPtr<Poco::Net::ShutdownNotification>& pNf) {
    Log::message("cr-server", "Receipt server shutdown");
}
