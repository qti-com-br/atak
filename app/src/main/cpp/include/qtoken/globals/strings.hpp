#ifndef STRINGS_H
#define STRINGS_H

#include <string>

namespace Qtoken {

inline const std::string usage_message(
    "Set up a qtoken node for secure file transmission across a Virgil systems "
    "network.");
inline const std::string config_io_failure(
    "I/O error while reading config file \"");
inline const std::string release_year("2020");
inline const std::string pub_key_request("pubkey");
inline const std::string cr_server_unknown("Unknown request");
inline const std::string receipt_recieved("Receipt received");
inline const std::string cr_gather_fail("Gather failed");
inline const std::string init_stream_session("init stream");
inline const std::string end_stream_session("end stream");

}  // namespace Qtoken

#endif  // STRINGS_H
