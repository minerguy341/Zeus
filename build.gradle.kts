import gg.meza.stonecraft.mod

plugins {
    id("gg.meza.stonecraft")
}

repositories {
    maven {
        name = "TeamResourceful"
        url = uri("https://maven.teamresourceful.com/repository/maven-public/")
    }
}

dependencies {
    // Athena only for compile (mixin target).
    modCompileOnly("earth.terrarium.athena:athena-fabric-1.21.1:${mod.prop("athena_version")}")

    // Drop Fabric jars into repo-root mods/. Loom remaps them for Mojmap.
    // Do not use run/mods for intermediary jars - that folder is not remapped.
    modLocalRuntime(fileTree(mapOf("dir" to rootProject.file("mods"), "include" to listOf("*.jar"))))

    modLocalRuntime("com.teamresourceful:bytecodecs:1.1.3")
    modLocalRuntime("com.teamresourceful:yabn:1.0.3")
}

modSettings {
    clientOptions {
        fov = 90
        guiScale = 3
        narrator = false
        darkBackground = true
        musicVolume = 0.0
    }
}

publishMods {
    modrinth {
        if (mod.isFabric) requires("fabric-api")
        optional("athena-ctm")
        optional("fusion-connected-textures")
        optional("chipped")
    }

    curseforge {
        client = true
        server = false
        if (mod.isFabric) requires("fabric-api")
    }
}
