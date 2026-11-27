package br.senac.hotelflow.service;

import br.senac.hotelflow.config.ConnectionFactory;
import br.senac.hotelflow.dao.CupomDescontoDAO;
import br.senac.hotelflow.dao.FaturaDAO;
import br.senac.hotelflow.dao.PagamentoDAO;
import br.senac.hotelflow.dao.QuartoDAO;
import br.senac.hotelflow.dao.ReservaDAO;
import br.senac.hotelflow.exception.EntidadeNaoEncontradaException;
import br.senac.hotelflow.exception.RegraNegocioException;
import br.senac.hotelflow.model.CupomDesconto;
import br.senac.hotelflow.model.Fatura;
import br.senac.hotelflow.model.Pagamento;
import br.senac.hotelflow.model.Reserva;
import br.senac.hotelflow.model.Usuario;
import br.senac.hotelflow.model.enums.FormaPagamento;
import br.senac.hotelflow.model.enums.StatusReserva;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;

/**
 * Serviço orquestrador do processo crítico de Check-out e Faturamento ({@code UC13..UC16}),
 * utilizando transação JDBC única e bloqueio pessimista ({@code SELECT ... FOR UPDATE}) para
 * evitar leituras concorrentes de saldo defasado.
 *
 * <p>Rastreabilidade: {@code [Atende RN01, RN04, RN07, RN10, RN11, RN13..RN19, D02..D06]}
 */
public class CheckOutService {

    private final ConnectionFactory connectionFactory;
    private final ReservaDAO reservaDAO;
    private final FaturaDAO faturaDAO;
    private final PagamentoDAO pagamentoDAO;
    private final QuartoDAO quartoDAO;
    private final CupomDescontoDAO cupomDAO;
    private final GeradorReciboService geradorReciboService;

    public CheckOutService() {
        this(
                ConnectionFactory.getInstance(),
                new ReservaDAO(),
                new FaturaDAO(),
                new PagamentoDAO(),
                new QuartoDAO(),
                new CupomDescontoDAO(),
                new GeradorReciboService()
        );
    }

    public CheckOutService(ConnectionFactory connectionFactory, ReservaDAO reservaDAO,
                           FaturaDAO faturaDAO, PagamentoDAO pagamentoDAO,
                           QuartoDAO quartoDAO, CupomDescontoDAO cupomDAO,
                           GeradorReciboService geradorReciboService) {
        this.connectionFactory = connectionFactory;
        this.reservaDAO = reservaDAO;
        this.faturaDAO = faturaDAO;
        this.pagamentoDAO = pagamentoDAO;
        this.quartoDAO = quartoDAO;
        this.cupomDAO = cupomDAO;
        this.geradorReciboService = geradorReciboService;
    }

    /**
     * Abre o Check-out em transação com bloqueio pessimista, gerando a fatura consolidada
     * ou retomando a fatura aberta sem risco de duplicidade.
     *
     * <p>Rastreabilidade: {@code [Atende RN13, RN16, D02, D05, D06]}
     */
    public Fatura iniciarOuRetomarCheckOut(Usuario operador, int idReserva, LocalDateTime momentoReferencia) {
        AutenticacaoService.exigirPermissao(operador, "PROCESSAR_CHECKOUT");

        LocalDateTime agora = momentoReferencia != null ? momentoReferencia : LocalDateTime.now();
        Connection conn = null;
        try {
            if (connectionFactory != null) {
                conn = connectionFactory.getTransactionalConnection();
            }

            bloquearAgregadoCheckOut(conn, idReserva);

            Reserva reserva = reservaDAO.buscarPorId(conn, idReserva);
            if (reserva == null) {
                throw new EntidadeNaoEncontradaException("Reserva não encontrada (ID " + idReserva + ").");
            }

            if (reserva.getStatus() != StatusReserva.EM_ANDAMENTO) {
                throw new RegraNegocioException(
                        "O Check-out exige reserva Em Andamento (status atual: " + reserva.getStatus().getDescricao() + ").");
            }

            Fatura faturaResultado;
            Fatura faturaExistente = reserva.getFatura();
            if (faturaExistente != null) {
                boolean prorrogada = faturaExistente.atualizarSeProrrogada(agora.toLocalDate());
                if (prorrogada) {
                    if (conn != null) {
                        faturaDAO.atualizar(conn, faturaExistente);
                    } else {
                        faturaDAO.atualizar(faturaExistente);
                    }
                }
                faturaResultado = faturaExistente;
            } else {
                Fatura novaFatura = new Fatura(reserva, agora);
                if (conn != null) {
                    faturaResultado = faturaDAO.salvar(conn, novaFatura);
                } else {
                    faturaResultado = faturaDAO.salvar(novaFatura);
                }
            }

            ConnectionFactory.commit(conn);
            return faturaResultado;
        } catch (RuntimeException e) {
            ConnectionFactory.rollback(conn);
            throw e;
        } finally {
            ConnectionFactory.fecharSilenciosamente(conn);
        }
    }

