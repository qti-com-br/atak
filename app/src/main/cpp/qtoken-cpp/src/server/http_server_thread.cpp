#include "server/http_server_thread.hpp"

using namespace Qtoken;

HTTPServerThread::HTTPServerThread(const std::vector<std::string>& args)
    : _args(args){};
void HTTPServerThread::run() {
    Log::message("http", "Starting app on port: " + _args[0]);
    HTTPFormServer app;
    app.run(_args);
};
