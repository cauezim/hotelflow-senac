package br.senac.hotelflow.exception;

/**
 * Lançada quando um usuário autenticado tenta executar uma operação restrita a outro perfil
 * (ex.: Recepcionista tentando aplicar cupom de desconto ou gerar relatório gerencial).
 *
 * <p>Rastreabilidade: {@code [Atende RN01, RN12, RN14, RN20, D04]}
 */
public class AutorizacaoException extends HotelFlowException {

    public AutorizacaoException(String mensagem) {
        super(mensagem);
    }
}
