package br.senac.hotelflow.config;

import br.senac.hotelflow.exception.PersistenciaException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Fábrica centralizada de conexões JDBC para o MySQL 8 (padrão Singleton simples).
 *
 * <p>Justificativa Didática (Etapa E9):
 * <ul>
 *   <li>Carrega uma única vez as configurações do arquivo {@code db.properties};</li>
 *   <li>Fornece conexões simples ({@code autoCommit = true}) para consultas/cadastros e
 *       conexões transacionais ({@code autoCommit = false}) com bloqueio de linha para evitar
 *       conflitos simultâneos em reservas ({@code RN06}) e pagamentos ({@code RN15..RN17}).</li>
 * </ul>
 *
 * <p>Rastreabilidade: {@code [Atende RN01..RN20]}
 */
public final class ConnectionFactory {

    private static final String ARQUIVO_LOCAL = "/db.properties";
    private static final String ARQUIVO_EXEMPLO = "/db.properties.example";

    private static ConnectionFactory instancia;

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
     * Retorna a instância única do gerenciador de conexões.
     */
    public static synchronized ConnectionFactory getInstance() {
        if (instancia == null) {
            instancia = new ConnectionFactory();
        }
        return instancia;
    }

    /**
     * Abre uma nova conexão ativa com o banco MySQL 8 ({@code autoCommit = true}).
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
     * Abre uma conexão transacional ({@code autoCommit = false}) com isolamento {@code READ_COMMITTED}
     * para uso conjunto com {@code SELECT ... FOR UPDATE}.
     */
    public Connection getTransactionalConnection() {
        Connection conn = getConnection();
        try {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn.setAutoCommit(false);
            return conn;
        } catch (SQLException e) {
            fecharSilenciosamente(conn);
            throw new PersistenciaException("Não foi possível iniciar transação JDBC: " + e.getMessage(), e);
        }
    }

    public static void commit(Connection conn) {
        if (conn != null) {
            try {
                conn.commit();
            } catch (SQLException e) {
                throw new PersistenciaException("Erro ao confirmar transação (commit): " + e.getMessage(), e);
            }
        }
    }

    public static void rollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
                // Preserva a exceção principal que motivou o rollback
            }
        }
    }

    public static void fecharSilenciosamente(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {
                // Fechamento seguro da conexão
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
            // Usa valores padrão locais
        }
        return props;
    }

    private String resolverConfiguracao(String envKey, Properties props, String propKey, String padrao) {
        String valorEnv = System.getenv(envKey);
        if (valorEnv != null && !valorEnv.trim().isEmpty()) {
            return valorEnv.trim();
        }
        String valorProp = props.getProperty(propKey);
        if (valorProp != null && !valorProp.trim().isEmpty()) {
            return valorProp.trim();
        }
        return padrao;
    }
}
