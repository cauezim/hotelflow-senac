package br.senac.hotelflow.service;

import br.senac.hotelflow.dao.ConsumoFrigobarDAO;
import br.senac.hotelflow.dao.QuartoDAO;
import br.senac.hotelflow.dao.ReservaDAO;
import br.senac.hotelflow.dao.ServicoQuartoDAO;
import br.senac.hotelflow.exception.EntidadeNaoEncontradaException;
import br.senac.hotelflow.model.ConsumoFrigobar;
import br.senac.hotelflow.model.Quarto;
import br.senac.hotelflow.model.Reserva;
import br.senac.hotelflow.model.ServicoQuarto;
import br.senac.hotelflow.model.Usuario;
import br.senac.hotelflow.model.enums.StatusLimpeza;
import java.math.BigDecimal;
import java.util.List;

/**
 * Serviço responsável pelas operações durante a estadia e governança hoteleira:
 * lançamento de consumo de frigobar ({@code RN10}), serviços de quarto ({@code RN11})
 * e atualização do status de limpeza pela Camareira ({@code RN12}).
 *
 * <p>Rastreabilidade: {@code [Atende RN01, RN10, RN11, RN12, D04, D06]}
 */
public class HospedagemService {

    private final ReservaDAO reservaDAO;
    private final ConsumoFrigobarDAO consumoDAO;
    private final ServicoQuartoDAO servicoDAO;
    private final QuartoDAO quartoDAO;

    public HospedagemService() {
        this(new ReservaDAO(), new ConsumoFrigobarDAO(), new ServicoQuartoDAO(), new QuartoDAO());
    }

    public HospedagemService(ReservaDAO reservaDAO, ConsumoFrigobarDAO consumoDAO,
                             ServicoQuartoDAO servicoDAO, QuartoDAO quartoDAO) {
        this.reservaDAO = reservaDAO;
        this.consumoDAO = consumoDAO;
        this.servicoDAO = servicoDAO;
        this.quartoDAO = quartoDAO;
    }

    /**
     * Lança um item de frigobar na conta de uma reserva {@code EM_ANDAMENTO}.
     *
     * <p>Rastreabilidade: {@code [Atende RN10, D06]}
     */
    public ConsumoFrigobar lancarConsumoFrigobar(Usuario operador, int idReserva,
                                                 String produto, int quantidade, BigDecimal valorUnitario) {
        AutenticacaoService.exigirPermissao(operador, "LANCAR_CONSUMO");

        Reserva reserva = reservaDAO.buscarPorId(idReserva)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Reserva não encontrada (ID " + idReserva + ")."));

        ConsumoFrigobar novoConsumo = new ConsumoFrigobar(produto, quantidade, valorUnitario);
        reserva.adicionarConsumo(novoConsumo);
        return consumoDAO.salvar(idReserva, novoConsumo);
    }

    /**
     * Solicita e registra um serviço de quarto (lavanderia, refeição extra, etc.) na reserva {@code EM_ANDAMENTO}.
     *
     * <p>Rastreabilidade: {@code [Atende RN11, D06]}
     */
    public ServicoQuarto registrarServicoQuarto(Usuario operador, int idReserva,
                                                String descricao, BigDecimal valor) {
        AutenticacaoService.exigirPermissao(operador, "REGISTRAR_SERVICO");

        Reserva reserva = reservaDAO.buscarPorId(idReserva)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Reserva não encontrada (ID " + idReserva + ")."));

        ServicoQuarto novoServico = new ServicoQuarto(descricao, valor);
        reserva.adicionarServico(novoServico);
        return servicoDAO.salvar(idReserva, novoServico);
    }

    /**
     * Permite exclusivamente à Camareira atualizar o status de higienização do quarto
     * ({@code SUJO}, {@code EM_LIMPEZA} ou {@code LIMPO}).
     *
     * <p>Rastreabilidade: {@code [Atende RN01, RN12]}
     */
    public Quarto atualizarLimpezaQuarto(Usuario operadorCamareira, int idQuarto, StatusLimpeza novoStatus) {
        AutenticacaoService.exigirPermissao(operadorCamareira, "ATUALIZAR_LIMPEZA");

        Quarto quarto = quartoDAO.buscarPorId(idQuarto)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Quarto não encontrado (ID " + idQuarto + ")."));

        quarto.atualizarLimpeza(novoStatus);
        quartoDAO.atualizar(quarto);
        return quarto;
    }

    public List<Quarto> listarQuartosParaGovernanca(Usuario operadorCamareira) {
        AutenticacaoService.exigirPermissao(operadorCamareira, "ATUALIZAR_LIMPEZA");
        return quartoDAO.listarTodos();
    }
}
