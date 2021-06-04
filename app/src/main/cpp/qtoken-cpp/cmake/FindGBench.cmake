## Setup GoogleBench
#configure_file(${CMAKE_MODULE_PATH}/DownloadGBench.cmake googlebenchmark-download/CMakeLists.txt)
#execute_process(COMMAND ${CMAKE_COMMAND} -G "${CMAKE_GENERATOR}" .
#  RESULT_VARIABLE result
#  WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/googlebenchmark-download )
#if(result)
#  message(FATAL_ERROR "CMake step for googlebenchmark failed: ${result}")
#endif()
#execute_process(COMMAND ${CMAKE_COMMAND} --build .
#  RESULT_VARIABLE result
#  WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/googlebenchmark-download )
#if(result)
#  message(FATAL_ERROR "Build step for googlebenchmark failed: ${result}")
#endif()
#
## Add googlebench directly to our build. This defines
## the benchmark and benchmark_main targets.
#add_subdirectory(${CMAKE_CURRENT_BINARY_DIR}/googlebenchmark-src
#                 ${CMAKE_CURRENT_BINARY_DIR}/googlebenchmark-build
#                 EXCLUDE_FROM_ALL)