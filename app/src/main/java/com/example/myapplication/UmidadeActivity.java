package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UmidadeActivity extends AppCompatActivity {

    private TextView umidadeTextView;
    private LineChart umidadeChart;
    private List<Double> umidadeList;

    public enum EstadoUmidade {
        BAIXA(12.0),
        ALTA(60.0);

        private final double limite;

        EstadoUmidade(double limite) {
            this.limite = limite;
        }

        public double getLimite() {
            return limite;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_umidade);

        umidadeTextView = findViewById(R.id.umidadeTextView);
        umidadeChart = findViewById(R.id.umidadeChart);
        umidadeList = new ArrayList<>();

        configureChart();
        carregarDadosDoFirebase();

        DatabaseReference umidadeRef = FirebaseDatabase.getInstance().getReference().child("Humidity:");
        umidadeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Double umidade = dataSnapshot.getValue(Double.class);

                if (umidade != null) {
                    if (umidadeList.isEmpty() || Math.abs(umidade - umidadeList.get(umidadeList.size() - 1)) >= 0.5) {
                        umidadeList.add(umidade);
                        salvarListaNoFirebase();
                        atualizarTextView();
                        atualizarGrafico();
                        verificarUmidade(umidade);
                        verificarLimiteLista();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void configureChart() {
        umidadeChart.setTouchEnabled(true);
        umidadeChart.setDragEnabled(true);
        umidadeChart.setScaleEnabled(true);

        Description description = new Description();
        description.setText("Histórico de Umidade");
        umidadeChart.setDescription(description);
    }

    private void atualizarGrafico() {
        if (umidadeList.size() <= 1) {
            return;
        }

        List<Entry> entries = new ArrayList<>();

        for (int i = 0; i < umidadeList.size(); i++) {
            entries.add(new Entry(i, umidadeList.get(i).floatValue()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Umidade");
        LineData lineData = new LineData(dataSet);

        umidadeChart.setData(lineData);
        umidadeChart.invalidate();
    }

    private void verificarLimiteLista() {
        if (umidadeList.size() >= 50) {
            limparLista();
        }
    }

    @SuppressLint("SetTextI18n")
    private void limparLista() {
        umidadeList.clear();
        umidadeChart.clear();
        umidadeTextView.setText("Sem Dados de Umidade");
        DatabaseReference umidadeListaRef = FirebaseDatabase.getInstance().getReference().child("UmidadeLista");
        umidadeListaRef.removeValue();
    }

    private void salvarListaNoFirebase() {
        DatabaseReference umidadeListaRef = FirebaseDatabase.getInstance().getReference().child("UmidadeLista");
        umidadeListaRef.setValue(umidadeList);

        if (umidadeList.size() >= 100) {
            umidadeListaRef.removeValue();
            limparLista();
        }
    }

    @SuppressLint("SetTextI18n")
    private void atualizarTextView() {
        if (!umidadeList.isEmpty()) {
            Double ultimaUmidade = umidadeList.get(umidadeList.size() - 1);
            umidadeTextView.setText("Umidade Atual: " + ultimaUmidade + "%");
        } else {
            umidadeTextView.setText("Sem Dados de Umidade");
        }
    }

    private void verificarUmidade(double umidade) {
        if (umidade < EstadoUmidade.BAIXA.getLimite()) {
            enviarNotificacao("Alerta! Umidade Baixa", "A umidade está baixa! " + umidade + "%");
        } else if (umidade > EstadoUmidade.ALTA.getLimite()) {
            enviarNotificacao("Alerta! Umidade Alta", "A umidade está alta! " + umidade + "%");
        }
        adicionarAoHistorico(umidade);
    }

    private void adicionarAoHistorico(Double valor) {
        DatabaseReference historicoRef = FirebaseDatabase.getInstance().getReference().child("Historico");

        Date dataAtual = new Date();
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
        String dataHora = dateFormat.format(dataAtual);

        HistoricoItem historicoItem = new HistoricoItem(dataHora, valor, "Umidade");
        historicoRef.push().setValue(historicoItem);
    }

    private void enviarNotificacao(String titulo, String mensagem) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        criarCanalNotificacao(notificationManager);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "canal_id")
                .setSmallIcon(R.drawable.umidade)
                .setContentTitle(titulo)
                .setContentText(mensagem)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify(2, builder.build());
    }

    private void criarCanalNotificacao(NotificationManager notificationManager) {
        String channelId = "canal_id";
        CharSequence channelName = "Canal de Notificações";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        notificationManager.createNotificationChannel(channel);
    }

    private void carregarDadosDoFirebase() {
        DatabaseReference umidadeListaRef = FirebaseDatabase.getInstance().getReference().child("UmidadeLista");

        umidadeListaRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                umidadeList.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Double umidade = snapshot.getValue(Double.class);
                    if (umidade != null) {
                        umidadeList.add(umidade);
                    }
                }

                atualizarGrafico();
                atualizarTextView();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }
}
