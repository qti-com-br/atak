#ifndef SHARE_CONNECTION_HANDLER_H
#define SHARE_CONNECTION_HANDLER_H

#include <functional>
#include <iostream>
#include <memory>

#include "Poco/Exception.h"
#include "Poco/NObserver.h"
#include "Poco/Net/NetException.h"
#include "Poco/Net/SocketNotification.h"
#include "Poco/Net/SocketReactor.h"
#include "Poco/Net/SocketStream.h"
#include "Poco/Net/StreamSocket.h"
#include "Poco/Thread.h"
#include "Poco/Util/ServerApplication.h"

#include "globals/config.hpp"
#include "globals/globals.hpp"
#include "globals/logger.hpp"
#include "nodes/node.hpp"
#include "receipt/receipt_service.hpp"
#include "tools/uuid.hpp"

namespace Qtoken {

enum MessageType {
    START_STREAM_SESSION,
    RECEIPT_STREAM,
    END_STREAM_SESSION,
    UNKNOWN
};

class ReceiptService;

/**
 * ShareConnectionHandler handles receipts send to the node by the share command
 * and posts them to the receipt service for handling.
 */
class ShareConnectionHandler {
public:
    ShareConnectionHandler(Poco::Net::StreamSocket& socket,
                           Poco::Net::SocketReactor& reactor);
    ~ShareConnectionHandler() noexcept(false);
    void onReadable(const Poco::AutoPtr<Poco::Net::ReadableNotification>& pNf);
    void onShutdown(const Poco::AutoPtr<Poco::Net::ShutdownNotification>& pNf);

private:
    MessageType parse_message(const Bytelist& buff_vec);
    std::string parse_session_token(const Bytelist& buff_vec);

    void handle_start_session();
    void handle_cr(std::string session_token, const Bytelist& buff);
    void handle_end_session(std::string session_token);

    // https://gitlab.optimusprime.ai/virgilsystems/prototypes/qtoken-cpp/-/issues/13
    std::unique_ptr<ReceiptService> rs;
    Poco::Net::StreamSocket _socket;
    Poco::Net::SocketReactor& _reactor;
    std::string received_dir;
};

}  // namespace Qtoken

#endif  // SHARE_CONNECTION_HANDLER_H
