#ifndef REQUEST_HANDLERS_H
#define REQUEST_HANDLERS_H

#include <signal.h>
#include <sstream>

#include <Poco/Net/HTTPRequestHandler.h>
#include <Poco/Net/HTTPServerRequest.h>
#include <Poco/Net/HTTPServerResponse.h>
#include <Poco/Net/MessageHeader.h>
#include "Poco/JSON/Object.h"
#include "Poco/JSON/Parser.h"
#include "Poco/JSON/Stringifier.h"
#include "Poco/Net/HTMLForm.h"
#include "Poco/Net/PartHandler.h"
#include "Poco/Process.h"
#include "Poco/StreamCopier.h"

#include "globals/globals.hpp"

using Poco::Net::HTMLForm;
using Poco::Net::HTTPServerRequest;
using Poco::Net::HTTPServerResponse;
using Poco::Net::MessageHeader;

namespace Qtoken {

class FormRequestHandler : public Poco::Net::HTTPRequestHandler {
public:
    FormRequestHandler();

    void handleRequest(Poco::Net::HTTPServerRequest& req,
                       Poco::Net::HTTPServerResponse& resp);
};

class ExitHandler : public Poco::Net::HTTPRequestHandler {
public:
    virtual void handleRequest(Poco::Net::HTTPServerRequest& req,
                               Poco::Net::HTTPServerResponse& resp) override;
};

class SpreadHandler : public Poco::Net::HTTPRequestHandler {
private:
    void handleRequest(Poco::Net::HTTPServerRequest& req,
                       Poco::Net::HTTPServerResponse& resp) override;
};

class ShareHandler : public Poco::Net::HTTPRequestHandler {
private:
    void handleRequest(Poco::Net::HTTPServerRequest& req,
                       Poco::Net::HTTPServerResponse& resp) override;
};

class StreamHandler : public Poco::Net::HTTPRequestHandler {
private:
    void handleRequest(Poco::Net::HTTPServerRequest& req,
                       Poco::Net::HTTPServerResponse& resp) override;
};

class GatherHandler : public Poco::Net::HTTPRequestHandler {
private:
    void handleRequest(Poco::Net::HTTPServerRequest& req,
                       Poco::Net::HTTPServerResponse& resp) override;
};

class GetPeersHandler : public Poco::Net::HTTPRequestHandler {
public:
    virtual void handleRequest(Poco::Net::HTTPServerRequest& req,
                               Poco::Net::HTTPServerResponse& resp) override;
};

class PutHandler : public Poco::Net::HTTPRequestHandler {
public:
    virtual void handleRequest(Poco::Net::HTTPServerRequest& req,
                               Poco::Net::HTTPServerResponse& resp) override;
};

class GetHandler : public Poco::Net::HTTPRequestHandler {
public:
    virtual void handleRequest(Poco::Net::HTTPServerRequest& req,
                               Poco::Net::HTTPServerResponse& resp) override;
};

}  // namespace Qtoken

#endif  // REQUEST_HANDLERS_H
