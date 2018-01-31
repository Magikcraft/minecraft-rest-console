package io.magikcraft.rest

import org.apache.commons.lang.StringUtils
import org.bukkit.plugin.java.JavaPlugin
import org.json.simple.JSONObject
import java.net.URLDecoder
import java.util.logging.Level

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

    private class Httpd(plugin: MinecraftRestConsole, port: Int, key: String?) : NanoHTTPD(port) {

        private val plugin = plugin
        private val logger = plugin.logger
        private val port = port
        private val API_KEY = key

        private fun log (msg: String) {
            logger.info(msg);
        }
        private fun isAuthorised(session: IHTTPSession): Boolean {
            return (API_KEY == null || API_KEY == getParameter("apikey", session, ""))
        }

        private fun getParameter(key: String, session: IHTTPSession): String {
            return getParameter(key, session, "")
        }

        private fun getParameter(key: String, session: IHTTPSession, defaultValue: String): String {
            val params = session.parms
            if (!params.isEmpty()) {
                if (params.containsKey(key)) {
                    val proxyRequestParam = params[key]
                    try {
                        return URLDecoder.decode(proxyRequestParam?.replace("+", "%2B"), "UTF-8")
                                .replace("%2B", "+")
                    } catch (ex: Throwable) {
                        this.logger.log(Level.WARNING, ex.toString())
                    }

                }
            }
            return defaultValue
        }

        private fun OK(msg: String): NanoHTTPD.Response {
            return newFixedLengthResponse(Response.Status.OK,
                    "application/json",
                    "{\"ok\":\"true\", \"msg\":\"$msg\"}")
        }

        private fun OK(json: JSONObject): NanoHTTPD.Response {
            return newFixedLengthResponse(Response.Status.OK,
                    "application/json",
                    json.toJSONString())
        }

        private fun notOK(msg: String): NanoHTTPD.Response {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                    "application/json",
                    "{\"ok\":\"false\", \"msg\":\"$msg\"}")
        }

        private fun _404(): NanoHTTPD.Response {
            return newFixedLengthResponse(Response.Status.NOT_FOUND,
                    NanoHTTPD.MIME_PLAINTEXT,
                    "Error 404: Not Found - The requested resource was not found on this server.")
        }

        private fun _403(): NanoHTTPD.Response {
            logger.log(Level.INFO, "Request not authorised - 403")
            return newFixedLengthResponse(Response.Status.FORBIDDEN,
                    NanoHTTPD.MIME_PLAINTEXT,
                    "Error 403: Forbidden - You do not have permission to access the requested resource.")
        }

        private fun messageDataToPlayer(session: IHTTPSession, playerName: String) {
            val msg = getParameter("data", session)
            plugin.sendMessageTo(playerName, msg)
        }

        private fun messageDataToPlayers(session: IHTTPSession) {
            val msg = getParameter("data", session)
            println("Message to all players: " + msg)
            plugin.broadcastMessage(msg)
        }

        override fun serve(session: IHTTPSession): NanoHTTPD.Response {
            val args = StringUtils.split(session.uri, "/")
            if (args.size > 0) {
                val route = args[0]
                when (route) {
                    "remoteExecuteCommand" -> return remoteExecuteCommand(session, args)
                    "sendMessageToPlayer" -> return sendMessageToPlayer(session, args)
                    "echo" -> return echo(session, args)
                }
            }
            return _404()
        }

        private fun echo(session: IHTTPSession, args: Array<String>): NanoHTTPD.Response {
            val message = getParameter("message", session)
            logger.info("Echo: $message")
            return OK(message)
        }

        private fun remoteExecuteCommand(session: IHTTPSession, args: Array<String>): NanoHTTPD.Response {
            if (!isAuthorised(session)) {
                return _403()
            }
            val playerName = getParameter("player", session, "server")
            val command = getParameter("command", session)
            log("$playerName remote executes $command");
            if ("server".equals(playerName, ignoreCase = true)) { // Run as server
                val sender = plugin.getServer().getConsoleSender()
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, { plugin.getServer().dispatchCommand(sender, command) }, 1)
                return OK("Command scheduled for execution")
            } else {
                val sender = plugin.getServer().getPlayer(playerName)
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, { plugin.getServer().dispatchCommand(sender, command) }, 1)
                return OK("Command returned true")
            }
        }

        private fun sendMessageToPlayer(session: IHTTPSession, args: Array<String>): NanoHTTPD.Response {
            val playerName = getParameter("player", session)
            val msg = getParameter("msg", session)
            val success = plugin.sendMessageTo(playerName, msg)
            return if (success) {
                logger.log(Level.INFO, "Sent to player $playerName: $msg")
                OK("Message sent")
            } else {
                logger.log(Level.INFO, "Send message $msg to player $playerName failed. Player not found.")
                notOK("Sending message failed - Player $playerName not found.")
            }
        }

    }
}