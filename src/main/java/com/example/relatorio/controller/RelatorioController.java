package com.example.relatorio.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.relatorio.DTO.ParametroRelatorioDTO;
import com.example.relatorio.DTO.RequisicaoRelatorioDTO;
import com.example.relatorio.service.RelatorioService;
 

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    @Autowired
    private RelatorioService relatorioService;

    @GetMapping("/teste")
    public String teste() {
        return "Funcionando";
    }

    // 1. Retorna os parâmetros esperados para um relatório
    @GetMapping("/get-parametros/{relatorio:.+}/{codigo}")
    public ResponseEntity<List<ParametroRelatorioDTO>> getParametros(@PathVariable String relatorio, @PathVariable String codigo) {
        List<ParametroRelatorioDTO> parametros = relatorioService.getParametros(relatorio, codigo);
        return ResponseEntity.ok(parametros);
    }

    // 2. Gera o relatório com base nos dados enviados
    @PostMapping("/gera-relatorio")
    public ResponseEntity<byte[]> gerarRelatorio(@RequestBody RequisicaoRelatorioDTO requisicao) throws Exception {

        System.out.println("Relatório recebido: " + requisicao.getRelatorio());
        System.out.println("Dado criptografado: " + requisicao.getDados());

        String relatorio = requisicao.getRelatorio();
        Map<String, Object> parametros = requisicao.getParametros();
        String formato = requisicao.getFormato();
        //String dadoscript = requisicao.getDados();

        /* byte[] arquivo = relatorioService.gerarRelatorioAntigo(relatorio, parametros, formato, dadoscript); */
        byte[] arquivo = relatorioService. gerarRelatorioAntigo(relatorio, parametros, formato);

        String contentType = formato.equalsIgnoreCase("XLS") || formato.equalsIgnoreCase("XLSX")
        ? "application/vnd.ms-excel"
        : "application/pdf";

        String fileName = relatorio + "." + formato.toLowerCase();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + fileName)
                .contentType(MediaType.parseMediaType(contentType))
                .body(arquivo);
    }
}
