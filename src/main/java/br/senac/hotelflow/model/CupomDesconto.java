package br.senac.hotelflow.model;

import br.senac.hotelflow.exception.RegraNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Cupom promocional percentual pré-cadastrado e auditável para concessão exclusiva pelo Gerente.
 *
 * <p>Rastreabilidade: {@code [Atende RN14, D03, D06]}
 */
public class CupomDesconto {

    private static final BigDecimal CEM = new BigDecimal("100.00");

    private Integer idCupom;
    private String codigo;
    private BigDecimal percentual;
    private boolean ativo;

    public CupomDesconto(String codigo, BigDecimal percentual) {
        this(null, codigo, percentual, true);
    }

    public CupomDesconto(Integer idCupom, String codigo, BigDecimal percentual, boolean ativo) {
        this.idCupom = idCupom;
        if (codigo == null || codigo.isBlank()) {
            throw new RegraNegocioException("O código do cupom de desconto é obrigatório.");
        }
        if (percentual == null
                || percentual.compareTo(BigDecimal.ZERO) <= 0
                || percentual.compareTo(CEM) >= 0) {
            throw new RegraNegocioException("O percentual do cupom deve ser maior que 0% e menor que 100%.");
        }
        this.codigo = codigo.trim().toUpperCase();
        this.percentual = percentual.setScale(2, RoundingMode.HALF_UP);
        this.ativo = ativo;
    }

    public void ativar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    /**
     * Verifica se o cupom encontra-se ativo e dentro da faixa percentual válida.
     */
    public boolean validar() {
        return ativo
                && percentual.compareTo(BigDecimal.ZERO) > 0
                && percentual.compareTo(CEM) < 0;
    }

    /**
     * Calcula o valor monetário de abatimento sobre o valor bruto informado.
     *
     * <p>Rastreabilidade: {@code [Atende RN14, D06]}
     */
    public BigDecimal calcularDesconto(BigDecimal valorBruto) {
        if (!validar()) {
            throw new RegraNegocioException("O cupom '" + codigo + "' está inativo e não pode ser aplicado.");
        }
        if (valorBruto == null || valorBruto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException("O valor bruto para cálculo do desconto deve ser positivo.");
        }
        return valorBruto
                .multiply(percentual)
                .divide(CEM, 2, RoundingMode.HALF_UP);
    }

    public Integer getIdCupom() {
        return idCupom;
    }

    public void setIdCupom(Integer idCupom) {
        this.idCupom = idCupom;
    }

    public String getCodigo() {
        return codigo;
    }

    public BigDecimal getPercentual() {
        return percentual;
    }

    public boolean isAtivo() {
        return ativo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CupomDesconto that)) return false;
        return Objects.equals(codigo, that.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }

    @Override
    public String toString() {
        return "CupomDesconto{codigo='" + codigo + "', percentual=" + percentual + "%, ativo=" + ativo + "}";
    }
}
