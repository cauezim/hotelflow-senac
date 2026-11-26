package br.senac.hotelflow.model;

import br.senac.hotelflow.exception.RegraNegocioException;
import br.senac.hotelflow.model.enums.PerfilUsuario;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Entidade de domínio que representa um colaborador autenticado do hotel, encapsulando
 * verificação criptográfica SHA-256 de senha e checagem de permissões por perfil.
 *
 * <p>Rastreabilidade: {@code [Atende RN01, D04]}
 */
public class Usuario {

    private Integer idUsuario;
    private String nome;
    private String login;
    private String senhaHash;
    private PerfilUsuario perfil;

    /**
     * Construtor de criação de novo usuário a partir de senha em texto plano (gera hash automaticamente).
     */
    public Usuario(String nome, String login, String senhaTextoPlano, PerfilUsuario perfil) {
        setNome(nome);
        setLogin(login);
        definirSenhaTextoPlano(senhaTextoPlano);
        setPerfil(perfil);
    }

    /**
     * Construtor completo para hidratação a partir do banco de dados ({@code UsuarioDAO}).
     */
    public Usuario(Integer idUsuario, String nome, String login, String senhaHash, PerfilUsuario perfil) {
        this.idUsuario = idUsuario;
        setNome(nome);
        setLogin(login);
        setSenhaHash(senhaHash);
        setPerfil(perfil);
    }

    /**
     * Verifica se a senha em texto plano corresponde ao {@code senhaHash} armazenado.
     *
     * <p>Rastreabilidade: {@code [Atende RN01]}
     *
     * @param senha senha informada no login
     * @return {@code true} se o hash coincidir
     */
    public boolean autenticar(String senha) {
        if (senha == null || senha.isBlank()) {
            return false;
        }
        String hashCalculado = gerarHashSha256(senha);
        return MessageDigest.isEqual(
                hashCalculado.getBytes(StandardCharsets.UTF_8),
                this.senhaHash.toLowerCase().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Verifica se o perfil do usuário detém autorização para executar a operação informada.
     *
     * <p>Rastreabilidade: {@code [Atende RN01, RN12, RN14, RN20, D04]}
     *
     * @param operacao código da operação de sistema
     * @return {@code true} se autorizado
     */
    public boolean possuiPermissao(String operacao) {
        return this.perfil != null && this.perfil.possuiPermissao(operacao);
    }

    /**
     * Utilitário determinístico SHA-256 hexadecimal minúsculo compatível com o script de seed SQL.
     */
    public static String gerarHashSha256(String senhaTextoPlano) {
        if (senhaTextoPlano == null || senhaTextoPlano.isBlank()) {
            throw new RegraNegocioException("A senha do usuário não pode ser vazia.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(senhaTextoPlano.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível na JVM.", e);
        }
    }

    public final void definirSenhaTextoPlano(String senhaTextoPlano) {
        this.senhaHash = gerarHashSha256(senhaTextoPlano);
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNome() {
        return nome;
    }

    public final void setNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("O nome do usuário é obrigatório.");
        }
        this.nome = nome.trim();
    }

    public String getLogin() {
        return login;
    }

    public final void setLogin(String login) {
        if (login == null || login.isBlank()) {
            throw new RegraNegocioException("O login do usuário é obrigatório.");
        }
        this.login = login.trim().toLowerCase();
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public final void setSenhaHash(String senhaHash) {
        if (senhaHash == null || senhaHash.isBlank()) {
            throw new RegraNegocioException("O hash da senha é obrigatório.");
        }
        this.senhaHash = senhaHash.trim().toLowerCase();
    }

    public PerfilUsuario getPerfil() {
        return perfil;
    }

    public final void setPerfil(PerfilUsuario perfil) {
        if (perfil == null) {
            throw new RegraNegocioException("O perfil de acesso do usuário é obrigatório.");
        }
        this.perfil = perfil;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario that)) return false;
        return Objects.equals(login, that.login);
    }

    @Override
    public int hashCode() {
        return Objects.hash(login);
    }

    @Override
    public String toString() {
        return "Usuario{id=" + idUsuario + ", login='" + login + "', perfil=" + perfil + "}";
    }
}
