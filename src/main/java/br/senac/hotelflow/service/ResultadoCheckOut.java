package br.senac.hotelflow.service;

import br.senac.hotelflow.model.Fatura;
import br.senac.hotelflow.model.Reserva;

/**
 * Objeto de transferência imutável (Java 17 {@code record}) com o desfecho de um lançamento
 * de pagamento no Check-out (parcial ou encerramento completo com recibo textual).
 *
 * <p>Rastreabilidade: {@code [Atende RN15, RN16, RN17, RN18]}
 */
public record ResultadoCheckOut(
        Fatura fatura,
        Reserva reserva,
        boolean checkOutEncerrado,
        String reciboTexto
) {
}
