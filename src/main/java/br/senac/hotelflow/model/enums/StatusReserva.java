package br.senac.hotelflow.model.enums;

/**
 * Estados do ciclo de vida de uma {@code Reserva}.
 *
 * <p>Rastreabilidade: {@code [Atende RN07]}
 */
public enum StatusReserva {

    PENDENTE("Pendente"),
    CONFIRMADA("Confirmada"),
    EM_ANDAMENTO("Em Andamento"),
    FINALIZADA("Finalizada"),
    CANCELADA("Cancelada");

    private final String descricao;

    StatusReserva(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
