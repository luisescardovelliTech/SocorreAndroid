package com.luistech.socorre.domain.modelo;

import java.util.ArrayList;
import java.util.List;

/** Status independente de cada canal de alerta (evento.envios). */
public class Envios {
    public Sms sms = new Sms();
    public Api api = new Api();

    public Envios() {
    }

    public static class Sms {
        public String status = StatusEnvio.PENDENTE;
        public List<String> enviadosPara = new ArrayList<>();
        /** Confirmados pela operadora (callback de envio). */
        public List<String> confirmados = new ArrayList<>();
        public List<String> falhas = new ArrayList<>();

        public Sms() {
        }
    }

    public static class Api {
        public String status = StatusEnvio.PENDENTE;
        public Integer httpCode;
        public int tentativas;
        public String ultimoErro;

        public Api() {
        }
    }

}
