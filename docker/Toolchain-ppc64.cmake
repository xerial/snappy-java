set(CMAKE_SYSTEM_NAME Linux)
set(CMAKE_SYSTEM_VERSION 1)
set(CMAKE_SYSTEM_PROCESSOR ppc64)

set(cross_triple "ppc64-linux-gnu")

set(CMAKE_C_COMPILER /usr/bin/${cross_triple}-cc)
set(CMAKE_CXX_COMPILER /usr/bin/${cross_triple}-c++)
set(CMAKE_Fortran_COMPILER /usr/bin/${cross_triple}-gfortran)

# Discard path returned by pkg-config and associated with HINTS in module
# like FindOpenSSL.
set(CMAKE_IGNORE_PATH /usr/lib/x86_64-linux-gnu/ /usr/lib/x86_64-linux-gnu/lib/)

set(CMAKE_CROSSCOMPILING_EMULATOR /usr/bin/qemu-ppc64)