package br.senac.hotelflow.model;

import br.senac.hotelflow.exception.RegraNegocioException;
import br.senac.hotelflow.model.enums.PerfilUsuario;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    public Usuario(String nome, String login, String senhaTextoPlano, PerfilUsuario perfil) {
        setNome(nome);
        setLogin(login);
        definirSenhaTextoPlano(senhaTextoPlano);
        setPerfil(perfil);
    }

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
     */
    public boolean autenticar(String senha) {
        if (senha == null || senha.trim().isEmpty()) {
            return false;
        }
        String hashCalculado = gerarHashSha256(senha);
        return hashCalculado.equalsIgnoreCase(this.senhaHash);
    }

    /**
     * Verifica se o perfil do usuário detém autorização para executar a operação informada.
     *
     * <p>Rastreabilidade: {@code [Atende RN01, RN12, RN14, RN20, D04]}
     */
    public boolean possuiPermissao(String operacao) {
        return this.perfil != null && this.perfil.possuiPermissao(operacao);
    }

    /**
     * Gera o resumo criptográfico SHA-256 em texto hexadecimal minúsculo.
     */
    public static String gerarHashSha256(String senhaTextoPlano) {
        if (senhaTextoPlano == null || senhaTextoPlano.trim().isEmpty()) {
            throw new RegraNegocioException("A senha do usuário não pode ser vazia.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(senhaTextoPlano.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                hex.append(String.format("%02x", bytes[i]));
            }
            return hex.toString();
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
        if (nome == null || nome.trim().isEmpty()) {
            throw new RegraNegocioException("O nome do usuário é obrigatório.");
        }
        this.nome = nome.trim();
    }

    public String getLogin() {
        return login;
    }

    public final void setLogin(String login) {
        if (login == null || login.trim().isEmpty()) {
            throw new RegraNegocioException("O login do usuário é obrigatório.");
        }
        this.login = login.trim().toLowerCase();
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public final void setSenhaHash(String senhaHash) {
        if (senhaHash == null || senhaHash.trim().isEmpty()) {
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
        if (this == o) {
            return true;
        }
        if (!(o instanceof Usuario)) {
            return false;
        }
        Usuario outro = (Usuario) o;
        return Objects.equals(this.login, outro.login);
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
