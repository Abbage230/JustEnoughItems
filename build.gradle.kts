import se.bjurr.gitchangelog.plugin.gradle.GitChangelogTask

buildscript {
    dependencies {
        constraints {
            // Force ASM on buildscript/plugin classpath to 9.2 instead of an outdated version from gitchangelog
            classpath("org.ow2.asm:asm:9.2")
        }
    }
}

plugins {
	id("se.bjurr.gitchangelog.git-changelog-gradle-plugin") version("1.71.4")
	id("com.diffplug.spotless") version("5.14.3")
}
apply {
	from("buildtools/ColoredOutput.gradle")
}

// gradle.properties
val curseHomepageLink: String by extra
val curseProjectId: String by extra
val forgeVersion: String by extra
val forgeVersionRange: String by extra
val githubUrl: String by extra
val loaderVersionRange: String by extra
val mappingsChannel: String by extra
val mappingsVersion: String by extra
val minecraftVersion: String by extra
val minecraftVersionRange: String by extra
val modAuthor: String by extra
val modGroup: String by extra
val modId: String by extra
val modJavaVersion: String by extra
val modName: String by extra
val specificationVersion: String by extra

tasks.register<GitChangelogTask>("makeChangelog") {
	fromRepo = projectDir.absolutePath.toString()
	file = file("changelog.html")
	untaggedName = "Current release $specificationVersion"
	fromCommit = "e72e49fa7a072755e7f96cad65388205f6a010dc"
	toRef = "HEAD"
	templateContent = file("changelog.mustache").readText()
}

spotless {
	java {
		target("*/src/*/java/mezz/jei/**/*.java")

		endWithNewline()
		trimTrailingWhitespace()
		removeUnusedImports()
	}
}

subprojects {
    //adds the build number to the end of the version string if on a build server
    var buildNumber = project.findProperty("BUILD_NUMBER")
    if (buildNumber == null) {
        buildNumber = "9999"
    }

    version = "${specificationVersion}.${buildNumber}"
    group = modGroup

    tasks.withType<Javadoc> {
        // workaround cast for https://github.com/gradle/gradle/issues/7038
        val standardJavadocDocletOptions = options as StandardJavadocDocletOptions
        // prevent java 8's strict doclint for javadocs from failing builds
        standardJavadocDocletOptions.addStringOption("Xdoclint:none", "-quiet")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(JavaLanguageVersion.of(modJavaVersion).asInt())
    }

    tasks.withType<Jar> {
        val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(java.util.Date())
        manifest {
            attributes(mapOf(
                "Specification-Title" to modName,
                "Specification-Vendor" to modAuthor,
                "Specification-Version" to specificationVersion,
                "Implementation-Title" to name,
                "Implementation-Version" to archiveVersion,
                "Implementation-Vendor" to modAuthor,
                "Implementation-Timestamp" to now,
            ))
        }
    }

    tasks.withType<ProcessResources> {
        // this will ensure that this task is redone when the versions change.
        inputs.property("version", version)

        filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) {
            expand(mapOf(
                "modId" to modId,
                "modName" to modName,
                "version" to version,
                "minecraftVersionRange" to minecraftVersionRange,
                "forgeVersionRange" to forgeVersionRange,
                "loaderVersionRange" to loaderVersionRange,
                "githubUrl" to githubUrl
            ))
        }
    }
}
