#ifndef GLOBALS_H
#define GLOBALS_H

#include <android/log.h>
#include <libconfig.h++>

namespace Qtoken {

class Node;

extern Node* global_node;
extern libconfig::Config *cfg;

}  // namespace Qtoken

#endif // GLOBALS_H