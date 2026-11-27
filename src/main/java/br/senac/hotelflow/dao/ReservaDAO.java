package br.senac.hotelflow.dao;

import br.senac.hotelflow.config.ConnectionFactory;
import br.senac.hotelflow.exception.PersistenciaException;
import br.senac.hotelflow.model.ConsumoFrigobar;
import br.senac.hotelflow.model.Hospede;
import br.senac.hotelflow.model.Quarto;
import br.senac.hotelflow.model.Reserva;
import br.senac.hotelflow.model.ServicoQuarto;
import br.senac.hotelflow.model.enums.StatusReserva;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO JDBC responsável pelo agregado {@link Reserva}, incluindo verificação SQL transacional
 * de conflito em intervalo semiaberto ({@code RN06, D02}), histórico de reservas finalizadas
 * ({@code RN19}) e consulta de ocupação efetiva mensal ({@code RN20, D05}).
 *
 * <p>Rastreabilidade: {@code [Atende RN05..RN11, RN13, RN17, RN19, RN20, D01, D02, D05]}
 */
public class ReservaDAO {

    private static final String SELECT_BASE = """
            SELECT r.id_reserva, r.data_checkin_prevista, r.data_checkout_prevista,
                   r.data_hora_checkin, r.data_hora_checkout, r.status,
                   h.id_hospede, h.nome_completo, h.documento, h.telefone, h.email, h.endereco,
                   q.id_quarto, q.numero, q.andar, q.capacidade_maxima, q.status_ocupacao, q.status_limpeza,
                   c.id_categoria, c.nome AS cat_nome, c.tarifa_diaria
            FROM reserva r
            INNER JOIN hospede h          ON h.id_hospede = r.id_hospede
            INNER JOIN quarto q           ON q.id_quarto = r.id_quarto
            INNER JOIN categoria_quarto c ON c.id_categoria = q.id_categoria
            """;

    private final ConnectionFactory connectionFactory;
    private final ConsumoFrigobarDAO consumoDAO;
    private final ServicoQuartoDAO servicoDAO;
    private final FaturaDAO faturaDAO;

    public ReservaDAO() {
        this(ConnectionFactory.getInstance());
    }

    public ReservaDAO(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        this.consumoDAO = new ConsumoFrigobarDAO(connectionFactory);
        this.servicoDAO = new ServicoQuartoDAO(connectionFactory);
        this.faturaDAO = new FaturaDAO(connectionFactory);
    }

