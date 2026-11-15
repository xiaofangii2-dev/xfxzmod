package main

import (
    "fmt"
    "os"
    "os/exec"
    "path/filepath"
)

const version = "1.0.0"

var allTasks = []string{
    ":xfxzlib:build",
    ":xfxzPluginLoader:build",
    ":plugins:MyThon:build",
    ":xfxzmod:build",
}

func main() {
    if len(os.Args) < 2 {
        printVersion()
        return
    }
    switch os.Args[1] {
    case "version", "-v", "--version", "-version":
        printVersion()
    case "build":
        runBuild(os.Args[2:])
    default:
        fmt.Fprintf(os.Stderr, "未知参数: %s\n", os.Args[1])
        fmt.Fprintln(os.Stderr, "用法: xfxzcli version | xfxzcli build [目标]")
        os.Exit(1)
    }
}

func printVersion() {
    fmt.Printf("xfxzcli %s\n", version)
}

func runBuild(targets []string) {
    exe, err := os.Executable()
    if err != nil {
        fmt.Fprintln(os.Stderr, "获取可执行文件路径失败:", err)
        os.Exit(1)
    }
    dir := filepath.Dir(exe)
    projectRoot := filepath.Dir(dir)

    var tasks []string
    if len(targets) == 0 {
        tasks = allTasks
    } else {
        for _, t := range targets {
            switch t {
            case "lib":
                tasks = append(tasks, ":xfxzlib:build")
            case "loader":
                tasks = append(tasks, ":xfxzPluginLoader:build")
            case "mython":
                tasks = append(tasks, ":plugins:MyThon:build")
            case "mod":
                tasks = append(tasks, ":xfxzmod:build")
            case "all":
                tasks = allTasks
            default:
                fmt.Fprintf(os.Stderr, "未知目标: %s\n", t)
                fmt.Fprintln(os.Stderr, "可选: lib, loader, mython, mod, all")
                os.Exit(1)
            }
        }
    }

    for _, task := range tasks {
        fmt.Printf("===== %s =====\n", task)
        cmd := exec.Command("./gradlew", "--configure-on-demand", task)
        cmd.Dir = projectRoot
        cmd.Stdout = os.Stdout
        cmd.Stderr = os.Stderr
        if err := cmd.Run(); err != nil {
            fmt.Fprintf(os.Stderr, "构建失败: %s (%v)\n", task, err)
            os.Exit(1)
        }
        fmt.Println()
    }

    fmt.Println("===== 产物 =====")
    for _, p := range []string{
        "xfxzlib/build/libs",
        "xfxzPluginLoader/build/libs",
        "plugins/MyThon/build/libs",
        "xfxzmod/build/libs",
    } {
        full := filepath.Join(projectRoot, p)
        entries, err := os.ReadDir(full)
        if err != nil {
            continue
        }
        for _, e := range entries {
            fmt.Println(e.Name())
        }
    }
}
