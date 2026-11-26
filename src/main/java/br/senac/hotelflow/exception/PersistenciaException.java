package br.senac.hotelflow.exception;

/**
 * Encapsula falhas técnicas de acesso a dados (SQLException / JDBC), impedindo vazamento
 * de detalhes de infraestrutura SQL para as camadas de serviço e apresentação.
 *
 * <p>Rastreabilidade: {@code [Atende RN01..RN20]}
 */
public class PersistenciaException extends HotelFlowException {

    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
