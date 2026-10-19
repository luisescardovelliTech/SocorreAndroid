package com.luistech.socorre.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class Formatos {

    public static final Locale PT_BR = new Locale("pt", "BR");

    private Formatos() {
    }

    public static String dataHora(Date d) {
        return d == null ? "" : new SimpleDateFormat("dd/MM/yyyy HH:mm", PT_BR).format(d);
    }

    public static String km(android.content.Context c, double metros) {
        return c.getString(com.luistech.socorre.R.string.formato_km, String.format(PT_BR, "%.2f", metros / 1000.0));
    }

    public static String kmh(android.content.Context c, double v) {
        return c.getString(com.luistech.socorre.R.string.formato_kmh, String.format(PT_BR, "%.1f", v));
    }

    public static String duracao(long segundos) {
        long h = segundos / 3600;
        long m = (segundos % 3600) / 60;
        long s = segundos % 60;
        return h > 0 ? String.format(PT_BR, "%d:%02d:%02d", h, m, s) : String.format(PT_BR, "%02d:%02d", m, s);
    }

    public static String telefone(String numero) {
        if (numero == null) return "";
        String d = numero.replaceAll("[^0-9]", "");
        return d.length() >= 10 && d.length() <= 13
                ? com.luistech.socorre.domain.caso_de_uso.ValidadorTelefone.mascarar(numero) : numero;
    }

    public static void mascararTelefone(android.widget.EditText campo) {
        campo.addTextChangedListener(new android.text.TextWatcher() {
            private boolean editando;

            @Override
            public void beforeTextChanged(CharSequence s, int i, int a, int d) {
            }

            @Override
            public void onTextChanged(CharSequence s, int i, int a, int d) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (editando) return;
                String m = com.luistech.socorre.domain.caso_de_uso.ValidadorTelefone.mascarar(s.toString());
                if (m.contentEquals(s)) return;
                editando = true;
                s.replace(0, s.length(), m);
                editando = false;
            }
        });
    }

    public static String decimal(double v) {
        return String.format(PT_BR, "%.1f", v);
    }
}
