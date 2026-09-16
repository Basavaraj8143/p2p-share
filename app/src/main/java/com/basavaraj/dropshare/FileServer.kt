package com.basavaraj.dropshare

import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket

class FileServer(
    private val fileName: String,
    private val fileSize: Long,
    private val openFile: () -> InputStream?
) {

    private var serverSocket: ServerSocket? = null

    fun start() {

        try {
            serverSocket = ServerSocket(8080)

            while (serverSocket?.isClosed == false) {

                val socket = serverSocket?.accept() ?: break

                Thread {
                    handleClient(socket)
                }.start()
            }
        } catch (_: Exception) {
            // Server socket closed or interrupted
        }
    }

    private fun handleClient(socket: Socket) {

        socket.use { client ->

            val reader = client.getInputStream()
                .bufferedReader()

            // Read request line
            reader.readLine()

            // Read HTTP headers
            while (reader.readLine() != "") {
                // Ignore headers for V0
            }

            val output = client.getOutputStream()

            val inputStream = openFile()

            if (inputStream == null) {

                output.write(
                    """
                    HTTP/1.1 404 Not Found
                    Content-Length: 0
                    
                    """.trimIndent()
                        .replace("\n", "\r\n")
                        .toByteArray()
                )

                return
            }

            val contentType = getContentType(fileName)

            val headers =
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: $contentType\r\n" +
                        "Content-Length: $fileSize\r\n" +
                        "Content-Disposition: attachment; filename=\"$fileName\"\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"

            output.write(headers.toByteArray())

            inputStream.use { input ->

                val buffer = ByteArray(8192)

                while (true) {

                    val count = input.read(buffer)

                    if (count == -1) break

                    output.write(buffer, 0, count)
                }
            }

            output.flush()
        }
    }

    private fun getContentType(fileName: String): String {

        return when {

            fileName.endsWith(".pdf", true) ->
                "application/pdf"

            fileName.endsWith(".jpg", true) ||
                    fileName.endsWith(".jpeg", true) ->
                "image/jpeg"

            fileName.endsWith(".png", true) ->
                "image/png"

            fileName.endsWith(".txt", true) ->
                "text/plain"

            fileName.endsWith(".mp4", true) ->
                "video/mp4"

            fileName.endsWith(".mp3", true) ->
                "audio/mpeg"

            fileName.endsWith(".zip", true) ->
                "application/zip"

            else ->
                "application/octet-stream"
        }
    }

    fun stop() {

        serverSocket?.close()

        serverSocket = null
    }
}