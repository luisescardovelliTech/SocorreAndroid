package com.luistech.socorre.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.ExistingWorkPolicy;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.luistech.socorre.BuildConfig;
import com.luistech.socorre.alert.EnviadorApi;
import com.luistech.socorre.data.api.ApiEventos;
import com.luistech.socorre.data.repositorio.RepositorioEventos;
import com.luistech.socorre.domain.modelo.StatusEnvio;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;

@HiltWorker
public class TrabalhadorReenvioApi extends Worker {

    public static final String UID = "uid";
    public static final String EVENTO_ID = "eventoId";
    public static final String PAYLOAD = "payload";
    public static final String TENTATIVA = "tentativa";

    static final long[] ATRASOS_SEG = {10, 30, 60, 300, 900};
    static final int MAX_REENVIOS = ATRASOS_SEG.length;
    private static final String TAG = "Socorre.Api";

    private final ApiEventos api;
    private final RepositorioEventos repositorio;

    @AssistedInject
    public TrabalhadorReenvioApi(@Assisted @NonNull Context contexto, @Assisted @NonNull WorkerParameters params,
                                 ApiEventos api, RepositorioEventos repositorio) {
        super(contexto, params);
        this.api = api;
        this.repositorio = repositorio;
    }

    // envia o registro pra api; se falhar tenta de novo depois
    @NonNull
    @Override
    public Result doWork() {
        String uid = getInputData().getString(UID);
        String eventoId = getInputData().getString(EVENTO_ID);
        String payload = getInputData().getString(PAYLOAD);
        int tentativa = getInputData().getInt(TENTATIVA, 1);
        if (uid == null || eventoId == null || payload == null) return Result.failure();

        Map<String, Object> campos = new HashMap<>();
        campos.put("envios.api.tentativas", tentativa);
        Integer codigo = null;
        String erro;
        boolean reenviavel;
        try {
            RequestBody corpo = RequestBody.create(payload, MediaType.get("application/json; charset=utf-8"));
            Response<ResponseBody> resposta = api.registrar(BuildConfig.API_URL_EVENTOS, corpo).execute();
            codigo = resposta.code();
            ResponseBody b = resposta.isSuccessful() ? resposta.body() : resposta.errorBody();
            if (b != null) b.close();
            if (resposta.isSuccessful()) {
                campos.put("envios.api.status", StatusEnvio.ENVIADO);
                campos.put("envios.api.httpCode", codigo);
                campos.put("envios.api.ultimoErro", null);
                repositorio.atualizar(uid, eventoId, campos);
                Log.i(TAG, "api ok evento=" + eventoId + " http=" + codigo + " tentativa=" + tentativa);
                return Result.success();
            }
            erro = "HTTP " + codigo;
            reenviavel = codigo >= 500 || codigo == 408 || codigo == 429;
        } catch (IOException | RuntimeException e) {
            erro = e.getClass().getSimpleName();
            reenviavel = true;
        }

        campos.put("envios.api.httpCode", codigo);
        campos.put("envios.api.ultimoErro", erro);
        if (reenviavel && tentativa <= MAX_REENVIOS) {
            campos.put("envios.api.status", StatusEnvio.PENDENTE);
            EnviadorApi.agendar(getApplicationContext(), uid, eventoId, payload,
                    tentativa + 1, ATRASOS_SEG[tentativa - 1], ExistingWorkPolicy.APPEND_OR_REPLACE);
        } else {
            campos.put("envios.api.status", StatusEnvio.FALHOU);
        }
        repositorio.atualizar(uid, eventoId, campos);
        Log.w(TAG, "api falhou evento=" + eventoId + " erro=" + erro + " tentativa=" + tentativa);
        return Result.success();
    }
}
