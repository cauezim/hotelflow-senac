package br.senac.hotelflow;

import br.senac.hotelflow.exception.RegraNegocioException;
import br.senac.hotelflow.model.CategoriaQuarto;
import br.senac.hotelflow.model.ConsumoFrigobar;
import br.senac.hotelflow.model.CupomDesconto;
import br.senac.hotelflow.model.Fatura;
import br.senac.hotelflow.model.Hospede;
import br.senac.hotelflow.model.Pagamento;
import br.senac.hotelflow.model.Quarto;
import br.senac.hotelflow.model.Reserva;
import br.senac.hotelflow.model.ServicoQuarto;
import br.senac.hotelflow.model.enums.FormaPagamento;
import br.senac.hotelflow.model.enums.StatusFatura;
import br.senac.hotelflow.model.enums.StatusLimpeza;
import br.senac.hotelflow.model.enums.StatusOcupacao;
import br.senac.hotelflow.model.enums.StatusReserva;
import br.senac.hotelflow.service.GeradorReciboService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários do faturamento consolidado, congelamento contábil ({@code D06}), prorrogação
 * de diárias, cupom de desconto, pagamentos fracionados, liberação pós-check-out e recibo textual.
 *
 * <p>Rastreabilidade: {@code [Atende RN10, RN11, RN13..RN18, D02, D03, D05, D06]}
 */
class FaturamentoPagamentosEReciboTest {

    @Test
    @DisplayName("RN13..RN18 / D06: Deve processar exemplo completo de faturamento, desconto, parcelamento, liberação e recibo")
    void deveProcessarCicloCompletoDeFaturamentoEPagamentoFracionado() {
        CategoriaQuarto std = new CategoriaQuarto(1, "Standard", new BigDecimal("200.00"));
        Quarto quarto = new Quarto(2, "102", 1, 3, std, StatusOcupacao.DISPONIVEL, StatusLimpeza.LIMPO);
        Hospede hospede = new Hospede(1, "Mariana Oliveira", "39148271805", "11998123344", "mariana@email.com", "Av. Paulista");

        Reserva reserva = new Reserva(1, hospede, quarto,
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 13), null, null, StatusReserva.PENDENTE);
        reserva.confirmar();
        reserva.realizarCheckIn(LocalDateTime.of(2026, 11, 10, 14, 0));

        // Despesas extras: Frigobar R$ 80,00 + Serviços R$ 120,00 (RN10, RN11)
        reserva.adicionarConsumo(new ConsumoFrigobar("Água Mineral", 4, new BigDecimal("8.00")));
        reserva.adicionarConsumo(new ConsumoFrigobar("Refrigerante", 4, new BigDecimal("12.00")));
        reserva.adicionarServico(new ServicoQuarto("Lavanderia", new BigDecimal("60.00")));
        reserva.adicionarServico(new ServicoQuarto("Jantar Executivo", new BigDecimal("60.00")));

        // Emissão da fatura no dia 13/11 (3 diárias x R$ 200 = R$ 600 + R$ 200 extras = R$ 800,00) [RN13]
        Fatura fatura = new Fatura(reserva, LocalDateTime.of(2026, 11, 13, 11, 0));
        assertEquals(3L, fatura.getQuantidadeDiarias());
        assertEquals(new BigDecimal("800.00"), fatura.calcularValorBruto());
        assertEquals(StatusFatura.AGUARDANDO_PAGAMENTO, fatura.getStatus());

        // Congelamento histórico (D06): reajustar tarifa da categoria após emissão NÃO altera a fatura
        std.atualizarTarifa(new BigDecimal("950.00"));
        assertEquals(new BigDecimal("200.00"), fatura.getTarifaDiariaAplicada());
        assertEquals(new BigDecimal("800.00"), fatura.calcularValorBruto());

        // Cupom SENAC10 (10%) aplicado antes de pagamentos -> Desconto R$ 80,00, Líquido R$ 720,00 [RN14]
        CupomDesconto cupom = new CupomDesconto(1, "SENAC10", new BigDecimal("10.00"), true);
        fatura.aplicarDesconto(cupom);
        assertEquals(new BigDecimal("80.00"), fatura.getValorDesconto());
        assertEquals(new BigDecimal("720.00"), fatura.calcularValorLiquido());

