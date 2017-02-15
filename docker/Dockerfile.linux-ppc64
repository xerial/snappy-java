FROM dockcross/base:latest
MAINTAINER Taro L. Saito "leo@xerial.org"

# Add the cross compiler sources
RUN echo "deb http://emdebian.org/tools/debian/ jessie main" >> /etc/apt/sources.list && \
  dpkg --add-architecture powerpc && \
  curl http://emdebian.org/tools/debian/emdebian-toolchain-archive.key | apt-key add -

RUN apt-get update && apt-get install -y \
  crossbuild-essential-powerpc \
  gfortran-powerpc-linux-gnu \
  libbz2-dev:powerpc \
  libexpat1-dev:powerpc \
  ncurses-dev:powerpc \
  libssl-dev:powerpc

WORKDIR /usr/src

RUN apt-get update && \
  apt-get install -y libglib2.0-dev zlib1g-dev libpixman-1-dev && \
  curl -L http://wiki.qemu-project.org/download/qemu-2.6.0.tar.bz2 | tar xj && \
  cd qemu-2.6.0 && \
  ./configure --target-list=ppc64-linux-user --prefix=/usr && \
  make -j$(nproc) && \
  make install && \
  cd .. && rm -rf qemu-2.6.0

ENV CROSS_TRIPLE powerpc-linux-gnu
ENV CROSS_ROOT /usr/${CROSS_TRIPLE}
ENV AS=/usr/bin/${CROSS_TRIPLE}-as \
    AR=/usr/bin/${CROSS_TRIPLE}-ar \
    CC=/usr/bin/${CROSS_TRIPLE}-gcc \
    CPP=/usr/bin/${CROSS_TRIPLE}-cpp \
    CXX=/usr/bin/${CROSS_TRIPLE}-g++ \
    LD=/usr/bin/${CROSS_TRIPLE}-ld

ENV DEFAULT_DOCKCROSS_IMAGE dockcross/linux-ppc64
WORKDIR /work

# Note: Toolchain file support is currently in debian Experimental according to:
# https://wiki.debian.org/CrossToolchains#In_jessie_.28Debian_8.29
# We can switch to that when it becomes stable.
COPY Toolchain-ppc64.cmake /usr/lib/${CROSS_TRIPLE}/Toolchain.cmake
ENV CMAKE_TOOLCHAIN_FILE /usr/lib/${CROSS_TRIPLE}/Toolchain.cmake

# Build-time metadata as defined at http://label-schema.org
ARG BUILD_DATE
ARG IMAGE
ARG VCS_REF
ARG VCS_URL
LABEL org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.name=$IMAGE \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.vcs-url=$VCS_URL \
      org.label-schema.schema-version="1.0"