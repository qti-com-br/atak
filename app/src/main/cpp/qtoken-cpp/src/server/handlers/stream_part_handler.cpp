#include "server/handlers/stream_part_handler.hpp"

using namespace Qtoken;
using Qtoken::CryptoReceipt;
using Qtoken::global_node;

using Poco::URI;
using Poco::Net::NameValueCollection;

StreamPartHandler::StreamPartHandler(HTTPServerRequest& request)
    : _request(request) {}

void StreamPartHandler::handlePart(const MessageHeader& header,
                                   std::istream& stream) {
    _type = header.get("Content-Type", "(unspecified)");
    URI uri(_request.getURI());

    // get args from req header
    std::string share_endpoint = _request.get("endpoint", "");
    if (share_endpoint == "")
        // FIXME: throw bug
        return;
    std::string end_stream = _request.get("end_stream", "");

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
    if (end_stream == "true") {
        Log::message("http", "end_stream");
        crs = global_node->doShare(stream, share_address, share_port, false);
    } else
        crs = global_node->doShare(stream, share_address, share_port, true);
}

std::vector<CryptoReceipt> StreamPartHandler::cryptoReceiptVec() const {
    return crs;
}

const std::string& StreamPartHandler::filePath() const {
    return _filePath;
}
