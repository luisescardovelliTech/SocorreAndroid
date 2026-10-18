package com.luistech.socorre.ui.componente;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.luistech.socorre.R;

/**
 * Botão de pânico circular: é preciso SEGURAR por 3 s (anel de progresso) para evitar
 * acionamento acidental. Soltar antes cancela. Funciona com toque, teclado/controle (Enter)
 * e TalkBack (tocar duas vezes e segurar).
 */
public class BotaoPanico extends View {

    public interface Ouvinte {
        void aoAcionar();
    }

    public static final long DURACAO_MS = 3_000;

    private final Paint fundo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint anel = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titulo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subtitulo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF area = new RectF();
    private final String textoTitulo;
    private final String textoSubtitulo;
    private ValueAnimator animador;
    private float progresso;
    private Ouvinte ouvinte;

    public BotaoPanico(Context c, @Nullable AttributeSet attrs) {
        super(c, attrs);
        fundo.setColor(ContextCompat.getColor(c, R.color.emergencia));
        anel.setColor(ContextCompat.getColor(c, R.color.sobre_emergencia));
        anel.setStyle(Paint.Style.STROKE);
        anel.setStrokeCap(Paint.Cap.ROUND);
        titulo.setColor(ContextCompat.getColor(c, R.color.sobre_emergencia));
        titulo.setTextAlign(Paint.Align.CENTER);
        titulo.setTypeface(Typeface.DEFAULT_BOLD);
        subtitulo.setColor(ContextCompat.getColor(c, R.color.sobre_emergencia));
        subtitulo.setTextAlign(Paint.Align.CENTER);
        textoTitulo = c.getString(R.string.panico_titulo);
        textoSubtitulo = c.getString(R.string.panico_segure);
        setFocusable(true);
        setClickable(true);
        setContentDescription(c.getString(R.string.panico_descricao));
    }

    public void definirOuvinte(Ouvinte o) {
        this.ouvinte = o;
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        float lado = Math.min(w, h);
        float traco = lado * 0.06f;
        anel.setStrokeWidth(traco);
        area.set((w - lado) / 2 + traco, (h - lado) / 2 + traco, (w + lado) / 2 - traco, (h + lado) / 2 - traco);
        // Texto proporcional ao círculo para nunca vazar, mesmo com fonte do sistema em 200%.
        titulo.setTextSize(lado * 0.17f);
        subtitulo.setTextSize(lado * 0.09f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float raio = Math.min(getWidth(), getHeight()) / 2f;
        fundo.setAlpha(isPressed() ? 220 : 255);
        canvas.drawCircle(cx, cy, raio, fundo);
        if (progresso > 0) canvas.drawArc(area, -90, 360 * progresso, false, anel);
        canvas.drawText(textoTitulo, cx, cy, titulo);
        canvas.drawText(textoSubtitulo, cx, cy + subtitulo.getTextSize() * 1.6f, subtitulo);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                comecar();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                cancelar();
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (ehConfirmar(keyCode)) {
            if (e.getRepeatCount() == 0) comecar();
            return true;
        }
        return super.onKeyDown(keyCode, e);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent e) {
        if (ehConfirmar(keyCode)) {
            cancelar();
            return true;
        }
        return super.onKeyUp(keyCode, e);
    }

    @Override
    public boolean performClick() {
        // Um toque simples não aciona: só informa como usar.
        super.performClick();
        announceForAccessibility(getContext().getString(R.string.panico_dica));
        return true;
    }

    private static boolean ehConfirmar(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER;
    }

    private void comecar() {
        if (animador != null && animador.isRunning()) return;
        setPressed(true);
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        animador = ValueAnimator.ofFloat(0f, 1f);
        animador.setDuration(DURACAO_MS);
        animador.setInterpolator(new LinearInterpolator());
        animador.addUpdateListener(a -> {
            progresso = (float) a.getAnimatedValue();
            invalidate();
            if (progresso >= 1f) acionar();
        });
        animador.start();
    }

    private void cancelar() {
        if (animador != null && animador.isRunning()) {
            animador.cancel();
            if (progresso < 1f) performClick();
        }
        progresso = 0;
        setPressed(false);
        invalidate();
    }

    private void acionar() {
        animador.removeAllUpdateListeners();
        progresso = 0;
        setPressed(false);
        invalidate();
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        if (ouvinte != null) ouvinte.aoAcionar();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animador != null) animador.cancel();
        super.onDetachedFromWindow();
    }
}
