package com.example.registrosatenciones.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

public class Conectividad {

    private Conectividad() {}

    public static boolean hayConexion(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network red = cm.getActiveNetwork();
        if (red == null) return false;

        NetworkCapabilities capacidades = cm.getNetworkCapabilities(red);
        return capacidades != null
                && capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
