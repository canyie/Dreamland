cmake_minimum_required(VERSION 3.4.1)

add_definitions(-DRIRU_NEW_MODULE_API_VERSION=${RIRU_NEW_MODULE_API_VERSION})
add_definitions(-DRIRU_MODULE_VERSION_NAME="${RIRU_MODULE_VERSION_NAME}")

add_library(riru_dreamland
        SHARED

        utils/well_known_classes.cpp
        main.cpp
        dreamland/dreamland.cpp
        dreamland/android.cpp
        dreamland/resources_hook.cpp
        dreamland/binder.cpp
        dreamland/dex_loader.cpp

        # XHook
        external/xhook/xhook.c
        external/xhook/xh_core.c
        external/xhook/xh_elf.c
        external/xhook/xh_log.c
        external/xhook/xh_util.c
        external/xhook/xh_version.c
        )

find_library(log-lib log)

add_library(pine STATIC IMPORTED)
add_library(pine-enhances STATIC IMPORTED)
add_library(dobby STATIC IMPORTED)

get_filename_component(current_source_dir ${CMAKE_CURRENT_SOURCE_DIR} ABSOLUTE)

set(external_dir "${current_source_dir}/../../../../external")
get_filename_component(export_dir ${external_dir} ABSOLUTE)

set_target_properties(pine PROPERTIES IMPORTED_LOCATION ${external_dir}/pine/${ANDROID_ABI}/libpine.a)
set_target_properties(pine-enhances PROPERTIES IMPORTED_LOCATION ${external_dir}/pine-enhances/${ANDROID_ABI}/libpine-enhances.a)
set_target_properties(dobby PROPERTIES IMPORTED_LOCATION ${external_dir}/dobby/${ANDROID_ABI}/libdobby.a)

target_link_libraries(riru_dreamland ${log-lib} pine pine-enhances dobby)

ENABLE_LANGUAGE(ASM)