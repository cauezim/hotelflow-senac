package br.senac.hotelflow.model.enums;

/**
 * Estados financeiros do ciclo de liquidação de uma {@code Fatura}.
 *
 * <p>Rastreabilidade: {@code [Atende RN13, RN16]}
 */
public enum StatusFatura {

    AGUARDANDO_PAGAMENTO("Aguardando Pagamento"),
    PARCIALMENTE_PAGA("Parcialmente Paga"),
    PAGA("Paga");

    private final String descricao;

    StatusFatura(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
