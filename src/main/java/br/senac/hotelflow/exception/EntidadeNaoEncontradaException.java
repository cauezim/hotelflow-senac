package br.senac.hotelflow.exception;

/**
 * Lançada quando uma entidade solicitada (Reserva, Hóspede, Quarto, Fatura ou Cupom) não existe.
 *
 * <p>Rastreabilidade: {@code [Atende RN02..RN05, RN13, RN14]}
 */
public class EntidadeNaoEncontradaException extends HotelFlowException {

    public EntidadeNaoEncontradaException(String mensagem) {
        super(mensagem);
    }
}
