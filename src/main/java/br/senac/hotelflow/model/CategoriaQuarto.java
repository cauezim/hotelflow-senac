package br.senac.hotelflow.model;

import br.senac.hotelflow.exception.RegraNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Classificação de padrão das acomodações ({@code Standard}, {@code Luxo} ou {@code Suíte})
 * que determina e permite atualizar a tarifa diária vigente.
 *
 * <p>Rastreabilidade: {@code [Atende RN03, RN04]}
 */
public class CategoriaQuarto {

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
        if (nome == null || nome.trim().isEmpty()) {
            throw new RegraNegocioException("O nome da categoria de quarto é obrigatório.");
        }
        String limpo = nome.trim();
        String maiusculo = limpo.toUpperCase();
        switch (maiusculo) {
            case "STANDARD":
                this.nome = "Standard";
                break;
            case "LUXO":
                this.nome = "Luxo";
                break;
            case "SUÍTE":
            case "SUITE":
                this.nome = "Suíte";
                break;
            default:
                throw new RegraNegocioException(
                        "Categoria inválida ('" + limpo + "'). Valores permitidos: Standard, Luxo ou Suíte.");
        }
    }

    public BigDecimal getTarifaDiaria() {
        return tarifaDiaria;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CategoriaQuarto)) {
            return false;
        }
        CategoriaQuarto outra = (CategoriaQuarto) o;
        return Objects.equals(this.nome, outra.nome);
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
