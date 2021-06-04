#ifndef GLOBALS_H
#define GLOBALS_H

#include <memory>

#ifdef __ANDROID__
#include <android/log.h>
#endif

#include "Poco/AutoPtr.h"
#include "Poco/Event.h"

namespace Qtoken {

class Node;

// doesn't need to be smart-ptr'd as it represents the daemon
extern Node* global_node;
extern std::unique_ptr<Poco::Event> server_wait_for_shutdown;

}  // namespace Qtoken

#endif  // GLOBALS_H
