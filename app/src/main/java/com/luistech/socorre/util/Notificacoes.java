package com.luistech.socorre.util;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.luistech.socorre.R;
import com.luistech.socorre.alert.ReceptorAcoes;
import com.luistech.socorre.alert.ReceptorCheckin;
import com.luistech.socorre.security.GerenciadorPermissoes;
import com.luistech.socorre.service.ServicoMonitoramento;
import com.luistech.socorre.ui.activity.AlertaEnviadoActivity;
import com.luistech.socorre.ui.activity.ContagemActivity;
import com.luistech.socorre.ui.activity.MainActivity;

public final class Notificacoes {

    public static final String CANAL_MONITORAMENTO = "monitoramento";
    public static final String CANAL_EMERGENCIA = "emergencia";
    public static final String CANAL_CHECKIN = "checkin";
    public static final String CANAL_AVISOS = "avisos";

    public static final int ID_MONITORAMENTO = 1;
    public static final int ID_EMERGENCIA = 2;
    public static final int ID_CHECKIN = 3;
    public static final int ID_AVISO = 4;

    private Notificacoes() {
    }

    public static void criarCanais(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        NotificationChannel monitoramento = new NotificationChannel(CANAL_MONITORAMENTO,
                c.getString(R.string.canal_monitoramento), NotificationManager.IMPORTANCE_LOW);
        NotificationChannel emergencia = new NotificationChannel(CANAL_EMERGENCIA,
                c.getString(R.string.canal_emergencia), NotificationManager.IMPORTANCE_HIGH);
        emergencia.setSound(null, null);
        emergencia.enableVibration(false);
        emergencia.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        emergencia.setBypassDnd(true);
        NotificationChannel checkin = new NotificationChannel(CANAL_CHECKIN,
                c.getString(R.string.canal_checkin), NotificationManager.IMPORTANCE_HIGH);
        NotificationChannel avisos = new NotificationChannel(CANAL_AVISOS,
                c.getString(R.string.canal_avisos), NotificationManager.IMPORTANCE_DEFAULT);
        nm.createNotificationChannel(monitoramento);
        nm.createNotificationChannel(emergencia);
        nm.createNotificationChannel(checkin);
        nm.createNotificationChannel(avisos);
    }

    private static PendingIntent abrirApp(Context c) {
        Intent i = new Intent(c, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(c, 10, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static PendingIntent panico(Context c) {
        Intent i = new Intent(c, ServicoMonitoramento.class).setAction(ServicoMonitoramento.ACAO_PANICO);
        return PendingIntent.getForegroundService(c, 11, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static Notification monitoramento(Context c, boolean monitorando, boolean trajeto, boolean emergencia) {
        int texto = emergencia ? R.string.status_emergencia
                : trajeto ? R.string.notif_trajeto_ativo
                : monitorando ? R.string.status_ativo : R.string.status_pausado;
        return new NotificationCompat.Builder(c, CANAL_MONITORAMENTO)
                .setSmallIcon(R.drawable.ic_notificacao)
                .setContentTitle(c.getString(R.string.app_name))
                .setContentText(c.getString(texto))
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setContentIntent(abrirApp(c))
                .addAction(R.drawable.ic_notif_alerta, c.getString(R.string.notif_acao_panico), panico(c))
                .build();
    }

    public static Notification contagem(Context c, int segundos, boolean teste) {
        Intent tela = new Intent(c, ContagemActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent abrir = PendingIntent.getActivity(c, 20, tela, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CANAL_EMERGENCIA)
                .setSmallIcon(R.drawable.ic_notif_alerta)
                .setContentTitle(c.getString(teste ? R.string.notif_contagem_titulo_teste : R.string.notif_contagem_titulo))
                .setContentText(c.getResources().getQuantityString(R.plurals.notif_contagem_texto, segundos, segundos))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(abrir)
                .addAction(R.drawable.ic_notif_check, c.getString(R.string.contagem_estou_bem),
                        ReceptorAcoes.intencao(c, ReceptorAcoes.ACAO_CANCELAR))
                .addAction(R.drawable.ic_notif_alerta, c.getString(R.string.contagem_pedir_ajuda),
                        ReceptorAcoes.intencao(c, ReceptorAcoes.ACAO_ENVIAR_AGORA));
        if (GerenciadorPermissoes.podeTelaCheia(c)) b.setFullScreenIntent(abrir, true);
        return b.build();
    }

    public static Notification alertaEnviado(Context c, boolean teste) {
        Intent tela = new Intent(c, AlertaEnviadoActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent abrir = PendingIntent.getActivity(c, 21, tela, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new NotificationCompat.Builder(c, CANAL_EMERGENCIA)
                .setSmallIcon(R.drawable.ic_notif_alerta)
                .setContentTitle(c.getString(teste ? R.string.alerta_titulo_teste : R.string.alerta_titulo))
                .setContentText(c.getString(R.string.notif_alerta_texto))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true)
                .setContentIntent(abrir)
                .build();
    }

    public static Notification checkin(Context c) {
        return new NotificationCompat.Builder(c, CANAL_CHECKIN)
                .setSmallIcon(R.drawable.ic_notificacao)
                .setContentTitle(c.getString(R.string.checkin_titulo))
                .setContentText(c.getString(R.string.checkin_texto))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(ReceptorCheckin.intencao(c, ReceptorCheckin.ACAO_OK))
                .addAction(R.drawable.ic_notif_check, c.getString(R.string.checkin_estou_bem),
                        ReceptorCheckin.intencao(c, ReceptorCheckin.ACAO_OK))
                .build();
    }

    public static Notification aviso(Context c, int titulo, int texto) {
        return new NotificationCompat.Builder(c, CANAL_AVISOS)
                .setSmallIcon(R.drawable.ic_notificacao)
                .setContentTitle(c.getString(titulo))
                .setContentText(c.getString(texto))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(c.getString(texto)))
                .setAutoCancel(true)
                .setContentIntent(abrirApp(c))
                .build();
    }

    @SuppressLint("MissingPermission")
    public static void mostrar(Context c, int id, Notification n) {
        if (!GerenciadorPermissoes.temNotificacoes(c)) return;
        try {
            NotificationManagerCompat.from(c).notify(id, n);
        } catch (SecurityException e) {
            Log.w("Socorre.Notif", "sem permissao para notificar", e);
        }
    }

    public static void remover(Context c, int id) {
        NotificationManagerCompat.from(c).cancel(id);
    }
}
