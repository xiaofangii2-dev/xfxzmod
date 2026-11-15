# XFXZ Mod

一个面向 Minecraft Fabric 26.3 服务端的综合模组，包含维度传送、AI 聊天、插件加载、OP 管理、控制台增强和 Web 面板。项目由主模组、依赖库、插件加载器、示例插件、Web 面板插件、Android 客户端和命令行工具组成。

## 项目结构

xfxzmod：主模组，提供 /xftp、/ai、/xfxz 命令和控制台增强。

xfxzlib：依赖库模组。

xfxzPluginLoader：插件加载器，把 .xfxzpe 插件打包成 Fabric 可加载的 jar。

plugins/MyThon：示例插件。

plugins/XfPanel：Web 面板插件，提供 HTTP API 和 WebSocket 推送。

xfxzapp：Android 客户端，连接 XfPanel 查看状态和执行 RCON 命令。

xfxzcli：Go 编写的命令行工具，用于构建项目。

## 命令

### /xftp

传送玩家到指定维度。

/xftp <玩家> <维度ID>

把目标玩家传送到维度配置文件里的坐标。维度参数支持 Tab 补全，带冒号的 namespace:id 格式可直接输入，不需要加引号。

维度配置放在 config/xfxz/world_config/<命名空间>/<维度名>.cfg，格式为 namespace、id、coordinates 三个字段，coordinates 是 x y z 三个数字。

首次启动生成 xfxz 命名空间下的 bw2、rural、main 三个默认维度，坐标均为 0 64 0。配置文件已存在时不覆盖。

### /ai

AI 聊天开关。

/ai chat true

开启聊天模式，玩家在聊天栏发送的每句话转发给配置的 AI 服务，回复以系统消息返回给该玩家。每个玩家的开关独立，按 UUID 记录。

/ai chat false

关闭聊天模式。

AI 配置放在 config/xfxz/ai/ai.cfg，字段为 api_url、api_key、model，兼容 OpenAI 的 Chat Completions 接口。请求体包含 model 和 messages 数组，响应从 choices[0].message.content 取回复文本。api_key 为空时不发送 Authorization 头。首次启动自动创建，三个字段默认为空。

### /xfxz

主命令，包含版本、重载、OP 管理和插件管理。

/xfxz version

输出 xfxzmod 和 xfxzlib 的版本号。

/xfxz reload

重新读取维度配置、AI 配置和 OP 配置，并重新扫描 config/xfxz/plugins/ 生成插件包 mods/xfxzplugins.jar。配置即时生效，插件本身需要重启服务器才加载。

/xfxz op add <玩家>

把指定玩家设为 OP。

/xfxz op rm <玩家>

取消指定玩家的 OP。

/xfxz op ls

列出所有玩家的 OP 状态。

/xfxz plugins list

列出 config/xfxz/plugins/ 下所有插件的 name 字段。

/xfxz plugins add <ID>

从 GitHub releases 的 plugins 路径下载 <ID>.xfxzpe 到 config/xfxz/plugins/。下载后需要 /xfxz reload 重新生成插件包并重启服务器。

/xfxz plugins rm <ID>

删除 config/xfxz/plugins/ 下对应的 <ID>.xfxzpe。

### OP 系统

config/xfxz/user/op.ini 记录玩家 OP 状态，每个玩家一个 section，包含 op、id、uuid 三个字段。uuid 是 24 位小写字母数字，格式为 xxxx-xxxx-xxxx-xxxx-xxxx-xxxx，首次登记时随机生成。

玩家首次进服时自动登记，op 默认为 false。管理员用 /xfxz op add <玩家> 设为 OP，/xfxz op rm 取消。

只有 op 为 true 的玩家能执行 /xfxz plugins 下的 list、add、rm 三个子命令。

### 控制台增强

服务端控制台使用 TerminalConsoleAppender 和 JLine，提供固定在底部的输入行，提示符为 >，日志输出不会打断正在输入的命令。不带命令补全。

实现方式是通过 Mixin 注入 DedicatedServer 的 initServer 方法，在 RETURN 处拦截原版控制台线程的 Thread.start() 调用，替换为自定义的 XfxzConsole。原版线程不启动，由 XfxzConsole 接管控制台输入，命令通过 server.handleConsoleInput 交给原版分发器执行。

### 插件系统

.xfxzpe 是带 plugin.xfxzjson 元数据的 Fabric 模组 jar。plugin.xfxzjson 放在 assets/ 下，和 fabric.mod.json 并存。必填字段为 id、version、name、authors、description、depends，其中 depends 里必须包含 xfxzpluginloader 的版本约束。其他字段可选。

xfxzPluginLoader 在 preLaunch 阶段扫描 config/xfxz/plugins/，按 id 分组，同 ID 多版本时取版本号最大的一个，把所有选中的插件打包进 mods/xfxzplugins.jar 的 META-INF/jars/ 下，外层 fabric.mod.json 的 jars 字段列出所有嵌套 jar。Fabric 加载外层 jar 时会把嵌套 jar 当插件加载。

### Web 面板 XfPanel

服务端启动后在 18923 端口启动 HTTP 和 WebSocket 服务。token 首次启动生成，写在 config/xfxz/panel.cfg 的 token 字段。

GET /api/status

返回服务器状态 JSON，包含 version、players、maxPlayers、uptimeMs、tps、mspt、memoryUsed、memoryMax。需要请求头 Authorization: Bearer <token>。

POST /api/rcon

发送命令到服务器，body 为命令原文。需要请求头 Authorization: Bearer <token>。

/ws

WebSocket 端点，每 5 秒推送一次和 /api/status 相同的 JSON。

### Android 客户端 xfxzapp

打开后先请求通知权限和 Shizuku 权限，两者都授权后点“下一步”。然后填服务器地址和 token，验证通过后进入主页面。

主页面底部三个 Tab：主页显示服务器状态卡片，RCON 页发送命令，设置页只有“关于”跳转 GitHub。RCON 页在 token 未配对时拒绝执行。

服务器地址填 127.0.0.1:18923（本地）或服务器 IP 加端口，token 填 config/xfxz/panel.cfg 里的值。

### 命令行工具 xfxzcli

Go 编写的构建工具，放在 xfxzcli/ 下。

./xfxzcli version

输出 xfxzcli 版本号。

./xfxzcli build

依次构建 xfxzlib、xfxzPluginLoader、MyThon、XfPanel、xfxzmod。

./xfxzcli build lib

只构建 xfxzlib。可选目标为 lib、loader、mython、panel、mod、all。

## 构建

需要 JDK 25。在项目根目录执行：

./gradlew build

产物在各自子项目的 build/libs/ 下。xfxzmod 的 jar 里嵌套了 xfxzlib 和 xfxzpluginloader。

Android 客户端在 xfxzapp/ 下，用 Gradle 编 APK：

cd xfxzapp
./gradlew assembleDebug

Termux 里编 Android 项目需要设置 android.aapt2FromMavenOverride 指向系统 aapt2。

## 运行环境

Minecraft 26.3

Fabric Loader 0.19.5 及以上

Fabric API

Java 25

Android 客户端需要 Android 8.0 及以上

## 许可证

CC BY-NC-SA 4.0。允许使用、修改、分发，不得用于商业用途，修改后的版本必须以相同协议开源。详见 LICENSE。

## 作者

xiaofangii2

## 引用

~~https://github.com/xiaofang/MyThon~~

本项目作者本人的项目，无需引用。
