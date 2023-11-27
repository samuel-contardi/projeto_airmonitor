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
    private LineChart temperaturaChart; // Adiciona o gráfico
    private List<Double> temperaturaList; // Lista para armazenar as temperaturas
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


        // Inicializa o TextView no layout
        temperaturaTextView = findViewById(R.id.temperaturaTextView);
        temperaturaChart = findViewById(R.id.temperaturaChart); // Inicializa o gráfico

        // Inicializa a lista para armazenar as temperaturas
        temperaturaList = new ArrayList<>();

        // Configurações iniciais do gráfico
        configureChart();

        // Inicializa a referência ao nó "Temperature:" no Firebase
        DatabaseReference temperaturaRef = FirebaseDatabase.getInstance().getReference().child("Temperature:");

        // Adiciona um listener para receber atualizações de temperatura
        temperaturaRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Obtém o valor da última temperatura do snapshot
                Double temperatura = dataSnapshot.getValue(Double.class);

                // Atualiza a lista com o novo valor da temperatura
                if (temperatura != null) {
                    temperaturaList.add(temperatura);
                    atualizarTextView(); // Atualiza o TextView para exibir a última temperatura
                    atualizarGrafico(); // Atualiza o gráfico com a nova temperatura
                    verificarTemperatura(temperatura); // Verifica se a temperatura é alta
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Trata erros, se necessário
            }
        });
    }

    // Configurações iniciais do gráfico
    private void configureChart() {
        temperaturaChart.setTouchEnabled(true);
        temperaturaChart.setDragEnabled(true);
        temperaturaChart.setScaleEnabled(true);

        Description description = new Description();
        description.setText("Histórico de Temperaturas");
        temperaturaChart.setDescription(description);
    }

    // Atualiza o gráfico com a lista de temperaturas
    private void atualizarGrafico() {
        if (temperaturaList.size() <= 1) {
            // Não há variação suficiente para atualizar o gráfico
            return;
        }

        List<Entry> entries = new ArrayList<>();
        Double ultimaTemperatura = temperaturaList.get(temperaturaList.size() - 1);

        for (int i = 0; i < temperaturaList.size() - 1; i++) {
            double variacao = Math.abs(temperaturaList.get(i) - temperaturaList.get(i + 1));
            if (variacao >= 0.5) {
                entries.add(new Entry(i, temperaturaList.get(i).floatValue()));
            }
        }

        // Adiciona a última temperatura
        entries.add(new Entry(temperaturaList.size() - 1, ultimaTemperatura.floatValue()));

        LineDataSet dataSet = new LineDataSet(entries, "Temperaturas");
        LineData lineData = new LineData(dataSet);

        temperaturaChart.setData(lineData);
        temperaturaChart.invalidate();
    }

    // Atualiza o TextView para exibir a última temperatura

    @SuppressLint("SetTextI18n")
    private void atualizarTextView() {
        if (!temperaturaList.isEmpty()) {
            // Exibe a temperatura mais recente no TextView
            Double ultimaTemperatura = temperaturaList.get(temperaturaList.size() - 1);
            temperaturaTextView.setText("Temperatura Atual: " + ultimaTemperatura + "°C");
        } else {
            temperaturaTextView.setText("Sem Dados de Temperatura");
        }
    }

    // Verifica se a temperatura é alta e envia uma notificação se necessário
    private void verificarTemperatura(Double temperatura) {
        if (temperatura < EstadoTemperatura.BAIXA.getLimite()) {
            enviarNotificacao("Alerta! Temperatura Baixa", "A temperatura está baixa! " + temperatura + "°C");

        } else if (temperatura > EstadoTemperatura.ALTA.getLimite()) {
            enviarNotificacao("Alerta! Temperatura Alta", "A temperatura está alta! " + temperatura + "°C");
        }
        adicionarAoHistorico(temperatura);
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
        HistoricoItem historicoItem = new HistoricoItem(dataHora, valor, "Temperatura");

        // Adiciona ao Firebase
        historicoRef.push().setValue(historicoItem);
    }

    // Método para enviar uma notificação
    private void enviarNotificacao(String titulo, String mensagem) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        criarCanalNotificacao(notificationManager);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "canal_id")
                .setSmallIcon(R.drawable.temperatura)
                .setContentTitle(titulo)
                .setContentText(mensagem)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Usar um ID de notificação exclusivo para cada notificação
        notificationManager.notify(1, builder.build());
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