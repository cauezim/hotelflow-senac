package br.senac.hotelflow.service;

import br.senac.hotelflow.dao.CategoriaQuartoDAO;
import br.senac.hotelflow.dao.CupomDescontoDAO;
import br.senac.hotelflow.dao.HospedeDAO;
import br.senac.hotelflow.dao.QuartoDAO;
import br.senac.hotelflow.exception.EntidadeNaoEncontradaException;
import br.senac.hotelflow.exception.RegraNegocioException;
import br.senac.hotelflow.model.CategoriaQuarto;
import br.senac.hotelflow.model.CupomDesconto;
import br.senac.hotelflow.model.Hospede;
import br.senac.hotelflow.model.Quarto;
import br.senac.hotelflow.model.Usuario;
import java.math.BigDecimal;
import java.util.List;

/**
 * Serviço de aplicação para manutenção dos cadastros estruturais do hotel:
 * Hóspedes ({@code RN02}), Quartos ({@code RN03}), Categorias/Tarifas ({@code RN04})
 * e Cupons de Desconto ({@code RN14, D03}), aplicando controle de acesso ({@code RN01, D04}).
 *
 * <p>Rastreabilidade: {@code [Atende RN01, RN02, RN03, RN04, RN14, D03, D04]}
 */
public class CadastroService {

    private final HospedeDAO hospedeDAO;
    private final CategoriaQuartoDAO categoriaDAO;
    private final QuartoDAO quartoDAO;
    private final CupomDescontoDAO cupomDAO;

    public CadastroService() {
        this(new HospedeDAO(), new CategoriaQuartoDAO(), new QuartoDAO(), new CupomDescontoDAO());
    }

    public CadastroService(HospedeDAO hospedeDAO, CategoriaQuartoDAO categoriaDAO,
                           QuartoDAO quartoDAO, CupomDescontoDAO cupomDAO) {
        this.hospedeDAO = hospedeDAO;
        this.categoriaDAO = categoriaDAO;
        this.quartoDAO = quartoDAO;
        this.cupomDAO = cupomDAO;
    }

    // =========================================================================
    // HÓSPEDES [Atende RN02]
    // =========================================================================

    public Hospede cadastrarHospede(Usuario operador, Hospede novoHospede) {
        AutenticacaoService.exigirPermissao(operador, "MANTER_HOSPEDES");
        if (novoHospede == null) {
            throw new RegraNegocioException("Os dados do hóspede são obrigatórios.");
        }
        Hospede existente = hospedeDAO.buscarPorDocumento(novoHospede.getDocumento());
        if (existente != null) {
            throw new RegraNegocioException(
                    "Já existe um hóspede cadastrado com o documento '" + novoHospede.getDocumento() + "'.");
        }
        return hospedeDAO.salvar(novoHospede);
    }

    public void atualizarHospede(Usuario operador, Hospede hospede) {
        AutenticacaoService.exigirPermissao(operador, "MANTER_HOSPEDES");
        if (hospede == null || hospede.getIdHospede() == null) {
            throw new RegraNegocioException("Informe um hóspede já cadastrado para atualização.");
        }
        Hospede atual = hospedeDAO.buscarPorId(hospede.getIdHospede());
        if (atual == null) {
            throw new EntidadeNaoEncontradaException("Hóspede não encontrado (ID " + hospede.getIdHospede() + ").");
        }

        Hospede docConflitante = hospedeDAO.buscarPorDocumento(hospede.getDocumento());
        if (docConflitante != null && !docConflitante.getIdHospede().equals(hospede.getIdHospede())) {
            throw new RegraNegocioException(
                    "O documento '" + hospede.getDocumento() + "' já pertence a outro hóspede cadastrado.");
        }
        hospedeDAO.atualizar(hospede);
    }

    public List<Hospede> listarHospedes(Usuario operador) {
        AutenticacaoService.exigirPermissao(operador, "MANTER_HOSPEDES");
        return hospedeDAO.listarTodos();
    }

    public Hospede buscarHospedePorDocumento(Usuario operador, String documento) {
        AutenticacaoService.exigirPermissao(operador, "MANTER_HOSPEDES");
        return hospedeDAO.buscarPorDocumento(documento);
    }

    // =========================================================================
    // CATEGORIAS E TARIFAS DIÁRIAS [Atende RN03, RN04, D04]
    // =========================================================================