        // 1º Pagamento parcial PIX R$ 300,00 -> Saldo R$ 420,00, Status PARCIALMENTE_PAGA [RN15, RN16]
        fatura.registrarPagamento(new Pagamento(new BigDecimal("300.00"),
                LocalDateTime.of(2026, 11, 13, 11, 5), FormaPagamento.PIX));
        assertEquals(StatusFatura.PARCIALMENTE_PAGA, fatura.getStatus());
        assertEquals(new BigDecimal("420.00"), fatura.calcularSaldoDevedor());
        assertFalse(fatura.estaQuitada());

        // Bloqueios de regra: desconto após pagamento parcial (D06), encerramento com saldo e recibo precoce
        CupomDesconto outroCupom = new CupomDesconto(2, "VIP20", new BigDecimal("20.00"), true);
        assertThrows(RegraNegocioException.class, () -> fatura.aplicarDesconto(outroCupom));
        assertThrows(RegraNegocioException.class, () -> reserva.finalizar(LocalDateTime.of(2026, 11, 13, 11, 10)));
        GeradorReciboService reciboService = new GeradorReciboService();
        assertThrows(RegraNegocioException.class, () -> reciboService.emitirReciboTexto(fatura));

        // Bloqueia pagamento acima do saldo devedor (R$ 500 > R$ 420)
        assertThrows(RegraNegocioException.class, () -> fatura.registrarPagamento(
                new Pagamento(new BigDecimal("500.00"), LocalDateTime.now(), FormaPagamento.CARTAO_CREDITO)));

        // 2º Pagamento quitador Cartão R$ 420,00 -> Saldo R$ 0,00, Status PAGA
        fatura.registrarPagamento(new Pagamento(new BigDecimal("420.00"),
                LocalDateTime.of(2026, 11, 13, 11, 15), FormaPagamento.CARTAO_CREDITO));
        assertTrue(fatura.estaQuitada());
        assertEquals(StatusFatura.PAGA, fatura.getStatus());

        // Encerramento libera quarto imediatamente como DISPONIVEL e SUJO [RN17]
        reserva.finalizar(LocalDateTime.of(2026, 11, 13, 11, 20));
        assertEquals(StatusReserva.FINALIZADA, reserva.getStatus());
        assertEquals(StatusOcupacao.DISPONIVEL, quarto.getStatusOcupacao());
        assertEquals(StatusLimpeza.SUJO, quarto.getStatusLimpeza());

        // Emissão do recibo textual após encerramento [RN18]
        String recibo = reciboService.emitirReciboTexto(fatura);
        assertTrue(recibo.contains("Mariana Oliveira"));
        assertTrue(recibo.contains("SENAC10"));
        assertTrue(recibo.contains("720.00"));
        assertTrue(recibo.contains("FATURA INTEGRALMENTE QUITADA"));
    }

    @Test
    @DisplayName("D02 / D05 / D06: Day-use deve cobrar no mínimo 1 diária e prorrogação deve ajustar fatura aberta")
    void deveCobrarMinimoUmaDiariaEAtualizarProrrogacao() {
        CategoriaQuarto luxo = new CategoriaQuarto(2, "Luxo", new BigDecimal("350.00"));
        Quarto quarto = new Quarto(3, "201", 2, 2, luxo, StatusOcupacao.DISPONIVEL, StatusLimpeza.LIMPO);
        Hospede hospede = new Hospede(2, "Roberto Prado", "21463985040", "11987451020", "roberto@email.com", "Rua Augusta");

        Reserva dayUse = new Reserva(5, hospede, quarto,
                LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 20), null, null, StatusReserva.PENDENTE);
        dayUse.confirmar();
        dayUse.realizarCheckIn(LocalDateTime.of(2026, 11, 20, 10, 0));

        // Mesmo dia cobra mínimo de 1 diária (R$ 350,00) [D02]
        Fatura fatura = new Fatura(dayUse, LocalDateTime.of(2026, 11, 20, 18, 0));
        assertEquals(1L, fatura.getQuantidadeDiarias());
        assertEquals(new BigDecimal("350.00"), fatura.calcularValorBruto());

        // Se o hóspede pagar R$ 100 parcial e permanecer até 22/11 (2 diárias), prorrogação ajusta bruto p/ R$ 700
        fatura.registrarPagamento(new Pagamento(new BigDecimal("100.00"),
                LocalDateTime.of(2026, 11, 20, 18, 5), FormaPagamento.DINHEIRO));
        assertTrue(fatura.atualizarSeProrrogada(LocalDate.of(2026, 11, 22)));
        assertEquals(2L, fatura.getQuantidadeDiarias());
        assertEquals(new BigDecimal("700.00"), fatura.calcularValorBruto());
        assertEquals(new BigDecimal("600.00"), fatura.calcularSaldoDevedor());
    }
}
