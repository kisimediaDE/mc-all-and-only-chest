plugins {
    java
}

group = "dev.playmonkeei"
version = "0.1.0-beta.2"

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    implementation("org.xerial:sqlite-jdbc:3.53.2.0")

    testImplementation("io.papermc.paper:paper-api:26.2.build.+")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks {
    test {
        useJUnitPlatform()
        jvmArgs("--enable-native-access=ALL-UNNAMED")
    }

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
