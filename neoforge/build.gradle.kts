plugins {
    java
    id("net.neoforged.moddev") version ("2.0.148")
}

version = property("mod_version")!!
group = "com.hbm.thunder"
base.archivesName = "ntm-next-distant-thunder-neoforge"

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

sourceSets.main {
    java.srcDir("../common/src/main/java")
    resources.srcDir("../common/src/main/resources")
}

neoForge {
    version = property("neoforge_version").toString()
}

tasks.processResources {
    inputs.property("version", version)
    filesMatching("META-INF/neoforge.mods.toml") { expand("version" to version) }
}
