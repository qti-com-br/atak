# include modules include("${CMAKE_MODULE_PATH}/AddClangFormat.cmake")
# Format all source files with 'make formatall'

# get all project files
file(GLOB_RECURSE ALL_SOURCE_ALL_TEST_FILES test/*.cpp test/*.hpp src/*.cpp src/*.hpp)

foreach (SOURCE_FILE ${ALL_SOURCE_ALL_TEST_FILES})
        get_filename_component(FILE_NAME "${SOURCE_FILE}" NAME ABSOLUTE)

        add_custom_target(
                format_${FILE_NAME}
                COMMAND clang-format
                -style=file
                -i
                ${SOURCE_FILE}
        )
endforeach ()

add_custom_target(
        formatall
        COMMAND clang-format
        -style=file
        -i
        ${ALL_SOURCE_ALL_TEST_FILES}
)