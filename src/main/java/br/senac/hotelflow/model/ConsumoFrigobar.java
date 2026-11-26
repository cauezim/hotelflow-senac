package br.senac.hotelflow.model;

import br.senac.hotelflow.exception.RegraNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Item de consumo de frigobar lançado na conta de uma {@link Reserva} durante a estadia.
 *
 * <p>Rastreabilidade: {@code [Atende RN10, RN13]}
 */
public class ConsumoFrigobar {

    private Integer idConsumo;
    private String produto;
    private int quantidade;
    private BigDecimal valorUnitario;
    private LocalDateTime dataHoraRegistro;

    public ConsumoFrigobar(String produto, int quantidade, BigDecimal valorUnitario) {
        this(null, produto, quantidade, valorUnitario, LocalDateTime.now());
    }

    public ConsumoFrigobar(Integer idConsumo, String produto, int quantidade,
                           BigDecimal valorUnitario, LocalDateTime dataHoraRegistro) {
        this.idConsumo = idConsumo;
        if (produto == null || produto.isBlank()) {
            throw new RegraNegocioException("A descrição do produto de frigobar é obrigatória.");
        }
        if (quantidade <= 0) {
            throw new RegraNegocioException("A quantidade consumida deve ser maior que zero.");
        }
        if (valorUnitario == null || valorUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException("O valor unitário do item de frigobar deve ser maior que zero.");
        }
        this.produto = produto.trim();
        this.quantidade = quantidade;
        this.valorUnitario = valorUnitario.setScale(2, RoundingMode.HALF_UP);
        this.dataHoraRegistro = dataHoraRegistro != null ? dataHoraRegistro : LocalDateTime.now();
    }

    /**
     * Calcula o subtotal financeiro do lançamento ({@code quantidade * valorUnitario}).
     *
     * <p>Rastreabilidade: {@code [Atende RN10, RN13]}
     */
    public BigDecimal calcularSubtotal() {
        return valorUnitario
                .multiply(BigDecimal.valueOf(quantidade))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Integer getIdConsumo() {
        return idConsumo;
    }

    public void setIdConsumo(Integer idConsumo) {
        this.idConsumo = idConsumo;
    }

    public String getProduto() {
        return produto;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }

    public LocalDateTime getDataHoraRegistro() {
        return dataHoraRegistro;
    }

    @Override
    public String toString() {
        return quantidade + "x " + produto + " (R$ " + valorUnitario + " = R$ " + calcularSubtotal() + ")";
    }
}
