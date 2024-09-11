package com.qos.testnet.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class IperfExecutor {

    private static final String TAG = "IperfExecutor";

    public static String executeIperf(String command) throws IOException {
        // Replace "arm64-v8a/iperf3.16" with the actual path within your project
        String binaryPath = "assets/arm64-v8a/iperf3.16";

        // Construct the full command with path to the binary
        String fullCommand = String.format("sh %s %s", binaryPath, command);

        try {
            Process process = Runtime.getRuntime().exec(fullCommand);
            StringBuilder output = new StringBuilder();

            // Read the output from the process
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            reader.close();

            // Extract performance metrics from output (replace with your parsing logic)
            return parseOutputForMetrics(output.toString());
        } catch (IOException e) {
            Log.e(TAG, "Error executing iperf: " + e.getMessage());
            throw e;
        }
    }

    // Implement parsing logic to extract metrics from iperf output (replace with your logic)
    private static String parseOutputForMetrics(String output) {
        // This is a simplified example, actual parsing would involve searching for keywords and patterns
        // within the output text. You might need external libraries for robust parsing.
        int indexOfSentBytes = output.indexOf("sender sent");
        if (indexOfSentBytes != -1) {
            String[] tokens = output.split(" ", 3);
            return tokens[1]; // Assuming second token is the number of bytes sent
        }
        return "Parsing failed";
    }
}
