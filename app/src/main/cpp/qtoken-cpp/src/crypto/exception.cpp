#include "crypto/exception.hpp"

#include <openssl/err.h>

using namespace std;

namespace Qtoken {
namespace Crypto {
OpenSSLException::OpenSSLException(const string& msg, const string& func,
                                   int line, bool opensslerr) {
    this->msg = msg;
    this->func = func;
    this->line = line;
    if (opensslerr) {
        ERR_load_crypto_strings();

        string oerr = ERR_error_string(ERR_peek_last_error(), nullptr);
        this->msg += ": " + oerr;
    }
}
}  // namespace Crypto
}  // namespace Qtoken