    /**
     * Aplica um cupom promocional percentual ativo mediante autorização exclusiva de {@code GERENTE}
     * e somente antes do primeiro pagamento ({@code totalPago == 0}).
     *
     * <p>Rastreabilidade: {@code [Atende RN01, RN14, D03, D04, D06]}
     */
    public Fatura aplicarCupomDesconto(Usuario operadorGerente, int idReserva, String codigoCupom) {
        AutenticacaoService.exigirPermissao(operadorGerente, "APLICAR_DESCONTO");

        CupomDesconto cupom = cupomDAO.buscarPorCodigo(codigoCupom);
        if (cupom == null) {
            throw new EntidadeNaoEncontradaException("Cupom de desconto não encontrado: '" + codigoCupom + "'.");
        }

        Connection conn = null;
        try {
            if (connectionFactory != null) {
                conn = connectionFactory.getTransactionalConnection();
            }

            bloquearAgregadoCheckOut(conn, idReserva);

            Reserva reserva = reservaDAO.buscarPorId(conn, idReserva);
            if (reserva == null) {
                throw new EntidadeNaoEncontradaException("Reserva não encontrada (ID " + idReserva + ").");
            }

            Fatura fatura = reserva.getFatura();
            if (fatura == null) {
                throw new RegraNegocioException("Inicie o Check-out da reserva antes de aplicar um cupom de desconto.");
            }

            fatura.aplicarDesconto(cupom);
            if (conn != null) {
                faturaDAO.atualizar(conn, fatura);
                ConnectionFactory.commit(conn);
            } else {
                faturaDAO.atualizar(fatura);
            }
            return fatura;
        } catch (RuntimeException e) {
            ConnectionFactory.rollback(conn);
            throw e;
        } finally {
            ConnectionFactory.fecharSilenciosamente(conn);
        }
    }

    /**
     * Registra uma parcela de pagamento sob bloqueio exclusivo ({@code SELECT ... FOR UPDATE}) na
     * reserva, quarto e fatura e, caso o saldo devedor atinja zero, executa na mesma transação
     * a finalização da reserva e a liberação do quarto ({@code SUJO}).
     *
     * <p>Rastreabilidade: {@code [Atende RN07, RN15, RN16, RN17, RN18, RN19, D05]}
     */
    public ResultadoCheckOut registrarPagamento(Usuario operador, int idReserva,
                                                BigDecimal valor, FormaPagamento formaPagamento,
                                                LocalDateTime momentoPagamento) {
        AutenticacaoService.exigirPermissao(operador, "REGISTRAR_PAGAMENTO");

        LocalDateTime agora = momentoPagamento != null ? momentoPagamento : LocalDateTime.now();
        Connection conn = null;
        try {
            if (connectionFactory != null) {
                conn = connectionFactory.getTransactionalConnection();
            }

            bloquearAgregadoCheckOut(conn, idReserva);

            Reserva reserva = reservaDAO.buscarPorId(conn, idReserva);
            if (reserva == null) {
                throw new EntidadeNaoEncontradaException("Reserva não encontrada (ID " + idReserva + ").");
            }

            if (reserva.getStatus() != StatusReserva.EM_ANDAMENTO) {
                throw new RegraNegocioException("A reserva informada não está Em Andamento.");
            }
            Fatura fatura = reserva.getFatura();
            if (fatura == null) {
                throw new RegraNegocioException("A fatura desta reserva ainda não foi aberta para pagamento.");
            }

            fatura.atualizarSeProrrogada(agora.toLocalDate());

            Pagamento novoPagamento = new Pagamento(valor, agora, formaPagamento);
            fatura.registrarPagamento(novoPagamento);

            if (conn != null) {
                pagamentoDAO.salvar(conn, fatura.getIdFatura(), novoPagamento);
                faturaDAO.atualizar(conn, fatura);
            } else {
                pagamentoDAO.salvar(fatura.getIdFatura(), novoPagamento);
                faturaDAO.atualizar(fatura);
            }

            boolean quitada = fatura.estaQuitada();
            if (quitada) {
                reserva.finalizar(agora);
                if (conn != null) {
                    reservaDAO.atualizar(conn, reserva);
                    quartoDAO.atualizar(conn, reserva.getQuarto());
                } else {
                    reservaDAO.atualizar(reserva);
                    quartoDAO.atualizar(reserva.getQuarto());
                }
            }

            ConnectionFactory.commit(conn);

            String reciboTexto = quitada ? geradorReciboService.emitirReciboTexto(fatura) : null;
            return new ResultadoCheckOut(fatura, reserva, quitada, reciboTexto);
        } catch (RuntimeException e) {
            ConnectionFactory.rollback(conn);
            throw e;
        } finally {
            ConnectionFactory.fecharSilenciosamente(conn);
        }
    }

    private void bloquearAgregadoCheckOut(Connection conn, int idReserva) {
        Integer idQuarto = reservaDAO.bloquearObterIdQuarto(conn, idReserva);
        if (idQuarto != null) {
            quartoDAO.bloquearPorId(conn, idQuarto);
        }
        faturaDAO.bloquearPorReserva(conn, idReserva);
    }
}
