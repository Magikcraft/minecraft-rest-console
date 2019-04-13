package io.magikcraft.rest

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.entity.Player


@Suppress("unused")
class MinecraftRestConsole() : JavaPlugin() {
    private lateinit var httpd: Httpd
    private val apiKeyEnvVarName: String = "MINECRAFT_REST_CONSOLE_API_KEY"
    private val portEnvVarName: String = "MINECRAFT_REST_CONSOLE_PORT"

    /**
     * This is a singleton, but we have to use a Kotlin class because it is managed by Bukkit.
     * So we use a companion object to enforce the singleton pattern.
     */
    companion object {
        var instance: MinecraftRestConsole? = null
            private set
    }

    public var singleEngineMode: Boolean = java.lang.System.getenv("SINGLE_ENGINE_MODE") == "true"

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
//        if (this.singleEngineMode) {
//            logger.info("Running in Single Engine mode")
//        } else {
//            logger.info("Running in Multi-engine mode")
//        }
        if (key == null)
            logger.info("REST endpoint unsecured")
        else
            logger.info("REST endpoint secured with key")
    }

    fun sendMessageTo(playerName: String, msg: String): Boolean {
        val player: Player? = server.getPlayer(playerName)
        if (player != null) {
            player.sendMessage(msg)
            return true
        }
        return false
    }

    fun broadcastMessage(msg: String) {
        server.broadcastMessage(msg)
    }
}