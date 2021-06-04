#include "globals/config.hpp"

using namespace Qtoken;

bool Config::init_config(std::string file_path) {
    std::ifstream json_file(file_path);

    if (!json_file) {
        return false;
    }

    std::string json_str((std::istreambuf_iterator<char>(json_file)),
                         std::istreambuf_iterator<char>());

    Poco::JSON::Parser parser;
    Poco::Dynamic::Var result = parser.parse(json_str);
    Poco::JSON::Object::Ptr json_obj =
        result.extract<Poco::JSON::Object::Ptr>();
    Config::query = Poco::JSON::Query(json_obj);

    return true;
}

std::string Config::get(std::string s) {
    Poco::Dynamic::Var result = Config::query.find(s);
    return result.toString();
}
