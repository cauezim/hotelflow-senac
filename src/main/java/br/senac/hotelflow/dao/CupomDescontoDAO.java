package br.senac.hotelflow.dao;

import br.senac.hotelflow.config.ConnectionFactory;
import br.senac.hotelflow.exception.PersistenciaException;
import br.senac.hotelflow.model.CupomDesconto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO JDBC responsável pelo gerenciamento de cupons promocionais na tabela {@code cupom_desconto}.
 *
 * <p>Rastreabilidade: {@code [Atende RN14, D03, D06]}
 */
public class CupomDescontoDAO implements CrudDAO<CupomDesconto, Integer> {

    private final ConnectionFactory connectionFactory;

    public CupomDescontoDAO() {
        this(ConnectionFactory.getInstance());
    }

    public CupomDescontoDAO(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public CupomDesconto salvar(CupomDesconto cupom) {
        String sql = "INSERT INTO cupom_desconto (codigo, percentual, ativo) VALUES (?, ?, ?)";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, cupom.getCodigo());
            ps.setBigDecimal(2, cupom.getPercentual());
            ps.setBoolean(3, cupom.isAtivo());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    cupom.setIdCupom(rs.getInt(1));
                }
            }
            return cupom;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao cadastrar cupom '" + cupom.getCodigo() + "': " + e.getMessage(), e);
        }
    }

    @Override
    public void atualizar(CupomDesconto cupom) {
        String sql = "UPDATE cupom_desconto SET codigo = ?, percentual = ?, ativo = ? WHERE id_cupom = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cupom.getCodigo());
            ps.setBigDecimal(2, cupom.getPercentual());
            ps.setBoolean(3, cupom.isAtivo());
            ps.setInt(4, cupom.getIdCupom());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao atualizar cupom ID " + cupom.getIdCupom() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<CupomDesconto> buscarPorId(Integer id) {
        String sql = "SELECT id_cupom, codigo, percentual, ativo FROM cupom_desconto WHERE id_cupom = ?";
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
            throw new PersistenciaException("Erro ao buscar cupom ID " + id + ": " + e.getMessage(), e);
        }
    }

    public Optional<CupomDesconto> buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        String sql = "SELECT id_cupom, codigo, percentual, ativo FROM cupom_desconto WHERE codigo = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, codigo.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar cupom pelo código '" + codigo + "': " + e.getMessage(), e);
        }
    }

    @Override
    public List<CupomDesconto> listarTodos() {
        String sql = "SELECT id_cupom, codigo, percentual, ativo FROM cupom_desconto ORDER BY codigo";
        List<CupomDesconto> lista = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao listar cupons de desconto: " + e.getMessage(), e);
        }
    }

    static CupomDesconto mapear(ResultSet rs) throws SQLException {
        return new CupomDesconto(
                rs.getInt("id_cupom"),
                rs.getString("codigo"),
                rs.getBigDecimal("percentual"),
                rs.getBoolean("ativo")
        );
    }
}
