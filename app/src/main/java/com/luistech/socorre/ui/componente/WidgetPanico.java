package com.luistech.socorre.ui.componente;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import com.luistech.socorre.R;
import com.luistech.socorre.util.Notificacoes;

/**
 * Widget de pânico na tela inicial. Um toque abre a contagem regressiva (que pode ser
 * cancelada), já que o widget não permite exigir "segurar por 3 s".
 */
public class WidgetPanico extends AppWidgetProvider {

    @Override
    public void onUpdate(Context c, AppWidgetManager gerenciador, int[] ids) {
        for (int id : ids) {
            RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_panico);
            v.setOnClickPendingIntent(R.id.widget_botao_panico, Notificacoes.panico(c));
            gerenciador.updateAppWidget(id, v);
        }
    }
}
