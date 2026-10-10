plugins {
    java
    id("net.fabricmc.fabric-loom") version ("1.18.2")
}

version = property("mod_version")!!
group = "com.hbm.thunder"
base.archivesName = "ntm-next-distant-thunder"

java.toolchain.languageVersion = JavaLanguageVersion.of(25)


dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
}

tasks.processResources {
    inputs.property("version", version)
    filesMatching("fabric.mod.json") { expand("version" to version) }
}
