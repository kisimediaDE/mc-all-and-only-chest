plugins {
    java
}

group = "dev.simonkirchner"
version = "0.1.0-SNAPSHOT"

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    implementation("org.xerial:sqlite-jdbc:3.53.2.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 25
    }

    jar {
        archiveBaseName = "AllAndOnlyChests"
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from({
            configurations.runtimeClasspath.get().map { dependency ->
                if (dependency.isDirectory) dependency else zipTree(dependency)
            }
        })
        exclude("META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.SF")
    }

    register<Copy>("deployToTestServer") {
        group = "development"
        description = "Builds the plugin and copies it into run/plugins."
        dependsOn(jar)
        from(jar)
        into(layout.projectDirectory.dir("run/plugins"))
    }
}
