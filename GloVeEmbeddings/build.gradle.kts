plugins {
    // Declared here (apply false) so each plugin is loaded once for the whole build.
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
}

// CI yarn.lock policy: the kotlin-js-store/**/yarn.lock the Kotlin/JS toolchain
// generates on CI (Linux) can differ from a dev machine's, which makes
// :kotlinWasmStoreYarnLock fail with "Lock file was changed" and breaks the Pages
// deploy. Accept/regenerate the lock instead of hard-failing.
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().apply {
        yarnLockMismatchReport = org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport.WARNING
        reportNewYarnLock = false
        yarnLockAutoReplace = true
    }
}

// The wasm target uses a SEPARATE yarn root (WasmYarnPlugin), so it needs its own
// policy in addition to the JS one above — this is what :kotlinWasmStoreYarnLock uses.
plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootExtension>().apply {
        yarnLockMismatchReport = org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport.WARNING
        reportNewYarnLock = false
        yarnLockAutoReplace = true
    }
}
