package br.senac.hotelflow.dao;

import br.senac.hotelflow.config.ConnectionFactory;
import br.senac.hotelflow.exception.PersistenciaException;
import br.senac.hotelflow.model.CategoriaQuarto;
import br.senac.hotelflow.model.Quarto;
import br.senac.hotelflow.model.enums.StatusLimpeza;
import br.senac.hotelflow.model.enums.StatusOcupacao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO JDBC responsável pelo acesso à tabela {@code quarto} com {@code JOIN} direto em
 * {@code categoria_quarto} e suporte a transações atômicas de Reserva, Check-in e Check-out.
 *
 * <p>Rastreabilidade: {@code [Atende RN03, RN06, RN09, RN12, RN17, RN20, D01]}
 */
public class QuartoDAO {

    private static final String SELECT_BASE = """
            SELECT q.id_quarto, q.numero, q.andar, q.capacidade_maxima,
                   q.status_ocupacao, q.status_limpeza,
                   c.id_categoria, c.nome AS cat_nome, c.tarifa_diaria
            FROM quarto q
            INNER JOIN categoria_quarto c ON c.id_categoria = q.id_categoria
            """;

    private final ConnectionFactory connectionFactory;

    public QuartoDAO() {
        this(ConnectionFactory.getInstance());
    }

    public QuartoDAO(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public Quarto salvar(Quarto quarto) {
        String sql = """
                INSERT INTO quarto (id_categoria, numero, andar, capacidade_maxima, status_ocupacao, status_limpeza)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, quarto.getCategoria().getIdCategoria());
            ps.setString(2, quarto.getNumero());
            ps.setInt(3, quarto.getAndar());
            ps.setInt(4, quarto.getCapacidadeMaxima());
            ps.setString(5, quarto.getStatusOcupacao().name());
            ps.setString(6, quarto.getStatusLimpeza().name());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    quarto.setIdQuarto(rs.getInt(1));
                }
            }
            return quarto;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao cadastrar quarto '" + quarto.getNumero() + "': " + e.getMessage(), e);
        }
    }

    public void atualizar(Quarto quarto) {
        try (Connection conn = connectionFactory.getConnection()) {
            atualizar(conn, quarto);
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao atualizar quarto '" + quarto.getNumero() + "': " + e.getMessage(), e);
        }
    }

    /**
     * Adquire bloqueio pessimista exclusivo ({@code SELECT ... FOR UPDATE}) sobre a linha do quarto
     * na transação atual, serializando confirmações de reserva simultâneas ({@code RN06}) e
     * operações concorrentes de Check-in/Check-out ({@code RN09, RN17}).
     *
     * <p>Rastreabilidade: {@code [Atende RN06, RN09, RN17]}
     */
    public void bloquearPorId(Connection conn, int idQuarto) {
        if (conn == null) {
            return;
        }
        String sql = "SELECT id_quarto FROM quarto WHERE id_quarto = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idQuarto);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao bloquear registro do quarto ID " + idQuarto + ": " + e.getMessage(), e);
        }
    }

    public void atualizar(Connection conn, Quarto quarto) {
        String sql = """
                UPDATE quarto
                SET id_categoria = ?, numero = ?, andar = ?, capacidade_maxima = ?,
                    status_ocupacao = ?, status_limpeza = ?
                WHERE id_quarto = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quarto.getCategoria().getIdCategoria());
            ps.setString(2, quarto.getNumero());
            ps.setInt(3, quarto.getAndar());
            ps.setInt(4, quarto.getCapacidadeMaxima());
            ps.setString(5, quarto.getStatusOcupacao().name());
            ps.setString(6, quarto.getStatusLimpeza().name());
            ps.setInt(7, quarto.getIdQuarto());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Erro transacional ao atualizar quarto ID " + quarto.getIdQuarto() + ": " + e.getMessage(), e);
        }
    }

    public Quarto buscarPorId(Integer id) {
        String sql = SELECT_BASE + " WHERE q.id_quarto = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar quarto ID " + id + ": " + e.getMessage(), e);
        }
    }

    public Quarto buscarPorNumero(String numero) {
        if (numero == null || numero.trim().isEmpty()) {
            return null;
        }
        String sql = SELECT_BASE + " WHERE q.numero = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, numero.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar quarto nº '" + numero + "': " + e.getMessage(), e);
        }
    }

    public List<Quarto> listarTodos() {
        String sql = SELECT_BASE + " ORDER BY q.andar, q.numero";
        List<Quarto> lista = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao listar quartos: " + e.getMessage(), e);
        }
    }

    public List<Quarto> listarPorStatusLimpeza(StatusLimpeza statusLimpeza) {
        String sql = SELECT_BASE + " WHERE q.status_limpeza = ? ORDER BY q.andar, q.numero";
        List<Quarto> lista = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, statusLimpeza.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao filtrar quartos por limpeza: " + e.getMessage(), e);
        }
    }

    static Quarto mapear(ResultSet rs) throws SQLException {
        CategoriaQuarto categoria = new CategoriaQuarto(
                rs.getInt("id_categoria"),
                rs.getString("cat_nome"),
                rs.getBigDecimal("tarifa_diaria")
        );
        return new Quarto(
                rs.getInt("id_quarto"),
                rs.getString("numero"),
                rs.getInt("andar"),
                rs.getInt("capacidade_maxima"),
                categoria,
                StatusOcupacao.valueOf(rs.getString("status_ocupacao")),
                StatusLimpeza.valueOf(rs.getString("status_limpeza"))
        );
    }
}
