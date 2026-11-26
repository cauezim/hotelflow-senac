package br.senac.hotelflow.exception;

/**
 * Exceção base verificada em tempo de execução para o domínio e serviços do HotelFlow.
 *
 * <p>Rastreabilidade: {@code [Atende RN01..RN20]}
 */
public class HotelFlowException extends RuntimeException {

    public HotelFlowException(String mensagem) {
        super(mensagem);
    }

    public HotelFlowException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
