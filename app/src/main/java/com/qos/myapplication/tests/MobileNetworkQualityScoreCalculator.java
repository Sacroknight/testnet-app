package com.qos.myapplication.tests;

public class MobileNetworkQualityScoreCalculator {

    public static double calcPing(double mP, double mJ) {
        double pingScore = 0;
        if (mP < 90 && mJ < 30) {
            pingScore = 40;
        } else if (mP >= 90 && mP <= 1000 && mJ < 30) {
            pingScore = 40 * ((-0.875 / 910) * mP + 1.06159);
        } else if (mP > 1000 && mP <= 4000 && mJ < 30) {
            pingScore = 40 * (((-0.1 / 2999) * mP) + 0.13337);
        }  else if (mP < 90 && mJ <= 50 && mJ >= 30) {
            pingScore = 40 * 0.9;
        } else if (mP >= 90 && mP <= 1000 && mJ >= 30 && mJ <= 50) {
            pingScore = 40 * ((-0.875 / 910) * mP + 1.06159) * 0.9;
        } else if (mP > 1000 && mP <= 4000 && mJ >= 30 && mJ <= 50) {
            pingScore = 40 * ((-0.1 / 2999) * mP + 0.13337) * 0.9;
        } else if (mP < 90 && mJ <= 500 && mJ > 50) {
            pingScore = 40 * (((double) -1 /500) * mJ + 1);
        } else if (mP >= 90 && mP <= 1000 && mJ <= 500 && mJ > 50) {
            pingScore = 40 * ((-0.875 / 910) * mP + 1.06159) * (((double) -1 /500) * mJ + 1);
        } else if (mP > 1000 && mP <= 4000 && mJ <= 500 && mJ > 50) {
            pingScore = 40 * ((-0.1 / 2999) * mP + 0.13337) * (((double) -1 /500) * mJ + 1);
        } else if (mJ > 500) {
            pingScore = 0;
        } else if (mP > 4000) {
            pingScore = 0;
        }

        return pingScore;
    }
    public static double calcDownloadSpeed(double mV) {
        double downloadScore = 0;
        String msg;
        // Condiciones para la calculadora de descarga
        if (mV < 0) {
            msg ="Error en la medición de velocidad de descarga";
            System.out.println(msg);
        } else if (mV <= 0 && mV < 0.5) {
            downloadScore = 0;
        } else if (mV == 0.5) {
            downloadScore = 1;
        } else if (mV > 0.5 && mV <= 25) {
            downloadScore = (0.9667 / 24.5) * 30 * mV;
        } else if (mV > 25) {
            downloadScore = 30;
        }
        return downloadScore;
    }
    public static double calcUploadSpeed(double mVc){
        double uploadScore = 0;
        if (mVc < 0) {
            String msg = "Error en la medición de velocidad de carga";
            System.out.println(msg);
        } else if (mVc <= 0 && mVc < 0.5) {
            uploadScore = 0;
        } else if (mVc == 0.5) {
            uploadScore = 1;
        } else if (mVc > 0.5 && mVc <= 10) {
            uploadScore = 0.1 * 20 * mVc;
        } else if (mVc > 10) {
            uploadScore = 20;
        }
        return uploadScore;
    }
    public static double calcSignalStrength(double mI) {
        // Calcula la puntuación de la señal a partir de la intensidad de la señal (dBm)

        String msg;
        double signalScore = 0;

        // Rango 10: -51dBm a -79dBm
        if (mI <= -51 && mI >= -79) {
            signalScore = 10;
        }

        // Rango 7-9: -80dBm a -95dBm
        else if (mI <= -80 && mI >= -95) {
            signalScore = 9+((2 / 15.0) * mI + (32 / 3.0));
        }

        // Rango 6-3: -96dBm a -104dBm
        else if ((mI <= -96) && (mI >= -104)) {
            signalScore = (3 / 8.0) * mI + 42;
        }

        // Rango 1-2: -105dBm a -112dBm
        else if ((mI <= -105) && (mI >= -112)) {
            signalScore = (1 / 7.0) * mI + 17;
        }

        // Rango 0: menor a -112dBm
        else if (mI < -112) {
            signalScore = 0;
        }

        // Valor fuera del rango
        else if (mI > -51) {
            msg = "Error en la medición de la intensidad de la señal: valor fuera del rango (-114dBm, -51dBm)";
            System.out.println(msg);
            return Double.NaN;
        }

        return signalScore;
    }


    /* Algoritmo para el calculo de la penalización/bonificación en el puntaje final
    * */
        public static double applyBonusPenalty(double score, double signalStrength) {
        if (signalStrength >= (20 / 3.0) && score < (200 / 3.0)) {
            score *= 0.9;  // Penalización del 10%
        } else if (signalStrength >= 9 && score < (200 / 3.0)) {
            score *= 0.8;  // Penalización del 20%
        } else if (score >= (200 / 3.0) && signalStrength < (20 / 3.0)) {
            score *= 1.1;  // Bonificación del 10%
        } else if (signalStrength >= (20 / 3.0) && signalStrength <= 3) {
            score *= 1.2;  // Bonificación del 20%
        }

        return score;
    }
   /* public static void main(String[] args) {
        // Prueba
        double mP = 80;
        double mJ = 25;
        double mV = 25;
        double mVc =15;
        double mI = -88;
        double resultado = calcPing(mP, mJ);
        double resultado1 = calcDownloadSpeed(mV);
        double resultado2 = calcUploadSpeed(mVc);
        double resultado3 = calcSignalStrength(mI);
        double score = resultado +resultado1 + resultado2 + resultado3;

        //Penalización/Bonificación situacional
        //score = applybonus_penalty(score, resultado3);

        System.out.println("Calificación de ping: " + resultado);
        System.out.println("Calificación de descarga: " + resultado1);
        System.out.println("Calificación de carga: " + resultado2);
        System.out.println("Calificación de fuerza de señal: " + resultado3);
        System.out.println("Calificación total: " + score);
        System.out.println("Calificación con bonus: "+ applyBonusPenalty(resultado, resultado3));
    }*/
}

