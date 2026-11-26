package br.senac.hotelflow.model;

import br.senac.hotelflow.exception.RegraNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Set;

/**
 * Classificação de padrão das acomodações ({@code Standard}, {@code Luxo} ou {@code Suíte})
 * que determina e permite atualizar a tarifa diária vigente.
 *
 * <p>Rastreabilidade: {@code [Atende RN03, RN04]}
 */
public class CategoriaQuarto {

    private static final Set<String> NOMES_VALIDOS = Set.of("STANDARD", "LUXO", "SUÍTE", "SUITE");

    private Integer idCategoria;
    private String nome;
    private BigDecimal tarifaDiaria;

    public CategoriaQuarto(String nome, BigDecimal tarifaDiaria) {
        this(null, nome, tarifaDiaria);
    }

    public CategoriaQuarto(Integer idCategoria, String nome, BigDecimal tarifaDiaria) {
        this.idCategoria = idCategoria;
        setNome(nome);
        atualizarTarifa(tarifaDiaria);
    }

    /**
     * Atualiza o valor da tarifa diária vigente para novas faturas desta categoria.
     *
     * <p>Rastreabilidade: {@code [Atende RN04]}
     *
     * @param novoValor novo valor positivo em reais
     */
    public final void atualizarTarifa(BigDecimal novoValor) {
        if (novoValor == null || novoValor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException("A tarifa diária da categoria deve ser maior que zero.");
        }
        this.tarifaDiaria = novoValor.setScale(2, RoundingMode.HALF_UP);
    }

    public Integer getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(Integer idCategoria) {
        this.idCategoria = idCategoria;
    }

    public String getNome() {
        return nome;
    }

    public final void setNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("O nome da categoria de quarto é obrigatório.");
        }
        String limpo = nome.trim();
        if (!NOMES_VALIDOS.contains(limpo.toUpperCase())) {
            throw new RegraNegocioException("Categoria inválida ('" + limpo + "'). Valores permitidos: Standard, Luxo ou Suíte.");
        }
        this.nome = normalizarNomeCanonica(limpo);
    }

    public BigDecimal getTarifaDiaria() {
        return tarifaDiaria;
    }

    private String normalizarNomeCanonica(String valor) {
        return switch (valor.toUpperCase()) {
            case "STANDARD" -> "Standard";
            case "LUXO" -> "Luxo";
            default -> "Suíte";
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CategoriaQuarto that)) return false;
        return Objects.equals(nome, that.nome);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nome);
    }

    @Override
    public String toString() {
        return "CategoriaQuarto{nome='" + nome + "', tarifaDiaria=R$ " + tarifaDiaria + "}";
    }
}
