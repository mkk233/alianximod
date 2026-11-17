#!/usr/bin/env bash
# Offline Linux/macOS entry for libxueximod_compute.
# This file was not executed on the Windows machine that added it.
# It does not download packages, change the caller environment, or copy the library.

set -euo pipefail

if [[ "${BASH_SOURCE[0]}" != "$0" ]]; then
    echo "[xueximod_compute] ERROR 9: execute this script; do not source it."
    return 1
fi

write_info() {
    printf '%s\n' "[xueximod_compute] $*"
}

exit_build() {
    printf '%s\n' "[xueximod_compute] ERROR $1: $2"
    exit "$1"
}

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
source_file="$script_dir/src/xueximod_compute.c"
jdk_home=""
explicit_jdk=0
cc_bin=""
cmake_bin=""
build_choice="build"
configuration="Release"
do_clean=0

while [[ $# -gt 0 ]]; do
    case "$1" in
        --jdk-home)
            jdk_home=${2:-}
            explicit_jdk=1
            shift 2
            ;;
        --cc)
            cc_bin=${2:-}
            shift 2
            ;;
        --cmake)
            cmake_bin=${2:-}
            shift 2
            ;;
        --build-dir)
            build_choice=${2:-}
            shift 2
            ;;
        --configuration)
            configuration=${2:-}
            shift 2
            ;;
        --clean)
            do_clean=1
            shift
            ;;
        *)
            exit_build 9 "Unknown argument: $1"
            ;;
    esac
done

write_info "Offline Unix build entry. Not verified on the Windows authoring machine."
case "$(uname -s)" in
    Linux|Darwin)
        ;;
    *)
        exit_build 9 "This host is $(uname -s). Windows builds use build-windows.ps1."
        ;;
esac

if [[ ! -f "$source_file" ]]; then
    exit_build 2 "C source is not present: $source_file. Waiting for the frozen Java/JNI ABI. This script does not invent signatures or write an empty implementation."
fi

case "$build_choice" in
    build|out)
        build_dir="$script_dir/$build_choice"
        ;;
    "$script_dir/build"|"$script_dir/out")
        build_dir=$build_choice
        ;;
    *)
        exit_build 9 "Refusing build directory '$build_choice'. Only build or out under native/compute is allowed."
        ;;
esac

case "$configuration" in
    Release|RelWithDebInfo|Debug)
        ;;
    *)
        exit_build 9 "Unsupported configuration '$configuration'."
        ;;
esac

case "$(uname -m)" in
    x86_64|amd64|aarch64|arm64)
        ;;
    *)
        exit_build 6 "Refusing CPU $(uname -m). The library must be 64-bit, matching the JVM."
        ;;
esac

is_jdk17() {
    local root=$1
    local md=$2
    if [[ -z "$root" || ! -f "$root/include/jni.h" || ! -f "$md" || ! -f "$root/release" ]]; then
        return 1
    fi
    if grep -E 'JAVA_VERSION="17(\.|")' "$root/release" >/dev/null; then
        return 0
    fi
    return 1
}

case "$(uname -s)" in
    Linux) jni_md="include/linux/jni_md.h" ;;
    Darwin) jni_md="include/darwin/jni_md.h" ;;
esac

if [[ -z "$jdk_home" ]]; then
    if [[ -n "${JDK_HOME:-}" ]]; then
        jdk_home=$JDK_HOME
    elif [[ -n "${JAVA_HOME:-}" ]]; then
        jdk_home=$JAVA_HOME
    fi
fi

if ! is_jdk17 "$jdk_home" "$jdk_home/$jni_md"; then
    if [[ "$explicit_jdk" -eq 1 ]]; then
        exit_build 3 "JDK 17 headers were not found at --jdk-home '$jdk_home'. The environment was not modified, and libjvm is not linked."
    fi
    jdk_home=""
    candidates=()
    case "$(uname -s)" in
        Linux)
            candidates=(/usr/lib/jvm/java-17-openjdk /usr/lib/jvm/java-17-openjdk-amd64 /usr/lib/jvm/jdk-17)
            ;;
        Darwin)
            shopt -s nullglob
            candidates=(/Library/Java/JavaVirtualMachines/jdk-17*/Contents/Home)
            shopt -u nullglob
            ;;
    esac
    for candidate in "${candidates[@]+"${candidates[@]}"}"; do
        if is_jdk17 "$candidate" "$candidate/$jni_md"; then
            jdk_home=$candidate
            break
        fi
    done
