package io.magikcraft.rest

import org.apache.commons.lang.StringUtils
import org.json.simple.JSONObject
import java.net.URLDecoder
import java.util.logging.Level

class Httpd(plugin: MinecraftRestConsole, port: Int, key: String?) : NanoHTTPD(port) {

    private val plugin = plugin
    private val logger = plugin.logger
    private val port = port
    private val API_KEY = key

    override fun serve(session: IHTTPSession): NanoHTTPD.Response {
        val args = StringUtils.split(session.uri, "/")
        if (args.size > 0) {
            val route = args[0]
            when (route) {
                "remoteExecuteCommand" -> return remoteExecuteCommand(session, args)
                "sendMessageToPlayer" -> return sendMessageToPlayer(session, args)
                "echo" -> return echo(session, args)
                "getOnlinePlayers" -> return getOnlinePlayers(session, args)
                "getEngineMode" -> return getEngineMode(session, args)
            }
        }
        return HttpResponse._404()
    }

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

    private fun messageDataToPlayer(session: IHTTPSession, playerName: String) {
        val msg = getParameter("data", session)
        plugin.sendMessageTo(playerName, msg)
    }

    private fun getEngineMode(session: IHTTPSession, args: Array<String>): NanoHTTPD.Response {
        if (plugin.singleEngineMode) {
            return HttpResponse.OK("single")
        } else {
            return HttpResponse.OK("multi")
        }
    }

    private fun messageDataToPlayers(session: IHTTPSession) {
        val msg = getParameter("data", session)
        println("Message to all players: " + msg)
        plugin.broadcastMessage(msg)
    }

    private fun echo(session: IHTTPSession, args: Array<String>): NanoHTTPD.Response {
        val message = getParameter("message", session)
        logger.info("Echo: $message")
        return HttpResponse.OK(message)
    }

    private fun remoteExecuteCommand(session: IHTTPSession, args: Array<String>): NanoHTTPD.Response {
        if (!isAuthorised(session)) {
            return HttpResponse._403()
        }
        val playerName = getParameter("player", session, "server")
        val command = getParameter("command", session)
        log("$playerName remote executes $command");
        val sender = plugin.getServer().getConsoleSender()
        val server = plugin.server
        try {
            server.scheduler.callSyncMethod(plugin) {
                server.dispatchCommand(sender, command)
            }
        } finally {
            // Close the HTTP connection
            return HttpResponse.OK("Command executed")
        }
    }

    private fun sendMessageToPlayer(session: IHTTPSession, args: Array<String>): NanoHTTPD.Response {
        val playerName = getParameter("player", session)
        val msg = getParameter("msg", session)
        val success = plugin.sendMessageTo(playerName, msg)
        return if (success) {
            logger.log(Level.INFO, "Sent to player $playerName: $msg")
            HttpResponse.OK("Message sent")
        } else {
            logger.log(Level.INFO, "Send message $msg to player $playerName failed. Player not found.")
            HttpResponse.OK("Sending message failed - Player $playerName not found.")
        }
    }


    fun getOnlinePlayers(session: IHTTPSession, args: Array<String>): NanoHTTPD.Response {
        if (!isAuthorised(session)) {
            return HttpResponse._403()
        }
        var playerMap = mutableMapOf<String, List<String>>();
        val players = plugin.server.onlinePlayers.map{ it.name }
        playerMap.put("players", players)
        return HttpResponse.OK(JSONObject(playerMap))
    }
}