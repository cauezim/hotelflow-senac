package br.senac.hotelflow.model.enums;

/**
 * Eixo ortogonal de governança e higienização de um {@code Quarto}.
 *
 * <p>Rastreabilidade: {@code [Atende RN12, RN17, D01]}
 */
public enum StatusLimpeza {

    SUJO("Sujo"),
    EM_LIMPEZA("Em Limpeza"),
    LIMPO("Limpo");

    private final String descricao;

    StatusLimpeza(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
