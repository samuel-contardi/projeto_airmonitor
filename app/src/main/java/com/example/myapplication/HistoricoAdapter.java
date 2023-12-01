package com.example.myapplication;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


public class HistoricoAdapter extends RecyclerView.Adapter<HistoricoAdapter.HistoricoViewHolder> {

    private final List<HistoricoItem> historicoItemList;

    public HistoricoAdapter(List<HistoricoItem> historicoItemList) {
        this.historicoItemList = historicoItemList;
    }

    @NonNull
    @Override
    public HistoricoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_historico, parent, false);
        return new HistoricoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoricoViewHolder holder, int position) {
        HistoricoItem historicoItem = historicoItemList.get(position);
        holder.bind(historicoItem);
    }

    @Override
    public int getItemCount() {
        return historicoItemList.size();
    }

    static class HistoricoViewHolder extends RecyclerView.ViewHolder {
        private final TextView dataHoraTextView;
        private final TextView categoriaTextView;

        public HistoricoViewHolder(@NonNull View itemView) {
            super(itemView);
            dataHoraTextView = itemView.findViewById(R.id.dataHoraTextView);
            categoriaTextView = itemView.findViewById(R.id.categoriaTextView);
        }

        @SuppressLint("SetTextI18n")
        public void bind(HistoricoItem historicoItem) {
            dataHoraTextView.setText("Data/Hora: " + historicoItem.getDataHora());

            // Verifica o tipo de leitura
            if ("Temperatura".equals(historicoItem.getTipoLeitura())) {
                categoriaTextView.setText("Temperatura: " + historicoItem.getValor() + "°C");
            } else if ("Umidade".equals(historicoItem.getTipoLeitura())) {
                categoriaTextView.setText("Umidade: " + historicoItem.getValor() + "%");
            } else if ("GLP".equals(historicoItem.getTipoLeitura())) {
                categoriaTextView.setText("GLP: " + historicoItem.getValor() + "ppm");
            } else if ("C02".equals(historicoItem.getTipoLeitura())) {
                categoriaTextView.setText("C02: " + historicoItem.getValor() + "%");
            }
        }
    }
}