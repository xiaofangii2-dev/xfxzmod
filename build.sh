#!/data/data/com.termux/files/usr/bin/bash

cd /data/data/com.termux/files/home/xfxzmod

run_task() {
    echo "===== $1 ====="
    ./gradlew --configure-on-demand "$1"
    echo ""
}

if [ $# -eq 0 ]; then
    run_task ":xfxzlib:build"
    run_task ":xfxzPluginLoader:build"
    run_task ":plugins:MyThon:build"
    run_task ":xfxzmod:build"
    echo "===== 产物 ====="
    ls /data/data/com.termux/files/home/xfxzmod/xfxzlib/build/libs/ 2>/dev/null
    ls /data/data/com.termux/files/home/xfxzmod/xfxzPluginLoader/build/libs/ 2>/dev/null
    ls /data/data/com.termux/files/home/xfxzmod/plugins/MyThon/build/libs/ 2>/dev/null
    ls /data/data/com.termux/files/home/xfxzmod/xfxzmod/build/libs/ 2>/dev/null
else
    for t in "$@"; do
        run_task "$t"
    done
fi
