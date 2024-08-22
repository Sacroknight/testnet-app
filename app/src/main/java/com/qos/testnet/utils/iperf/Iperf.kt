package com.qos.testnet.utils.iperf

import java.io.*

class Iperf {

    // Método para copiar el archivo binario de iperf3 según la arquitectura
    @Throws(IOException::class)
    fun copyIperfBinary(architecture: String) {
        // Determinar la ruta de la carpeta correspondiente a la arquitectura
        val folderName = when (architecture) {
            "jniLibs/armeabi-v7a" -> "jniLibs/armeabi-v7a"
            "jniLibs/arm64-v8a" -> "jniLibs/arm64-v8a"
            "jniLibs/x86" -> "jniLibs/x86"
            "jniLibs/x86_64" -> "jniLibs/x86_64"
            else -> throw IllegalArgumentException("Arquitectura no compatible: $architecture")
        }

        // Abrir un InputStream para leer el archivo binario de iperf3 desde los recursos
        val inputStream: InputStream? = javaClass.getResourceAsStream("/assets/$folderName/iperf3")

        // Verificar que el InputStream no sea nulo
        inputStream ?: throw IOException("Archivo iperf3 no encontrado para la arquitectura: $architecture")

        // Crear un archivo de salida en la ruta /data/tmp/iperf
        val outputFile = File("/data/tmp/iperf3")
        val outputStream = FileOutputStream(outputFile)

        // Leer y escribir el contenido del archivo binario
        val buffer = ByteArray(1024)
        var length: Int
        while (inputStream.read(buffer).also { length = it } > 0) {
            outputStream.write(buffer, 0, length)
        }

        // Cerrar los streams
        inputStream.close()
        outputStream.close()

        // Dar permisos de ejecución al archivo copiado
        outputFile.setExecutable(true)
    }

    // Método para ejecutar el comando iperf3 y obtener el resultado
    @Throws(IOException::class)
    fun executeIperfCommand(command: String): String {
        // Ejecutar el comando utilizando ProcessBuilder
        val processBuilder = ProcessBuilder(*command.split(" ").toTypedArray())
        processBuilder.redirectErrorStream(true)

        // Iniciar el proceso
        val process = processBuilder.start()

        // Leer la salida del proceso
        val inputStream: InputStream = process.inputStream
        val reader = BufferedReader(InputStreamReader(inputStream))

        // Leer las líneas de la salida
        val output = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            output.append(line).append("\n")
        }

        // Esperar a que el proceso termine y obtener el código de salida
        try {
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw IOException("Error al ejecutar el comando iperf3: $exitCode")
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        // Cerrar el BufferedReader y devolver el resultado como String
        reader.close()
        return output.toString()
    }

//    @JvmStatic
//    fun main(args: Array<String>) {
//        try {
//            // Copiar el archivo binario de iperf3 para la arquitectura específica (ejemplo: arm64-v8a)
//            val architecture = "arm64-v8a" // Cambia esto según la arquitectura deseada
//            copyIperfBinary(architecture)
//
//            // Verificar si el archivo iperf3 existe en /data/tmp/
//            val iperfFile = File("/data/tmp/iperf")
//            if (!iperfFile.exists()) {
//                throw IOException("El archivo iperf3 no se ha copiado correctamente.")
//            }
//
//            // Ejecutar el comando iperf3
//            val result = executeIperfCommand("/data/tmp/iperf -c 192.168.1.100")
//            println(result)
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//    }
}
