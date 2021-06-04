#include "server/handlers/spread_part_handler.hpp"

using namespace Qtoken;
using Qtoken::CryptoReceipt;
using Qtoken::global_node;

using Poco::URI;
using Poco::Net::NameValueCollection;

SpreadPartHandler::SpreadPartHandler(HTTPServerRequest& request)
    : _request(request) {}

void SpreadPartHandler::handlePart(const MessageHeader& header,
                                   std::istream& stream) {
    _type = header.get("Content-Type", "(unspecified)");
    URI uri(_request.getURI());

    // Get data out of json form
    if (header.has("Content-Disposition")) {
        std::string disp;
        NameValueCollection params;
        MessageHeader::splitParameters(header["Content-Disposition"], disp,
                                       params);
        _cryptoReceipt = params.get("receipt", "(unnamed)");
        _filePath = params.get("filepath", "(unnamed)");
    }

    // Do spread with the node.
    _cryptoReceiptObj = global_node->doSpread(stream);

    _cryptoReceipt = _cryptoReceiptObj.serialize().str();

    // Prepare cryptoreceipt
    std::string receipt_dir = Config::get("files.receipts.sent");
    std::string save_path(receipt_dir + "CR" +
                          std::to_string(get_rand_seed_uint32_t()));

    // Save cryptoreceipt
    _cryptoReceiptObj.save(save_path);
    _filePath = save_path;
    Log::message("http", "Receipt saved to " + save_path);
}

const std::string& SpreadPartHandler::cryptoReceipt() const {
    return _cryptoReceipt;
}

CryptoReceipt SpreadPartHandler::cryptoReceiptObj() {
    return _cryptoReceiptObj;
}

const std::string& SpreadPartHandler::filePath() const {
    return _filePath;
}
