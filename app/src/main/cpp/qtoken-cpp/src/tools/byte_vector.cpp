#include <cstddef>
#include <iomanip>
#include <sstream>
#include <stdexcept>

#include "../tools/byte_vector.hpp"

using namespace std;

static const string base64_chars =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    "abcdefghijklmnopqrstuvwxyz"
    "0123456789+/";

static inline bool is_base64(unsigned char c) {
    return (isalnum(c) || c == '+' || c == '/');
}

namespace Qtoken {
/*
 * Constructor to build byte_vector from unsigned char *buf pointer. For
 * interoperability with C libraries that use unsigned char for byte.
 */
byte_vector::byte_vector(const unsigned char* buf, size_t siz) {
    this->resize(siz);
    for (size_t i = 0; i < siz; i++) this->at(i) = static_cast<byte>(buf[i]);
}

/*
 * Get unsigned char * pointer to underlying data of vector. Used for C
 * libraries that need to write data to this vector.
 */
unsigned char* byte_vector::c_data() noexcept {
    return reinterpret_cast<unsigned char*>(this->data());
}

/*
 * Get const unsigned char * pointer for underlying data of vector. Used for C
 * libraries that need to read data from this vector.
 */
const unsigned char* byte_vector::c_data() const noexcept {
    return reinterpret_cast<const unsigned char*>(this->data());
}

/*
 * Convert byte vector to hex string. Each byte is two hex characters wide. Can
 * be forced to all upper case.
 */
string byte_vector::ToHexString(bool upper) {
    ostringstream hs;

    hs << hex;
    if (upper)
        hs << uppercase;
    for (const auto& b : *this) hs << setw(2) << setfill('0') << int(b);

    return hs.str();
}

/*
 * Fill in this byte_vector from a hex string.
 */
void byte_vector::FromHexString(const string& value) {
    stringstream hs;
    size_t offset = 0;
    unsigned int buffer;

    if ((value.size() % 2) != 0)
        throw invalid_argument("invalid hex string");

    this->resize(0);
    hs << hex;
    while (offset < value.length()) {
        hs.clear();
        if (!isxdigit(value[offset]) || !isxdigit(value[offset + 1]))
            throw invalid_argument("invalid hex string");

        hs << value.substr(offset, 2);
        hs >> buffer;
        this->push_back(static_cast<byte>(buffer));
        offset += 2;
    }
}

string byte_vector::ToBase64() {
    int i, j;
    size_t in_len = this->size();
    string ret;
    unsigned char char_array_3[3];
    unsigned char char_array_4[4];
    byte* bytes_to_encode = this->data();

    i = j = 0;
    while (in_len--) {
        char_array_3[i++] = (unsigned char)*(bytes_to_encode++);
        if (i == 3) {
            char_array_4[0] = ((char_array_3[0] & 0xfc) >> 2);
            char_array_4[1] = ((char_array_3[0] & 0x03) << 4) +
                              ((char_array_3[1] & 0xf0) >> 4);
            char_array_4[2] = ((char_array_3[1] & 0x0f) << 2) +
                              ((char_array_3[2] & 0xc0) >> 6);
            char_array_4[3] = char_array_3[2] & 0x3f;

            for (i = 0; i < 4; i++) ret += base64_chars[char_array_4[i]];
            i = 0;
        }
    }

    if (i) {
        for (j = i; j < 3; j++) char_array_3[j] = '\0';

        char_array_4[0] = (char_array_3[0] & 0xfc) >> 2;
        char_array_4[1] =
            ((char_array_3[0] & 0x03) << 4) + ((char_array_3[1] & 0xf0) >> 4);
        char_array_4[2] =
            ((char_array_3[1] & 0x0f) << 2) + ((char_array_3[2] & 0xc0) >> 6);
        char_array_4[3] = char_array_3[2] & 0x3f;

        for (j = 0; (j < i + 1); j++) ret += base64_chars[char_array_4[j]];

        while (i++ < 3) ret += '=';
    }

    return ret;
}

void byte_vector::FromBase64(const string& value) {
    size_t in_len = value.length();
    size_t i, j;
    int in_ = 0;
    unsigned char char_array_3[3];
    unsigned char char_array_4[4];

    i = j = 0;
    this->resize(0);

    while (in_len-- && (value[in_] != '=') && is_base64(value[in_])) {
        char_array_4[i++] = value[in_++];

        if (i == 4) {
            for (i = 0; i < 4; i++)
                char_array_4[i] = base64_chars.find(char_array_4[i]);

            char_array_3[0] =
                (char_array_4[0] << 2) + ((char_array_4[1] & 0x30) >> 4);
            char_array_3[1] = ((char_array_4[1] & 0xf) << 4) +
                              ((char_array_4[2] & 0x3c) >> 2);
            char_array_3[2] = ((char_array_4[2] & 0x3) << 6) + char_array_4[3];

            for (i = 0; i < 3; i++)
                this->push_back(static_cast<byte>(char_array_3[i]));

            i = 0;
        }
    }

    if (i) {
        for (j = i; j < 4; j++) char_array_4[j] = 0;

        for (j = 0; j < 4; j++)
            char_array_4[j] = base64_chars.find(char_array_4[j]);

        char_array_3[0] =
            (char_array_4[0] << 2) + ((char_array_4[1] & 0x30) >> 4);
        char_array_3[1] =
            ((char_array_4[1] & 0xf) << 4) + ((char_array_4[2] & 0x3c) >> 2);
        char_array_3[2] = ((char_array_4[2] & 0x3) << 6) + char_array_4[3];

        for (j = 0; j < i - 1; j++)
            this->push_back(static_cast<byte>(char_array_3[j]));
    }
}

/*
 * Grab first count number of bytes from byte_vector as a new vector.
 */
byte_vector byte_vector::SubArray(size_t count) {
    return SubArray(0, count);
}

/*
 * Grab a sub section of the byte_vector (like bytes 2 through 7 of a vector
 * that's 16 bytes long) and return as a new vector.
 */
byte_vector byte_vector::SubArray(size_t offset, size_t count) {
    if ((count + offset) > this->size()) {
        if (offset >= this->size()) {
            // This would make a zero-size byte_vector
            byte_vector empty(0);
            return empty;
        } else
            count = this->size() - offset;
    }

    byte_vector rv(this->begin() + offset, this->begin() + offset + count);
    return rv;
}

void byte_vector::FromString(const string& value) {
    this->resize(value.size());
    for (size_t i = 0; i < value.size(); i++)
        this->at(i) = static_cast<byte>(value[i]);
}

/*
 * Concatenate this byte_vector with another one (append).
 */
byte_vector byte_vector::operator+=(const byte_vector& value) {
    this->insert(this->end(), value.begin(), value.end());
    return *this;
}

/*
 * XOR this byte vector with another one. If other byte_vector is longer than
 * this, we stop after we hit end of this byte_vector.
 */
byte_vector byte_vector::operator^=(const byte_vector& value) {
    size_t siz = (this->size() < value.size() ? this->size() : value.size());

    for (size_t i = 0; i < siz; i++) this->at(i) ^= value.at(i);

    return *this;
}

/*
 * Check if two byte_vectors are equal.
 */
bool byte_vector::operator==(const byte_vector& value) {
    if (this->size() != value.size())
        return false;

    for (size_t i = 0; i < this->size(); i++) {
        if (this->at(i) != value.at(i))
            return false;
    }

    return true;
}

/*
 * Check if two byte_vectors are not equal.
 */
bool byte_vector::operator!=(const byte_vector& value) {
    return !this->operator==(value);
}

byte_vector operator+(const byte_vector& v1, const byte_vector& v2) {
    byte_vector outb(v1);
    outb += v2;
    return outb;
}

byte_vector operator^(const byte_vector& v1, const byte_vector& v2) {
    byte_vector outb(v1);
    outb ^= v2;
    return outb;
}
}  // namespace Qtoken
