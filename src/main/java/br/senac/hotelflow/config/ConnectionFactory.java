package br.senac.hotelflow.config;

import br.senac.hotelflow.exception.PersistenciaException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Gerenciador Singleton de configuração e fábrica de conexões JDBC para MySQL 8.
 *
 * <p>Justificativa Arquitetural (Etapa E9):
 * <ul>
 *   <li><b>Singleton na configuração:</b> evita releitura repetitiva de arquivos {@code .properties}
 *       a cada operação de banco, centralizando URL, credenciais e parâmetros de timezone/charset;</li>
 *   <li><b>Conexão por unidade de trabalho (Unit of Work):</b> fornece conexões limpas para operações
 *       simples e suporte explícito a transações ACID ({@code setAutoCommit(false)}) para fluxos
 *       multi-tabela críticos como Check-in ({@code RN09}) e Check-out ({@code RN16, RN17}).</li>
 * </ul>
 *
 * <p>Rastreabilidade: {@code [Atende RN01..RN20]}
 */
public final class ConnectionFactory {

    private static final String ARQUIVO_LOCAL = "/db.properties";
    private static final String ARQUIVO_EXEMPLO = "/db.properties.example";

    private static volatile ConnectionFactory instancia;

    private final String url;
    private final String usuario;
    private final String senha;

    private ConnectionFactory() {
        Properties props = carregarPropriedades();
        this.url = resolverConfiguracao("HOTELFLOW_DB_URL", props, "db.url",
                "jdbc:mysql://localhost:3306/hotelflow?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Sao_Paulo&characterEncoding=UTF-8");
        this.usuario = resolverConfiguracao("HOTELFLOW_DB_USER", props, "db.user", "root");
        this.senha = resolverConfiguracao("HOTELFLOW_DB_PASSWORD", props, "db.password", "root");
    }

    /**
     * Obtém a instância única (Double-Checked Locking thread-safe) do configurador JDBC.
     *
     * @return instância única de {@link ConnectionFactory}
     */
    public static ConnectionFactory getInstance() {
        if (instancia == null) {
            synchronized (ConnectionFactory.class) {
                if (instancia == null) {
                    instancia = new ConnectionFactory();
                }
            }
        }
        return instancia;
    }

    /**
     * Abre uma nova conexão ativa com o banco MySQL 8 (com {@code autoCommit = true} por padrão).
     *
     * @return conexão JDBC aberta
     * @throws PersistenciaException caso o servidor MySQL esteja indisponível ou credenciais inválidas
     */
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, usuario, senha);
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Falha ao conectar ao banco MySQL (verifique se o MySQL 8 está ativo e as credenciais em db.properties): "
                    + e.getMessage(), e);
        }
    }

    /**
     * Abre uma conexão com controle transacional manual ({@code autoCommit = false}) para operações
     * atômicas que envolvem múltiplas tabelas (ex.: Check-in RN09 e Encerramento de Check-out RN17).
     *
     * @return conexão JDBC em modo transacional
     */
    public Connection getTransactionalConnection() {
        Connection conn = getConnection();
        try {
            conn.setAutoCommit(false);
            return conn;
        } catch (SQLException e) {
            fecharSilenciosamente(conn);
            throw new PersistenciaException("Não foi possível iniciar transação JDBC: " + e.getMessage(), e);
        }
    }

    /**
     * Efetiva (commit) todas as operações da transação atual.
     *
     * @param conn conexão transacional aberta
     */
    public static void commit(Connection conn) {
        if (conn != null) {
            try {
                conn.commit();
            } catch (SQLException e) {
                throw new PersistenciaException("Erro ao confirmar transação (commit): " + e.getMessage(), e);
            }
        }
    }

    /**
     * Desfaz (rollback) todas as alterações da transação em caso de falha de regra ou persistência.
     *
     * @param conn conexão transacional aberta
     */
    public static void rollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
                // Preserva a exceção original que motivou o rollback
            }
        }
    }

    /**
     * Fecha recursos JDBC ({@code ResultSet}, {@code Statement}, {@code Connection}) sem lançar exceções secundárias.
     *
     * @param recursos recursos {@link AutoCloseable} a encerrar
     */
    public static void fecharSilenciosamente(AutoCloseable... recursos) {
        if (recursos == null) {
            return;
        }
        for (AutoCloseable recurso : recursos) {
            if (recurso != null) {
                try {
                    recurso.close();
                } catch (Exception ignored) {
                    // Fechamento seguro
                }
            }
        }
    }

    public String getUrl() {
        return url;
    }

    private Properties carregarPropriedades() {
        Properties props = new Properties();
        try (InputStream localStream = ConnectionFactory.class.getResourceAsStream(ARQUIVO_LOCAL)) {
            if (localStream != null) {
                props.load(localStream);
                return props;
            }
        } catch (IOException ignored) {
            // Segue para arquivo de exemplo padrão
        }

        try (InputStream exemploStream = ConnectionFactory.class.getResourceAsStream(ARQUIVO_EXEMPLO)) {
            if (exemploStream != null) {
                props.load(exemploStream);
            }
        } catch (IOException ignored) {
            // Usa valores default seguros
        }
        return props;
    }

    private String resolverConfiguracao(String envKey, Properties props, String propKey, String padrao) {
        String valorEnv = System.getenv(envKey);
        if (valorEnv != null && !valorEnv.isBlank()) {
            return valorEnv.trim();
        }
        String valorProp = props.getProperty(propKey);
        if (valorProp != null && !valorProp.isBlank()) {
            return valorProp.trim();
        }
        return padrao;
    }
}
