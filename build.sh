#!/data/data/com.termux/files/usr/bin/bash

cd /data/data/com.termux/files/home/xfxzmod

echo "===== 1/4 xfxzlib ====="
./gradlew --configure-on-demand :xfxzlib:build
echo ""

echo "===== 2/4 xfxzPluginLoader ====="
./gradlew --configure-on-demand :xfxzPluginLoader:build
echo ""

echo "===== 3/4 MyThon ====="
./gradlew --configure-on-demand :plugins:MyThon:build
echo ""

echo "===== 4/4 xfxzmod ====="
./gradlew --configure-on-demand :xfxzmod:build
echo ""

echo "===== 产物 ====="
ls /data/data/com.termux/files/home/xfxzmod/xfxzlib/build/libs/
ls /data/data/com.termux/files/home/xfxzmod/xfxzPluginLoader/build/libs/
ls /data/data/com.termux/files/home/xfxzmod/plugins/MyThon/build/libs/
ls /data/data/com.termux/files/home/xfxzmod/xfxzmod/build/libs/
