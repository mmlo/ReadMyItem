import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.Duration

plugins {
	id("net.fabricmc.fabric-loom-remap")
	id("maven-publish")
}

version = "${property("mod_version")}+${property("minecraft_version")}"
group = property("maven_group") as String

base {
	archivesName.set(property("archives_base_name") as String)
}

repositories {
	maven("https://maven.terraformersmc.com/") { name = "Terraformers" }
	maven("https://maven.shedaniel.me/") { name = "Shedaniel" }
	maven("https://maven.architectury.dev/") { name = "Architectury" }
	mavenCentral()
}

loom {
	mods {
		create("readmyitem") {
			sourceSet(sourceSets.main.get())
		}
	}
	runs.configureEach {
		// Piper/ONNX default is "use all cores"; on a notebook that freezes the game.
		environmentVariable("OMP_NUM_THREADS", "1")
		environmentVariable("ORT_INTRA_OP_NUM_THREADS", "1")
		environmentVariable("OPENBLAS_NUM_THREADS", "1")
	}
}

dependencies {
	minecraft("com.mojang:minecraft:${property("minecraft_version")}")
	mappings(loom.officialMojangMappings())
	modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
	modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")

	modImplementation("me.shedaniel.cloth:cloth-config-fabric:${property("cloth_config_version")}")
	modCompileOnly("com.terraformersmc:modmenu:${property("modmenu_version")}")
	modLocalRuntime("com.terraformersmc:modmenu:${property("modmenu_version")}")

	// Piper TTS in-process (JNI). Nested in the mod jar; natives are loaded
	// by piper-jni itself via System.load of bundled shared libraries — never a subprocess.
	val piper = "io.github.jvoice-project:piper-jni:${property("piper_jni_version")}"
	implementation(piper)
	include(piper)

	testImplementation(platform("org.junit:junit-bom:5.11.4"))
	testImplementation("org.junit.jupiter:junit-jupiter")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
	useJUnitPlatform()
	testLogging {
		events("passed", "failed", "skipped")
	}
}

val piperVoiceFiles = listOf(
	mapOf(
		"name" to "pt_BR-edresson-low.onnx",
		"url" to "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/pt/pt_BR/edresson/low/pt_BR-edresson-low.onnx",
		"sha256" to "de4cecee38b30bb1a6378a337af605d59f0c377df702c6a6752870db8991cd84",
		"required" to "true",
	),
	mapOf(
		"name" to "pt_BR-edresson-low.onnx.json",
		"url" to "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/pt/pt_BR/edresson/low/pt_BR-edresson-low.onnx.json",
		"sha256" to "f138992d2e777d1e3aa0bbb14c2d324307b0f342c1bcf20978765b3bea506c56",
		"required" to "true",
	),
	mapOf(
		"name" to "pt_BR-faber-medium.onnx",
		"url" to "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/pt/pt_BR/faber/medium/pt_BR-faber-medium.onnx",
		"sha256" to "858555e3a064209c57088fe6bd70c4c3dc54d03eaa00c45d5ecaf43a33f95aa7",
		"required" to "true",
	),
	mapOf(
		"name" to "pt_BR-faber-medium.onnx.json",
		"url" to "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/pt/pt_BR/faber/medium/pt_BR-faber-medium.onnx.json",
		"sha256" to "7e694de195ae3fc36dd732c445eb04fb49b649854893cb5506b978f0d50a1d6f",
		"required" to "true",
	),
	mapOf(
		"name" to "en_US-lessac-low.onnx",
		"url" to "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/lessac/low/en_US-lessac-low.onnx",
		"sha256" to "f7d01dde371555732c4c314111ac79672b1a5ce2fc19266ab42178fd8df7f375",
		"required" to "true",
	),
	mapOf(
		"name" to "en_US-lessac-low.onnx.json",
		"url" to "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/lessac/low/en_US-lessac-low.onnx.json",
		"sha256" to "45754dfdebb3b8661c3fc564713772deec6e064feeb5b4e9594857dc7305193a",
		"required" to "true",
	),
)

val piperVoiceDestRoot = rootProject.layout.projectDirectory.dir("src/main/resources/assets/readmyitem/voices")
val piperVoiceCache = rootProject.layout.projectDirectory.dir("voices-cache")

