rootProject.name = "identity-provider"
include("access-token", "device-management")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}