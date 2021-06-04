#ifndef SPREAD_PART_HANDLER_H
#define SPREAD_PART_HANDLER_H

#include <functional>
#include <iostream>

#include <Poco/Net/HTTPServerRequest.h>
#include <Poco/Net/MessageHeader.h>
#include <Poco/Net/PartHandler.h>
#include <Poco/URI.h>

#include "../globals/globals.hpp"
#include "../globals/config.hpp"
#include "../nodes/node.hpp"
#include "../receipt/crypto_receipt.hpp"

using Poco::Net::HTTPServerRequest;
using Poco::Net::MessageHeader;

namespace Qtoken {

class SpreadPartHandler : public Poco::Net::PartHandler {
public:
    explicit SpreadPartHandler(HTTPServerRequest& request);
    void handlePart(const MessageHeader& header, std::istream& stream);
    const std::string& fileName() const;
    const std::string& filePath() const;
    const std::string& cryptoReceipt() const;
    CryptoReceipt cryptoReceiptObj();
    const std::string& port() const;

private:
    std::string _type;
    std::string _fileName;
    std::string _filePath;
    std::string _cryptoReceipt;
    CryptoReceipt _cryptoReceiptObj;
    HTTPServerRequest& _request;
};

}  // namespace Qtoken

#endif  // SPREAD_PART_HANDLER_H