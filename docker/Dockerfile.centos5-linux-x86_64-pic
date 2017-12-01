FROM centos:5
MAINTAINER Taro L. Saito <leo@xerial.org>

RUN sed -i 's/enabled=1/enabled=0/' /etc/yum/pluginconf.d/fastestmirror.conf
RUN sed -i 's/mirrorlist/#mirrorlist/' /etc/yum.repos.d/*.repo
RUN sed -i 's|#baseurl=http://mirror.centos.org/centos/$releasever|baseurl=http://vault.centos.org/5.11|' /etc/yum.repos.d/*.repo

RUN yum -y install make gcc gcc-c++ glibc-devel perl wget bzip2 curl \
 && rm -rf /var/lib/apt/lists/*

RUN mkdir /tmp/work \
  && cd /tmp/work \
  && wget http://www.netgull.com/gcc/releases/gcc-4.8.3/gcc-4.8.3.tar.gz \
  && tar xvfz gcc-4.8.3.tar.gz \
  && cd gcc-4.8.3 \
  && ./contrib/download_prerequisites \
  && cd .. \
  && mkdir objdir

RUN cd /tmp/work/objdir \
  && ../gcc-4.8.3/configure --prefix=/usr/local/gcc-4.8.3 CXXFLAGS=-fPIC CFLAGS=-fPIC --enable-languages=c,c++ \
  && make

RUN cd /tmp/work/objdir \
  && make install \
  && rm -rf /tmp/work

#RUN mkdir /tmp/cmake \
#  && cd /tmp/cmake \
#  && wget --no-check-certificate https://cmake.org/files/v3.10/cmake-3.10.0.tar.gz \
#  && tar xvfz cmake-3.10.0.tar.gz \
#  && cd cmake-3.10.0 \
#  && CXX=/usr/local/gcc-4.8.3/bin/g++ ./bootstrap \
#  && make \
#  && make install \
#  && rm -rf /tmp/cmake

ENV PATH /usr/local/gcc-4.8.3/bin:$PATH
ENV LD_LIBRARY_PATH /usr/local/gcc-4.8.3/lib64/:$LD_LIBRARY_PATH

WORKDIR /work
