plugins {
    id("fabric-loom") version "1.8.13"
    id("maven-publish")
    id("com.google.protobuf") version "0.9.4"
}

version = project.properties["mod_version"]!!
group = project.properties["maven_group"]!!

base {
    archivesName = project.properties["archives_base_name"] as String
}

val mcVersion = project.properties["minecraft_version"]!!
val yarnVersion = project.properties["yarn_mappings"]!!
val loaderVersion = project.properties["loader_version"]!!
val fabricVersion = project.properties["fabric_version"]!!

repositories {
    maven("https://maven.shedaniel.me/")
    maven("https://maven.terraformersmc.com/")
}

// Configuration for included dependencies
val included = configurations.create("included")

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings("net.fabricmc:yarn:$yarnVersion:v2")
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")

    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")

    // ZeroMQ for inter-process communication
    modImplementation("org.zeromq:jeromq:0.5.4")
    included("org.zeromq:jeromq:0.5.4")
    include("org.zeromq:jeromq:0.5.4")

    // Protocol Buffers for serialization
    modImplementation("com.google.protobuf:protobuf-java:4.26.1")
    included("com.google.protobuf:protobuf-java:4.26.1")
    include("com.google.protobuf:protobuf-java:4.26.1")

    // Cloth Config API for mod configuration
    modApi("me.shedaniel.cloth:cloth-config-fabric:11.1.136") {
        exclude(group = "net.fabricmc.fabric-api")
    }

    // Mod Menu for configuration UI
    modCompileOnly("com.terraformersmc:modmenu:8.0.0")
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", mcVersion)
    inputs.property("loader_version", loaderVersion)
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to mcVersion,
            "loader_version" to loaderVersion
        )
    }
}

tasks.jar {
    from("LICENSE")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)

    // Include dependencies
    from(included.map { zipTree(it) }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/LICENSE*", "META-INF/NOTICE*")
    }
}

// Configure remapJar to include dependencies
tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    // Ensure included dependencies are in the classpath for remapping
    from(included.map { zipTree(it) }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/LICENSE*", "META-INF/NOTICE*")
    }
}

// Copy dev jar to libs as final output (remapJar has compatibility issues with Gradle 9.3.1)
tasks.register<Copy>("copyDevJar") {
    dependsOn("jar")
    from(file("${layout.buildDirectory.get()}/devlibs/${base.archivesName.get()}-${version}-dev.jar"))
    into(file("${layout.buildDirectory.get()}/libs"))
    rename { "${base.archivesName.get()}-${version}.jar" }
}

// Copy dev sources jar to libs
tasks.register<Copy>("copyDevSourcesJar") {
    dependsOn("sourcesJar")
    from(file("${layout.buildDirectory.get()}/devlibs/${base.archivesName.get()}-${version}-sources.jar"))
    into(file("${layout.buildDirectory.get()}/libs"))
    rename { "${base.archivesName.get()}-${version}-sources.jar" }
}

tasks.named("build") {
    dependsOn("copyDevJar")
    dependsOn("copyDevSourcesJar")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(17)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    withSourcesJar()
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.26.1"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                named("java") {
                    option("lite")
                }
            }
        }
    }
}

sourceSets {
    main {
        proto {
            srcDir("src/main/proto")
        }
        java {
            srcDirs("build/generated/source/proto/main/java")
        }
    }
}