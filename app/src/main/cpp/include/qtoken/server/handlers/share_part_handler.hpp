#ifndef SHARE_PART_HANDLER_H
#define SHARE_PART_HANDLER_H

#include <functional>
#include <iostream>

#include "Poco/Net/HTTPServerRequest.h"
#include "Poco/Net/MessageHeader.h"
#include "Poco/Net/PartHandler.h"
#include "Poco/URI.h"

#include "globals/globals.hpp"
#include "nodes/node.hpp"
#include "receipt/crypto_receipt.hpp"

using Poco::Net::HTTPServerRequest;
using Poco::Net::MessageHeader;

namespace Qtoken {

class SharePartHandler : public Poco::Net::PartHandler {
public:
    explicit SharePartHandler(HTTPServerRequest& request);
    void handlePart(const MessageHeader& header, std::istream& stream);
    const std::string& filePath() const;
    std::vector<CryptoReceipt> cryptoReceiptVec() const;

private:
    std::string _type;
    std::string _filePath;
    std::vector<CryptoReceipt> crs;
    HTTPServerRequest& _request;
};

}  // namespace Qtoken

#endif  // SHARE_PART_HANDLER_H