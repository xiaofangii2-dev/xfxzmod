plugins {
    id("net.fabricmc.fabric-loom")
}

version = "1.0.0"
group = "org.xiaofang_dev"

base {
    archivesName.set("xfxzPluginLoader")
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
}

dependencies {
    minecraft("com.mojang:minecraft:26.3")
    implementation("net.fabricmc:fabric-loader:0.19.5")
    implementation("net.fabricmc.fabric-api:fabric-api:0.162.0+26.3")
    implementation("com.google.code.gson:gson:2.11.0")
    include("com.google.code.gson:gson:2.11.0")
}

val copyPluginJar = tasks.register<Copy>("copyPluginJar") {
    dependsOn(project(":xfxzPluginLoader:plugin").tasks.named("jar"))
    from(project(":xfxzPluginLoader:plugin").layout.buildDirectory.file("libs/plugin.jar"))
    into(layout.projectDirectory.dir("src/main/resources"))
}

tasks.named("processResources") {
    dependsOn(copyPluginJar)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
