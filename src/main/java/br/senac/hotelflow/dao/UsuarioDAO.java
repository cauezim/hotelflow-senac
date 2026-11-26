package br.senac.hotelflow.dao;

import br.senac.hotelflow.config.ConnectionFactory;
import br.senac.hotelflow.exception.PersistenciaException;
import br.senac.hotelflow.model.Usuario;
import br.senac.hotelflow.model.enums.PerfilUsuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO JDBC responsável pelo acesso à tabela {@code usuario}.
 *
 * <p>Rastreabilidade: {@code [Atende RN01, D04]}
 */
public class UsuarioDAO implements CrudDAO<Usuario, Integer> {

    private final ConnectionFactory connectionFactory;

    public UsuarioDAO() {
        this(ConnectionFactory.getInstance());
    }

    public UsuarioDAO(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Usuario salvar(Usuario usuario) {
        String sql = "INSERT INTO usuario (nome, login, senha_hash, perfil) VALUES (?, ?, ?, ?)";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, usuario.getNome());
            ps.setString(2, usuario.getLogin());
            ps.setString(3, usuario.getSenhaHash());
            ps.setString(4, usuario.getPerfil().name());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    usuario.setIdUsuario(rs.getInt(1));
                }
            }
            return usuario;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao salvar usuário '" + usuario.getLogin() + "': " + e.getMessage(), e);
        }
    }

    @Override
    public void atualizar(Usuario usuario) {
        String sql = "UPDATE usuario SET nome = ?, login = ?, senha_hash = ?, perfil = ? WHERE id_usuario = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, usuario.getNome());
            ps.setString(2, usuario.getLogin());
            ps.setString(3, usuario.getSenhaHash());
            ps.setString(4, usuario.getPerfil().name());
            ps.setInt(5, usuario.getIdUsuario());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao atualizar usuário ID " + usuario.getIdUsuario() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorId(Integer id) {
        String sql = "SELECT id_usuario, nome, login, senha_hash, perfil FROM usuario WHERE id_usuario = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar usuário por ID " + id + ": " + e.getMessage(), e);
        }
    }

    /**
     * Localiza um colaborador pelo login único para autenticação.
     *
     * <p>Rastreabilidade: {@code [Atende RN01]}
     */
    public Optional<Usuario> buscarPorLogin(String login) {
        if (login == null || login.isBlank()) {
            return Optional.empty();
        }
        String sql = "SELECT id_usuario, nome, login, senha_hash, perfil FROM usuario WHERE login = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, login.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar usuário pelo login '" + login + "': " + e.getMessage(), e);
        }
    }

    @Override
    public List<Usuario> listarTodos() {
        String sql = "SELECT id_usuario, nome, login, senha_hash, perfil FROM usuario ORDER BY nome";
        List<Usuario> lista = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao listar usuários: " + e.getMessage(), e);
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        return new Usuario(
                rs.getInt("id_usuario"),
                rs.getString("nome"),
                rs.getString("login"),
                rs.getString("senha_hash"),
                PerfilUsuario.valueOf(rs.getString("perfil"))
        );
    }
}
