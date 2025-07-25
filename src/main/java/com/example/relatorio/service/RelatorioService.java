package com.example.relatorio.service;

import net.sf.jasperreports.engine.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.relatorio.DTO.ParametroRelatorioDTO;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.sf.jasperreports.engine.design.JasperDesign;

import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

/* import com.example.relatorio.DTO.ConexaoBancoDTO;
import com.example.relatorio.config.DataSourceConfig;
import com.example.relatorio.util.CriptoUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput; */

@Service
public class RelatorioService {

    @Value("${relatorios.path}")
    private String basePath;

    @Autowired
    private DataSource dataSource;

    public List<ParametroRelatorioDTO> getParametros(String nomeRelatorio, String codrelatorio) {
        List<ParametroRelatorioDTO> parametros = new ArrayList<>();

        String caminhoRelatorio = basePath + File.separator + codrelatorio + File.separator + nomeRelatorio;

        // Verifica extensão do arquivo
        if (caminhoRelatorio.endsWith(".jrxml")) {
            try (InputStream input = new FileInputStream(caminhoRelatorio)) {

                JasperDesign jasperDesign = JRXmlLoader.load(input);

                for (JRParameter param : jasperDesign.getParameters()) {
                    if (!param.isSystemDefined()) {
                        ParametroRelatorioDTO dto = new ParametroRelatorioDTO();

                        dto.setNome(param.getName());
                        dto.setDescricao(param.getDescription() != null ? param.getDescription() : param.getName());
                        dto.setTipo(param.getValueClass().getSimpleName());
                        dto.setObrigatorio(param.getDefaultValueExpression() != null ? false : true);
                        dto.setExibir(param.isForPrompting() == true ? "true" : "false");
                        parametros.add(dto);
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException(
                        "Erro ao ler parâmetros do relatório .jrxml '" + nomeRelatorio + "': " + e.getMessage(), e);
            }

        } else if (caminhoRelatorio.endsWith(".jasper")) {
            // Normalmente arquivos .jasper já estão compilados e não têm os parâmetros
            // acessíveis facilmente.
            // Mas você pode tentar carregar e obter os parâmetros compilados:
            try {
                JasperReport report = (JasperReport) JRLoader.loadObject(new File(caminhoRelatorio));

                for (JRParameter param : report.getParameters()) {
                    if (!param.isSystemDefined()) {
                        ParametroRelatorioDTO dto = new ParametroRelatorioDTO();
                        dto.setNome(param.getName());
                        dto.setDescricao(param.getDescription() != null ? param.getDescription() : param.getName());
                        dto.setTipo(param.getValueClass().getSimpleName());
                        dto.setObrigatorio(param.getDefaultValueExpression() != null ? false : true);
                        dto.setExibir(param.isForPrompting() == true ? "true" : "false");
                        parametros.add(dto);
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException(
                        "Erro ao ler parâmetros do relatório .jasper '" + nomeRelatorio + "': " + e.getMessage(), e);
            }

        } else {
            throw new IllegalArgumentException("Extensão de relatório não suportada: " + nomeRelatorio);
        }

        return parametros;
    }

    public byte[] gerarRelatorioAntigo(String nomeRelatorio, Map<String, Object> parametros, String formato,
            String codrelatorio) {

        System.out.println("Nome do relatório: " + nomeRelatorio);
        System.out.println("Parâmetros: " + parametros);

        try (Connection conn = dataSource.getConnection()) {
            // Caminho da pasta do relatório
            File pastaRelatorio = new File(basePath + File.separator + codrelatorio);
            if (!pastaRelatorio.exists()) {
                throw new RuntimeException("Pasta do relatório não encontrada: " + pastaRelatorio.getAbsolutePath());
            }

            // Compila todos os .jrxml para .jasper
            compilarRelatoriosDaPasta(pastaRelatorio);

            // Usa o .jasper do relatório principal
            String nomeJasper = nomeRelatorio.replace(".jrxml", ".jasper");
            File arquivoJasper = new File(pastaRelatorio, nomeJasper);
            if (!arquivoJasper.exists()) {
                throw new RuntimeException("Arquivo .jasper não encontrado: " + arquivoJasper.getAbsolutePath());
            }

            JasperReport jasperReport;
            try (InputStream input = new FileInputStream(arquivoJasper)) {
                jasperReport = (JasperReport) JRLoader.loadObject(input);
            }

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, conn);
            return JasperExportManager.exportReportToPdf(jasperPrint);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar o relatório '" + nomeRelatorio + "': " + e.getMessage(), e);
        }
    }

    private void compilarRelatoriosDaPasta(File pastaRelatorio) {
        File[] arquivos = pastaRelatorio.listFiles((dir, name) -> name.endsWith(".jrxml"));
        if (arquivos != null) {
            for (File jrxml : arquivos) {
                try {
                    String pathJasper = jrxml.getAbsolutePath().replace(".jrxml", ".jasper");
                    JasperCompileManager.compileReportToFile(jrxml.getAbsolutePath(), pathJasper);
                    System.out.println("Compilado: " + jrxml.getName());
                } catch (Exception e) {
                    System.err.println("Erro ao compilar: " + jrxml.getName());
                    e.printStackTrace();
                }
            }
        }
    }

    /*
     * @Autowired
     * private DataSourceConfig dataSourceConfig;
     * 
     * public byte[] gerarRelatorio(String nomeRelatorio, Map<String, Object>
     * parametros, String formato,
     * String dadoscript) {
     * try {
     * // Validando
     * if (dadoscript == null || dadoscript.isEmpty()) {
     * throw new
     * IllegalArgumentException("Texto criptografado (dadoscript) não pode ser nulo ou vazio"
     * );
     * }
     * 
     * // 1. Descriptografa
     * String chaveSecreta = "1234567890abcdef";
     * String json = CriptoUtil.decryptAES(dadoscript, chaveSecreta);
     * 
     * ObjectMapper mapper = new ObjectMapper();
     * ConexaoBancoDTO dados = mapper.readValue(json, ConexaoBancoDTO.class);
     * 
     * System.out.println(dados.getBanco());
     * 
     * DataSource ds = dataSourceConfig.createDataSource(
     * dados.getBanco(),
     * dados.getEndereco(),
     * dados.getUsuario(),
     * dados.getSenha());
     * 
     * try (Connection conn = ds.getConnection()) {
     * InputStream input = getClass().getResourceAsStream("/relatorios/" +
     * nomeRelatorio + ".jrxml");
     * 
     * if (input == null)
     * throw new RuntimeException("Relatório não encontrado: " + nomeRelatorio);
     * 
     * JasperReport report = JasperCompileManager.compileReport(input);
     * JasperPrint print = JasperFillManager.fillReport(report, parametros, conn);
     * 
     * if ("PDF".equalsIgnoreCase(formato)) {
     * return JasperExportManager.exportReportToPdf(print);
     * } else if ("XLS".equalsIgnoreCase(formato) ||
     * "XLSX".equalsIgnoreCase(formato)) {
     * ByteArrayOutputStream xlsStream = new ByteArrayOutputStream();
     * JRXlsxExporter exporter = new JRXlsxExporter();
     * exporter.setExporterInput(new SimpleExporterInput(print));
     * exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(xlsStream));
     * exporter.exportReport();
     * return xlsStream.toByteArray();
     * } else {
     * throw new IllegalArgumentException("Formato não suportado: " + formato);
     * }
     * }
     * } catch (Exception e) {
     * throw new RuntimeException("Erro ao gerar relatório: " + e.getMessage(), e);
     * }
     * }
     */

}
