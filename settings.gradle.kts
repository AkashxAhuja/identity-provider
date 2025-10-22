pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

rootProject.name = "identity-provider"

include("device-management")
include("access-token")
