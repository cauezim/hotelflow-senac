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
 * Serviço orquestrador do processo crítico de Check-out e Faturamento ({@code UC13..UC16}):
 * <ul>
 *   <li>Gera ou retoma a {@link Fatura} única da reserva, atualizando diárias em caso de prorrogação ({@code D05, D06});</li>
 *   <li>Aplica cupom de desconto autorizado exclusivamente pelo {@code GERENTE} ({@code RN14, D03, D04, D06});</li>
 *   <li>Registra pagamentos fracionados ou integrais ({@code RN15, RN16}) e, na parcela quitadora, executa
 *       em <b>transação atômica única JDBC</b> o pagamento + quitação + finalização da reserva + liberação do quarto
 *       ({@code RN07, RN17, RN19}), emitindo o recibo textual somente após o {@code commit} ({@code RN18}).</li>
 * </ul>
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
     * Abre o Check-out gerando a fatura consolidada (na 1ª vez) ou retomando a fatura aberta
     * (sem duplicá-la e ajustando diárias caso o hóspede tenha prorrogado pernoites).
     *
     * <p>Rastreabilidade: {@code [Atende RN13, RN16, D02, D05, D06]}
     */
    public Fatura iniciarOuRetomarCheckOut(Usuario operador, int idReserva, LocalDateTime momentoReferencia) {
        AutenticacaoService.exigirPermissao(operador, "PROCESSAR_CHECKOUT");

        Reserva reserva = reservaDAO.buscarPorId(idReserva)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Reserva não encontrada (ID " + idReserva + ")."));

        if (reserva.getStatus() != StatusReserva.EM_ANDAMENTO) {
            throw new RegraNegocioException(
                    "O Check-out exige reserva Em Andamento (status atual: " + reserva.getStatus().getDescricao() + ").");
        }

        LocalDateTime agora = momentoReferencia != null ? momentoReferencia : LocalDateTime.now();
        Fatura faturaExistente = reserva.getFatura();
        if (faturaExistente != null) {
            boolean prorrogada = faturaExistente.atualizarSeProrrogada(agora.toLocalDate());
            if (prorrogada) {
                faturaDAO.atualizar(faturaExistente);
            }
            return faturaExistente;
        }

        Fatura novaFatura = new Fatura(reserva, agora);
        return faturaDAO.salvar(novaFatura);
    }

    /**
     * Aplica um cupom promocional percentual ativo mediante autorização exclusiva de {@code GERENTE}
     * e somente antes do primeiro pagamento ({@code totalPago == 0}).
     *
     * <p>Rastreabilidade: {@code [Atende RN01, RN14, D03, D04, D06]}
     */
    public Fatura aplicarCupomDesconto(Usuario operadorGerente, int idReserva, String codigoCupom) {
        AutenticacaoService.exigirPermissao(operadorGerente, "APLICAR_DESCONTO");

        Reserva reserva = reservaDAO.buscarPorId(idReserva)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Reserva não encontrada (ID " + idReserva + ")."));

        Fatura fatura = reserva.getFatura();
        if (fatura == null) {
            throw new RegraNegocioException("Inicie o Check-out da reserva antes de aplicar um cupom de desconto.");
        }

        CupomDesconto cupom = cupomDAO.buscarPorCodigo(codigoCupom)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Cupom de desconto não encontrado: '" + codigoCupom + "'."));

        fatura.aplicarDesconto(cupom);
        faturaDAO.atualizar(fatura);
        return fatura;
    }

    /**
     * Registra uma parcela de pagamento e, caso o saldo devedor atinja zero, executa na MESMA
     * transação JDBC a finalização da reserva e a liberação do quarto para limpeza ({@code SUJO}),
     * gerando o recibo textual somente após a confirmação do {@code commit}.
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
            conn = connectionFactory.getTransactionalConnection();

            Reserva reserva = reservaDAO.buscarPorId(conn, idReserva)
                    .orElseThrow(() -> new EntidadeNaoEncontradaException("Reserva não encontrada (ID " + idReserva + ")."));

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

            pagamentoDAO.salvar(conn, fatura.getIdFatura(), novoPagamento);
            faturaDAO.atualizar(conn, fatura);

            boolean quitada = fatura.estaQuitada();
            if (quitada) {
                // Reserva.finalizar() encapsula status = FINALIZADA + quarto.liberar() + quarto.atualizarLimpeza(SUJO)
                reserva.finalizar(agora);
                reservaDAO.atualizar(conn, reserva);
                quartoDAO.atualizar(conn, reserva.getQuarto());
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
}
