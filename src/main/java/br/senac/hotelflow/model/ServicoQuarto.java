package br.senac.hotelflow.model;

import br.senac.hotelflow.exception.RegraNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Solicitação e registro de serviço de quarto (ex.: lavanderia, refeição extra) vinculado à estadia.
 *
 * <p>Rastreabilidade: {@code [Atende RN11, RN13]}
 */
public class ServicoQuarto {

    private Integer idServico;
    private LocalDateTime dataHoraSolicitacao;
    private String descricao;
    private BigDecimal valor;

    public ServicoQuarto(String descricao, BigDecimal valor) {
        this(null, LocalDateTime.now(), descricao, valor);
    }

    public ServicoQuarto(Integer idServico, LocalDateTime dataHoraSolicitacao, String descricao, BigDecimal valor) {
        this.idServico = idServico;
        if (descricao == null || descricao.isBlank()) {
            throw new RegraNegocioException("A descrição do serviço de quarto é obrigatória.");
        }
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException("O valor do serviço de quarto deve ser maior que zero.");
        }
        this.dataHoraSolicitacao = dataHoraSolicitacao != null ? dataHoraSolicitacao : LocalDateTime.now();
        this.descricao = descricao.trim();
        this.valor = valor.setScale(2, RoundingMode.HALF_UP);
    }

    public Integer getIdServico() {
        return idServico;
    }

    public void setIdServico(Integer idServico) {
        this.idServico = idServico;
    }

    public LocalDateTime getDataHoraSolicitacao() {
        return dataHoraSolicitacao;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    @Override
    public String toString() {
        return descricao + " (R$ " + valor + ")";
    }
}
