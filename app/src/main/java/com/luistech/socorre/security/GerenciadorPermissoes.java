package com.luistech.socorre.security;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;

import androidx.core.content.ContextCompat;

/** Consultas de estado de permissões. Nenhum método lança exceção por permissão negada. */
public final class GerenciadorPermissoes {

    private GerenciadorPermissoes() {
    }

    private static boolean tem(Context c, String permissao) {
        return ContextCompat.checkSelfPermission(c, permissao) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean temLocalizacao(Context c) {
        return tem(c, Manifest.permission.ACCESS_FINE_LOCATION) || tem(c, Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    public static boolean temLocalizacaoPrecisa(Context c) {
        return tem(c, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    public static boolean temLocalizacaoSegundoPlano(Context c) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return temLocalizacao(c);
        return tem(c, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
    }

    public static boolean temSms(Context c) {
        return tem(c, Manifest.permission.SEND_SMS)
                && c.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    public static boolean temNotificacoes(Context c) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
        return tem(c, Manifest.permission.POST_NOTIFICATIONS);
    }

    public static boolean podeAlarmeExato(Context c) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager am = c.getSystemService(AlarmManager.class);
        return am != null && am.canScheduleExactAlarms();
    }

    /** Android 14+: tela cheia só é liberada para alguns tipos de app; senão usamos notificação de alta prioridade. */
    public static boolean podeTelaCheia(Context c) {
        if (Build.VERSION.SDK_INT < 34) return true;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        return nm != null && nm.canUseFullScreenIntent();
    }

    public static boolean ignoraOtimizacaoBateria(Context c) {
        PowerManager pm = c.getSystemService(PowerManager.class);
        return pm != null && pm.isIgnoringBatteryOptimizations(c.getPackageName());
    }
}
