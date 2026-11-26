package br.senac.hotelflow.dao;

import br.senac.hotelflow.config.ConnectionFactory;
import br.senac.hotelflow.exception.PersistenciaException;
import br.senac.hotelflow.model.Hospede;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO JDBC responsável pelo cadastro e consulta de clientes na tabela {@code hospede}.
 *
 * <p>Rastreabilidade: {@code [Atende RN02, RN19]}
 */
public class HospedeDAO implements CrudDAO<Hospede, Integer> {

    private final ConnectionFactory connectionFactory;

    public HospedeDAO() {
        this(ConnectionFactory.getInstance());
    }

    public HospedeDAO(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Hospede salvar(Hospede hospede) {
        String sql = "INSERT INTO hospede (nome_completo, documento, telefone, email, endereco) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, hospede.getNomeCompleto());
            ps.setString(2, hospede.getDocumento());
            ps.setString(3, hospede.getTelefone());
            ps.setString(4, hospede.getEmail());
            ps.setString(5, hospede.getEndereco());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    hospede.setIdHospede(rs.getInt(1));
                }
            }
            return hospede;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao cadastrar hóspede (doc: " + hospede.getDocumento() + "): " + e.getMessage(), e);
        }
    }

    @Override
    public void atualizar(Hospede hospede) {
        String sql = "UPDATE hospede SET nome_completo = ?, documento = ?, telefone = ?, email = ?, endereco = ? WHERE id_hospede = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, hospede.getNomeCompleto());
            ps.setString(2, hospede.getDocumento());
            ps.setString(3, hospede.getTelefone());
            ps.setString(4, hospede.getEmail());
            ps.setString(5, hospede.getEndereco());
            ps.setInt(6, hospede.getIdHospede());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao atualizar hóspede ID " + hospede.getIdHospede() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Hospede> buscarPorId(Integer id) {
        String sql = "SELECT id_hospede, nome_completo, documento, telefone, email, endereco FROM hospede WHERE id_hospede = ?";
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
            throw new PersistenciaException("Erro ao buscar hóspede ID " + id + ": " + e.getMessage(), e);
        }
    }

    /**
     * Localiza um hóspede pelo CPF ou Passaporte padronizado (ignorando pontos/traços na entrada).
     *
     * <p>Rastreabilidade: {@code [Atende RN02]}
     */
    public Optional<Hospede> buscarPorDocumento(String documento) {
        if (documento == null || documento.isBlank()) {
            return Optional.empty();
        }
        String docNormalizado = Hospede.normalizarDocumento(documento);
        String sql = "SELECT id_hospede, nome_completo, documento, telefone, email, endereco "
                + "FROM hospede WHERE REPLACE(REPLACE(REPLACE(documento, '.', ''), '-', ''), ' ', '') = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, docNormalizado);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar hóspede pelo documento '" + documento + "': " + e.getMessage(), e);
        }
    }

    public List<Hospede> buscarPorNome(String termo) {
        String sql = "SELECT id_hospede, nome_completo, documento, telefone, email, endereco "
                + "FROM hospede WHERE nome_completo LIKE ? ORDER BY nome_completo";
        List<Hospede> lista = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + (termo == null ? "" : termo.trim()) + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao pesquisar hóspedes por nome: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Hospede> listarTodos() {
        String sql = "SELECT id_hospede, nome_completo, documento, telefone, email, endereco FROM hospede ORDER BY nome_completo";
        List<Hospede> lista = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao listar hóspedes: " + e.getMessage(), e);
        }
    }

    static Hospede mapear(ResultSet rs) throws SQLException {
        return new Hospede(
                rs.getInt("id_hospede"),
                rs.getString("nome_completo"),
                rs.getString("documento"),
                rs.getString("telefone"),
                rs.getString("email"),
                rs.getString("endereco")
        );
    }
}
