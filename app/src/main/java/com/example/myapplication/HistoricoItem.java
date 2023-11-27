package com.example.myapplication;

public class HistoricoItem {
    private String dataHora;
    private double valor;
    private String tipoLeitura;  // Pode ser "Temperatura" ou "Umidade"

    // Construtor padrão sem argumentos (necessário para Firebase)
    public HistoricoItem() {
        // Construtor vazio necessário para o Firebase
    }

    // Construtor principal
    public HistoricoItem(String dataHora, double valor, String tipoLeitura) {
        this.dataHora = dataHora;
        this.valor = valor;
        this.tipoLeitura = tipoLeitura;
    }

    // Getter para dataHora
    public String getDataHora() {
        return dataHora;
    }

    // Getter para valor
    public double getValor() {
        return valor;
    }

    // Getter para tipoLeitura
    public String getTipoLeitura() {
        return tipoLeitura;
    }
}

