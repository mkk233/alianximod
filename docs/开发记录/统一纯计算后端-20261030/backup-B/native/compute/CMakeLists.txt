cmake_minimum_required(VERSION 3.21)

# The C kernel is intentionally absent until author A freezes the Java/JNI ABI.
# Do not add an empty translation unit or invent JNI signatures here.
if(NOT EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/src/xueximod_compute.c")
  message(FATAL_ERROR
    "Missing ${CMAKE_CURRENT_SOURCE_DIR}/src/xueximod_compute.c. "
    "Waiting for the frozen Java/JNI ABI. "
    "This configure step does not create a stub implementation.")
endif()

project(xueximod_compute LANGUAGES C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_C_EXTENSIONS OFF)

# JNI headers only. find_package(JNI) would also look for the JVM library.
set(CMAKE_DISABLE_FIND_PACKAGE_JNI TRUE)
set(FETCHCONTENT_FULLY_DISCONNECTED ON CACHE BOOL
  "Offline configure. Do not download dependencies." FORCE)

if(NOT JDK_HOME AND DEFINED ENV{JDK_HOME} AND NOT "$ENV{JDK_HOME}" STREQUAL "")
  set(JDK_HOME "$ENV{JDK_HOME}")
endif()
if(NOT JDK_HOME AND DEFINED ENV{JAVA_HOME} AND NOT "$ENV{JAVA_HOME}" STREQUAL "")
  set(JDK_HOME "$ENV{JAVA_HOME}")
endif()
if(NOT JDK_HOME)
  message(FATAL_ERROR
    "JDK_HOME is required. Pass -DJDK_HOME=<jdk-17> or set JDK_HOME/JAVA_HOME for this process. "
    "JNI needs include/jni.h only. Do not link jvm.lib, libjvm.so, or libjvm.dylib.")
endif()
file(TO_CMAKE_PATH "${JDK_HOME}" JDK_HOME)

if(NOT CMAKE_SIZEOF_VOID_P EQUAL 8)
  message(FATAL_ERROR
    "Refusing pointer size ${CMAKE_SIZEOF_VOID_P} bytes. "
    "xueximod_compute must be 64-bit to match the 64-bit JVM.")
endif()
if(DEFINED CMAKE_GENERATOR_PLATFORM AND NOT "${CMAKE_GENERATOR_PLATFORM}" STREQUAL "")
  if(NOT "${CMAKE_GENERATOR_PLATFORM}" STREQUAL "x64")
    message(FATAL_ERROR
      "Refusing CMAKE_GENERATOR_PLATFORM=${CMAKE_GENERATOR_PLATFORM}. Use x64.")
  endif()
endif()

# Drop whole flags that reorder floating-point math or contract mul/add into FMA.
# Java 17 evaluates in source order and does not contract expressions into Math.fma.
foreach(flag_var
    CMAKE_C_FLAGS
    CMAKE_C_FLAGS_DEBUG
    CMAKE_C_FLAGS_RELEASE
    CMAKE_C_FLAGS_RELWITHDEBINFO
    CMAKE_C_FLAGS_MINSIZEREL)
  if(DEFINED ${flag_var})
    set(_flags " ${${flag_var}} ")
    foreach(_bad IN ITEMS
        " /fp:fast " " -fp:fast " " /Qfast_transcendentals "
        " -ffast-math " " -Ofast " " /GL " " -GL " " /LTCG " " -LTCG "
        " -flto " " -flto=thin " " -flto=auto " " -flto=full "
        " -funsafe-math-optimizations " " -fassociative-math " " -freciprocal-math ")
      string(REPLACE "${_bad}" " " _flags "${_flags}")
    endforeach()
    string(REPLACE " -ffp-contract=fast " " -ffp-contract=off " _flags "${_flags}")
    string(REPLACE " -ffp-contract=on " " -ffp-contract=off " _flags "${_flags}")
    string(STRIP "${_flags}" _flags)
    set(${flag_var} "${_flags}")
  endif()
endforeach()
if(NOT MSVC)
  string(REPLACE "-O3" "-O2" CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE}")
endif()

set(JNI_INCLUDE "${JDK_HOME}/include")
if(WIN32)
  set(JNI_MD_INCLUDE "${JDK_HOME}/include/win32")
elseif(APPLE)
  set(JNI_MD_INCLUDE "${JDK_HOME}/include/darwin")
else()
  set(JNI_MD_INCLUDE "${JDK_HOME}/include/linux")
endif()
if(NOT EXISTS "${JNI_INCLUDE}/jni.h")
  message(FATAL_ERROR
    "jni.h not found at ${JNI_INCLUDE}/jni.h. Pass a JDK 17 home, not a JRE. "
    "Linking the JVM is not required and is not done.")
endif()
if(NOT EXISTS "${JNI_MD_INCLUDE}/jni_md.h")
  message(FATAL_ERROR "jni_md.h not found at ${JNI_MD_INCLUDE}/jni_md.h")
endif()

add_library(xueximod_compute SHARED
  "${CMAKE_CURRENT_SOURCE_DIR}/src/xueximod_compute.c")
target_include_directories(xueximod_compute PRIVATE
  "${JNI_INCLUDE}"
  "${JNI_MD_INCLUDE}")
target_compile_definitions(xueximod_compute PRIVATE
  XUEXIMOD_COMPUTE_FP_PRECISE=1)

set_target_properties(xueximod_compute PROPERTIES
  OUTPUT_NAME "xueximod_compute"
  RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}"
  LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}"
  ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}")
foreach(cfg Debug Release RelWithDebInfo MinSizeRel)
  string(TOUPPER "${cfg}" cfg_upper)
  set_target_properties(xueximod_compute PROPERTIES
    "RUNTIME_OUTPUT_DIRECTORY_${cfg_upper}" "${CMAKE_BINARY_DIR}"
    "LIBRARY_OUTPUT_DIRECTORY_${cfg_upper}" "${CMAKE_BINARY_DIR}"
    "ARCHIVE_OUTPUT_DIRECTORY_${cfg_upper}" "${CMAKE_BINARY_DIR}")
endforeach()

if(WIN32)
  set_target_properties(xueximod_compute PROPERTIES
    PREFIX ""
    SUFFIX ".dll")
endif()

if(MSVC)
  target_compile_options(xueximod_compute PRIVATE
    /std:c11
    /fp:precise
    /W3)
else()
  target_compile_options(xueximod_compute PRIVATE
    -std=c11
    -ffp-contract=off
    -fno-fast-math
    -fno-unsafe-math-optimizations
    -fno-associative-math
    -fno-reciprocal-math
    -Wall)
  if(CMAKE_C_COMPILER_ID STREQUAL "GNU")
    target_compile_options(xueximod_compute PRIVATE
      -fexcess-precision=standard)
  endif()
endif()

message(STATUS "xueximod_compute output base name: xueximod_compute")
message(STATUS "xueximod_compute JDK headers: ${JNI_INCLUDE} and ${JNI_MD_INCLUDE}")
message(STATUS "xueximod_compute FP policy: C11, precise order, no fast-math, no FMA contraction, no LTO")
message(STATUS "xueximod_compute does not link the JVM")
