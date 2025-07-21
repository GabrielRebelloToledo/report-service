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
import java.util.HashMap;
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
            String caminhoRelatorio = basePath + File.separator + codrelatorio + File.separator + nomeRelatorio;
            String caminhoSubRelatorios = basePath + File.separator + codrelatorio + File.separator;

            File arquivoRelatorio = new File(caminhoRelatorio);

            if (!arquivoRelatorio.exists()) {
                throw new RuntimeException("Arquivo do relatório não encontrado: " + nomeRelatorio);
            }

            prepararSubrelatorios(caminhoRelatorio, caminhoSubRelatorios); // só depois de verificar

            if (parametros == null) {
                parametros = new HashMap<>();
            }
            parametros.put("SUBREPORT_DIR", caminhoSubRelatorios);

            JasperReport jasperReport;

            if (nomeRelatorio.endsWith(".jrxml")) {
                try (InputStream input = new FileInputStream(arquivoRelatorio)) {
                    jasperReport = JasperCompileManager.compileReport(input);
                }
            } else if (nomeRelatorio.endsWith(".jasper")) {
                try (InputStream input = new FileInputStream(arquivoRelatorio)) {
                    jasperReport = (JasperReport) JRLoader.loadObject(input);
                }
            } else {
                throw new RuntimeException("Extensão de arquivo não suportada: " + nomeRelatorio);
            }

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, conn);
            return JasperExportManager.exportReportToPdf(jasperPrint);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar o relatório '" + nomeRelatorio + "': " + e.getMessage(), e);
        }
    }

    public void prepararSubrelatorios(String caminhoRelatorio, String subreportDir) {
        try {
            // Carrega o design do relatório principal (.jrxml)
            JasperDesign design = JRXmlLoader.load(new File(caminhoRelatorio));

            // Percorre os elementos do relatório para encontrar subreports
            for (JRBand band : new JRBand[] { design.getTitle(), design.getPageHeader(),
                    design.getDetailSection().getBands()[0], design.getPageFooter(), design.getSummary() }) {
                if (band != null) {
                    for (JRElement element : band.getElements()) {
                        if (element instanceof JRSubreport) {
                            JRSubreport subreport = (JRSubreport) element;
                            JRExpression expression = subreport.getExpression();

                            if (expression != null && expression.getText().endsWith(".jasper")) {
                                String nomeSub = expression.getText().replace("\"", ""); // remove aspas
                                File arquivoJasper = new File(subreportDir, nomeSub);
                                File arquivoJrxml = new File(subreportDir, nomeSub.replace(".jasper", ".jrxml"));

                                if (!arquivoJasper.exists() && arquivoJrxml.exists()) {
                                    // Compila o .jrxml para .jasper
                                    JasperCompileManager.compileReportToFile(arquivoJrxml.getAbsolutePath(),
                                            arquivoJasper.getAbsolutePath());
                                    System.out.println("Subrelatório compilado: " + nomeSub);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao preparar subrelatórios: " + e.getMessage(), e);
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
