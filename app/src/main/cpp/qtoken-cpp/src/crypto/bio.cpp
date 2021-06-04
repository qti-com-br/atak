#include <openssl/pem.h>

#include "../crypto/bio.hpp"
#include "../crypto/ec.hpp"
#include "../crypto/exception.hpp"

using namespace std;

namespace Qtoken {
namespace Crypto {
const BIO_METHOD* Bio::GetBioMethod(Bio::BioType method) {
    switch (method) {
    case Bio::BioType::Memory:
        return BIO_s_mem();
    case Bio::BioType::SecureMemory:
        return BIO_s_secmem();
    default:
        throw OpenSSLException("Unknown bio method", __func__, __LINE__);
    }
}

Bio::Bio() {
    this->bio = nullptr;
}

Bio::~Bio() {
    Close();
}

/*
 * Create Bio object from file.
 */
Bio& Bio::ReadFile(string file) {
    auto bio = new Bio();
    bio->bio = BIO_new_file(file.c_str(), "r");
    if (bio->bio == nullptr)
        throw OpenSSLException("BIO_new_file", __func__, __LINE__, true);

    return *bio;
}

/*
 * Free bio memory
 */
void Bio::Close() {
    if (this->bio != nullptr) {
        BIO_free(bio);
        this->bio = nullptr;
    }
}

/*
 * Return RsaKey object from bio that contains RSA private key.
 */
RsaKey& Bio::ReadRsaKey() {
    RSA* key = PEM_read_bio_RSAPrivateKey(this->bio, nullptr, nullptr, nullptr);
    if (key == nullptr)
        throw OpenSSLException("PEM_read_bio_RSAPrivateKey", __func__, __LINE__,
                               true);

    auto rkey = new RsaKey(key);
    RSA_free(key);
    return *rkey;
}

/*
 * Return EC object from bio that contains EC private key.
 */
EC& Bio::ReadEcKey() {
    EC_KEY* key =
        PEM_read_bio_ECPrivateKey(this->bio, nullptr, nullptr, nullptr);
    if (key == nullptr)
        throw OpenSSLException("PEM_read_bio_ECPrivateKey", __func__, __LINE__,
                               true);

    auto ekey = new EC(key);
    EC_KEY_free(key);
    return *ekey;
}

/*
 * Get memory and size of memory used by memory bio.
 */
int MemoryBioBase::GetBufferSize(void* ptr) {
    return BIO_get_mem_data(this->bio, ptr);
}

/*
 * Return memory bio contents as byte_vector.
 */
byte_vector MemoryBioBase::GetData() {
    unsigned char* ptr;
    // ptr should now point to this bio's memory, so we don't need to free it
    // after
    int siz = GetBufferSize(&ptr);

    byte_vector data(ptr, siz);
    return data;
}

/*
 * Write byte_vector contents to memory bio.
 */
void MemoryBioBase::Write(const byte_vector& data) {
    int rv = BIO_write(this->bio, data.c_data(), data.size());
    if (rv != (int)data.size())
        throw OpenSSLException("BIO_write", __func__, __LINE__, true);
}

/*
 * Create new MemoryBio object
 */
MemoryBio::MemoryBio() {
    this->bio = BIO_new(GetBioMethod(BioType::Memory));
    if (this->bio == nullptr)
        throw OpenSSLException("BIO_new", __func__, __LINE__, true);
}

/*
 * Create new SecureMemoryBio object
 */
SecureMemoryBio::SecureMemoryBio() {
    this->bio = BIO_new(GetBioMethod(BioType::SecureMemory));
    if (this->bio == nullptr)
        throw OpenSSLException("BIO_new", __func__, __LINE__, true);
}
}  // namespace Crypto
}  // namespace Qtoken
