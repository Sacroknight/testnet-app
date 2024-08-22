package com.qos.testnet.utils.iperf;

public class IperfLib {
    static {
        System.loadLibrary("iperf3");  // Cargar la biblioteca nativa
    }

    // Declarar métodos nativos
    public native int startIperf(String[] args);
}

