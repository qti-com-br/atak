#ifndef _CRYPTO_BIO_H
#define _CRYPTO_BIO_H

#include <string>

#include <openssl/bio.h>

#include "tools/byte_vector.hpp"
#include "crypto/EVP/pkey.hpp"
#include "crypto/rsakey.hpp"
#include "crypto/x509.hpp"

namespace Qtoken {
namespace Crypto {
class CMS;
class EC;
class Bio {
    friend EVP::PKey;
    friend CMS;
    friend EC;
    friend RsaKey;
    friend x509;

protected:
    enum class BioType {
        Memory,
        SecureMemory,
    };

    BIO* bio;

    static const BIO_METHOD* GetBioMethod(BioType);

    Bio();

public:
    ~Bio();

    static Bio& ReadFile(std::string);

    void Close();
    RsaKey& ReadRsaKey();
    EC& ReadEcKey();
};

class MemoryBioBase : public Bio {
protected:
    int GetBufferSize(void* ptr);
    virtual void f() = 0;  // make abstract
public:
    byte_vector GetData();
    void Write(const byte_vector&);
};

class MemoryBio : public MemoryBioBase {
protected:
    void f() override {}

public:
    MemoryBio();
};

class SecureMemoryBio : public MemoryBioBase {
protected:
    void f() override {}

public:
    SecureMemoryBio();
};
}  // namespace Crypto
}  // namespace Qtoken

#endif /* !_CRYPTO_BIO_H */