fun sha256File(file: java.io.File): String {
	val digest = MessageDigest.getInstance("SHA-256")
	file.inputStream().use { input ->
		val buf = ByteArray(32 * 1024)
		while (true) {
			val n = input.read(buf)
			if (n < 0) {
				break
			}
			if (n > 0) {
				digest.update(buf, 0, n)
			}
		}
	}
	return digest.digest().joinToString("") { b: Byte -> "%02x".format(b) }
}

tasks.register("downloadPiperVoices") {
	group = "build"
	description = "Baixa vozes Piper oficiais (allowlist + SHA-256) para o jar"
	doLast {
		val cacheDir = piperVoiceCache.asFile
		cacheDir.mkdirs()
		val client = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NORMAL)
			.connectTimeout(Duration.ofSeconds(30))
			.build()
		for (voice in piperVoiceFiles) {
			val name = voice.getValue("name")
			val url = voice.getValue("url")
			val expected = voice.getValue("sha256")
			val langSubdir = if (name.startsWith("en_US-")) "en_US" else "pt_BR"
			val destDir = piperVoiceDestRoot.dir(langSubdir).asFile
			destDir.mkdirs()
			if (!url.startsWith("https://huggingface.co/rhasspy/piper-voices/")) {
				throw GradleException("URL fora da allowlist: $url")
			}
			val dest = destDir.resolve(name)
			val cache = cacheDir.resolve(name)
			if (dest.isFile && sha256File(dest) == expected) {
				continue
			}
			if (!(cache.isFile && sha256File(cache) == expected)) {
				logger.lifecycle("[ReadMyItem] baixando $name …")
				val tmp = cacheDir.resolve("$name.part")
				val request = HttpRequest.newBuilder(URI.create(url))
					.timeout(Duration.ofMinutes(10))
					.header("User-Agent", "ReadMyItem-build/1.0")
					.GET()
					.build()
				val response = client.send(request, HttpResponse.BodyHandlers.ofFile(tmp.toPath()))
				if (response.statusCode() != 200) {
					tmp.delete()
					throw GradleException(
						"Falha ao baixar $name (HTTP ${response.statusCode()}). Rode ./gradlew downloadPiperVoices com rede."
					)
				}
				val actual = sha256File(tmp)
				if (actual != expected) {
					tmp.delete()
					throw GradleException("SHA-256 de $name não confere (esperado $expected, obtido $actual)")
				}
				if (!tmp.renameTo(cache)) {
					tmp.copyTo(cache, overwrite = true)
					tmp.delete()
				}
			}
			cache.copyTo(dest, overwrite = true)
			if (sha256File(dest) != expected) {
				throw GradleException("Cópia de $name para resources falhou no checksum")
			}
		}
	}
}

tasks.processResources {
	dependsOn("downloadPiperVoices")
	val mcVer = project.property("minecraft_version").toString()
	val mcCompat = if (mcVer.startsWith("1.20")) ">=1.20.1 <1.20.2" else "~1.21.11"
	val javaReq = if (mcVer.startsWith("1.20")) ">=17" else ">=21"
	val props = mapOf(
		"version" to project.version.toString(),
		"minecraft_version" to mcVer,
		"mc_compat" to mcCompat,
		"java_version" to javaReq,
		"java_release" to if (mcVer.startsWith("1.20")) 17 else 21
	)
	inputs.properties(props)
	filesMatching(listOf("fabric.mod.json", "readmyitem.mixins.json")) {
		expand(props)
	}
}

// 1.20.1 requires Java 17; 1.21.11 requires Java 21.
val mcVersion = property("minecraft_version").toString()
val javaVersion = if (mcVersion.startsWith("1.20")) JavaVersion.VERSION_17 else JavaVersion.VERSION_21
val javaRelease = if (mcVersion.startsWith("1.20")) 17 else 21

tasks.withType<JavaCompile>().configureEach {
	options.encoding = "UTF-8"
	options.release.set(javaRelease)
}

java {
	withSourcesJar()
	sourceCompatibility = javaVersion
	targetCompatibility = javaVersion
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(javaRelease))
	}
}

tasks.jar {
	from(rootProject.file("LICENSE")) {
		rename { "${it}_${base.archivesName.get()}" }
	}
	from(rootProject.file("NOTICE")) {
		rename { "${it}_${base.archivesName.get()}" }
	}
}

tasks.withType<Jar>().configureEach {
	if (name == "sourcesJar") {
		exclude("**/*.onnx")
		dependsOn("downloadPiperVoices")
	}
}

publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}
}

tasks.matching { it.name == "stonecutterPrepare" }.configureEach {
	dependsOn("downloadPiperVoices")
}
