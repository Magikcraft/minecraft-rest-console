# Magikcraft Rest Console

This Minecraft plugin provides a REST server that allows you to issue commands via REST to execute as the server console or as any online player.

## Configuration
The port is configurable via the environment. Set the environment variable `MINECRAFT_REST_CONSOLE_PORT` to the port you want the server to run on.

The default is port 8086, if no port is set.

The endpoint can be secured with an API key. Set the environment variable `MINECRAFT_REST_CONSOLE_API_KEY` to the API key you with to use to secure the endpoint.

The default is to run the endpoint unsecured.

## Usage

### echo

The echo route provides a simple method to test that the endpoint is operating.

It will respond with the message that you send, and also log the message out to the server console / server log.

The following route will work with the default configuration (unsecured, port 8086) endpoint:

```bash
http://localhost:8086/echo?message=this%20is%20the%20message
```

With an API key, the request would look like this:

```bash
http://localhost:8086/echo?message=this%20is%20the%20message&apikey=MySuperSecretKey1001
```

### remoteExecuteCommand

Assuming that the endpoint is running on the default port of 8086 and unsecured (by default), the following command will execute `js refresh()` as the server console:

```bash
http://localhost:8086/remoteExecuteCommand?player=server&command=js%20refresh()
```

With an API key set, the request would look like:

```bash
http://localhost:8086/remoteExecuteCommand?player=server&command=js%20refresh()&apikey=SomeSecretKey
```

### sendMessageToPlayer

You can send a message to an online player:

```bash
http://localhost:8086/sendMessageToPlayer?player=sitapati&message=this%20is%20the%20message
```

With an API key:


```bash
http://localhost:8086/sendMessageToPlayer?player=sitapati&message=this%20is%20the%20message&apikey=13242345jkldsf*
```

## Responses

When the endpoint is secured, if you are not authorised with the correct API Key, you will get a `403` Forbidden response.

If the endpoint is not secured, or you have the correct API key, the response is always a HTTP Status code `200`.

It will be a JSON object with an `ok` field. This will be `true` if the request processed ok, and `false` if it did not. 

For example, if you try to send a message to a player who is not online, you will get a `{ok: false}` response.