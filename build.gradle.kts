plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

group = "com.tamaygz"
version = "1.0.2"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    intellijPlatform {
        create("DB", "2024.3")
        bundledPlugin("com.intellij.database")
    }

    implementation("com.google.code.gson:gson:2.10.1")
}

intellijPlatform {
    pluginConfiguration {
        id = "com.tamaygz.colorfuldiag"
        name = "Colorful Diagrams"
        version = project.version.toString()
        description = """
            Extends DataGrip's database diagram designer with visual organization features:
            <ul>
                <li>Color tables with custom colors</li>
                <li>Create visual containers to group tables</li>
                <li>Add sticky notes for documentation</li>
                <li>All changes are purely visual and stored in JSON metadata files</li>
            </ul>
        """.trimIndent()
        changeNotes = """
            <ul>
                <li>Initial release</li>
                <li>Table coloring support</li>
                <li>Visual containers</li>
                <li>Sticky notes</li>
            </ul>
        """.trimIndent()

        ideaVersion {
            sinceBuild = "243"
            untilBuild = provider { null }
        }

        vendor {
            name = "Tamay Gündüz"
            url = "https://github.com/tamaygz"
        }
    }

}

tasks {
    wrapper {
        gradleVersion = "8.10"
    }

    buildSearchableOptions {
        enabled = false
    }
}
