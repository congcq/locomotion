import io.github.pacifistmc.forgix.plugin.ForgixMergeExtension.FabricContainer

plugins {
	id("dev.kikugie.stonecutter")
	id("dev.architectury.loom") version "1.10-SNAPSHOT" apply false
	id("architectury-plugin") version "3.4-SNAPSHOT" apply false
	id("com.github.johnrengelman.shadow") version "8.1.1" apply false
	id("io.github.pacifistmc.forgix") version "1.2.9"
}
stonecutter active "1.21.5" /* [SC] DO NOT EDIT */

// Builds every version into `build/libs/{mod.version}/{loader}`
stonecutter registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) {
	group = "project"
	ofTask("buildAndCollect")
}

// Builds loader-specific versions into `build/libs/{mod.version}/{loader}`
for (it in stonecutter.tree.branches) {
	if (it.id.isEmpty()) continue
	val loader = it.id.replaceFirstChar { it.uppercaseChar() }
	stonecutter registerChiseled tasks.register("chiseledBuild$loader", stonecutter.chiseled) {
		group = "project"
		versions { branch, _ -> branch == it.id }
		ofTask("buildAndCollect")
	}
}

// Runs active versions for each loader
for (it in stonecutter.tree.nodes) {
	if (it.metadata != stonecutter.current || it.branch.id.isEmpty()) continue
	val types = listOf("Client", "Server")
	val loader = it.branch.id.replaceFirstChar { it.uppercaseChar() }
	for (type in types) it.project.tasks.register("runActive$type$loader") {
		group = "project"
		dependsOn("run$type")
	}
}

forgix {
	val minecraft = stonecutter.current.version
	val version = "${prop("mod.version")}+${minecraft}"
	group = "${prop("mod.group")}.${prop("mod.id")}"
	mergedJarName = "${prop("mod.id")}-${version}.jar"
	outputDir = "build/libs/merged"

	if (findProject(":fabric") != null) {
		fabricContainer = FabricContainer().apply {
			jarLocation = "versions/${minecraft}/build/libs/${prop("mod.id")}-fabric-${version}.jar"
		}
	}

	if (findProject(":quilt") != null) {
		quiltContainer = QuiltContainer().apply {
			jarLocation = "versions/${minecraft}/build/libs/${prop("mod.id")}-quilt-${version}.jar"
		}
	}

	if (findProject(":forge") != null) {
		forgeContainer = ForgeContainer().apply {
			jarLocation = "versions/${minecraft}/build/libs/${prop("mod.id")}-forge-${version}.jar"
		}
	}

	if (findProject(":neoforge") != null) {
		neoForgeContainer = NeoForgeContainer().apply {
			jarLocation = "versions/${minecraft}/build/libs/${prop("mod.id")}-neoforge-${version}.jar"
		}
	}

	removeDuplicate("${prop("mod.group")}.${prop("mod.id")}")
}