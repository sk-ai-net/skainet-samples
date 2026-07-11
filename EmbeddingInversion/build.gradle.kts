// Root build for the EmbeddingInversion example. Plugin versions are declared once here
// (apply false) so the :cli and :app subprojects can apply them without repeating versions.
plugins {
    kotlin("jvm") version "2.4.0" apply false
    id("org.jetbrains.compose") version "1.10.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0" apply false
}
