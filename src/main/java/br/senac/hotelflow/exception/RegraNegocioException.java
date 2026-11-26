package br.senac.hotelflow.exception;

/**
 * Lançada quando uma invariante ou regra de negócio do domínio hoteleiro é violada
 * (ex.: conflito de reserva RN06, quarto sujo no check-in D01, desconto após pagamento parcial D06).
 *
 * <p>Rastreabilidade: {@code [Atende RN04..RN18, D01..D06]}
 */
public class RegraNegocioException extends HotelFlowException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
