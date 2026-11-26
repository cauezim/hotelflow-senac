package br.senac.hotelflow.exception;

/**
 * Lançada quando as credenciais de login ou senha informadas são inválidas.
 *
 * <p>Rastreabilidade: {@code [Atende RN01]}
 */
public class AutenticacaoException extends HotelFlowException {

    public AutenticacaoException(String mensagem) {
        super(mensagem);
    }
}
