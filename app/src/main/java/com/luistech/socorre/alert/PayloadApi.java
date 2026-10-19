package com.luistech.socorre.alert;

import com.google.gson.JsonObject;
import com.luistech.socorre.domain.modelo.Evento;

// monta o json enviado para a api socorro
public final class PayloadApi {

    public static final int REGISTRO_ROTINA = 1;
    public static final int REGISTRO_QUEDA = 2;

    private PayloadApi() {
    }

    public static String montar(Evento e, String usuario, String deviceId, String modelo,
                                Float pressaoHpa, Double temperaturaC) {
        return montar(e, usuario, deviceId, modelo, pressaoHpa, temperaturaC, REGISTRO_QUEDA);
    }

    public static String montar(Evento e, String usuario, String deviceId, String modelo,
                                Float pressaoHpa, Double temperaturaC, int tipoRegistro) {
        JsonObject j = new JsonObject();
        j.addProperty("usuario", limitar(usuario, 150));
        j.addProperty("tipo_registro", tipoRegistro);
        j.addProperty("device_id", limitar(deviceId, 100));
        j.addProperty("modelo", limitar(modelo, 100));
        if (e.bateriaPct != null) j.addProperty("bateria", e.bateriaPct);
        if (e.localizacao != null) {
            j.addProperty("latitude", e.localizacao.lat);
            j.addProperty("longitude", e.localizacao.lng);
        }
        if (e.leituras != null) {
            if (e.leituras.velocidadeAntesKmh != null) {
                j.addProperty("velocidade", Math.max(0, e.leituras.velocidadeAntesKmh));
            }
            j.addProperty("nivel_queda", e.leituras.picoG);
        }
        if (temperaturaC != null) j.addProperty("temperatura", temperaturaC);
        if (pressaoHpa != null) j.addProperty("pressao", pressaoHpa);
        return j.toString();
    }

    private static String limitar(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
