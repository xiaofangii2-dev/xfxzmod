plugins {
    `java-library`
}

version = "1.0.0"
group = "org.xiaofang_dev"

base {
    archivesName.set("plugin")
}

tasks.named<Jar>("jar") {
    archiveFileName.set("plugin.jar")
}
