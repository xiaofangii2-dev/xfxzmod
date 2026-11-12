# XFXZ Mod

一个面向 Minecraft Fabric 26.3 服务端的综合模组，包含维度传送、AI 聊天、插件加载、OP 管理和控制台增强。项目由三个子模组组成，通过 Gradle 子项目构建，xfxzlib 和 xfxzpluginloader 会被自动打包进 xfxzmod 的 META-INF/jars/ 目录。

## 项目结构

xfxzmod：主模组，提供 /xftp、/ai、/xfxz 命令和控制台增强。

xfxzlib：依赖库模组，提供加密等基础能力。

xfxzpluginloader：插件加载器，让 .xfxzpe 文件被 Fabric 当作模组加载。

三个模组版本号均为 1.0.0，xfxzmod 的 fabric.mod.json 里对另外两个模组声明了精确版本依赖和 breaks 排除，版本不一致时 Loader 会拒绝加载。

## 功能

### 维度传送

命令格式：/xftp <玩家> <维度ID>

把目标玩家传送到指定维度配置文件里的坐标。维度配置放在 config/xfxz/world_config/<命名空间>/<维度名>.cfg，格式为 namespace、id、coordinates 三个字段，coordinates 是 x y z 三个数字。

首次启动会自动生成 xfxz 命名空间下的 bw2、rural、main 三个默认维度配置，坐标均为 0 64 0。配置文件已存在时不会覆盖。

命令的维度参数使用 word 类型，带冒号的 namespace:id 格式可以直接输入，不需要加引号。命令带 Tab 补全，补全列表来自已加载的维度配置。

### AI 聊天

命令 /ai chat true 开启聊天模式，/ai chat false 关闭。开启后玩家在聊天栏发送的每句话会转发给配置的 AI 服务，回复以系统消息形式返回给该玩家。每个玩家的开关状态独立，按 UUID 记录。

AI 配置放在 config/xfxz/ai/ai.cfg，字段为 api_url、api_key、model。接口格式兼容 OpenAI 的 Chat Completions，请求体包含 model 和 messages 数组，响应从 choices 数组第一个元素的 message 对象里取 content 作为回复文本。api_key 为空时不发送 Authorization 头。

首次启动会自动创建 ai.cfg，三个字段默认为空，由服务器管理员自行填写。

### 插件系统

命令 /xfxz plugins list 列出已安装插件，只读取每个插件 plugin.xfxzjson 里的 name 字段。

命令 /xfxz plugins add <ID> 从 GitHub releases 的 plugins 路径下载对应 ID 的 .xfxzpe 文件到 config/xfxz/plugins/，下载后下次启动生效。

命令 /xfxz plugins rm <ID> 删除对应的 .xfxzpe 文件，重启后生效。

插件文件格式为 .xfxzpe，本质是带 plugin.xfxzjson 元数据的 Fabric 模组 jar。plugin.xfxzjson 放在 META-INF/ 目录下，和 fabric.mod.json 并存。必填字段为 id、version、name、authors、description、depends，其中 depends 里必须包含 xfxzpluginloader 的版本约束。其他字段可选。

xfxzpluginloader 在 preLaunch 阶段扫描 config/xfxz/plugins/ 下的所有 .xfxzpe 文件，按 id 分组，同 ID 多版本时取版本号最大的一个，把选中文件的绝对路径拼进 fabric.addMods 系统属性，交给 Fabric Loader 加载。

三个插件子命令只有 op 为 true 的玩家能执行。

### OP 系统

config/xfxz/user/op.ini 记录玩家 OP 状态。每个玩家一个 section，包含 op、id、uuid 三个字段。uuid 是 24 位小写字母数字，格式为 xxxx-xxxx-xxxx-xxxx-xxxx-xxxx，首次登记时随机生成。

玩家首次进服时自动登记，op 默认为 false。管理员用 /xfxz op add <玩家> 设为 OP，/xfxz op rm <玩家> 取消，/xfxz op ls 列出所有记录和当前状态。

只有 op 为 true 的玩家能执行 /xfxz plugins 下的子命令。

### 版本查询

命令 /xfxz version 输出 xfxzmod 和 xfxzlib 的版本号。

### 控制台增强

服务端控制台使用 TerminalConsoleAppender 和 JLine，提供固定在底部的输入行，日志输出时不会打断正在输入的命令。输入行提示符为 >，不带补全功能。

实现方式是 Mixin 注入 DedicatedServer 的 initServer 方法，在 RETURN 处拦截原版控制台线程的 Thread.start() 调用，替换为自定义的 XfxzConsole。原版线程不启动，由 XfxzConsole 接管控制台输入，命令通过 server.handleConsoleInput 交给原版分发器执行。

### 默认配置

首次启动会在 config/xfxz/ 下自动创建以下内容：

world_config/xfxz/ 目录，包含 bw2.cfg、rural.cfg、main.cfg 三个维度配置。

ai/ai.cfg，AI 配置模板，字段为空。

user/op.ini，空文件，玩家进服时自动写入。

plugins/ 目录，空目录，用于存放 .xfxzpe 插件。

config/xfxz 目录已存在时不会重复创建。

## 构建

项目使用 Gradle 子项目结构，需要 JDK 25。在项目根目录执行：

./gradlew :xfxzmod:build

产物在 xfxzmod/build/libs/xfxzmod-1.0.0.jar，xfxzlib 和 xfxzpluginloader 已嵌套在内。

单独构建子项目：

./gradlew :xfxzlib:build

./gradlew :xfxzPluginLoader:build

## 运行环境

Minecraft 26.3

Fabric Loader 0.19.5 及以上

Fabric API

Java 25

## 许可证

本项目采用 CC BY-NC-SA 4.0 许可协议。允许使用、修改、分发，不得用于商业用途，修改后的版本必须以相同协议开源。详见 LICENSE 文件。

## 作者

xiaofangii2
