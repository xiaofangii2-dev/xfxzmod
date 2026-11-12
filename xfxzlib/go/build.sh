#!/data/data/com.termux/files/usr/bin/bash

cd /data/data/com.termux/files/home/xfxzmod/xfxzlib/go

unset CGO_CFLAGS
unset CGO_LDFLAGS

# Android arm64
CGO_ENABLED=1 GOOS=android GOARCH=arm64 \
  CC=/data/data/com.termux/files/home/xfxzmod/xfxzlib/go/android-ndk/toolchains/llvm/prebuilt/linux-x86_64/bin/clang \
  CGO_CFLAGS="--target=aarch64-linux-android29 --sysroot=/data/data/com.termux/files/home/xfxzmod/xfxzlib/go/android-ndk/toolchains/llvm/prebuilt/linux-x86_64/sysroot" \
  CGO_LDFLAGS="--target=aarch64-linux-android29 --sysroot=/data/data/com.termux/files/home/xfxzmod/xfxzlib/go/android-ndk/toolchains/llvm/prebuilt/linux-x86_64/sysroot" \
  go build -buildmode=c-shared -o /data/data/com.termux/files/home/xfxzmod/xfxzlib/src/main/resources/natives/android-arm64/libxfxzcrypto.so /data/data/com.termux/files/home/xfxzmod/xfxzlib/go/xfxzcrypto.go

# Linux arm64
CGO_ENABLED=1 GOOS=linux GOARCH=arm64 \
  go build -buildmode=c-shared -o /data/data/com.termux/files/home/xfxzmod/xfxzlib/src/main/resources/natives/linux-arm64/libxfxzcrypto.so /data/data/com.termux/files/home/xfxzmod/xfxzlib/go/xfxzcrypto.go

# Windows x64
CGO_ENABLED=1 GOOS=windows GOARCH=amd64 \
  CC=/data/data/com.termux/files/usr/bin/x86_64-w64-mingw32-clang \
  CGO_CFLAGS="--sysroot=/data/data/com.termux/files/usr/opt/llvm-mingw-w64/x86_64-w64-mingw32" \
  CGO_LDFLAGS="--sysroot=/data/data/com.termux/files/usr/opt/llvm-mingw-w64/x86_64-w64-mingw32" \
  go build -buildmode=c-shared -o /data/data/com.termux/files/home/xfxzmod/xfxzlib/src/main/resources/natives/windows-x64/xfxzcrypto.dll /data/data/com.termux/files/home/xfxzmod/xfxzlib/go/xfxzcrypto.go

# Windows x86
CGO_ENABLED=1 GOOS=windows GOARCH=386 \
  CC=/data/data/com.termux/files/usr/bin/i686-w64-mingw32-clang \
  CGO_CFLAGS="--sysroot=/data/data/com.termux/files/usr/opt/llvm-mingw-w64/i686-w64-mingw32" \
  CGO_LDFLAGS="--sysroot=/data/data/com.termux/files/usr/opt/llvm-mingw-w64/i686-w64-mingw32" \
  go build -buildmode=c-shared -o /data/data/com.termux/files/home/xfxzmod/xfxzlib/src/main/resources/natives/windows-x86/xfxzcrypto.dll /data/data/com.termux/files/home/xfxzmod/xfxzlib/go/xfxzcrypto.go

# Windows arm64
CGO_ENABLED=1 GOOS=windows GOARCH=arm64 \
  CC=/data/data/com.termux/files/usr/bin/aarch64-w64-mingw32-clang \
  CGO_CFLAGS="--sysroot=/data/data/com.termux/files/usr/opt/llvm-mingw-w64/aarch64-w64-mingw32" \
  CGO_LDFLAGS="--sysroot=/data/data/com.termux/files/usr/opt/llvm-mingw-w64/aarch64-w64-mingw32" \
  go build -buildmode=c-shared -o /data/data/com.termux/files/home/xfxzmod/xfxzlib/src/main/resources/natives/windows-arm64/xfxzcrypto.dll /data/data/com.termux/files/home/xfxzmod/xfxzlib/go/xfxzcrypto.go

find /data/data/com.termux/files/home/xfxzmod/xfxzlib/src/main/resources/natives -name "*.dll" -o -name "*.so" | sort
