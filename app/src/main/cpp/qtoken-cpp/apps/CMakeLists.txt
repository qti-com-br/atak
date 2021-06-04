# Create VIN executable from selected main
#if( (QTOKEN_BUILD_ARCH STREQUAL "x86_64") OR (QTOKEN_BUILD_ARCH STREQUAL "armv7l") )
#        message("Building package for ${QTOKEN_BUILD_ARCH}")
#        add_executable(VIN ${PROJECT_SOURCE_DIR}/apps/main.cpp)
#        # Link the VIN.exe
#        target_link_libraries(VIN PRIVATE VIN_LIB)
#
#        install(TARGETS VIN
#                PUBLIC_HEADER DESTINATION include
#                ARCHIVE DESTINATION lib
#                LIBRARY DESTINATION lib
#        )
#
#        #================================ DEB PACKAGE STRUCTURE HANDLING ================================
#        # Setup deb package
#        install(FILES ${VIN_CONFIG_FILE_PATH}
#                DESTINATION /etc/opt/VIN
#                PERMISSIONS
#                OWNER_WRITE OWNER_READ OWNER_EXECUTE
#                GROUP_WRITE GROUP_READ GROUP_EXECUTE
#                WORLD_WRITE WORLD_READ WORLD_EXECUTE
#        )
#
#        # Create logs directory
#        install(DIRECTORY
#                DESTINATION /var/log/VIN/logs
#                DIRECTORY_PERMISSIONS
#                OWNER_WRITE OWNER_READ OWNER_EXECUTE
#                GROUP_WRITE GROUP_READ GROUP_EXECUTE
#                WORLD_WRITE WORLD_READ WORLD_EXECUTE
#        )
#
#        install(DIRECTORY
#                DESTINATION /opt/VIN/keys
#                DIRECTORY_PERMISSIONS
#                OWNER_WRITE OWNER_READ OWNER_EXECUTE
#                GROUP_WRITE GROUP_READ GROUP_EXECUTE
#        )
#
#        install(DIRECTORY
#                DESTINATION /opt/VIN/outputs
#                DIRECTORY_PERMISSIONS
#                OWNER_WRITE OWNER_READ OWNER_EXECUTE
#                GROUP_WRITE GROUP_READ GROUP_EXECUTE
#        )
#
#        install(DIRECTORY
#                DESTINATION /opt/VIN/receipts/received
#                DIRECTORY_PERMISSIONS
#                OWNER_WRITE OWNER_READ OWNER_EXECUTE
#                GROUP_WRITE GROUP_READ GROUP_EXECUTE
#        )
#
#        install(DIRECTORY
#                DESTINATION /opt/VIN/receipts/sent
#                DIRECTORY_PERMISSIONS
#                OWNER_WRITE OWNER_READ OWNER_EXECUTE
#                GROUP_WRITE GROUP_READ GROUP_EXECUTE
#        )
#        #============================ END DEB PACKAGE STRUCTURE HANDLING ============================
#
#        set(CPACK_PACKAGE_VENDOR "Virgil Systems")
#        set(CPACK_PACKAGE_DESCRIPTION_SUMMARY "Virgil Integrity Network Package")
#        set(CPACK_DEBIAN_FILE_NAME "${PROJECT_NAME}_${PROJECT_VERSION}-${QTOKEN_BUILD_ARCH}.deb")
#        set(CPACK_PACKAGE_VERSION_MAJOR ${PROJECT_VERSION_MAJOR})
#        set(CPACK_PACKAGE_VERSION_MINOR ${PROJECT_VERSION_MINOR})
#        set(CPACK_PACKAGE_VERSION_PATCH ${PROJECT_VERSION_PATCH})
#
#        set(CPACK_GENERATOR "DEB")
#        set(CPACK_DEBIAN_PACKAGE_MAINTAINER "virgilsystems.com")
#        include(CPack)
#endif()