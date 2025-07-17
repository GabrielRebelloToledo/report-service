package com.example.relatorio.config;

import javax.sql.DataSource;
import org.springframework.stereotype.Component;
import com.zaxxer.hikari.HikariDataSource;

@Component
public class DataSourceConfig {

    public DataSource createDataSource(String banco, String url, String username, String password) {
        HikariDataSource ds = new HikariDataSource();

        String jdbcUrl;

        switch (banco.toLowerCase()) {
            case "mysql":
                jdbcUrl = "jdbc:mysql://" + url + "?useSSL=false&serverTimezone=UTC";
                ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
                break;
            case "postgres":
            case "postgresql":
                jdbcUrl = "jdbc:postgresql://" + url;
                ds.setDriverClassName("org.postgresql.Driver");
                break;
            case "sqlserver":
            case "mssql":
                jdbcUrl = "jdbc:sqlserver://" + url;
                ds.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                break;
            case "oracle":
                jdbcUrl = "jdbc:oracle:thin:@" + url;
                ds.setDriverClassName("oracle.jdbc.OracleDriver");
                break;
            default:
                throw new IllegalArgumentException("Banco de dados não suportado: " + banco);
        }

        ds.setJdbcUrl(jdbcUrl);
        ds.setUsername(username);
        ds.setPassword(password);

        return ds;
    }
}