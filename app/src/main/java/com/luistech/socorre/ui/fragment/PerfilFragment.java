package com.luistech.socorre.ui.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.luistech.socorre.R;
import com.luistech.socorre.databinding.FragmentPerfilBinding;
import com.luistech.socorre.domain.modelo.FichaMedica;
import com.luistech.socorre.domain.modelo.PerfilAtividade;
import com.luistech.socorre.domain.modelo.Usuario;
import com.luistech.socorre.ui.viewmodel.PerfilViewModel;
import com.luistech.socorre.util.Formatos;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint(Fragment.class)
public class PerfilFragment extends Hilt_PerfilFragment {

    private FragmentPerfilBinding b;
    private PerfilViewModel vm;
    private String dataNascimentoIso;
    private String fotoCaminho;
    private boolean preenchido;

    private final ActivityResultLauncher<PickVisualMediaRequest> escolherFoto =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) vm.definirFoto(uri);
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentPerfilBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        vm = new ViewModelProvider(this).get(PerfilViewModel.class);
        com.luistech.socorre.util.Formatos.mascararTelefone(b.campoTelefone);
        boolean inicial = Navegacao.emConfigInicial(this);
        b.botaoSalvar.setText(inicial ? R.string.acao_continuar : R.string.acao_salvar);

        b.campoTipoSanguineo.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.tipos_sanguineos)));
        b.switchGuardarFicha.setOnCheckedChangeListener((x, marcado) ->
                b.grupoFicha.setVisibility(marcado ? View.VISIBLE : View.GONE));
        b.campoNascimento.setOnClickListener(x -> escolherData());
        b.botaoFoto.setOnClickListener(x -> escolherFoto.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
        b.botaoSalvar.setOnClickListener(x -> vm.salvar(formulario()));

        vm.usuario().observe(getViewLifecycleOwner(), this::preencher);
        vm.foto().observe(getViewLifecycleOwner(), caminho -> {
            fotoCaminho = caminho;
            mostrarFoto();
        });
        vm.erros().observe(getViewLifecycleOwner(), e -> {
            b.layoutNome.setError(e.nome == 0 ? null : getString(e.nome));
            b.layoutTelefone.setError(e.telefone == 0 ? null : getString(e.telefone));
        });
        vm.mensagem().observe(getViewLifecycleOwner(), ev -> {
            Integer m = ev.consumir();
            if (m != null) Snackbar.make(b.getRoot(), m, Snackbar.LENGTH_LONG).show();
        });
        vm.salvo().observe(getViewLifecycleOwner(), ev -> {
            if (ev.consumir() != null && inicial) {
                Navegacao.ir(this, R.id.contatosFragment, Navegacao.configInicial());
            }
        });
    }

    private void preencher(Usuario u) {
        if (preenchido || u == null) return;
        preenchido = true;
        b.campoNome.setText(u.nome);
        b.campoTelefone.setText(u.telefone);
        dataNascimentoIso = u.dataNascimento;
        b.campoNascimento.setText(exibirData(u.dataNascimento));
        fotoCaminho = u.fotoUrl;
        mostrarFoto();
        marcarPerfil(PerfilAtividade.deTexto(u.perfilAtividade));
        FichaMedica f = u.fichaMedica;
        b.switchGuardarFicha.setChecked(f != null);
        b.grupoFicha.setVisibility(f != null ? View.VISIBLE : View.GONE);
        if (f != null) {
            b.campoTipoSanguineo.setText(f.tipoSanguineo, false);
            b.campoAlergias.setText(f.alergias);
            b.campoMedicamentos.setText(f.medicamentos);
            b.campoCondicoes.setText(f.condicoes);
            b.switchCompartilharFicha.setChecked(f.compartilharNosAlertas);
        }
    }

    private PerfilViewModel.Formulario formulario() {
        PerfilViewModel.Formulario f = new PerfilViewModel.Formulario();
        f.nome = texto(b.campoNome.getText());
        f.telefone = texto(b.campoTelefone.getText());
        f.dataNascimento = dataNascimentoIso;
        f.fotoUrl = fotoCaminho;
        f.perfil = perfilMarcado();
        f.guardarFicha = b.switchGuardarFicha.isChecked();
        if (f.guardarFicha) {
            FichaMedica ficha = new FichaMedica();
            ficha.tipoSanguineo = texto(b.campoTipoSanguineo.getText());
            ficha.alergias = texto(b.campoAlergias.getText());
            ficha.medicamentos = texto(b.campoMedicamentos.getText());
            ficha.condicoes = texto(b.campoCondicoes.getText());
            ficha.compartilharNosAlertas = b.switchCompartilharFicha.isChecked();
            f.ficha = ficha;
        }
        return f;
    }

    private void marcarPerfil(PerfilAtividade p) {
        switch (p) {
            case BICICLETA:
                b.chipBicicleta.setChecked(true);
                break;
            case MOTO:
                b.chipMoto.setChecked(true);
                break;
            case TRABALHO_CAMPO:
                b.chipCampo.setChecked(true);
                break;
            case CAMINHADA_CASA:
            default:
                b.chipCasa.setChecked(true);
                break;
        }
    }

    private PerfilAtividade perfilMarcado() {
        int id = b.grupoPerfil.getCheckedChipId();
        if (id == R.id.chipBicicleta) return PerfilAtividade.BICICLETA;
        if (id == R.id.chipMoto) return PerfilAtividade.MOTO;
        if (id == R.id.chipCampo) return PerfilAtividade.TRABALHO_CAMPO;
        return PerfilAtividade.CAMINHADA_CASA;
    }

    private void escolherData() {
        MaterialDatePicker<Long> seletor = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.perfil_nascimento)
                .setInputMode(MaterialDatePicker.INPUT_MODE_TEXT)
                .build();
        seletor.addOnPositiveButtonClickListener(ms -> {
            SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            iso.setTimeZone(TimeZone.getTimeZone("UTC"));
            dataNascimentoIso = iso.format(new Date(ms));
            b.campoNascimento.setText(exibirData(dataNascimentoIso));
        });
        seletor.show(getChildFragmentManager(), "data");
    }

    private static String exibirData(String iso) {
        if (iso == null) return "";
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso);
            return d == null ? "" : new SimpleDateFormat("dd/MM/yyyy", Formatos.PT_BR).format(d);
        } catch (ParseException e) {
            return "";
        }
    }

    private void mostrarFoto() {
        if (fotoCaminho != null && new File(fotoCaminho).exists()) {
            b.imagemFoto.setImageURI(null);
            b.imagemFoto.setImageURI(Uri.fromFile(new File(fotoCaminho)));
        } else {
            b.imagemFoto.setImageResource(R.drawable.ic_pessoa);
        }
    }

    private static String texto(@Nullable CharSequence s) {
        return s == null ? "" : s.toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