    public Reserva salvar(Reserva reserva) {
        String sql = """
                INSERT INTO reserva (
                    id_hospede, id_quarto, data_checkin_prevista, data_checkout_prevista,
                    data_hora_checkin, data_hora_checkout, status
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, reserva.getHospede().getIdHospede());
            ps.setInt(2, reserva.getQuarto().getIdQuarto());
            ps.setDate(3, Date.valueOf(reserva.getDataCheckInPrevista()));
            ps.setDate(4, Date.valueOf(reserva.getDataCheckOutPrevista()));
            definirTimestampOpcional(ps, 5, reserva.getDataHoraCheckIn());
            definirTimestampOpcional(ps, 6, reserva.getDataHoraCheckOut());
            ps.setString(7, reserva.getStatus().name());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    reserva.setIdReserva(rs.getInt(1));
                }
            }
            return reserva;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao criar reserva: " + e.getMessage(), e);
        }
    }

    public void atualizar(Reserva reserva) {
        try (Connection conn = connectionFactory.getConnection()) {
            atualizar(conn, reserva);
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao atualizar reserva ID " + reserva.getIdReserva() + ": " + e.getMessage(), e);
        }
    }

    public void atualizar(Connection conn, Reserva reserva) {
        String sql = """
                UPDATE reserva
                SET data_checkin_prevista = ?, data_checkout_prevista = ?,
                    data_hora_checkin = ?, data_hora_checkout = ?, status = ?
                WHERE id_reserva = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(reserva.getDataCheckInPrevista()));
            ps.setDate(2, Date.valueOf(reserva.getDataCheckOutPrevista()));
            definirTimestampOpcional(ps, 3, reserva.getDataHoraCheckIn());
            definirTimestampOpcional(ps, 4, reserva.getDataHoraCheckOut());
            ps.setString(5, reserva.getStatus().name());
            ps.setInt(6, reserva.getIdReserva());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Erro transacional ao atualizar reserva ID " + reserva.getIdReserva() + ": " + e.getMessage(), e);
        }
    }

    public Reserva buscarPorId(Integer id) {
        try (Connection conn = connectionFactory.getConnection()) {
            return buscarPorId(conn, id);
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar reserva ID " + id + ": " + e.getMessage(), e);
        }
    }

    /**
     * Adquire bloqueio exclusivo ({@code SELECT ... FOR UPDATE}) sobre o registro da reserva na
     * transação ativa e retorna o {@code id_quarto} associado para encadeamento seguro de locks
     * na ordem estrita {@code reserva -> quarto -> fatura}.
     *
     * <p>Rastreabilidade: {@code [Atende RN06, RN09, RN13..RN17]}
     */
    public Integer bloquearObterIdQuarto(Connection conn, int idReserva) {
        if (conn == null) {
            return null;
        }
        String sql = "SELECT id_quarto FROM reserva WHERE id_reserva = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idReserva);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_quarto");
                }
                return null;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao bloquear reserva ID " + idReserva + ": " + e.getMessage(), e);
        }
    }

    public Reserva buscarPorId(Connection conn, Integer id) {
        if (conn == null) {
            return buscarPorId(id);
        }
        String sql = SELECT_BASE + " WHERE r.id_reserva = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearCompleta(conn, rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao carregar reserva ID " + id + ": " + e.getMessage(), e);
        }
    }

    public List<Reserva> listarTodos() {
        String sql = SELECT_BASE + " ORDER BY r.data_checkin_prevista DESC, r.id_reserva DESC";
        List<Reserva> lista = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearCompleta(conn, rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao listar reservas: " + e.getMessage(), e);
        }
    }

    public List<Reserva> listarPorStatus(StatusReserva status) {
        String sql = SELECT_BASE + " WHERE r.status = ? ORDER BY r.data_checkin_prevista, r.id_reserva";
        List<Reserva> lista = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearCompleta(conn, rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao listar reservas por status " + status + ": " + e.getMessage(), e);
        }
    }

    public boolean existeConflitoPeriodo(int idQuarto, LocalDate inicioPrevisto,
                                         LocalDate fimPrevisto, Integer idReservaIgnorada) {
        try (Connection conn = connectionFactory.getConnection()) {
            return existeConflitoPeriodo(conn, idQuarto, inicioPrevisto, fimPrevisto, idReservaIgnorada);
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao verificar conflito de disponibilidade do quarto: " + e.getMessage(), e);
        }
    }

    /**
     * Versão transacional com leitura bloqueante ({@code FOR UPDATE}) que garante leitura atual
     * (sem snapshot defasado) ao validar conflitos simultâneos de reserva para o mesmo quarto.
     *
     * <p>Rastreabilidade: {@code [Atende RN06, D02]}
     */
    public boolean existeConflitoPeriodo(Connection conn, int idQuarto, LocalDate inicioPrevisto,
                                         LocalDate fimPrevisto, Integer idReservaIgnorada) {
        if (conn == null) {
            return existeConflitoPeriodo(idQuarto, inicioPrevisto, fimPrevisto, idReservaIgnorada);
        }
        LocalDate fimEfetivoProposto = fimPrevisto.equals(inicioPrevisto)
                ? inicioPrevisto.plusDays(1)
                : fimPrevisto;

        String sql = """
                SELECT COUNT(*) AS total
                FROM reserva r
                WHERE r.id_quarto = ?
                  AND (? IS NULL OR r.id_reserva <> ?)
                  AND r.status IN ('CONFIRMADA', 'EM_ANDAMENTO')
                  AND r.data_checkin_prevista < ?
                  AND (
                      CASE
                          WHEN r.data_checkout_prevista = r.data_checkin_prevista
                              THEN DATE_ADD(r.data_checkin_prevista, INTERVAL 1 DAY)
                          ELSE r.data_checkout_prevista
                      END
                  ) > ?
                FOR UPDATE
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idQuarto);
            if (idReservaIgnorada != null) {
                ps.setInt(2, idReservaIgnorada);
                ps.setInt(3, idReservaIgnorada);
            } else {
                ps.setNull(2, Types.INTEGER);
                ps.setNull(3, Types.INTEGER);
            }
            ps.setDate(4, Date.valueOf(fimEfetivoProposto));
            ps.setDate(5, Date.valueOf(inicioPrevisto));

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("total") > 0;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro transacional ao verificar conflito de disponibilidade do quarto: " + e.getMessage(), e);
        }
    }

    public List<Reserva> listarFinalizadasPorHospede(int idHospede) {
        String sql = SELECT_BASE
                + " WHERE r.id_hospede = ? AND r.status = 'FINALIZADA' ORDER BY r.data_hora_checkout DESC";
        List<Reserva> lista = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idHospede);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearCompleta(conn, rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao consultar histórico do hóspede ID " + idHospede + ": " + e.getMessage(), e);
        }
    }

    public List<Reserva> listarEstadiasEfetivasNoPeriodo(LocalDate inicioPeriodo, LocalDate fimPeriodoExclusivo) {
        String sql = SELECT_BASE + """
                WHERE r.status IN ('EM_ANDAMENTO', 'FINALIZADA')
                  AND r.data_hora_checkin IS NOT NULL
                  AND DATE(r.data_hora_checkin) < ?
                  AND COALESCE(DATE(r.data_hora_checkout), ?) >= ?
                ORDER BY r.data_hora_checkin
                """;
        List<Reserva> lista = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(fimPeriodoExclusivo));
            ps.setDate(2, Date.valueOf(fimPeriodoExclusivo));
            ps.setDate(3, Date.valueOf(inicioPeriodo));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearCompleta(conn, rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao consultar estadias efetivas para relatório: " + e.getMessage(), e);
        }
    }

    private Reserva mapearCompleta(Connection conn, ResultSet rs) throws SQLException {
        Hospede hospede = HospedeDAO.mapear(rs);
        Quarto quarto = QuartoDAO.mapear(rs);

        Timestamp tsIn = rs.getTimestamp("data_hora_checkin");
        Timestamp tsOut = rs.getTimestamp("data_hora_checkout");

        Reserva reserva = new Reserva(
                rs.getInt("id_reserva"),
                hospede,
                quarto,
                rs.getDate("data_checkin_prevista").toLocalDate(),
                rs.getDate("data_checkout_prevista").toLocalDate(),
                tsIn != null ? tsIn.toLocalDateTime() : null,
                tsOut != null ? tsOut.toLocalDateTime() : null,
                StatusReserva.valueOf(rs.getString("status"))
        );

        for (ConsumoFrigobar consumo : consumoDAO.listarPorReserva(conn, reserva.getIdReserva())) {
            reserva.hidratarConsumo(consumo);
        }
        for (ServicoQuarto servico : servicoDAO.listarPorReserva(conn, reserva.getIdReserva())) {
            reserva.hidratarServico(servico);
        }
        faturaDAO.buscarPorReserva(conn, reserva);

        return reserva;
    }

    private void definirTimestampOpcional(PreparedStatement ps, int indice, LocalDateTime dataHora) throws SQLException {
        if (dataHora != null) {
            ps.setTimestamp(indice, Timestamp.valueOf(dataHora));
        } else {
            ps.setNull(indice, Types.TIMESTAMP);
        }
    }
}
