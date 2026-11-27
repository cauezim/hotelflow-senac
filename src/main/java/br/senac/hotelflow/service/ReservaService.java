package br.senac.hotelflow.service;

import br.senac.hotelflow.config.ConnectionFactory;
import br.senac.hotelflow.dao.HospedeDAO;
import br.senac.hotelflow.dao.QuartoDAO;
import br.senac.hotelflow.dao.ReservaDAO;
import br.senac.hotelflow.exception.EntidadeNaoEncontradaException;
import br.senac.hotelflow.exception.RegraNegocioException;
import br.senac.hotelflow.model.Hospede;
import br.senac.hotelflow.model.Quarto;
import br.senac.hotelflow.model.Reserva;
import br.senac.hotelflow.model.Usuario;
import br.senac.hotelflow.model.enums.StatusReserva;
import br.senac.hotelflow.service.notificacao.EmailService;
import br.senac.hotelflow.service.notificacao.SmtpEmailService;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Serviço responsável pelo ciclo de agendamento, validação transacional de conflitos ({@code RN06, D02}),
 * disparo automático de e-mail de confirmação ({@code RN08}), Check-in atômico ({@code RN09, D01})
 * e consulta de histórico de reservas finalizadas ({@code RN19}).
 *
 * <p>Rastreabilidade: {@code [Atende RN05, RN06, RN07, RN08, RN09, RN19, D01, D02, D05]}
 */
public class ReservaService {

    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ConnectionFactory connectionFactory;
    private final ReservaDAO reservaDAO;
    private final HospedeDAO hospedeDAO;
    private final QuartoDAO quartoDAO;
    private final EmailService emailService;

    public ReservaService() {
        this(new SmtpEmailService());
    }

    public ReservaService(EmailService emailService) {
        this(ConnectionFactory.getInstance(), new ReservaDAO(), new HospedeDAO(),
                new QuartoDAO(), emailService);
    }

    public ReservaService(ConnectionFactory connectionFactory, ReservaDAO reservaDAO,
                          HospedeDAO hospedeDAO, QuartoDAO quartoDAO, EmailService emailService) {
        this.connectionFactory = connectionFactory;
        this.reservaDAO = reservaDAO;
        this.hospedeDAO = hospedeDAO;
        this.quartoDAO = quartoDAO;
        this.emailService = emailService;
    }

    public Reserva criarReserva(Usuario operador, int idHospede, int idQuarto,
                                LocalDate dataCheckInPrevista, LocalDate dataCheckOutPrevista) {
        AutenticacaoService.exigirPermissao(operador, "CRIAR_RESERVA");

        Hospede hospede = hospedeDAO.buscarPorId(idHospede);
        if (hospede == null) {
            throw new EntidadeNaoEncontradaException("Hóspede não encontrado (ID " + idHospede + ").");
        }
        Quarto quarto = quartoDAO.buscarPorId(idQuarto);
        if (quarto == null) {
            throw new EntidadeNaoEncontradaException("Quarto não encontrado (ID " + idQuarto + ").");
        }

        Reserva novaReserva = new Reserva(hospede, quarto, dataCheckInPrevista, dataCheckOutPrevista);
        return reservaDAO.salvar(novaReserva);
    }

    /**
     * Confirma uma reserva {@code PENDENTE} em transação atômica com bloqueio pessimista no quarto
     * ({@code SELECT ... FOR UPDATE}), impedindo reservas simultâneas conflitantes ({@code RN06, D02})
     * e disparando o e-mail após o {@code commit} ({@code RN08}).
     *
     * <p>Rastreabilidade: {@code [Atende RN06, RN07, RN08, D02]}
     */
    public Reserva confirmarReserva(Usuario operador, int idReserva) {
        AutenticacaoService.exigirPermissao(operador, "CONFIRMAR_RESERVA");

        Connection conn = null;
        Reserva reservaConfirmada;
        try {
            if (connectionFactory != null) {
                conn = connectionFactory.getTransactionalConnection();
            }

            Integer idQuartoBloqueio = reservaDAO.bloquearObterIdQuarto(conn, idReserva);
            if (idQuartoBloqueio != null) {
                quartoDAO.bloquearPorId(conn, idQuartoBloqueio);
            }

            Reserva reserva = reservaDAO.buscarPorId(conn, idReserva);
            if (reserva == null) {
                throw new EntidadeNaoEncontradaException("Reserva não encontrada (ID " + idReserva + ").");
            }

            if (idQuartoBloqueio == null) {
                quartoDAO.bloquearPorId(conn, reserva.getQuarto().getIdQuarto());
            }

            boolean haConflito = reservaDAO.existeConflitoPeriodo(
                    conn,
                    reserva.getQuarto().getIdQuarto(),
                    reserva.getDataCheckInPrevista(),
                    reserva.getDataCheckOutPrevista(),
                    reserva.getIdReserva()
            );
            if (haConflito) {
                throw new RegraNegocioException(
                        "Confirmação bloqueada (RN06): o quarto " + reserva.getQuarto().getNumero()
                        + " já possui reserva ativa sobreposta no período de "
                        + reserva.getDataCheckInPrevista().format(DATA_BR) + " a "
                        + reserva.getDataCheckOutPrevista().format(DATA_BR) + ".");
            }

            reserva.confirmar();
            if (conn != null) {
                reservaDAO.atualizar(conn, reserva);
                ConnectionFactory.commit(conn);
            } else {
                reservaDAO.atualizar(reserva);
            }
            reservaConfirmada = reserva;
        } catch (RuntimeException e) {
            ConnectionFactory.rollback(conn);
            throw e;
        } finally {
            ConnectionFactory.fecharSilenciosamente(conn);
        }

        enviarEmailConfirmacao(reservaConfirmada);
        return reservaConfirmada;
    }

