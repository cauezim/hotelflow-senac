package br.senac.hotelflow.dao;

import br.senac.hotelflow.config.ConnectionFactory;
import br.senac.hotelflow.exception.PersistenciaException;
import br.senac.hotelflow.model.CupomDesconto;
import br.senac.hotelflow.model.Fatura;
import br.senac.hotelflow.model.Pagamento;
import br.senac.hotelflow.model.Reserva;
import br.senac.hotelflow.model.enums.StatusFatura;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;

/**
 * DAO JDBC responsável pelo acesso à tabela {@code fatura}, garantindo persistência dos
 * valores históricos congelados ({@code D06}) e bloqueio transacional em pagamentos simultâneos.
 *
 * <p>Rastreabilidade: {@code [Atende RN13..RN18, D06]}
 */
public class FaturaDAO {

    private final ConnectionFactory connectionFactory;
    private final PagamentoDAO pagamentoDAO;

    public FaturaDAO() {
        this(ConnectionFactory.getInstance());
    }

    public FaturaDAO(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        this.pagamentoDAO = new PagamentoDAO(connectionFactory);
    }

    public Fatura salvar(Fatura fatura) {
        try (Connection conn = connectionFactory.getConnection()) {
            return salvar(conn, fatura);
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao emitir fatura da reserva ID "
                    + fatura.getReserva().getIdReserva() + ": " + e.getMessage(), e);
        }
    }

    public Fatura salvar(Connection conn, Fatura fatura) {
        String sql = """
                INSERT INTO fatura (
                    id_reserva, id_cupom, data_emissao, quantidade_diarias,
                    tarifa_diaria_aplicada, valor_bruto, percentual_desconto,
                    valor_desconto, status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, fatura.getReserva().getIdReserva());
            if (fatura.getCupom() != null && fatura.getCupom().getIdCupom() != null) {
                ps.setInt(2, fatura.getCupom().getIdCupom());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setTimestamp(3, Timestamp.valueOf(fatura.getDataEmissao()));
            ps.setLong(4, fatura.getQuantidadeDiarias());
            ps.setBigDecimal(5, fatura.getTarifaDiariaAplicada());
            ps.setBigDecimal(6, fatura.calcularValorBruto());
            ps.setBigDecimal(7, fatura.getPercentualDesconto());
            ps.setBigDecimal(8, fatura.getValorDesconto());
            ps.setString(9, fatura.getStatus().name());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    fatura.setIdFatura(rs.getInt(1));
                }
            }
            return fatura;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao persistir fatura no banco: " + e.getMessage(), e);
        }
    }

    public void atualizar(Fatura fatura) {
        try (Connection conn = connectionFactory.getConnection()) {
            atualizar(conn, fatura);
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao atualizar fatura ID " + fatura.getIdFatura() + ": " + e.getMessage(), e);
        }
    }

    public void atualizar(Connection conn, Fatura fatura) {
        String sql = """
                UPDATE fatura
                SET id_cupom = ?, quantidade_diarias = ?, valor_bruto = ?,
                    percentual_desconto = ?, valor_desconto = ?, status = ?
                WHERE id_fatura = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (fatura.getCupom() != null && fatura.getCupom().getIdCupom() != null) {
                ps.setInt(1, fatura.getCupom().getIdCupom());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setLong(2, fatura.getQuantidadeDiarias());
            ps.setBigDecimal(3, fatura.calcularValorBruto());
            ps.setBigDecimal(4, fatura.getPercentualDesconto());
            ps.setBigDecimal(5, fatura.getValorDesconto());
            ps.setString(6, fatura.getStatus().name());
            ps.setInt(7, fatura.getIdFatura());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Erro transacional ao atualizar fatura ID " + fatura.getIdFatura() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Adquire bloqueio exclusivo ({@code SELECT ... FOR UPDATE}) sobre a fatura vinculada à reserva
     * na transação corrente, evitando leitura concorrente de saldo defasado ({@code RN14, RN15, RN16}).
     */
    public void bloquearPorReserva(Connection conn, int idReserva) {
        if (conn == null) {
            return;
        }
        String sql = "SELECT id_fatura FROM fatura WHERE id_reserva = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idReserva);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao bloquear fatura da reserva ID " + idReserva + ": " + e.getMessage(), e);
        }
    }

    public Fatura buscarPorReserva(Reserva reserva) {
        try (Connection conn = connectionFactory.getConnection()) {
            return buscarPorReserva(conn, reserva);
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar fatura da reserva ID " + reserva.getIdReserva() + ": " + e.getMessage(), e);
        }
    }

    public Fatura buscarPorReserva(Connection conn, Reserva reserva) {
        if (reserva == null || reserva.getIdReserva() == null) {
            return null;
        }
        if (reserva.getFatura() != null) {
            return reserva.getFatura();
        }
        String sql = """
                SELECT f.id_fatura, f.data_emissao, f.quantidade_diarias, f.tarifa_diaria_aplicada,
                       f.valor_bruto, f.percentual_desconto, f.valor_desconto, f.status,
                       cp.id_cupom, cp.codigo AS cupom_codigo, cp.percentual AS cupom_perc, cp.ativo AS cupom_ativo
                FROM fatura f
                LEFT JOIN cupom_desconto cp ON cp.id_cupom = f.id_cupom
                WHERE f.id_reserva = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reserva.getIdReserva());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                CupomDesconto cupom = null;
                int idCupom = rs.getInt("id_cupom");
                if (!rs.wasNull()) {
                    cupom = new CupomDesconto(
                            idCupom,
                            rs.getString("cupom_codigo"),
                            rs.getBigDecimal("cupom_perc"),
                            rs.getBoolean("cupom_ativo")
                    );
                }

                Fatura fatura = new Fatura(
                        rs.getInt("id_fatura"),
                        reserva,
                        cupom,
                        rs.getTimestamp("data_emissao").toLocalDateTime(),
                        rs.getLong("quantidade_diarias"),
                        rs.getBigDecimal("tarifa_diaria_aplicada"),
                        rs.getBigDecimal("valor_bruto"),
                        rs.getBigDecimal("percentual_desconto"),
                        rs.getBigDecimal("valor_desconto"),
                        StatusFatura.valueOf(rs.getString("status"))
                );

                for (Pagamento pag : pagamentoDAO.listarPorFatura(conn, fatura.getIdFatura())) {
                    fatura.hidratarPagamento(pag);
                }
                return fatura;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao hidratar fatura da reserva ID " + reserva.getIdReserva() + ": " + e.getMessage(), e);
        }
    }
}
