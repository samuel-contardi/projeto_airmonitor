package com.example.myapplication;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HistoricoActivity extends AppCompatActivity {

    private HistoricoAdapter historicoAdapter;
    private List<HistoricoItem> historicoItemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historico);

        // Inicialize a RecyclerView e o adaptador
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        historicoItemList = new ArrayList<>();
        historicoAdapter = new HistoricoAdapter(historicoItemList);

        // Configure o layout da RecyclerView (pode ser um LinearLayoutManager, GridLayoutManager, etc.)
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Defina o adaptador na RecyclerView
        recyclerView.setAdapter(historicoAdapter);

        // Carregue os dados do histórico do Firebase
        carregarHistoricoDoFirebase();
    }

    private void carregarHistoricoDoFirebase() {
        // Substitua o caminho abaixo com a referência correta ao nó "Historico" no seu Firebase
        DatabaseReference historicoRef = FirebaseDatabase.getInstance().getReference().child("Historico");

        historicoRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                historicoItemList.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    // Converte o snapshot para o tipo HistoricoItem
                    HistoricoItem historicoItem = snapshot.getValue(HistoricoItem.class);

                    if (historicoItem != null) {
                        historicoItemList.add(historicoItem);
                    }
                }

                historicoAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Trate erros, se necessário
            }
        });
    }
}
