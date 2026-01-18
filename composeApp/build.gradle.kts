import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.internal.com.squareup.kotlinpoet.FileSpec
import org.jetbrains.compose.internal.com.squareup.kotlinpoet.PropertySpec
import org.jetbrains.compose.internal.com.squareup.kotlinpoet.TypeSpec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.text.SimpleDateFormat
import java.util.Date

// 版本代码是yyyyMMdd动态生成
val verCode = SimpleDateFormat("yyyyMMdd").format(Date()).toInt()
// 当实现计划时记得撞♂版本号
val verName = "1.0.1"
// 包名
val appId = "me.earzuchan.noodle"

val myGeneratedCodeDir = "generated/source/buildConstants"

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlin.serialization)
}

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    
    jvm()
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)

            implementation(libs.androidx.appcompat)
        }

        commonMain.dependencies {

            implementation(compose.runtime)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(compose.preview)
            implementation(libs.cmp.darkModer)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)

            implementation(libs.androidx.datastore.preferences)

            // HACK：这个主要是为了加载FetchMoodle库（无内联依赖的版本），你自行构建
            implementation(fileTree("exLibs"))

            implementation(libs.jsoup)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)

            implementation(libs.decompose)
            implementation(libs.decomposeExtCmp)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspJvm", libs.androidx.room.compiler)
}

val generateBuildConstants = tasks.register("generateBuildConstants") {
    val outputDir = File(buildDir, myGeneratedCodeDir)
    val constClzName = "BuildConstants"

    outputs.dir(outputDir)

    doLast {// 创建 Kotlin 文件
        val buildConfigClass = TypeSpec.objectBuilder(constClzName)
            .addProperty(
                PropertySpec.builder("VERSION_NAME", String::class)
                    .initializer("%S", verName)
                    .build()
            ).addProperty(
                PropertySpec.builder("VERSION_CODE", Int::class)
                    .initializer("%L", verCode)
                    .build()
            )
            .build()

        // 生成文件
        val file = FileSpec.builder(appId, "BuildConstants")
            .addType(buildConfigClass)
            .build()

        outputDir.mkdirs()
        file.writeTo(outputDir)
    }
}

android {
    namespace = appId
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = appId
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = verCode
        versionName = verName
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
        }

        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "$appId.InitAppKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = appId
            packageVersion = verName
        }
    }
}
