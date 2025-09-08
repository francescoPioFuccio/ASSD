pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MuseoApp"
include(":app1")
include("UserDataModule2")
include("GestioneOpere")
include("GestioneOpere:GestioneMusei")
findProject(":GestioneOpere:GestioneMusei")?.name = "GestioneMusei"
include("GestioneMusei")
include("GestioneNavigazione")
include("GestioneQuest")
include("Gateway")
include("Kafka")
include("Grpc-api")
