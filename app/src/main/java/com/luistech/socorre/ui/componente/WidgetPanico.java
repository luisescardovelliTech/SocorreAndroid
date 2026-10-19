package com.luistech.socorre.ui.componente;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import com.luistech.socorre.R;
import com.luistech.socorre.util.Notificacoes;

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
