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

public class    Co2Activity extends AppCompatActivity {
    private TextView co2TextView;
    private LineChart co2Chart;
    private List<Double> co2List;
    public enum EstadoCo2 {
        alerta(25.0),
        ALTA(45.0);

        private final double limite;

        EstadoCo2(double limite) {
            this.limite = limite;
        }

        public double getLimite() {
            return limite;
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_co2);
        co2TextView = findViewById(R.id.co2TextView);
        co2Chart = findViewById(R.id.co2Chart);
        co2List = new ArrayList<>();

        configureChart();

        DatabaseReference co2Ref = FirebaseDatabase.getInstance().getReference().child("*****:");
        co2Ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Double co2 = dataSnapshot.getValue(Double.class);

                if (co2 != null) {
                    co2List.add(co2);
                    atualizarTextView();
                    atualizarGrafico();
                    verificarCo2(co2);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    private void configureChart() {
        co2Chart.setTouchEnabled(true);
        co2Chart.setDragEnabled(true);
        co2Chart.setScaleEnabled(true);

        Description description = new Description();
        description.setText("Histórico de Co2");
        co2Chart.setDescription(description);
    }
    private void atualizarGrafico() {
        if (co2List.size() <= 1){
            return;
        }
        List<Entry> entries = new ArrayList<>();
        Double ultimoCo2 = co2List.get(co2List.size() - 1);

        for (int i = 0; i < co2List.size() - 1; i++) {
            double variacao = Math.abs(co2List.get(i) - co2List.get(i + 1));
            if (variacao >= 0.5) {
                entries.add(new Entry(i, co2List.get(i).floatValue()));
            }
        }
        entries.add(new Entry(co2List.size() - 1, ultimoCo2.floatValue()));
        LineDataSet dataSet = new LineDataSet(entries, "Co2");
        LineData lineData = new LineData(dataSet);

        co2Chart.setData(lineData);
        co2Chart.invalidate();
    }

    @SuppressLint("SetTextI18n")
    private void atualizarTextView() {
        if (!co2List.isEmpty()) {
            Double ultimoCo2 = co2List.get(co2List.size() - 1);
            co2TextView.setText("Nível de Co2 Atual: " + ultimoCo2 + "%");
        } else {
            co2TextView.setText("Sem Dados de Co2");
        }
    }

    private void verificarCo2(double co2) {
        if (co2 < Co2Activity.EstadoCo2.alerta.getLimite()) {
            enviarNotificacao("Alerta! Co2 ", "O Nível de Co2 está aumentando! " + co2 + "%");
        } else if (co2 > Co2Activity.EstadoCo2.ALTA.getLimite()) {
            enviarNotificacao("Alerta! Co2 Alto", "O Nível de Co2 esta auto cuidado!  " + co2 + "%");
        }
        adicionarAoHistorico(co2);
    }
    private void adicionarAoHistorico(Double valor) {
        DatabaseReference historicoRef = FirebaseDatabase.getInstance().getReference().child("Historico");

        Date dataAtual = new Date();

        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

        String dataHora = dateFormat.format(dataAtual);

        HistoricoItem historicoItem = new HistoricoItem(dataHora, valor, "Co2");

        historicoRef.push().setValue(historicoItem);
    }
    private void enviarNotificacao(String titulo, String mensagem) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        criarCanalNotificacao(notificationManager);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "canal_id")
                .setSmallIcon(R.drawable.co2)
                .setContentTitle(titulo)
                .setContentText(mensagem)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify(4, builder.build());
    }

    private void criarCanalNotificacao(NotificationManager notificationManager) {
        String channelId = "canal_id";
        CharSequence channelName = "Canal de Notificações";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        notificationManager.createNotificationChannel(channel);
    }
}
