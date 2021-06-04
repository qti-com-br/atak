#!/bin/bash
cd ../

CMAKE_DEST_FILE=cmake/SourcesandHeaders.cmake

# Create cmake file to add variables to
touch $CMAKE_DEST_FILE

# Setup variable decleration stubs
SOURCES_STUB=$'set(SOURCES_LIST'
HEADERS_STUB=$'set(HEADERS_LIST'

for source_file in $(find src/ -name "*.cpp") ; do
    SOURCES_STUB+=$'\n\r   ${PROJECT_SOURCE_DIR}/'"${source_file}"
done

for header_file in $(find include/ -name "*.hpp") ; do
    HEADERS_STUB+=$'\n\r   ${PROJECT_SOURCE_DIR}/'"${header_file}"
done

SOURCES_STUB+=$')'
HEADERS_STUB+=$')'
echo $SOURCES_STUB > $CMAKE_DEST_FILE
echo "" >> $CMAKE_DEST_FILE
echo $HEADERS_STUB >> $CMAKE_DEST_FILE