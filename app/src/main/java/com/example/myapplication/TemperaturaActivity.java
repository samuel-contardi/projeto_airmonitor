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

public class TemperaturaActivity extends AppCompatActivity {

    private TextView temperaturaTextView;
    private LineChart temperaturaChart;
    private List<Double> temperaturaList;

    public enum EstadoTemperatura {
        BAIXA(0.0),
        ALTA(40.0);

        private final double limite;

        EstadoTemperatura(double limite) {
            this.limite = limite;
        }

        public double getLimite() {
            return limite;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temperatura);

        temperaturaTextView = findViewById(R.id.temperaturaTextView);
        temperaturaChart = findViewById(R.id.temperaturaChart);
        temperaturaList = new ArrayList<>();

        configureChart();
        carregarDadosDoFirebase();

        DatabaseReference temperaturaRef = FirebaseDatabase.getInstance().getReference().child("Temperature:");
        temperaturaRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Double temperatura = dataSnapshot.getValue(Double.class);

                if (temperatura != null) {
                    if (temperaturaList.isEmpty() || Math.abs(temperatura - temperaturaList.get(temperaturaList.size() - 1)) >= 0.5) {
                        temperaturaList.add(temperatura);
                        salvarListaNoFirebase();
                        atualizarTextView();
                        atualizarGrafico();
                        verificarTemperatura(temperatura);
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
        temperaturaChart.setTouchEnabled(true);
        temperaturaChart.setDragEnabled(true);
        temperaturaChart.setScaleEnabled(true);

        Description description = new Description();
        description.setText("Histórico de Temperaturas");
        temperaturaChart.setDescription(description);
    }

    private void atualizarGrafico() {
        if (temperaturaList.size() <= 1) {
            return;
        }

        List<Entry> entries = new ArrayList<>();

        for (int i = 0; i < temperaturaList.size(); i++) {
            entries.add(new Entry(i, temperaturaList.get(i).floatValue()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Temperaturas");
        LineData lineData = new LineData(dataSet);

        temperaturaChart.setData(lineData);
        temperaturaChart.invalidate();
    }

    private void verificarLimiteLista() {
        if (temperaturaList.size() >= 50) {
            limparLista();
        }
    }

    @SuppressLint("SetTextI18n")
    private void limparLista() {
        temperaturaList.clear();
        temperaturaChart.clear();
        temperaturaTextView.setText("Sem Dados de Temperatura");
        DatabaseReference temperaturaListaRef = FirebaseDatabase.getInstance().getReference().child("TemperaturaLista");
        temperaturaListaRef.removeValue();
    }

    private void salvarListaNoFirebase() {
        DatabaseReference temperaturaListaRef = FirebaseDatabase.getInstance().getReference().child("TemperaturaLista");
        temperaturaListaRef.setValue(temperaturaList);

        if (temperaturaList.size() >= 100) {
            temperaturaListaRef.removeValue();
            limparLista();
        }
    }

    @SuppressLint("SetTextI18n")
    private void atualizarTextView() {
        if (!temperaturaList.isEmpty()) {
            Double ultimaTemperatura = temperaturaList.get(temperaturaList.size() - 1);
            temperaturaTextView.setText("Temperatura Atual: " + ultimaTemperatura + "°C");
        } else {
            temperaturaTextView.setText("Sem Dados de Temperatura");
        }
    }

    private void verificarTemperatura(double temperatura) {
        if (temperatura < EstadoTemperatura.BAIXA.getLimite()) {
            enviarNotificacao("Alerta! Temperatura Baixa", "A temperatura está baixa! " + temperatura + "°C");
        } else if (temperatura > EstadoTemperatura.ALTA.getLimite()) {
            enviarNotificacao("Alerta! Temperatura Alta", "A temperatura está alta! " + temperatura + "°C");
        }
        adicionarAoHistorico(temperatura);
    }

    private void adicionarAoHistorico(Double valor) {
        DatabaseReference historicoRef = FirebaseDatabase.getInstance().getReference().child("Historico");

        Date dataAtual = new Date();
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
        String dataHora = dateFormat.format(dataAtual);

        HistoricoItem historicoItem = new HistoricoItem(dataHora, valor, "Temperatura");
        historicoRef.push().setValue(historicoItem);
    }

    private void enviarNotificacao(String titulo, String mensagem) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        criarCanalNotificacao(notificationManager);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "canal_id")
                .setSmallIcon(R.drawable.temperatura)
                .setContentTitle(titulo)
                .setContentText(mensagem)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify(1, builder.build());
    }

    private void criarCanalNotificacao(NotificationManager notificationManager) {
        String channelId = "canal_id";
        CharSequence channelName = "Canal de Notificações";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        notificationManager.createNotificationChannel(channel);
    }

    private void carregarDadosDoFirebase() {
        DatabaseReference temperaturaListaRef = FirebaseDatabase.getInstance().getReference().child("TemperaturaLista");

        temperaturaListaRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                temperaturaList.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Double temperatura = snapshot.getValue(Double.class);
                    if (temperatura != null) {
                        temperaturaList.add(temperatura);
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
