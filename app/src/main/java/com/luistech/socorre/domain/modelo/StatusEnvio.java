package com.luistech.socorre.domain.modelo;

/** Status de cada canal de envio (sms e api), gravados como texto. */
public final class StatusEnvio {
    public static final String PENDENTE = "PENDENTE";
    public static final String ENVIADO = "ENVIADO";
    public static final String FALHOU = "FALHOU";
    public static final String PARCIAL = "PARCIAL";
    public static final String SEM_PERMISSAO = "SEM_PERMISSAO";
    public static final String SEM_CONTATOS = "SEM_CONTATOS";
    public static final String NAO_CONFIGURADO = "NAO_CONFIGURADO";
    public static final String SIMULADO = "SIMULADO";

    private StatusEnvio() {
    }
}