    public Reserva cancelarReserva(Usuario operador, int idReserva) {
        AutenticacaoService.exigirPermissao(operador, "CANCELAR_RESERVA");

        Reserva reserva = buscarReservaObrigatoria(idReserva);
        reserva.cancelar();
        reservaDAO.atualizar(reserva);
        return reserva;
    }

    public Reserva realizarCheckIn(Usuario operador, int idReserva, LocalDateTime momentoEntrada) {
        AutenticacaoService.exigirPermissao(operador, "REALIZAR_CHECKIN");

        Connection conn = null;
        try {
            conn = connectionFactory.getTransactionalConnection();
            Integer idQuartoBloqueio = reservaDAO.bloquearObterIdQuarto(conn, idReserva);
            if (idQuartoBloqueio != null) {
                quartoDAO.bloquearPorId(conn, idQuartoBloqueio);
            }
            Reserva reserva = reservaDAO.buscarPorId(conn, idReserva);
            if (reserva == null) {
                throw new EntidadeNaoEncontradaException("Reserva não encontrada (ID " + idReserva + ").");
            }

            LocalDateTime entradaEfetiva = momentoEntrada != null ? momentoEntrada : LocalDateTime.now();
            reserva.realizarCheckIn(entradaEfetiva);

            reservaDAO.atualizar(conn, reserva);
            quartoDAO.atualizar(conn, reserva.getQuarto());

            ConnectionFactory.commit(conn);
            return reserva;
        } catch (RuntimeException e) {
            ConnectionFactory.rollback(conn);
            throw e;
        } finally {
            ConnectionFactory.fecharSilenciosamente(conn);
        }
    }

    public List<Reserva> consultarHistoricoHospede(Usuario operador, int idHospede) {
        AutenticacaoService.exigirPermissao(operador, "CONSULTAR_HISTORICO");
        Hospede hospede = hospedeDAO.buscarPorId(idHospede);
        if (hospede == null) {
            throw new EntidadeNaoEncontradaException("Hóspede não encontrado (ID " + idHospede + ").");
        }
        return reservaDAO.listarFinalizadasPorHospede(idHospede);
    }

    public List<Reserva> listarTodasReservas(Usuario operador) {
        AutenticacaoService.exigirPermissao(operador, "CRIAR_RESERVA");
        return reservaDAO.listarTodos();
    }

    public List<Reserva> listarReservasPorStatus(Usuario operador, StatusReserva status) {
        AutenticacaoService.exigirPermissao(operador, "CRIAR_RESERVA");
        return reservaDAO.listarPorStatus(status);
    }

    public Reserva buscarReservaObrigatoria(int idReserva) {
        Reserva reserva = reservaDAO.buscarPorId(idReserva);
        if (reserva == null) {
            throw new EntidadeNaoEncontradaException("Reserva não encontrada (ID " + idReserva + ").");
        }
        return reserva;
    }

    private void enviarEmailConfirmacao(Reserva reserva) {
        String assunto = "HotelFlow - Confirmação da Reserva #" + reserva.getIdReserva();
        String corpo = String.format(
                "Olá, %s!\n\n"
                + "Sua reserva no HotelFlow foi CONFIRMADA com sucesso.\n"
                + "• Código da Reserva : #%d\n"
                + "• Quarto Alocado    : %s (%s - Diária R$ %s)\n"
                + "• Check-in Previsto : %s\n"
                + "• Check-out Previsto: %s\n\n"
                + "Aguardamos sua chegada!\n"
                + "Equipe HotelFlow\n",
                reserva.getHospede().getNomeCompleto(),
                reserva.getIdReserva(),
                reserva.getQuarto().getNumero(),
                reserva.getQuarto().getCategoria().getNome(),
                reserva.getQuarto().getCategoria().getTarifaDiaria(),
                reserva.getDataCheckInPrevista().format(DATA_BR),
                reserva.getDataCheckOutPrevista().format(DATA_BR)
        );
        emailService.enviarConfirmacaoReserva(reserva.getHospede().getEmail(), assunto, corpo);
    }
}
