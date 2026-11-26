package br.senac.hotelflow.dao;

import br.senac.hotelflow.config.ConnectionFactory;
import br.senac.hotelflow.exception.PersistenciaException;
import br.senac.hotelflow.model.Pagamento;
import br.senac.hotelflow.model.enums.FormaPagamento;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO JDBC responsável pelas parcelas financeiras na tabela {@code pagamento}, com suporte
 * a conexões transacionais compartilhadas.
 *
 * <p>Rastreabilidade: {@code [Atende RN15, RN16]}
 */
public class PagamentoDAO {

    private final ConnectionFactory connectionFactory;

    public PagamentoDAO() {
        this(ConnectionFactory.getInstance());
    }

    public PagamentoDAO(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public Pagamento salvar(int idFatura, Pagamento pagamento) {
        try (Connection conn = connectionFactory.getConnection()) {
            return salvar(conn, idFatura, pagamento);
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao salvar pagamento da fatura ID " + idFatura + ": " + e.getMessage(), e);
        }
    }

    public Pagamento salvar(Connection conn, int idFatura, Pagamento pagamento) {
        String sql = """
                INSERT INTO pagamento (id_fatura, valor, data_pagamento, forma_pagamento)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idFatura);
            ps.setBigDecimal(2, pagamento.getValor());
            ps.setTimestamp(3, Timestamp.valueOf(pagamento.getDataPagamento()));
            ps.setString(4, pagamento.getFormaPagamento().name());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    pagamento.setIdPagamento(rs.getInt(1));
                }
            }
            return pagamento;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro transacional ao gravar pagamento na fatura ID " + idFatura + ": " + e.getMessage(), e);
        }
    }

    public List<Pagamento> listarPorFatura(int idFatura) {
        try (Connection conn = connectionFactory.getConnection()) {
            return listarPorFatura(conn, idFatura);
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao listar pagamentos da fatura ID " + idFatura + ": " + e.getMessage(), e);
        }
    }

    public List<Pagamento> listarPorFatura(Connection conn, int idFatura) {
        String sql = """
                SELECT id_pagamento, valor, data_pagamento, forma_pagamento
                FROM pagamento
                WHERE id_fatura = ?
                ORDER BY data_pagamento, id_pagamento
                """;
        List<Pagamento> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idFatura);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Pagamento(
                            rs.getInt("id_pagamento"),
                            rs.getBigDecimal("valor"),
                            rs.getTimestamp("data_pagamento").toLocalDateTime(),
                            FormaPagamento.valueOf(rs.getString("forma_pagamento"))
                    ));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao hidratar pagamentos da fatura ID " + idFatura + ": " + e.getMessage(), e);
        }
    }
}
