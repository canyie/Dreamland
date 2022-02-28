cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

set(C_FLAGS "-Wall -Wextra -ffixed-x18 -fvisibility=hidden -fvisibility-inlines-hidden -fno-exceptions -fno-rtti -flto=thin -Wno-builtin-macro-redefined -D__FILE__=__FILE_NAME__")
set(LINKER_FLAGS "-fuse-ld=lld -flto=thin -ffixed-x18 -Wl,--hash-style=both -Wl,--unresolved-symbols=ignore-all")

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${C_FLAGS}")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${C_FLAGS}")

set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} ${LINKER_FLAGS}")
set(CMAKE_MODULE_LINKER_FLAGS "${CMAKE_MODULE_LINKER_FLAGS} ${LINKER_FLAGS}")
set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} ${LINKER_FLAGS}")

add_definitions(-DRIRU_NEW_MODULE_API_VERSION=${RIRU_NEW_MODULE_API_VERSION})
add_definitions(-DRIRU_MODULE_VERSION_NAME="${RIRU_MODULE_VERSION_NAME}")
add_definitions(-DDREAMLAND_VERSION_CODE=${DREAMLAND_VERSION_CODE})

add_library(riru_dreamland
        SHARED

        utils/well_known_classes.cpp
        main.cpp
        dreamland/dreamland.cpp
        dreamland/android.cpp
        dreamland/resources_hook.cpp
        dreamland/binder.cpp
        dreamland/dex_loader.cpp
        )

find_library(log-lib log)
find_package(cxx REQUIRED CONFIG)
find_package(dobby REQUIRED CONFIG)

add_library(pine STATIC IMPORTED)
add_library(pine-enhances STATIC IMPORTED)

get_filename_component(current_source_dir ${CMAKE_CURRENT_SOURCE_DIR} ABSOLUTE)

set(external_dir "${current_source_dir}/../../../../external")
get_filename_component(export_dir ${external_dir} ABSOLUTE)

set_target_properties(pine PROPERTIES IMPORTED_LOCATION ${external_dir}/pine/${ANDROID_ABI}/libpine.a)
set_target_properties(pine-enhances PROPERTIES IMPORTED_LOCATION ${external_dir}/pine-enhances/${ANDROID_ABI}/libpine-enhances.a)

target_link_libraries(riru_dreamland ${log-lib} cxx::cxx dobby::dobby pine pine-enhances)

ENABLE_LANGUAGE(ASM)
