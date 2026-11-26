package br.senac.hotelflow.dao;

import br.senac.hotelflow.config.ConnectionFactory;
import br.senac.hotelflow.exception.PersistenciaException;
import br.senac.hotelflow.model.ServicoQuarto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO JDBC responsável pelos serviços extras solicitados na tabela {@code servico_quarto}.
 *
 * <p>Rastreabilidade: {@code [Atende RN11, RN13]}
 */
public class ServicoQuartoDAO {

    private final ConnectionFactory connectionFactory;

    public ServicoQuartoDAO() {
        this(ConnectionFactory.getInstance());
    }

    public ServicoQuartoDAO(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public ServicoQuarto salvar(int idReserva, ServicoQuarto servico) {
        String sql = """
                INSERT INTO servico_quarto (id_reserva, data_hora_solicitacao, descricao, valor)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, idReserva);
            ps.setTimestamp(2, Timestamp.valueOf(servico.getDataHoraSolicitacao()));
            ps.setString(3, servico.getDescricao());
            ps.setBigDecimal(4, servico.getValor());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    servico.setIdServico(rs.getInt(1));
                }
            }
            return servico;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao registrar serviço de quarto na reserva ID " + idReserva + ": " + e.getMessage(), e);
        }
    }

    public List<ServicoQuarto> listarPorReserva(int idReserva) {
        try (Connection conn = connectionFactory.getConnection()) {
            return listarPorReserva(conn, idReserva);
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao consultar serviços da reserva ID " + idReserva + ": " + e.getMessage(), e);
        }
    }

    public List<ServicoQuarto> listarPorReserva(Connection conn, int idReserva) {
        String sql = """
                SELECT id_servico, data_hora_solicitacao, descricao, valor
                FROM servico_quarto
                WHERE id_reserva = ?
                ORDER BY data_hora_solicitacao, id_servico
                """;
        List<ServicoQuarto> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idReserva);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new ServicoQuarto(
                            rs.getInt("id_servico"),
                            rs.getTimestamp("data_hora_solicitacao").toLocalDateTime(),
                            rs.getString("descricao"),
                            rs.getBigDecimal("valor")
                    ));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao hidratar serviços da reserva ID " + idReserva + ": " + e.getMessage(), e);
        }
    }
}
