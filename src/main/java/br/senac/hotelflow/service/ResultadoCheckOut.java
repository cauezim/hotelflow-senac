package br.senac.hotelflow.service;

import br.senac.hotelflow.model.Fatura;
import br.senac.hotelflow.model.Reserva;

/**
 * Classe que encapsula o resultado de um lançamento de pagamento no Check-out
 * (pagamento parcial ou encerramento completo com recibo textual).
 *
 * <p>Rastreabilidade: {@code [Atende RN15, RN16, RN17, RN18]}
 */
public class ResultadoCheckOut {

    private final Fatura fatura;
    private final Reserva reserva;
    private final boolean checkOutEncerrado;
    private final String reciboTexto;

    public ResultadoCheckOut(Fatura fatura, Reserva reserva,
                             boolean checkOutEncerrado, String reciboTexto) {
        this.fatura = fatura;
        this.reserva = reserva;
        this.checkOutEncerrado = checkOutEncerrado;
        this.reciboTexto = reciboTexto;
    }

    public Fatura getFatura() {
        return fatura;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public boolean isCheckOutEncerrado() {
        return checkOutEncerrado;
    }

    public String getReciboTexto() {
        return reciboTexto;
    }
}
