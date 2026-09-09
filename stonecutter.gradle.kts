plugins {
	id("dev.kikugie.stonecutter")
	id("net.fabricmc.fabric-loom-remap") version "1.17-SNAPSHOT" apply false
}

stonecutter active "1.21.11" /* [SC] DO NOT EDIT */

val collectJars = tasks.register<Copy>("collectJars") {
	group = "build"
	description = "Copia o JAR da versão 1.21.11 para build/libs/"
	from(file("versions/1.21.11/build/libs"))
	into(layout.buildDirectory.dir("libs"))
	include("*.jar")
	duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

gradle.projectsEvaluated {
	val versionBuild = project(":1.21.11").tasks.named("build")
	collectJars.configure { dependsOn(versionBuild) }
	versionBuild.configure { finalizedBy(collectJars) }
}
