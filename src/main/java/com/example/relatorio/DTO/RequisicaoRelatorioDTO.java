package com.example.relatorio.DTO;

import java.util.Map;

public class RequisicaoRelatorioDTO {
    private String relatorio;
    private Map<String, Object> parametros;
    private String formato;
    private String dados;
    private String codRelatorio;

    public String getRelatorio() {
        return relatorio;
    }

    public void setRelatorio(String relatorio) {
        this.relatorio = relatorio;
    }

    public Map<String, Object> getParametros() {
        return parametros;
    }

    public void setParametros(Map<String, Object> parametros) {
        this.parametros = parametros;
    }

    public String getFormato() {
        return formato;
    }

    public void setFormato(String formato) {
        this.formato = formato;
    }

    public String getDados() {
        return dados;
    }

    public void setDados(String dados) {
        this.dados = dados;
    }

    public String getCodRelatorio() {
        return codRelatorio;
    }

    public void setCodRelatorio(String codrelatorio) {
        this.codRelatorio = codrelatorio;
    }
}
