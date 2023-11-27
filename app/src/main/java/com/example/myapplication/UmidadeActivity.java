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
    public enum EstadoUmidade{
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

        DatabaseReference umidadeRef = FirebaseDatabase.getInstance().getReference().child("Humidity:");
        umidadeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Double umidade = dataSnapshot.getValue(Double.class);

                if (umidade != null) {
                    umidadeList.add(umidade);
                    atualizarTextView();
                    atualizarGrafico();
                    verificarUmidade(umidade);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

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
        if (umidadeList.size() <= 1){
            return;
        }
        List<Entry> entries = new ArrayList<>();
        Double ultimaUmidade = umidadeList.get(umidadeList.size() - 1);

        for (int i = 0; i < umidadeList.size() - 1; i++) {
            double variacao = Math.abs(umidadeList.get(i) - umidadeList.get(i + 1));
            if (variacao >= 0.5) {
                entries.add(new Entry(i, umidadeList.get(i).floatValue()));
            }
        }
        entries.add(new Entry(umidadeList.size() - 1, ultimaUmidade.floatValue()));
        LineDataSet dataSet = new LineDataSet(entries, "Umidade");
        LineData lineData = new LineData(dataSet);

        umidadeChart.setData(lineData);
        umidadeChart.invalidate();
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

        // Obtém a data e hora atual
        Date dataAtual = new Date();

        // Cria um formato de data e hora usando a configuração local
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

        // Formata a data e hora
        String dataHora = dateFormat.format(dataAtual);

        // Cria um novo item de histórico
        HistoricoItem historicoItem = new HistoricoItem(dataHora, valor, "Umidade");

        // Adiciona ao Firebase
        historicoRef.push().setValue(historicoItem);
    }
    private void enviarNotificacao(String titulo, String mensagem) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Verificar se o dispositivo está executando o Android 8.0 (Oreo) ou superior
        criarCanalNotificacao(notificationManager);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "canal_id")
                .setSmallIcon(R.drawable.umidade)
                .setContentTitle(titulo)
                .setContentText(mensagem)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Certifique-se de usar um ID de notificação exclusivo para cada notificação
        notificationManager.notify(2, builder.build());
    }

    // Método para criar um canal de notificação (Oreo e superior)

    private void criarCanalNotificacao(NotificationManager notificationManager) {
        String channelId = "canal_id";
        CharSequence channelName = "Canal de Notificações";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        notificationManager.createNotificationChannel(channel);
    }
}