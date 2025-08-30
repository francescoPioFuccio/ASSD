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
include(":app")
include(":UserDataModule")
include(":app1")
include("UserDataModule2")
include("ChatBotService")
include("app1:ChatBot")
findProject(":app1:ChatBot")?.name = "ChatBot"
include("app1:ChatBot")
findProject(":app1:ChatBot")?.name = "ChatBot"
include("ChatBot")
include("GestioneOpere")
include("GestioneOpere:GestioneMusei")
findProject(":GestioneOpere:GestioneMusei")?.name = "GestioneMusei"
include("GestioneMusei")
include("GestioneMusei")
include("GestioneMusei")
include("GestioneNavigazione")
include("GestioneQuest")
include("GateWay")
include("Gateway:Gateway2")
findProject(":Gateway:Gateway2")?.name = "Gateway2"
include("Gateway2")
include("Kafka")

