plugins {
    id("net.fabricmc.fabric-loom")
    `java-library`
}

val modVersion = "1.0.0"

version = modVersion
group = "org.xiaofang_dev"

base {
    archivesName.set("XfPanel")
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
}

dependencies {
    minecraft("com.mojang:minecraft:26.3")
    implementation("net.fabricmc:fabric-loader:0.19.5")
    implementation("net.fabricmc.fabric-api:fabric-api:0.162.0+26.3")

    implementation("org.eclipse.jetty:jetty-server:11.0.20")
    implementation("org.eclipse.jetty:jetty-servlet:11.0.20")
    implementation("org.eclipse.jetty.websocket:websocket-jetty-server:11.0.20")
    implementation("org.eclipse.jetty.websocket:websocket-jetty-api:11.0.20")
    implementation("org.eclipse.jetty.toolchain:jetty-jakarta-servlet-api:5.0.2")

    include("org.eclipse.jetty:jetty-server:11.0.20")
    include("org.eclipse.jetty:jetty-servlet:11.0.20")
    include("org.eclipse.jetty.websocket:websocket-jetty-server:11.0.20")
    include("org.eclipse.jetty.websocket:websocket-jetty-api:11.0.20")
    include("org.eclipse.jetty.websocket:websocket-jetty-common:11.0.20")
    include("org.eclipse.jetty.toolchain:jetty-jakarta-servlet-api:5.0.2")
    include("org.eclipse.jetty:jetty-http:11.0.20")
    include("org.eclipse.jetty:jetty-io:11.0.20")
    include("org.eclipse.jetty:jetty-util:11.0.20")
    include("org.eclipse.jetty:jetty-security:11.0.20")
    include("org.eclipse.jetty.websocket:websocket-core-server:11.0.20")
    include("org.eclipse.jetty.websocket:websocket-jetty-api:11.0.20")
    include("org.eclipse.jetty.websocket:websocket-jetty-common:11.0.20")
    include("org.eclipse.jetty.websocket:websocket-core-common:11.0.20")
}

tasks.processResources {
    inputs.property("version", modVersion)
    filesMatching("fabric.mod.json") {
        expand("version" to modVersion)
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.named<Jar>("jar") {
    archiveFileName.set("XfPanel.xfxzpe")
}
