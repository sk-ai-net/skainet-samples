package sk.ainet.apps.kllama.chat

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform