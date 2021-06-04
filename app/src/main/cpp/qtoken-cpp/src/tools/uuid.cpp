#include <sstream>

#include "crypto/random.hpp"

#include "tools/byte_vector.hpp"
#include "tools/uuid.hpp"

using namespace std;

namespace Qtoken {
/*
 * Takes the raw 128bits of data and unpacks it to different UUID pieces.
 * - time_low (32bits)
 * - time_mid (16bits)
 * - time_hi_and_version (16bits)
 * - clock_seq (16bits)
 * - node (48bits)
 */
void UUID::Unpack() {
    int ofst;

    byte_vector b(this->raw.begin(),
                  this->raw.begin() + sizeof(this->time_low));
    ofst = sizeof(this->time_low);
    this->time_low = b.GetInt<uint32_t>();

    b = byte_vector(this->raw.begin() + ofst,
                    this->raw.begin() + ofst + sizeof(this->time_mid));
    ofst += sizeof(this->time_mid);
    this->time_mid = b.GetInt<uint16_t>();

    b = byte_vector(
        this->raw.begin() + ofst,
        this->raw.begin() + ofst + sizeof(this->time_hi_and_version));
    ofst += sizeof(this->time_hi_and_version);
    this->time_hi_and_version = b.GetInt<uint16_t>();

    b = byte_vector(this->raw.begin() + ofst,
                    this->raw.begin() + ofst + sizeof(this->clock_seq));
    ofst += sizeof(this->clock_seq);
    this->clock_seq = b.GetInt<uint16_t>();

    this->node = byte_vector(this->raw.begin() + ofst, this->raw.end());
}

/*
 * Pack different UUID pieces into single 128bit array.
 */
void UUID::Pack() {
    byte_vector b;

    this->raw.SetFromInt<uint32_t>(this->time_low);

    b.SetFromInt<uint16_t>(this->time_mid);
    this->raw += b;

    b.SetFromInt<uint16_t>(this->time_hi_and_version);
    this->raw += b;

    b.SetFromInt<uint16_t>(this->clock_seq);
    this->raw += b;

    this->raw += this->node;
}

/*
 * Set raw byte_vector of UUID to proper format based on type
 */
void UUID::SetUuid(const UuidType type, const byte_vector& value) {
    if (value.size() != 16)
        throw invalid_argument("value must be 16 bytes");

    this->raw = value;
    this->Type = type;
    this->Unpack();

    // To put in UUID format, brings it down slightly to 122bits of random data
    // XXXXXXXX-XXXX-VXXX-YXXX-XXXXXXXXXXXX
    // Where Y is one of 8, 9, A, or B
    // And V is version specified by type parameter
    uint16_t type_int = static_cast<uint16_t>(type);
    type_int <<= 12;
    this->time_hi_and_version = (this->time_hi_and_version & 0x0FFF) | type_int;
    this->clock_seq = (this->clock_seq & 0x3FFF) | 0x8000;

    this->Pack();
}

/*
 * Generate random UUID
 */
UUID::UUID() {
    auto bv = Crypto::Random::Bytes(16);

    SetUuid(UuidType::UUIDv4, bv);
}

/*
 * Copy existing UUID.
 */
UUID::UUID(const UUID& u) {
    this->raw = u.raw;
    this->time_low = u.time_low;
    this->time_mid = u.time_mid;
    this->time_hi_and_version = u.time_hi_and_version;
    this->clock_seq = u.clock_seq;
    this->node = u.node;
    this->Type = u.Type;
}

/*
 * Take UUID string and create UUID object.
 */
UUID::UUID(const string& s) {
    string t;
    byte_vector b;

    if (s.length() != 36)
        throw invalid_argument("invalid UUID string");

    if (s[8] != '-' || s[13] != '-' || s[18] != '-' || s[23] != '-')
        throw invalid_argument("invalid UUID string");

    switch (s[14]) {
    case '4':
        this->Type = UuidType::UUIDv4;
        break;
    case '6':
        this->Type = UuidType::UUIDv6;
        break;
    default:
        throw invalid_argument("invalid UUID string");
    }

    switch (s[19]) {
    case '8':
    case '9':
    case 'A':
    case 'a':
    case 'B':
    case 'b':
        break;
    default:
        throw invalid_argument("invalid UUID string");
    }

    try {
        t = s.substr(0, 8);
        b.FromHexString(t);
        this->raw = b;

        t = s.substr(9, 4);
        b.FromHexString(t);
        this->raw += b;

        t = s.substr(14, 4);
        b.FromHexString(t);
        this->raw += b;

        t = s.substr(19, 4);
        b.FromHexString(t);
        this->raw += b;

        t = s.substr(24);
        b.FromHexString(t);
        this->raw += b;
    } catch (...) {
        throw invalid_argument("invalid UUID string");
    }

    this->Unpack();
}

/*
 * Create UUID from raw byte vector
 */
UUID::UUID(const UuidType type, const byte_vector& value) {
    SetUuid(type, value);
}

/*
 * Create new UUIDv6 object from a SHA256 hash. The hash must already be
 * converted to byte_vector.
 */
UUID::UUID(const byte_vector& hash) {
    if (hash.size() != 32)
        throw invalid_argument("invalid SHA256 hash");

    // Truncate SHA256 hash to 16 bytes
    SetUuid(UuidType::UUIDv6, byte_vector(hash.begin(), hash.begin() + 16));
}

/*
 * Convert UUID object to properly formatted string.
 */
string UUID::ToString() {
    byte_vector b;
    ostringstream o;

    b.SetFromInt<uint32_t>(this->time_low);
    o << b.ToHexString();

    b.SetFromInt<uint16_t>(this->time_mid);
    o << "-" << b.ToHexString();

    b.SetFromInt<uint16_t>(this->time_hi_and_version);
    o << "-" << b.ToHexString();

    b.SetFromInt<uint16_t>(this->clock_seq);
    o << "-" << b.ToHexString();

    o << "-" << this->node.ToHexString();

    return o.str();
}

/*
 * Return copy of raw byte vector of UUID.
 */
byte_vector UUID::ToByteVector() {
    byte_vector rv = byte_vector(this->raw);

    return rv;
}

/*
 * Check if UUID object is empty.
 */
bool UUID::empty() {
    return this->raw.size() == 0;
}

/*
 * Getter for Type.
 */
UuidType UUID::GetType() {
    return this->Type;
}

/*
 * Allow access to raw UUID using at() method.
 */
byte UUID::at(const size_t idx) {
    return this->raw.at(idx);
}

/*
 * Check if two UUID objects are equal.
 */
bool UUID::operator==(const UUID& value) {
    if (this->Type != value.Type)
        return false;

    return this->raw == value.raw;
}

/*
 * Check if two UUID objects are not equal.
 */
bool UUID::operator!=(const UUID& value) {
    return !this->operator==(value);
}
}  // namespace Qtoken
