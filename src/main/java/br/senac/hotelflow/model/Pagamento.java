package br.senac.hotelflow.model;

import br.senac.hotelflow.exception.RegraNegocioException;
import br.senac.hotelflow.model.enums.FormaPagamento;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Transação financeira individual (integral ou fracionada) registrada para amortizar uma {@link Fatura}.
 *
 * <p>Rastreabilidade: {@code [Atende RN15, RN16]}
 */
public class Pagamento {

    private Integer idPagamento;
    private BigDecimal valor;
    private LocalDateTime dataPagamento;
    private FormaPagamento formaPagamento;

    public Pagamento(BigDecimal valor, LocalDateTime dataPagamento, FormaPagamento formaPagamento) {
        this(null, valor, dataPagamento, formaPagamento);
    }

    public Pagamento(Integer idPagamento, BigDecimal valor,
                     LocalDateTime dataPagamento, FormaPagamento formaPagamento) {
        this.idPagamento = idPagamento;
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException("O valor do pagamento deve ser maior que zero.");
        }
        if (formaPagamento == null) {
            throw new RegraNegocioException("A forma de pagamento (Cartão de Crédito, PIX ou Dinheiro) é obrigatória.");
        }
        this.valor = valor.setScale(2, RoundingMode.HALF_UP);
        this.dataPagamento = dataPagamento != null ? dataPagamento : LocalDateTime.now();
        this.formaPagamento = formaPagamento;
    }

    public Integer getIdPagamento() {
        return idPagamento;
    }

    public void setIdPagamento(Integer idPagamento) {
        this.idPagamento = idPagamento;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDateTime getDataPagamento() {
        return dataPagamento;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    @Override
    public String toString() {
        return formaPagamento.getDescricao() + " - R$ " + valor;
    }
}
