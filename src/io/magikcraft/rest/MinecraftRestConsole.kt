package io.magikcraft.rest

import org.bukkit.plugin.java.JavaPlugin

@Suppress("unused")
class MinecraftRestConsole() : JavaPlugin() {
    private lateinit var httpd: Httpd
    private val apiKeyEnvVarName: String = "MINECRAFT_REST_CONSOLE_API_KEY"
    private val portEnvVarName: String = "MINECRAFT_REST_CONSOLE_PORT"
    val singleEngineMode = java.lang.System.getenv("SINGLE_ENGINE_MODE") == "true"

    /**
     * This is a singleton, but we have to use a Kotlin class because it is managed by Bukkit.
     * So we use a companion object to enforce the singleton pattern.
     */
    companion object {
        var instance: MinecraftRestConsole? = null
            private set
    }

    override fun onEnable() {
        instance = this
        val key = System.getenv(apiKeyEnvVarName)
        val configuredPort = System.getenv(portEnvVarName)
        var port: Int
        try {
            if (configuredPort != null) port = configuredPort.toInt()
            else port = 8086
        } catch (e: Exception) {
            port = 8086
        }
        httpd = Httpd(this, port, key)
        httpd.start()
        logger.info("Minecraft REST Console plugin enabled!")
        logger.info("Minecraft REST Console started on port " + port)
        if (key == null)
            logger.info("REST endpoint unsecured")
        else
            logger.info("REST endpoint secured with key")
    }

    fun sendMessageTo(playerName: String, msg: String): Boolean {
        if (server.getPlayer(playerName) != null) {
            server.getPlayer(playerName).sendMessage(msg)
            return true
        }
        return false
    }

    fun broadcastMessage(msg: String) {
        server.broadcastMessage(msg)
    }


}