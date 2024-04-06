package com.qos.testnet.utils.networkInformation

class IperfJNI {
    external fun runSpeedTest(serverAddress: String, port: String, mode: Boolean, duration: Int, udp: Boolean): String
    external fun runCustomTest(serverAddress: String, port: String, mode: Boolean, duration: Int, udp: Boolean, numTest: Int): String

    external fun getAvailableServers(): Array<String>
    external fun getSpeedTestResults(): Array<String>
    external fun getCustomTestResults(): Array<String>

    companion object {
        init {
            try {
                System.loadLibrary("Iperf")
            } catch (e: UnsatisfiedLinkError) {
                // Manejar cualquier error de carga de la biblioteca nativa
                println("Error loading native library: ${e.message}")
            }
        }
    }
}