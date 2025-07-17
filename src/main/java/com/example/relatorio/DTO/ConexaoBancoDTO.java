package com.example.relatorio.DTO;


import lombok.Data;

@Data
public class ConexaoBancoDTO {
    private String banco;  
    private String endereco;
    private String usuario;
    private String senha;
}
