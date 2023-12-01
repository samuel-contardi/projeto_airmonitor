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

public class GlpActivity extends AppCompatActivity {
    private TextView glpTextView;
    private LineChart glpChart;
    private List<Double> glpList;

    public enum EstadoGlp {
        ALERTA(1000.0),
        ALTA(1200.0);

        private final double limite;

        EstadoGlp(double limite) {
            this.limite = limite;
        }

        public double getLimite() {
            return limite;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glp);
        glpTextView = findViewById(R.id.glpTextView);
        glpChart = findViewById(R.id.glpChart);
        glpList = new ArrayList<>();

        configureChart();
        carregarDadosDoFirebase();

        DatabaseReference glpRef = FirebaseDatabase.getInstance().getReference().child("GLP:");
        glpRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Double glp = dataSnapshot.getValue(Double.class);

                if (glp != null) {
                    if (glpList.isEmpty() || Math.abs(glp - glpList.get(glpList.size() - 1)) >= 50.0) {
                        glpList.add(glp);
                        salvarListaNoFirebase();
                        atualizarTextView();
                        atualizarGrafico();
                        verificarGlp(glp);
                        verificarLimiteLista();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void configureChart() {
        glpChart.setTouchEnabled(true);
        glpChart.setDragEnabled(true);
        glpChart.setScaleEnabled(true);

        Description description = new Description();
        description.setText("Histórico de GLP");
        glpChart.setDescription(description);
    }

    private void atualizarGrafico() {
        if (glpList.size() <= 1) {
            return;
        }
        List<Entry> entries = new ArrayList<>();
        Double ultimoGlp = glpList.get(glpList.size() - 1);

        for (int i = 0; i < glpList.size() - 1; i++) {
            double variacao = Math.abs(glpList.get(i) - glpList.get(i + 1));
            if (variacao >= 0.5) {
                entries.add(new Entry(i, glpList.get(i).floatValue()));
            }
        }
        entries.add(new Entry(glpList.size() - 1, ultimoGlp.floatValue()));
        LineDataSet dataSet = new LineDataSet(entries, "Glp");
        LineData lineData = new LineData(dataSet);

        glpChart.setData(lineData);
        glpChart.invalidate();
    }

    @SuppressLint("SetTextI18n")
    private void atualizarTextView() {
        if (!glpList.isEmpty()) {
            Double ultimoGlp = glpList.get(glpList.size() - 1);
            glpTextView.setText("Nível de GLP Atual: " + ultimoGlp + "ppm");
        } else {
            glpTextView.setText("Sem Dados de GLP");
        }
    }

    private void verificarGlp(Double glp) {
        if (glp > EstadoGlp.ALERTA.getLimite() && glp < EstadoGlp.ALTA.getLimite()) {
            enviarNotificacao("Alerta! GLP", "O Nível de GLP está aumentando! " + glp + "ppm");
            adicionarAoHistorico(glp);
        } else if (glp > EstadoGlp.ALTA.getLimite()) {
            enviarNotificacao("Alerta! GLP Alto", "O Nível de GLP está alto! " + glp + "ppm");
            adicionarAoHistorico(glp);
        }
    }

    private void adicionarAoHistorico(Double valor) {
        DatabaseReference historicoRef = FirebaseDatabase.getInstance().getReference().child("Historico");

        Date dataAtual = new Date();

        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

        String dataHora = dateFormat.format(dataAtual);

        HistoricoItem historicoItem = new HistoricoItem(dataHora, valor, "GLP");

        historicoRef.push().setValue(historicoItem);
    }

    private void enviarNotificacao(String titulo, String mensagem) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        criarCanalNotificacao(notificationManager);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "canal_id")
                .setSmallIcon(R.drawable.glp)
                .setContentTitle(titulo)
                .setContentText(mensagem)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify(3, builder.build());
    }

    private void criarCanalNotificacao(NotificationManager notificationManager) {
        String channelId = "canal_id";
        CharSequence channelName = "Canal de Notificações";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        notificationManager.createNotificationChannel(channel);
    }

    private void verificarLimiteLista() {
        if (glpList.size() >= 50) {
            limparLista();
        }
    }

    @SuppressLint("SetTextI18n")
    private void limparLista() {
        glpList.clear();
        glpChart.clear();
        glpTextView.setText("Sem Dados de GLP");
        DatabaseReference glpListaRef = FirebaseDatabase.getInstance().getReference().child("GlpLista");
        glpListaRef.removeValue();
    }

    private void salvarListaNoFirebase() {
        DatabaseReference glpListaRef = FirebaseDatabase.getInstance().getReference().child("GlpLista");
        glpListaRef.setValue(glpList);

        if (glpList.size() >= 100) {
            glpListaRef.removeValue();
            limparLista();
        }
    }

    private void carregarDadosDoFirebase() {
        DatabaseReference glpListaRef = FirebaseDatabase.getInstance().getReference().child("GlpLista");

        glpListaRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                glpList.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Double glp = snapshot.getValue(Double.class);
                    if (glp != null) {
                        glpList.add(glp);
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
