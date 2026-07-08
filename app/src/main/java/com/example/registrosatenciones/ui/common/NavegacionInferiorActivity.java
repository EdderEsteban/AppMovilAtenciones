package com.example.registrosatenciones.ui.common;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.registrosatenciones.R;
import com.example.registrosatenciones.ui.inicio.InicioActivity;
import com.example.registrosatenciones.ui.pacientes.PacientesActivity;
import com.example.registrosatenciones.ui.perfil.PerfilActivity;
import com.example.registrosatenciones.ui.sincronizacion.SincronizacionActivity;

/**
 * Base común para las 4 pantallas de nivel superior (Inicio, Pacientes, Sincronizar, Perfil).
 * Centraliza la barra de navegación inferior para que no "desaparezca" al cambiar de pestaña.
 */
public abstract class NavegacionInferiorActivity extends AppCompatActivity {

    protected abstract BottomNavigationView getBottomNavigationView();

    protected abstract int getTabActual();

    protected void configurarNavInferior() {
        BottomNavigationView nav = getBottomNavigationView();
        nav.setSelectedItemId(getTabActual());

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == getTabActual()) {
                return true;
            } else if (id == R.id.nav_inicio) {
                irA(InicioActivity.class);
                return true;
            } else if (id == R.id.nav_pacientes) {
                irA(PacientesActivity.class);
                return true;
            } else if (id == R.id.nav_sincronizar) {
                irA(SincronizacionActivity.class);
                return true;
            } else if (id == R.id.nav_perfil) {
                irA(PerfilActivity.class);
                return true;
            }
            return false;
        });
    }

    private void irA(Class<?> destino) {
        startActivity(new Intent(this, destino));
        finish();
    }
}
