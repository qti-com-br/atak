#ifndef HTTP_SERVER_THREAD_H
#define HTTP_SERVER_THREAD_H

#include "Poco/Util/ServerApplication.h"

#include "http_form_server.hpp"
#include "../globals/logger.hpp"

namespace Qtoken {

class HTTPServerThread : public Poco::Runnable {
public:
    explicit HTTPServerThread(const std::vector<std::string>& args);
    ~HTTPServerThread(){};
    void run() override;

private:
    const std::vector<std::string>& _args;

};

}  // namespace Qtoken

#endif // HTTP_SERVER_THREAD_H