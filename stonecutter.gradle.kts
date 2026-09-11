plugins {
	id("dev.kikugie.stonecutter")
	id("net.fabricmc.fabric-loom-remap") version "1.17-SNAPSHOT" apply false
}

stonecutter active "1.21.11" /* [SC] DO NOT EDIT */

val collectJars = tasks.register<Copy>("collectJars") {
	group = "build"
	description = "Copia os JARs de todas as versões para build/libs/"
	from(file("versions/1.21.11/build/libs"))
	from(file("versions/1.20.1/build/libs"))
	into(layout.buildDirectory.dir("libs"))
	include("*.jar")
	duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

gradle.projectsEvaluated {
	val build1211 = project(":1.21.11").tasks.named("build")
	val build1201 = project(":1.20.1").tasks.named("build")
	collectJars.configure { dependsOn(build1211, build1201) }
	build1211.configure { finalizedBy(collectJars) }
	build1201.configure { finalizedBy(collectJars) }
}