    public CategoriaQuarto cadastrarCategoria(Usuario operadorGerente, CategoriaQuarto categoria) {
        AutenticacaoService.exigirPermissao(operadorGerente, "MANTER_CATEGORIAS_TARIFAS");
        if (categoriaDAO.buscarPorNome(categoria.getNome()) != null) {
            throw new RegraNegocioException("A categoria '" + categoria.getNome() + "' já está cadastrada.");
        }
        return categoriaDAO.salvar(categoria);
    }

    public CategoriaQuarto atualizarTarifaDiaria(Usuario operadorGerente, int idCategoria, BigDecimal novaTarifa) {
        AutenticacaoService.exigirPermissao(operadorGerente, "MANTER_CATEGORIAS_TARIFAS");
        CategoriaQuarto categoria = categoriaDAO.buscarPorId(idCategoria);
        if (categoria == null) {
            throw new EntidadeNaoEncontradaException("Categoria não encontrada (ID " + idCategoria + ").");
        }
        categoria.atualizarTarifa(novaTarifa);
        categoriaDAO.atualizar(categoria);
        return categoria;
    }

    public List<CategoriaQuarto> listarCategorias() {
        return categoriaDAO.listarTodos();
    }

    // =========================================================================
    // QUARTOS [Atende RN03, D04]
    // =========================================================================

    public Quarto cadastrarQuarto(Usuario operadorGerente, Quarto novoQuarto) {
        AutenticacaoService.exigirPermissao(operadorGerente, "MANTER_QUARTOS");
        if (novoQuarto == null) {
            throw new RegraNegocioException("Os dados do quarto são obrigatórios.");
        }
        if (quartoDAO.buscarPorNumero(novoQuarto.getNumero()) != null) {
            throw new RegraNegocioException("Já existe um quarto cadastrado com o número '" + novoQuarto.getNumero() + "'.");
        }
        return quartoDAO.salvar(novoQuarto);
    }

    public void atualizarQuarto(Usuario operadorGerente, Quarto quarto) {
        AutenticacaoService.exigirPermissao(operadorGerente, "MANTER_QUARTOS");
        if (quarto == null || quarto.getIdQuarto() == null) {
            throw new RegraNegocioException("Informe um quarto válido para atualização.");
        }
        Quarto atual = quartoDAO.buscarPorId(quarto.getIdQuarto());
        if (atual == null) {
            throw new EntidadeNaoEncontradaException("Quarto não encontrado (ID " + quarto.getIdQuarto() + ").");
        }

        Quarto numeroConflitante = quartoDAO.buscarPorNumero(quarto.getNumero());
        if (numeroConflitante != null && !numeroConflitante.getIdQuarto().equals(quarto.getIdQuarto())) {
            throw new RegraNegocioException("Já existe outro quarto cadastrado com o número '" + quarto.getNumero() + "'.");
        }
        quartoDAO.atualizar(quarto);
    }

    public List<Quarto> listarQuartos() {
        return quartoDAO.listarTodos();
    }

    // =========================================================================
    // CUPONS DE DESCONTO [Atende RN14, D03, D04]
    // =========================================================================

    public CupomDesconto cadastrarCupom(Usuario operadorGerente, CupomDesconto cupom) {
        AutenticacaoService.exigirPermissao(operadorGerente, "MANTER_CUPONS");
        if (cupom == null) {
            throw new RegraNegocioException("Os dados do cupom são obrigatórios.");
        }
        if (cupomDAO.buscarPorCodigo(cupom.getCodigo()) != null) {
            throw new RegraNegocioException("Já existe um cupom cadastrado com o código '" + cupom.getCodigo() + "'.");
        }
        return cupomDAO.salvar(cupom);
    }

    public CupomDesconto alterarAtivacaoCupom(Usuario operadorGerente, int idCupom, boolean ativo) {
        AutenticacaoService.exigirPermissao(operadorGerente, "MANTER_CUPONS");
        CupomDesconto cupom = cupomDAO.buscarPorId(idCupom);
        if (cupom == null) {
            throw new EntidadeNaoEncontradaException("Cupom não encontrado (ID " + idCupom + ").");
        }
        if (ativo) {
            cupom.ativar();
        } else {
            cupom.desativar();
        }
        cupomDAO.atualizar(cupom);
        return cupom;
    }

    public List<CupomDesconto> listarCupons(Usuario operadorGerente) {
        AutenticacaoService.exigirPermissao(operadorGerente, "MANTER_CUPONS");
        return cupomDAO.listarTodos();
    }
}
