#ifndef _CRYPTO_EXCEPTION_H
#define _CRYPTO_EXCEPTION_H

#include <string>

namespace Qtoken {
namespace Crypto {
class OpenSSLException {
public:
    std::string msg;
    std::string func;
    int line;

    OpenSSLException(const std::string& msg, const std::string& func, int line,
                     bool opensslerr = false);
};
}  // namespace Crypto
}  // namespace Qtoken
#endif /* !_CRYPTO_EXCEPTION_H */
