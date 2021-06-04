## Add test srcs and headers
#file(GLOB_RECURSE TEST_SOURCES *.cpp)
#file(GLOB_RECURSE TEST_HEADERS *.hpp)
#
## Create VIN_test executable
#add_executable(VIN_adv ${TEST_SOURCES} ${TEST_HEADERS})
#
## Link the test executable with VIN_LIB + dependancies
#target_link_libraries(VIN_adv
#  PUBLIC
#  VIN_LIB
#)
