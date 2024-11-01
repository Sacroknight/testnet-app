package com.qos.testnet.utils.network

import android.util.Log

class MobileNetworkQualityScoreCalculator {

    companion object {
        private const val MAX_PING_SCORE = 40.0
        private const val MAX_DOWNLOAD_SCORE = 35.0
        private const val MAX_UPLOAD_SCORE = 25.0

        private const val PING_THRESHOLD_1 = 90.0
        private const val PING_THRESHOLD_2 = 1000.0
        private const val PING_THRESHOLD_3 = 4000.0

        private const val JITTER_THRESHOLD_1 = 30.0
        private const val JITTER_THRESHOLD_2 = 50.0
        private const val JITTER_THRESHOLD_3 = 500.0

        private const val SIGNAL_STRENGTH_MIN = -112.0
        private const val SIGNAL_STRENGTH_MAX = -51.0

        private fun calcPing(mP: Int, mJ: Int): Double {
            require(mP >= 0) { "Ping value must be non-negative" }
            require(mJ >= 0) { "Jitter value must be non-negative" }

            val pingScore = when {
                mJ < JITTER_THRESHOLD_1 -> when {
                    mP < PING_THRESHOLD_1 -> MAX_PING_SCORE
                    mP <= PING_THRESHOLD_2 -> MAX_PING_SCORE * ((-0.875 / 910) * mP + 1.06159)
                    mP <= PING_THRESHOLD_3 -> MAX_PING_SCORE * ((-0.1 / 2999) * mP + 0.13337)
                    else -> 0.0
                }

                mJ <= JITTER_THRESHOLD_2 -> MAX_PING_SCORE * 0.9 // Reduce score due to jitter
                mJ <= JITTER_THRESHOLD_3 -> MAX_PING_SCORE * ((-1 / JITTER_THRESHOLD_3) * mJ + 1)
                else -> 0.0 // En caso de que mJ sea mayor que JITTER_THRESHOLD_3
            }

            return pingScore.coerceAtLeast(0.0) // Asegura que el puntaje no sea negativo
        }

        private fun calcDownloadSpeed(mV: Double): Double {
            require(mV >= 0) { "Download speed must be non-negative" }

            return when {
                mV < 0.5 -> 0.0
                mV in 0.5..1.0 -> 1.0
                mV <= 25 -> (0.97143 / 24.5) * MAX_DOWNLOAD_SCORE * mV
                else -> MAX_DOWNLOAD_SCORE
            }
        }

        private fun calcUploadSpeed(mVc: Double): Double {
            require(mVc >= 0) { "Upload speed must be non-negative" }

            return when {
                mVc < 0.5 -> 0.0
                mVc in 0.5..1.0 -> 1.0
                mVc <= 15 -> (0.96 / 14.5) * MAX_UPLOAD_SCORE * mVc
                else -> MAX_UPLOAD_SCORE
            }
        }

//        private fun calcSignalStrength(mI: Double): Double {
//            require(mI in SIGNAL_STRENGTH_MIN..SIGNAL_STRENGTH_MAX) {
//                "Signal strength must be in the range (-112dBm, -51dBm)"
//            }
//
//            return when {
//                mI <= -51 && mI >= -79 -> 10.0
//                mI <= -80 && mI >= -95 -> 9 + ((2 / 15.0) * mI + (32 / 3.0))
//                mI <= -96 && mI >= -104 -> (3 / 8.0) * mI + 42
//                mI <= -105 && mI >= -112 -> (1 / 7.0) * mI + 17
//                else -> 0.0 // Para valores menores a -112dBm
//            }
//        }

        private fun applyBonusPenalty(score: Double, signalStrength: Int): Double {
            if (signalStrength <= SIGNAL_STRENGTH_MAX && signalStrength >= SIGNAL_STRENGTH_MIN) {
                val finalScore = when {
                    signalStrength == -1 -> score
                    signalStrength >= (20 / 3.0) && signalStrength < 9 && score < (200 / 3.0) -> score * 0.95 // 5% penalty
                    signalStrength >= 9 && score < (200 / 3.0) -> score * 0.9 // 10% penalty
                    signalStrength < (20 / 3.0) && signalStrength >= (10 / 3.0) && score >= (200 / 3.0) -> score * 1.05 // 5% bonus
                    signalStrength <= (10 / 3.0) && score >= (200 / 3.0) -> score * 1.1 // 10% bonus
                    else -> score
                }
                Log.d("MobileNetworkQualityScoreCalculator", "Final score: $finalScore")
                return finalScore.coerceIn(0.0, 100.0)
            }
            // Si el signalStrength es inválido, se registra un error y se retorna el score original
            Log.e(
                "MobileNetworkQualityScoreCalculator",
                "Invalid signal strength value: $signalStrength"
            )
            return score
        }

        fun calculateOverallScore(
            ping: Int,
            jitter: Int,
            downloadSpeed: Double,
            uploadSpeed: Double,
            signalStrength: Int
        ): Double {
            val pingScore = calcPing(ping, jitter)
            val downloadScore = calcDownloadSpeed(downloadSpeed)
            val uploadScore = calcUploadSpeed(uploadSpeed)
//            val signalStrengthScore = calcSignalStrength(signalStrength)

            var totalScore = pingScore + downloadScore + uploadScore
            totalScore = applyBonusPenalty(totalScore, signalStrength)

            return totalScore
        }
    }
}