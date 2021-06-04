#include "../server/handlers/share_part_handler.hpp"

using namespace Qtoken;
using Qtoken::CryptoReceipt;
using Qtoken::global_node;

using Poco::URI;
using Poco::Net::NameValueCollection;

SharePartHandler::SharePartHandler(HTTPServerRequest& request)
    : _request(request) {}

void SharePartHandler::handlePart(const MessageHeader& header,
                                  std::istream& stream) {
    _type = header.get("Content-Type", "(unspecified)");
    URI uri(_request.getURI());

    // get args from req header
    std::string share_endpoint = _request.get("endpoint", "");
    if (share_endpoint == "")
        // FIXME: throw bug
        return;

    Log::message("http", "Server received spread request");

    // Get file data out of json form
    if (header.has("Content-Disposition")) {
        std::string disp;
        NameValueCollection params;
        MessageHeader::splitParameters(header["Content-Disposition"], disp,
                                       params);
        _filePath = params.get("filepath", "(unnamed)");
    }

    auto separatorIdx = share_endpoint.find(':');
    auto share_address = share_endpoint.substr(0, separatorIdx);
    auto share_port = share_endpoint.substr(separatorIdx + 1);

    crs = global_node->doShare(stream, share_address, share_port);
}

std::vector<CryptoReceipt> SharePartHandler::cryptoReceiptVec() const {
    return crs;
}

const std::string& SharePartHandler::filePath() const {
    return _filePath;
}
