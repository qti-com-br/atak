#ifndef HTTP_FORM_SERVER_H
#define HTTP_FORM_SERVER_H

#include "Poco/Net/HTTPServer.h"
#include "Poco/Net/HTTPServerParams.h"
#include "Poco/Net/ServerSocket.h"

#include "globals/globals.hpp"
#include "globals/logger.hpp"
#include "handlers/form_request_handler_factory.hpp"

using Poco::Net::HTTPServer;
using Poco::Net::HTTPServerParams;
using Poco::Net::ServerSocket;
using Poco::Util::Application;

namespace Qtoken {

class HTTPFormServer : public Poco::Util::ServerApplication
/// The main application class.
/// This is the HTTP Server that receives request from a client and 
/// makes the approriate calls to the node class.
/// To test the FormServer you can use any web browser (http://localhost:9980/).
{
public:
    HTTPFormServer();
    ~HTTPFormServer() {}

protected:
    /**
    * Main server function
    * @param args List of args. Currently only holds the port to start the
    *             server to start on at args[0].
    */
    int main(const std::vector<std::string>& args);
};

}  // namespace Qtoken

#endif  // HTTP_FORM_SERVER_H
