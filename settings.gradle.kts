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
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // 高德地图Maven仓库
        maven { url = uri("https://repo1.maven.org/maven2/") }
        maven { url = uri("https://developer.huawei.com/repo/") }
    }
}

rootProject.name = "GeoTask"
include(":app")
include(":kotlin-client")
project(":kotlin-client").projectDir = file("kotlin-client")

//include(":lib")
