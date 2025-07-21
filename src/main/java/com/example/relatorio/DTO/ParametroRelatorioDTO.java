package com.example.relatorio.DTO;

import lombok.Data;

@Data
public class ParametroRelatorioDTO {

    private String nome;
    private String descricao;
    private String tipo;
    private boolean obrigatorio;
    private String exibir;
}