fi

if ! is_jdk17 "$jdk_home" "$jdk_home/$jni_md"; then
    exit_build 3 "JDK 17 with jni.h and $jni_md was not found. Pass --jdk-home. The environment was not modified, and libjvm is not linked."
fi
write_info "JDK 17: $jdk_home"

if [[ -z "$cmake_bin" ]]; then
    if [[ -n "${CMAKE:-}" ]]; then
        cmake_bin=$CMAKE
    elif command -v cmake >/dev/null 2>&1; then
        cmake_bin=$(command -v cmake)
    else
        exit_build 5 "cmake was not found. Pass --cmake. This script does not download CMake."
    fi
fi
if [[ ! -x "$cmake_bin" ]]; then
    exit_build 5 "cmake is not executable: $cmake_bin"
fi
write_info "CMake: $cmake_bin"

if [[ -n "$cc_bin" && ! -x "$cc_bin" ]]; then
    exit_build 4 "C compiler is not executable: $cc_bin"
fi
if [[ -z "$cc_bin" && -n "${CC:-}" ]]; then
    cc_bin=$CC
fi
if [[ -n "$cc_bin" ]]; then
    write_info "C compiler: $cc_bin"
else
    write_info "C compiler: CMake default (cc, gcc, or clang on PATH)."
fi

if [[ -e "$build_dir" && ! -d "$build_dir" ]]; then
    exit_build 9 "Refusing to replace a non-directory at $build_dir"
fi
if [[ "$do_clean" -eq 1 && -d "$build_dir" ]]; then
    write_info "Removing only the allowed build directory: $build_dir"
    rm -rf -- "$build_dir"
fi
mkdir -p -- "$build_dir"

cmake_args=(
    -S "$script_dir"
    -B "$build_dir"
    -DCMAKE_BUILD_TYPE="$configuration"
    -DJDK_HOME="$jdk_home"
)
if [[ -n "$cc_bin" ]]; then
    cmake_args+=(-DCMAKE_C_COMPILER="$cc_bin")
fi

write_info "Configuring. JNI headers only; the JVM library is not linked."
if ! "$cmake_bin" "${cmake_args[@]}"; then
    exit_build 7 "CMake configure failed. If the C source or ABI is missing, that is expected until it is handed off."
fi

cache_file="$build_dir/CMakeCache.txt"
if [[ ! -f "$cache_file" ]]; then
    exit_build 7 "CMake configure did not write $cache_file"
fi
if ! grep -F 'CMAKE_SIZEOF_VOID_P:INTERNAL=8' "$cache_file" >/dev/null; then
    exit_build 6 "CMake pointer size is not 8 bytes."
fi
if grep -E 'jvm\.lib|libjvm\.so|libjvm\.dylib|-ljvm' "$build_dir/CMakeCache.txt" "$build_dir/build.ninja" "$build_dir"/CMakeFiles/*/flags.make "$build_dir"/CMakeFiles/*/link.txt >/dev/null 2>&1; then
    exit_build 7 "Refusing configure output that references the JVM library."
fi

write_info "Building $configuration."
if ! "$cmake_bin" --build "$build_dir"; then
    exit_build 8 "Build failed."
fi

case "$(uname -s)" in
    Linux) artifact="$build_dir/libxueximod_compute.so" ;;
    Darwin) artifact="$build_dir/libxueximod_compute.dylib" ;;
esac
if [[ ! -f "$artifact" ]]; then
    exit_build 8 "Build returned 0 but did not produce $artifact"
fi
write_info "Library: $artifact"
write_info "Left the library in the build directory. Did not copy it into resources or a game directory."
exit 0
