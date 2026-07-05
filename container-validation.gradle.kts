// container-validation.gradle.kts
import java.io.File
import java.util.concurrent.TimeUnit

allprojects {
    plugins.withId("org.springframework.boot") {

        val checkContainerEngine = tasks.register("checkContainerEngineStatus") {
            group = "verification"
            description = "Validates that Docker or Podman is active before compiling."

            val currentProjectName = name

            // Explicitly build a plain List of String pairs to isolate data for the Configuration Cache
            val dependenciesList = mutableListOf<String>()
            configurations.configureEach {
                dependencies.configureEach {
                    dependenciesList.add("${this.group}:${this.name}")
                }
            }

            doFirst {
                // Safely read the isolated plain strings list
                val hasComposePlugin = dependenciesList.any {
                    it == "org.springframework.boot:spring-boot-docker-compose"
                }

                if (!hasComposePlugin) return@doFirst

                val isEngineRunning = isContainerEngineAvailable()

                if (!isEngineRunning) {
                    throw GradleException(
                        """
                        |
                        |❌ [BUILD BLOCKER] Container Engine Failure!
                        |======================================================================
                        |Project :$currentProjectName requires a container engine to boot infrastructure,
                        |but neither Podman nor Docker appears to be running on your machine.
                        |
                        |👉 HOW TO FIX THIS ON WINDOWS:
                        |   1. Launch Podman Desktop or Docker Desktop.
                        |   2. If using Podman via CLI, run: podman machine start
                        |   3. Verify it is running by typing: podman machine list
                        |======================================================================
                        """.trimMargin()
                    )
                }
            }
        }

        // Bind by task type names as plain Strings
        tasks.withType(JavaCompile::class.java).configureEach {
            dependsOn(checkContainerEngine)
        }

        // Dynamic hook for bootRun to avoid referencing the Spring Boot specific class
        tasks.configureEach {
            if (name == "bootRun") {
                dependsOn(checkContainerEngine)
            }
        }
    }
}

fun isContainerEngineAvailable(): Boolean {
    val osName = System.getProperty("os.name").lowercase()

    return when {
        osName.contains("win") -> File("\\\\.\\pipe\\docker_engine").exists()
        osName.contains("nix") || osName.contains("nux") || osName.contains("aix") -> {
            val uid = "id -u".runUnixCommand().trim()
            File("/run/user/$uid/podman/podman.sock").exists() || File("/var/run/docker.sock").exists()
        }
        osName.contains("mac") -> {
            val userHome = System.getProperty("user.home")
            File("$userHome/.local/share/containers/podman/machine/qemu/podman.sock").exists() || File("/var/run/docker.sock").exists()
        }
        else -> false
    }
}

fun String.runUnixCommand(): String {
    return try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
        proc.waitFor(3, TimeUnit.SECONDS)
        proc.inputStream.bufferedReader().readText()
    } catch (e: Exception) {
        ""
    }
}
