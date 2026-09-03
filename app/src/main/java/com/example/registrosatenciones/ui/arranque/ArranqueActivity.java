package com.example.registrosatenciones.ui.arranque;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.registrosatenciones.response.InstitucionResponse;
import com.example.registrosatenciones.ui.inicio.InicioActivity;
import com.example.registrosatenciones.ui.login.LoginActivity;
import com.example.registrosatenciones.ui.seleccioninstitucion.SeleccionInstitucionActivity;
import com.example.registrosatenciones.util.PreferenciasUsuario;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// Punto de entrada de la app. Decide a dónde ir leyendo únicamente lo que hay
// guardado en el dispositivo, sin tocar la red: de eso depende que la app se
// pueda usar sin conexión. Antes el launcher era LoginActivity, que siempre
// llamaba a la API, así que sin señal la app quedaba inaccesible aunque la base
// local estuviera completa.
//
// No tiene layout: resuelve el destino y se saca de la pila enseguida.
public class ArranqueActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(armarIntentDestino());
        finish();
    }

    private Intent armarIntentDestino() {
        if (!PreferenciasUsuario.haySesionActiva(this)) {
            return new Intent(this, LoginActivity.class);
        }

        if (PreferenciasUsuario.getInstitucionActivaId(this) == -1) {
            // La pantalla de selección espera recibir la lista; se le pasa la que
            // quedó cacheada al loguear, para no depender de la red.
            List<InstitucionResponse> instituciones =
                    new ArrayList<>(PreferenciasUsuario.getInstituciones(this));

            // Sin lista cacheada esa pantalla no tiene nada que ofrecer y el
            // usuario quedaría sin salida. Se lo manda a loguear, que es lo único
            // que puede reponerla.
            if (instituciones.isEmpty()) {
                return new Intent(this, LoginActivity.class);
            }

            Intent intent = new Intent(this, SeleccionInstitucionActivity.class);
            intent.putExtra("instituciones", (Serializable) instituciones);
            return intent;
        }

        return new Intent(this, InicioActivity.class);
    }
}
