package br.senac.hotelflow.dao;

import br.senac.hotelflow.config.ConnectionFactory;
import br.senac.hotelflow.exception.PersistenciaException;
import br.senac.hotelflow.model.CategoriaQuarto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO JDBC responsável pelas categorias e tarifas diárias na tabela {@code categoria_quarto}.
 *
 * <p>Rastreabilidade: {@code [Atende RN03, RN04]}
 */
public class CategoriaQuartoDAO implements CrudDAO<CategoriaQuarto, Integer> {

    private final ConnectionFactory connectionFactory;

    public CategoriaQuartoDAO() {
        this(ConnectionFactory.getInstance());
    }

    public CategoriaQuartoDAO(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public CategoriaQuarto salvar(CategoriaQuarto categoria) {
        String sql = "INSERT INTO categoria_quarto (nome, tarifa_diaria) VALUES (?, ?)";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, categoria.getNome());
            ps.setBigDecimal(2, categoria.getTarifaDiaria());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    categoria.setIdCategoria(rs.getInt(1));
                }
            }
            return categoria;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao salvar categoria '" + categoria.getNome() + "': " + e.getMessage(), e);
        }
    }

    @Override
    public void atualizar(CategoriaQuarto categoria) {
        String sql = "UPDATE categoria_quarto SET nome = ?, tarifa_diaria = ? WHERE id_categoria = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, categoria.getNome());
            ps.setBigDecimal(2, categoria.getTarifaDiaria());
            ps.setInt(3, categoria.getIdCategoria());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao atualizar tarifa da categoria ID " + categoria.getIdCategoria() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<CategoriaQuarto> buscarPorId(Integer id) {
        String sql = "SELECT id_categoria, nome, tarifa_diaria FROM categoria_quarto WHERE id_categoria = ?";
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
            throw new PersistenciaException("Erro ao buscar categoria ID " + id + ": " + e.getMessage(), e);
        }
    }

    public Optional<CategoriaQuarto> buscarPorNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return Optional.empty();
        }
        String sql = "SELECT id_categoria, nome, tarifa_diaria FROM categoria_quarto WHERE nome = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nome.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar categoria '" + nome + "': " + e.getMessage(), e);
        }
    }

    @Override
    public List<CategoriaQuarto> listarTodos() {
        String sql = "SELECT id_categoria, nome, tarifa_diaria FROM categoria_quarto ORDER BY tarifa_diaria";
        List<CategoriaQuarto> lista = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao listar categorias de quarto: " + e.getMessage(), e);
        }
    }

    static CategoriaQuarto mapear(ResultSet rs) throws SQLException {
        return new CategoriaQuarto(
                rs.getInt("id_categoria"),
                rs.getString("nome"),
                rs.getBigDecimal("tarifa_diaria")
        );
    }
}
