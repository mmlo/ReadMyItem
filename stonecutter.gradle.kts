plugins {
	id("dev.kikugie.stonecutter")
	id("net.fabricmc.fabric-loom-remap") version "1.17-SNAPSHOT" apply false
	id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT" apply false
}

stonecutter active "1.21.11" /* [SC] DO NOT EDIT */

val collectJars = tasks.register<Copy>("collectJars") {
	group = "build"
	description = "Copia os JARs de todas as versões para build/libs/"
	into(layout.buildDirectory.dir("libs"))
}

gradle.projectsEvaluated {
	collectJars.configure {
		subprojects.forEach { target ->
			dependsOn(target.tasks.named("build"))
			val archiveTask = "remapJar"
			val sourcesTask = if (target.name.startsWith("26.")) "sourcesJar" else "remapSourcesJar"
			from(target.tasks.named(archiveTask))
			from(target.tasks.named(sourcesTask))
		}
	}
}

tasks.register("build") {
	group = "build"
	description = "Compila, testa e reúne os JARs de todas as versões"
	dependsOn(collectJars)
}
