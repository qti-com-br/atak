#ifndef CONFIG_H
#define CONFIG_H

#include <string>
#include <fstream>
#include <sstream>
#include <iostream>

#include <Poco/JSON/Parser.h>
#include <Poco/JSON/Query.h>

namespace Qtoken {

class Config {
    public:
        Config() = delete;
        static bool init_config(std::string file_path);
        static std::string get(std::string s);

    private:
        inline static Poco::JSON::Query query = Poco::JSON::Query(Poco::Dynamic::Var());
};

}  // namespace Qtoken

#endif  // CONFIG_H