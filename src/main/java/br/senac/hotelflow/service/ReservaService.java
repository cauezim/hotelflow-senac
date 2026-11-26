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
import br.senac.hotelflow.service.notificacao.ConsoleEmailService;
import br.senac.hotelflow.service.notificacao.EmailService;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Serviço responsável pelo ciclo de agendamento, validação de conflitos ({@code RN06, D02}),
 * disparo automático de e-mail de confirmação ({@code RN08}), Check-in transacional ({@code RN09, D01})
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
        this(ConnectionFactory.getInstance(), new ReservaDAO(), new HospedeDAO(),
                new QuartoDAO(), new ConsoleEmailService());
    }

    public ReservaService(ConnectionFactory connectionFactory, ReservaDAO reservaDAO,
                          HospedeDAO hospedeDAO, QuartoDAO quartoDAO, EmailService emailService) {
        this.connectionFactory = connectionFactory;
        this.reservaDAO = reservaDAO;
        this.hospedeDAO = hospedeDAO;
        this.quartoDAO = quartoDAO;
        this.emailService = emailService;
    }

    /**
     * Cria uma nova reserva em status {@code PENDENTE}.
     *
     * <p>Rastreabilidade: {@code [Atende RN05, RN07]}
     */
    public Reserva criarReserva(Usuario operador, int idHospede, int idQuarto,
                                LocalDate dataCheckInPrevista, LocalDate dataCheckOutPrevista) {
        AutenticacaoService.exigirPermissao(operador, "CRIAR_RESERVA");

        Hospede hospede = hospedeDAO.buscarPorId(idHospede)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Hóspede não encontrado (ID " + idHospede + ")."));
        Quarto quarto = quartoDAO.buscarPorId(idQuarto)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Quarto não encontrado (ID " + idQuarto + ")."));

        Reserva novaReserva = new Reserva(hospede, quarto, dataCheckInPrevista, dataCheckOutPrevista);
        return reservaDAO.salvar(novaReserva);
    }

    /**
     * Confirma uma reserva {@code PENDENTE} impedindo sobreposição de datas no mesmo quarto ({@code RN06, D02})
     * e disparando e-mail automático de confirmação ao hóspede ({@code RN08}).
     *
     * <p>Rastreabilidade: {@code [Atende RN06, RN07, RN08, D02]}
     */
    public Reserva confirmarReserva(Usuario operador, int idReserva) {
        AutenticacaoService.exigirPermissao(operador, "CONFIRMAR_RESERVA");

        Reserva reserva = buscarReservaObrigatoria(idReserva);

        boolean haConflito = reservaDAO.existeConflitoPeriodo(
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
        reservaDAO.atualizar(reserva);

        enviarEmailConfirmacao(reserva);
        return reserva;
    }

    /**
     * Cancela uma reserva em status {@code PENDENTE} ou {@code CONFIRMADA}.
     *
     * <p>Rastreabilidade: {@code [Atende RN07]}
     */
    public Reserva cancelarReserva(Usuario operador, int idReserva) {
        AutenticacaoService.exigirPermissao(operador, "CANCELAR_RESERVA");

        Reserva reserva = buscarReservaObrigatoria(idReserva);
        reserva.cancelar();
        reservaDAO.atualizar(reserva);
        return reserva;
    }

    /**
     * Efetiva o Check-in em transação atômica JDBC, exigindo quarto {@code DISPONIVEL} e {@code LIMPO} ({@code D01}),
     * alterando a reserva para {@code EM_ANDAMENTO} e o quarto para {@code OCUPADO}.
     *
     * <p>Rastreabilidade: {@code [Atende RN07, RN09, D01, D05]}
     */
    public Reserva realizarCheckIn(Usuario operador, int idReserva, LocalDateTime momentoEntrada) {
        AutenticacaoService.exigirPermissao(operador, "REALIZAR_CHECKIN");

        Connection conn = null;
        try {
            conn = connectionFactory.getTransactionalConnection();
            Reserva reserva = reservaDAO.buscarPorId(conn, idReserva)
                    .orElseThrow(() -> new EntidadeNaoEncontradaException("Reserva não encontrada (ID " + idReserva + ")."));

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

    /**
     * Consulta o histórico de todas as reservas {@code FINALIZADA}s de um hóspede.
     *
     * <p>Rastreabilidade: {@code [Atende RN19]}
     */
    public List<Reserva> consultarHistoricoHospede(Usuario operador, int idHospede) {
        AutenticacaoService.exigirPermissao(operador, "CONSULTAR_HISTORICO");
        hospedeDAO.buscarPorId(idHospede)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Hóspede não encontrado (ID " + idHospede + ")."));
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
        return reservaDAO.buscarPorId(idReserva)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Reserva não encontrada (ID " + idReserva + ")."));
    }

    private void enviarEmailConfirmacao(Reserva reserva) {
        String assunto = "HotelFlow - Confirmação da Reserva #" + reserva.getIdReserva();
        String corpo = """
                Olá, %s!

                Sua reserva no HotelFlow foi CONFIRMADA com sucesso.
                • Código da Reserva : #%d
                • Quarto Alocado    : %s (%s - Diária R$ %s)
                • Check-in Previsto : %s
                • Check-out Previsto: %s

                Aguardamos sua chegada!
                Equipe HotelFlow
                """.formatted(
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
