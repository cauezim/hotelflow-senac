package br.senac.hotelflow.service;

import br.senac.hotelflow.exception.RegraNegocioException;
import br.senac.hotelflow.model.ConsumoFrigobar;
import br.senac.hotelflow.model.Fatura;
import br.senac.hotelflow.model.Hospede;
import br.senac.hotelflow.model.Pagamento;
import br.senac.hotelflow.model.Quarto;
import br.senac.hotelflow.model.Reserva;
import br.senac.hotelflow.model.ServicoQuarto;
import br.senac.hotelflow.model.enums.StatusReserva;
import java.time.format.DateTimeFormatter;

/**
 * Serviço especializado na formatação e emissão do Recibo de Hospedagem em formato de texto,
 * executado exclusivamente após a quitação integral da fatura e conclusão do Check-out.
 *
 * <p>Rastreabilidade: {@code [Atende RN16, RN18]}
 */
public class GeradorReciboService {

    private static final DateTimeFormatter DATA_HORA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Gera o comprovante textual completo de quitação da estadia.
     *
     * <p>Rastreabilidade: {@code [Atende RN18]}
     */
    public String emitirReciboTexto(Fatura fatura) {
        if (fatura == null || !fatura.estaQuitada()) {
            throw new RegraNegocioException(
                    "Recibo bloqueado (RN18): a fatura ainda possui saldo pendente de quitação.");
        }
        Reserva reserva = fatura.getReserva();
        if (reserva == null || reserva.getStatus() != StatusReserva.FINALIZADA) {
            throw new RegraNegocioException(
                    "Recibo bloqueado: o encerramento da reserva deve ser concluído antes da emissão do recibo.");
        }

        Hospede hospede = reserva.getHospede();
        Quarto quarto = reserva.getQuarto();

        StringBuilder sb = new StringBuilder();
        sb.append("====================================================================\n");
        sb.append("              HOTELFLOW - RECIBO DE HOSPEDAGEM QUITADA              \n");
        sb.append("====================================================================\n");
        sb.append(String.format("Fatura Nº        : #%s  |  Reserva Nº: #%s%n",
                fatura.getIdFatura() != null ? fatura.getIdFatura() : 0,
                reserva.getIdReserva() != null ? reserva.getIdReserva() : 0));
        sb.append(String.format("Hóspede Titular  : %s%n", hospede.getNomeCompleto()));
        sb.append(String.format("Documento        : %s  |  Tel: %s%n", hospede.getDocumento(), hospede.getTelefone()));
        sb.append(String.format("Quarto           : %s (Andar %d - Categoria %s)%n",
                quarto.getNumero(), quarto.getAndar(), quarto.getCategoria().getNome()));
        sb.append(String.format("Check-in Efetivo : %s%n",
                reserva.getDataHoraCheckIn() != null ? reserva.getDataHoraCheckIn().format(DATA_HORA_BR) : "-"));
        sb.append(String.format("Check-out Efetivo: %s%n",
                reserva.getDataHoraCheckOut() != null ? reserva.getDataHoraCheckOut().format(DATA_HORA_BR) : "-"));
        sb.append("--------------------------------------------------------------------\n");
        sb.append("DISCRIMINAÇÃO DAS DESPESAS:\n");
        sb.append(String.format("  • Diárias (%d x R$ %s) ..................... R$ %s%n",
                fatura.getQuantidadeDiarias(),
                fatura.getTarifaDiariaAplicada(),
                fatura.calcularSubtotalDiarias()));

        if (!reserva.getConsumos().isEmpty()) {
            sb.append("  • Consumos de Frigobar:\n");
            for (ConsumoFrigobar item : reserva.getConsumos()) {
                sb.append(String.format("      - %dx %-32s R$ %s%n",
                        item.getQuantidade(), item.getProduto(), item.calcularSubtotal()));
            }
        } else {
            sb.append("  • Consumos de Frigobar ...................... R$ 0.00\n");
        }

        if (!reserva.getServicos().isEmpty()) {
            sb.append("  • Serviços de Quarto:\n");
            for (ServicoQuarto serv : reserva.getServicos()) {
                sb.append(String.format("      - %-36s R$ %s%n", serv.getDescricao(), serv.getValor()));
            }
        } else {
            sb.append("  • Serviços de Quarto ........................ R$ 0.00\n");
        }

        sb.append("--------------------------------------------------------------------\n");
        sb.append(String.format("VALOR BRUTO TOTAL   : R$ %s%n", fatura.calcularValorBruto()));
        if (fatura.getCupom() != null) {
            sb.append(String.format("DESCONTO (%s - %s%%) : -R$ %s%n",
                    fatura.getCupom().getCodigo(),
                    fatura.getPercentualDesconto(),
                    fatura.getValorDesconto()));
        } else {
            sb.append("DESCONTO APLICADO   : R$ 0.00\n");
        }
        sb.append(String.format("VALOR LÍQUIDO FINAL : R$ %s%n", fatura.calcularValorLiquido()));
        sb.append("--------------------------------------------------------------------\n");
        sb.append("PAGAMENTOS REGISTRADOS:\n");
        for (Pagamento pag : fatura.getPagamentos()) {
            sb.append(String.format("  • [%s] %-18s R$ %s%n",
                    pag.getDataPagamento().format(DATA_HORA_BR),
                    pag.getFormaPagamento().getDescricao(),
                    pag.getValor()));
        }
        sb.append(String.format("TOTAL PAGO          : R$ %s%n", fatura.calcularTotalPago()));
        sb.append(String.format("SALDO DEVEDOR       : R$ %s%n", fatura.calcularSaldoDevedor()));
        sb.append("SITUAÇÃO            : FATURA INTEGRALMENTE QUITADA\n");
        sb.append("====================================================================\n");
        sb.append("        Obrigado por hospedar-se conosco! Volte sempre!             \n");
        sb.append("====================================================================");
        return sb.toString();
    }
}
