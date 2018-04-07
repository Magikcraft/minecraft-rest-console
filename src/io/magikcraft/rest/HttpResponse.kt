package io.magikcraft.rest

import org.json.simple.JSONObject

class HttpResponse() {
    companion object {

        fun OK(msg: String): NanoHTTPD.Response {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK,
                    "application/json",
                    "{\"ok\":\"true\", \"msg\":\"$msg\"}")
        }

        fun OK(json: JSONObject): NanoHTTPD.Response {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK,
                    "application/json",
                    json.toJSONString())
        }

        fun notOK(msg: String): NanoHTTPD.Response {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR,
                    "application/json",
                    "{\"ok\":\"false\", \"msg\":\"$msg\"}")
        }

        fun _404(): NanoHTTPD.Response {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND,
                    NanoHTTPD.MIME_PLAINTEXT,
                    "Error 404: Not Found - The requested resource was not found on this server.")
        }

        fun _403(): NanoHTTPD.Response {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.FORBIDDEN,
                    NanoHTTPD.MIME_PLAINTEXT,
                    "Error 403: Forbidden - You do not have permission to access the requested resource.")
        }
    }
}