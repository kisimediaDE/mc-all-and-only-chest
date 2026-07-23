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
    }

    register<Copy>("deployToTestServer") {
        group = "development"
        description = "Builds the plugin and copies it into run/plugins."
        dependsOn(jar)
        from(jar)
        into(layout.projectDirectory.dir("run/plugins"))
    }
}
