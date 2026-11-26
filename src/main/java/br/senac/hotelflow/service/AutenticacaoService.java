package br.senac.hotelflow.service;

import br.senac.hotelflow.dao.UsuarioDAO;
import br.senac.hotelflow.exception.AutenticacaoException;
import br.senac.hotelflow.exception.AutorizacaoException;
import br.senac.hotelflow.model.Usuario;

/**
 * Serviço responsável pela autenticação de colaboradores (login/senha SHA-256), gestão
 * da sessão ativa e verificação centralizada de controle de acesso por perfil.
 *
 * <p>Rastreabilidade: {@code [Atende RN01, D04]}
 */
public class AutenticacaoService {

    private final UsuarioDAO usuarioDAO;
    private Usuario usuarioLogado;

    public AutenticacaoService() {
        this(new UsuarioDAO());
    }

    public AutenticacaoService(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    /**
     * Valida login e senha contra o cadastro e inicia a sessão autenticada do colaborador.
     *
     * <p>Rastreabilidade: {@code [Atende RN01]}
     */
    public Usuario autenticar(String login, String senha) {
        if (login == null || login.isBlank() || senha == null || senha.isBlank()) {
            throw new AutenticacaoException("Login e senha são obrigatórios para autenticação.");
        }
        Usuario usuario = usuarioDAO.buscarPorLogin(login)
                .orElseThrow(() -> new AutenticacaoException("Credenciais inválidas: login ou senha incorretos."));

        if (!usuario.autenticar(senha)) {
            throw new AutenticacaoException("Credenciais inválidas: login ou senha incorretos.");
        }
        this.usuarioLogado = usuario;
        return usuario;
    }

    public void encerrarSessao() {
        this.usuarioLogado = null;
    }

    public Usuario getUsuarioLogado() {
        return usuarioLogado;
    }

    /**
     * Guarda transversal de segurança: garante que há um operador autenticado e que seu perfil
     * detém permissão explícita para a operação solicitada ({@code D04}).
     *
     * <p>Rastreabilidade: {@code [Atende RN01, RN12, RN14, RN20, D04]}
     */
    public static void exigirPermissao(Usuario operador, String operacao) {
        if (operador == null) {
            throw new AutenticacaoException("Operação bloqueada: é necessário autenticar-se no sistema.");
        }
        if (!operador.possuiPermissao(operacao)) {
            throw new AutorizacaoException(
                    "Acesso negado: o perfil '" + operador.getPerfil().getDescricao()
                    + "' não possui permissão para executar '" + operacao + "'.");
        }
    }
}
