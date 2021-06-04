#include "../server/handlers/form_request_handler_factory.hpp"

namespace Qtoken {

Poco::Net::HTTPRequestHandler* FormRequestHandlerFactory::createRequestHandler(
    const Poco::Net::HTTPServerRequest& request) {
    Log::message("http", "Request from " + request.clientAddress().toString());

    if (request.getURI() == "/spread")
        return new SpreadHandler();

    if (request.getURI() == "/share")
        return new ShareHandler();

    if (request.getURI() == "/stream")
        return new StreamHandler();

    if (request.getURI() == "/gather")
        return new GatherHandler();

    if (request.getURI() == "/getPeers")
        return new GetPeersHandler();

    if (request.getURI() == "/putValue")
        return new PutHandler();

    if (request.getURI() == "/getValue")
        return new GetHandler();

    if (request.getURI() == "/exit?")
        return new ExitHandler();

    return nullptr;
}

}  // namespace Qtoken
