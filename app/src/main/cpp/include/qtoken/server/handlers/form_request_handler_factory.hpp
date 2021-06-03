#ifndef FORM_REQUEST_HANDLER_FACTORY_H
#define FORM_REQUEST_HANDLER_FACTORY_H

#include <Poco/Net/HTTPRequestHandlerFactory.h>
#include <Poco/Net/HTTPServerRequest.h>
#include <Poco/Util/ServerApplication.h>

#include "globals/logger.hpp"
#include "server/handlers/request_handlers.hpp"

namespace Qtoken {

class FormRequestHandlerFactory : public Poco::Net::HTTPRequestHandlerFactory {
private:
    Poco::Net::HTTPRequestHandler* createRequestHandler(
        const Poco::Net::HTTPServerRequest& request) override;
};

}  // namespace Qtoken

#endif  // FORM_REQUEST_HANDLER_FACTORY_H
