package br.senac.hotelflow.model.enums;

/**
 * Eixo ortogonal de ocupação física de um {@code Quarto}.
 *
 * <p>Rastreabilidade: {@code [Atende RN09, RN17]}
 */
public enum StatusOcupacao {

    DISPONIVEL("Disponível"),
    OCUPADO("Ocupado");

    private final String descricao;

    StatusOcupacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
