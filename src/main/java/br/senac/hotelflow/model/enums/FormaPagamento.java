package br.senac.hotelflow.model.enums;

/**
 * Meios de pagamento aceitos na amortização ou quitação de faturas.
 *
 * <p>Rastreabilidade: {@code [Atende RN15]}
 */
public enum FormaPagamento {

    CARTAO_CREDITO("Cartão de Crédito"),
    PIX("PIX"),
    DINHEIRO("Dinheiro");

    private final String descricao;

    FormaPagamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
