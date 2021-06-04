#include "globals/globals.hpp"

namespace Qtoken {

Node* global_node;
std::unique_ptr<Poco::Event> server_wait_for_shutdown(new Poco::Event());

}  // namespace Qtoken
