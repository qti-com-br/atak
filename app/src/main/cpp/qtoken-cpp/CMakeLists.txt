cmake_minimum_required(VERSION 3.10...3.18.1)

# Force C++17
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# This is needed to make linking VIN_LIB to the Qtoken.so possible
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Project name
project(QToken-CPP VERSION 1.1.0
                   DESCRIPTION "Virgil Integrity Network 2021"
)

# Set architecture variable
# TODO: Setup cached variable
if(NOT QTOKEN_BUILD_ARCH)
        set(QTOKEN_BUILD_ARCH x86_64)
endif()
IF(NOT EXISTS ${PROJECT_SOURCE_DIR}/libs/${QTOKEN_BUILD_ARCH})
        message("**********************************************")
        message("** NO LIBRARIES FOLDER FOUND FOR THIS ARCH. **")
        message("** PLEASE REFER TO ../docs/setup FOR HOW TO **")
        message("**  ACQUIRE THE NECESSARY BUILD LIBRARIES.  **")
        message("**********************************************")
        message(FATAL_ERROR "No build libraries found in ../libs.")
endif()
message("==== BUILDING FOR ${QTOKEN_BUILD_ARCH} ===")

option(BUILD_TESTING "Build tests for QToken" ON)
option(BUILD_DOCS "Build docs with doxygen" OFF)
option(KEEP_SOURCES "Don't regenerate the sources and headers variable file" ON)

# Setting Path Variables
set(LIB_DIR ${PROJECT_SOURCE_DIR}/../libs/arm64-v8a)
set(SOURCES_DIR ${PROJECT_SOURCE_DIR}/src)
set(INCLUDES_DIR ${PROJECT_SOURCE_DIR}/../include)

set(AFF3CTPATH ${INCLUDES_DIR}/aff3ct-2.3.5)
set(POCO_LIB_DIR ${LIB_DIR})
set(POCO_DIR ${INCLUDES_DIR}/Poco)
set(VIN_CONFIG_FILE_PATH ${PROJECT_SOURCE_DIR}/defaults.cfg)

# Recurse into the modules path and run module finding/loading cmakes
list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_LIST_DIR}/cmake")

# Get a list of the sources and headers for QToken project
if(NOT KEEP_SOURCES)
        execute_process(COMMAND ${PROJECT_SOURCE_DIR}/scripts/cmake_get_sources_and_headers.sh)
endif()

# Include generated sources and headers variables
include("${CMAKE_MODULE_PATH}/SourcesandHeaders.cmake")

find_package(Threads REQUIRED)

find_package(Doxygen)
if(Doxygen_FOUND AND BUILD_DOCS)
        add_subdirectory(docs)
else()
        message(STATUS "Doxygen not found, not building docs.")
endif()

# Link shared object libraries
add_library(aff3ct SHARED IMPORTED)
set_target_properties(aff3ct PROPERTIES IMPORTED_LOCATION
        ${LIB_DIR}/libaff3ct-2.3.5.so
)

add_library(crypto SHARED IMPORTED)
set_target_properties(crypto PROPERTIES IMPORTED_LOCATION
        ${LIB_DIR}/libcrypto.so
)

add_library(kademlia SHARED IMPORTED)
set_target_properties(kademlia PROPERTIES IMPORTED_LOCATION
        ${LIB_DIR}/libkademlia.so
)

# -- Poco -----------------------------------------------
add_library(PocoFoundation SHARED IMPORTED)
set_target_properties(PocoFoundation PROPERTIES IMPORTED_LOCATION
        ${POCO_LIB_DIR}/libPocoFoundation.so
)

add_library(PocoNet SHARED IMPORTED)
set_target_properties(PocoNet PROPERTIES IMPORTED_LOCATION
        ${POCO_LIB_DIR}/libPocoNet.so
)


add_library(PocoXML SHARED IMPORTED)
set_target_properties(PocoXML PROPERTIES IMPORTED_LOCATION
        ${POCO_LIB_DIR}/libPocoXML.so
)

