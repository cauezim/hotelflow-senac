package br.senac.hotelflow.dao;

import br.senac.hotelflow.config.ConnectionFactory;
import br.senac.hotelflow.exception.PersistenciaException;
import br.senac.hotelflow.model.ConsumoFrigobar;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO JDBC responsável pelos itens de frigobar na tabela {@code consumo_frigobar}.
 *
 * <p>Rastreabilidade: {@code [Atende RN10, RN13]}
 */
public class ConsumoFrigobarDAO {

    private final ConnectionFactory connectionFactory;

    public ConsumoFrigobarDAO() {
        this(ConnectionFactory.getInstance());
    }

    public ConsumoFrigobarDAO(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public ConsumoFrigobar salvar(int idReserva, ConsumoFrigobar consumo) {
        String sql = """
                INSERT INTO consumo_frigobar (id_reserva, produto, quantidade, valor_unitario, data_hora_registro)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, idReserva);
            ps.setString(2, consumo.getProduto());
            ps.setInt(3, consumo.getQuantidade());
            ps.setBigDecimal(4, consumo.getValorUnitario());
            ps.setTimestamp(5, Timestamp.valueOf(consumo.getDataHoraRegistro()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    consumo.setIdConsumo(rs.getInt(1));
                }
            }
            return consumo;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao registrar consumo de frigobar na reserva ID " + idReserva + ": " + e.getMessage(), e);
        }
    }

    public List<ConsumoFrigobar> listarPorReserva(int idReserva) {
        try (Connection conn = connectionFactory.getConnection()) {
            return listarPorReserva(conn, idReserva);
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao consultar consumos da reserva ID " + idReserva + ": " + e.getMessage(), e);
        }
    }

    public List<ConsumoFrigobar> listarPorReserva(Connection conn, int idReserva) {
        String sql = """
                SELECT id_consumo, produto, quantidade, valor_unitario, data_hora_registro
                FROM consumo_frigobar
                WHERE id_reserva = ?
                ORDER BY data_hora_registro, id_consumo
                """;
        List<ConsumoFrigobar> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idReserva);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new ConsumoFrigobar(
                            rs.getInt("id_consumo"),
                            rs.getString("produto"),
                            rs.getInt("quantidade"),
                            rs.getBigDecimal("valor_unitario"),
                            rs.getTimestamp("data_hora_registro").toLocalDateTime()
                    ));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao hidratar consumos da reserva ID " + idReserva + ": " + e.getMessage(), e);
        }
    }
}
