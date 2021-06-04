#include "server/http_form_server.hpp"

using namespace Qtoken;

HTTPFormServer::HTTPFormServer() {}

int HTTPFormServer::main(const std::vector<std::string>& args) {
    unsigned short port =
        (unsigned short)config().getInt("HTTPFormServer.port", stoi(args[0]));
    // set-up a server socket
    ServerSocket svs(port);
    // set-up a HTTPServer instance
    HTTPServer srv(new FormRequestHandlerFactory, svs, new HTTPServerParams);

    // start the HTTPServer
    srv.start();
    // wait for CTRL-C or shutdown request
    server_wait_for_shutdown->wait();
    // Stop the HTTPServer
    srv.stop();

    return Application::EXIT_OK;
}
