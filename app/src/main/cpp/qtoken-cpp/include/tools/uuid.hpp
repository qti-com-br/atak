#ifndef _UUID_H
#define _UUID_H

#include <cstdint>
#include <string>
#include <vector>

#include "byte_vector.hpp"

namespace Qtoken {
enum class UuidType {
    UUIDv4 = 4,
    UUIDv6 = 6,
};

const int UUID_LEN = 36;

class UUID {
private:
    std::uint32_t time_low;
    std::uint16_t time_mid;
    std::uint16_t time_hi_and_version;
    std::uint16_t clock_seq;
    byte_vector node;

    byte_vector raw;
    UuidType Type;

    void Unpack();
    void Pack();
    void SetUuid(const UuidType, const byte_vector&);

public:
    UUID();
    UUID(const UUID&);
    explicit UUID(const std::string&);
    UUID(const UuidType, const byte_vector&);
    explicit UUID(const byte_vector&);

    std::string ToString();
    byte_vector ToByteVector();
    bool empty();
    UuidType GetType();
    std::byte at(const size_t);

    bool operator==(const UUID&);
    bool operator!=(const UUID&);
};
}  // namespace Qtoken

#endif /* !_UUID_H */