# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        dreamland

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        utils/well_known_classes.cpp
        utils/selinux_helper.cpp
        dreamland/override_jni_methods.cpp
        main.cpp
        dreamland/dreamland.cpp
        utils/selinux.c

        # XHook
        external/xhook/xhook.c
        external/xhook/xh_core.c
        external/xhook/xh_elf.c
        external/xhook/xh_jni.c
        external/xhook/xh_log.c
        external/xhook/xh_util.c
        external/xhook/xh_version.c
        )


# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

add_library(sandhook STATIC IMPORTED)

get_filename_component(current_source_dir ${CMAKE_CURRENT_SOURCE_DIR} ABSOLUTE)
set(export_dir "${current_source_dir}/../../../../export")
get_filename_component(export_dir ${export_dir} ABSOLUTE)
set_target_properties(sandhook PROPERTIES IMPORTED_LOCATION ${export_dir}/sandhook/${ANDROID_ABI}/libsandhook.a)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        dreamland

        ${log-lib}
        sandhook)

ENABLE_LANGUAGE(ASM)