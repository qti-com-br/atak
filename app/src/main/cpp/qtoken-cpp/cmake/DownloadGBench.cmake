cmake_minimum_required (VERSION 3.5.1)

project(googlebenchmark-download NONE)

include(ExternalProject)

ExternalProject_Add(googlebenchmark
  GIT_REPOSITORY    https://github.com/google/benchmark.git
  GIT_TAG           main
  SOURCE_DIR        "${CMAKE_CURRENT_BINARY_DIR}/googlebenchmark-src"
  BINARY_DIR        "${CMAKE_CURRENT_BINARY_DIR}/googlebenchmark-build"
  CONFIGURE_COMMAND ""
  BUILD_COMMAND     ""
  INSTALL_COMMAND   ""
  TEST_COMMAND      "" 
)