add_library(PocoUtil SHARED IMPORTED)
set_target_properties(PocoUtil PROPERTIES IMPORTED_LOCATION
        ${POCO_LIB_DIR}/libPocoUtil.so
)

add_library(PocoEncodings SHARED IMPORTED)
set_target_properties(PocoEncodings PROPERTIES IMPORTED_LOCATION
        ${POCO_LIB_DIR}/libPocoEncodings.so
)
add_library(PocoData SHARED IMPORTED)
set_target_properties(PocoData PROPERTIES IMPORTED_LOCATION
        ${POCO_LIB_DIR}/libPocoData.so
)

add_library(PocoMongoDB SHARED IMPORTED)
set_target_properties(PocoMongoDB PROPERTIES IMPORTED_LOCATION
        ${POCO_LIB_DIR}/libPocoMongoDB.so
)

add_library(PocoRedis SHARED IMPORTED)
set_target_properties(PocoRedis PROPERTIES IMPORTED_LOCATION
        ${POCO_LIB_DIR}/libPocoRedis.so
)

add_library(PocoZip SHARED IMPORTED)
set_target_properties(PocoZip PROPERTIES IMPORTED_LOCATION
        ${POCO_LIB_DIR}/libPocoZip.so
)


add_library(PocoJSON SHARED IMPORTED)
set_target_properties(PocoJSON PROPERTIES IMPORTED_LOCATION
        ${POCO_LIB_DIR}/libPocoJSON.so
        )
#install(DIRECTORY ${POCO_LIB_DIR}/
#        DESTINATION lib
#        FILES_MATCHING
#          REGEX "libPocoFoundation?.*\.so"
#          REGEX "libPocoNet\.so?.*"
#          REGEX "libPocoJSON?.*\.so"
#          REGEX "libPocoXML?.*\.so"
#          REGEX "libPocoUtil?.*\.so"
#          REGEX "libPocoEncodings?.*\.so"
#          REGEX "libPocoData\.so?"
#          REGEX "libPocoMongoDB?.*\.so"
#          REGEX "libPocoRedis?.*\.so"
#          REGEX "libPocoZip?.*\.so"
#          PATTERN "cmake" EXCLUDE
#)
# ----------------------------------------------- Poco --

# Include extra modules
include("${CMAKE_MODULE_PATH}/AddCPPCheck.cmake") # 'make cppcheck'
include("${CMAKE_MODULE_PATH}/AddClangFormat.cmake") # 'make format_[example.cpp]'

include_directories(${INCLUDES_DIR})
include_directories(${INCLUDES_DIR}/aff3ct-2.3.5)
include_directories(${INCLUDES_DIR}/aff3ct-2.3.5/aff3ct)
include_directories(${INCLUDES_DIR}/aff3ct-2.3.5/MIPP)
include_directories(${INCLUDES_DIR}/aff3ct-2.3.5/rang)
include_directories(${INCLUDES_DIR}/libconfig)
include_directories(${INCLUDES_DIR}/kademlia)
include_directories(${INCLUDES_DIR}/openssl)

include_directories(${POCO_DIR})
include_directories(${POCO_DIR}/Net)
include_directories(${POCO_DIR}/XML)
include_directories(${POCO_DIR}/Util)
include_directories(${POCO_DIR}/Data)
include_directories(${POCO_DIR}/Data/MySQL)
include_directories(${POCO_DIR}/Data/ODBC)
include_directories(${POCO_DIR}/Data/SQLite)
include_directories(${POCO_DIR}/MongoDB)
include_directories(${POCO_DIR}/Redis)
include_directories(${POCO_DIR}/Zip)
include_directories(${POCO_DIR}/Crypto)
include_directories(${POCO_DIR}/DOM)
include_directories(${POCO_DIR}/Dynamic)
include_directories(${POCO_DIR}/SAX)
include_directories(${POCO_DIR}/JSON)

# Cmake for compiled library code
# NOTE: THIS SHOULD ALWAYS GO BEFORE add_subdirectory(apps) CMAKE
#       TO MAKE SURE THE apps exe HAS THE LIBS IT NEEDS.
add_subdirectory(src)