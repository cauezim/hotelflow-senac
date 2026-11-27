package br.senac.hotelflow;

import br.senac.hotelflow.dao.CupomDescontoDAO;
import br.senac.hotelflow.dao.FaturaDAO;
import br.senac.hotelflow.dao.PagamentoDAO;
import br.senac.hotelflow.dao.QuartoDAO;
import br.senac.hotelflow.dao.ReservaDAO;
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
import br.senac.hotelflow.model.Usuario;
import br.senac.hotelflow.model.enums.FormaPagamento;
import br.senac.hotelflow.model.enums.PerfilUsuario;
import br.senac.hotelflow.model.enums.StatusFatura;
import br.senac.hotelflow.model.enums.StatusLimpeza;
import br.senac.hotelflow.model.enums.StatusOcupacao;
import br.senac.hotelflow.model.enums.StatusReserva;
import br.senac.hotelflow.service.CheckOutService;
import br.senac.hotelflow.service.GeradorReciboService;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários do faturamento consolidado, congelamento contábil ({@code D06}), prorrogação
 * de diárias, cupom de desconto, pagamentos fracionados, bloqueio concorrente e recibo textual.
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

        reserva.adicionarConsumo(new ConsumoFrigobar("Água Mineral", 4, new BigDecimal("8.00")));
        reserva.adicionarConsumo(new ConsumoFrigobar("Refrigerante", 4, new BigDecimal("12.00")));
        reserva.adicionarServico(new ServicoQuarto("Lavanderia", new BigDecimal("60.00")));
        reserva.adicionarServico(new ServicoQuarto("Jantar Executivo", new BigDecimal("60.00")));

        Fatura fatura = new Fatura(reserva, LocalDateTime.of(2026, 11, 13, 11, 0));
        assertEquals(3L, fatura.getQuantidadeDiarias());
        assertEquals(new BigDecimal("800.00"), fatura.calcularValorBruto());
        assertEquals(StatusFatura.AGUARDANDO_PAGAMENTO, fatura.getStatus());

        std.atualizarTarifa(new BigDecimal("950.00"));
        assertEquals(new BigDecimal("200.00"), fatura.getTarifaDiariaAplicada());
        assertEquals(new BigDecimal("800.00"), fatura.calcularValorBruto());

        CupomDesconto cupom = new CupomDesconto(1, "SENAC10", new BigDecimal("10.00"), true);
        fatura.aplicarDesconto(cupom);
        assertEquals(new BigDecimal("80.00"), fatura.getValorDesconto());
        assertEquals(new BigDecimal("720.00"), fatura.calcularValorLiquido());

        fatura.registrarPagamento(new Pagamento(new BigDecimal("300.00"),
                LocalDateTime.of(2026, 11, 13, 11, 5), FormaPagamento.PIX));
        assertEquals(StatusFatura.PARCIALMENTE_PAGA, fatura.getStatus());
        assertEquals(new BigDecimal("420.00"), fatura.calcularSaldoDevedor());
        assertFalse(fatura.estaQuitada());

        CupomDesconto outroCupom = new CupomDesconto(2, "VIP20", new BigDecimal("20.00"), true);
        assertThrows(RegraNegocioException.class, () -> fatura.aplicarDesconto(outroCupom));
        assertThrows(RegraNegocioException.class, () -> reserva.finalizar(LocalDateTime.of(2026, 11, 13, 11, 10)));
        GeradorReciboService reciboService = new GeradorReciboService();
        assertThrows(RegraNegocioException.class, () -> reciboService.emitirReciboTexto(fatura));

        assertThrows(RegraNegocioException.class, () -> fatura.registrarPagamento(
                new Pagamento(new BigDecimal("500.00"), LocalDateTime.now(), FormaPagamento.CARTAO_CREDITO)));

        fatura.registrarPagamento(new Pagamento(new BigDecimal("420.00"),
                LocalDateTime.of(2026, 11, 13, 11, 15), FormaPagamento.CARTAO_CREDITO));
        assertTrue(fatura.estaQuitada());
        assertEquals(StatusFatura.PAGA, fatura.getStatus());

        reserva.finalizar(LocalDateTime.of(2026, 11, 13, 11, 20));
        assertEquals(StatusReserva.FINALIZADA, reserva.getStatus());
        assertEquals(StatusOcupacao.DISPONIVEL, quarto.getStatusOcupacao());
        assertEquals(StatusLimpeza.SUJO, quarto.getStatusLimpeza());

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

        Fatura fatura = new Fatura(dayUse, LocalDateTime.of(2026, 11, 20, 18, 0));
        assertEquals(1L, fatura.getQuantidadeDiarias());
        assertEquals(new BigDecimal("350.00"), fatura.calcularValorBruto());

        fatura.registrarPagamento(new Pagamento(new BigDecimal("100.00"),
                LocalDateTime.of(2026, 11, 20, 18, 5), FormaPagamento.DINHEIRO));
        assertTrue(fatura.atualizarSeProrrogada(LocalDate.of(2026, 11, 22)));
        assertEquals(2L, fatura.getQuantidadeDiarias());
        assertEquals(new BigDecimal("700.00"), fatura.calcularValorBruto());
        assertEquals(new BigDecimal("600.00"), fatura.calcularSaldoDevedor());
    }

    @Test
    @DisplayName("RN15 / RN16 (Concorrência): CheckOutService deve bloquear reserva, quarto e fatura antes de consultar saldo e impedir pagamento duplicado")
    void deveBloquearFaturaAntesDeConsultarSaldoEImpedirPagamentoConcorrenteDuplicado() {
        Usuario recepcionista = new Usuario("Carlos", "recep2", "Recepcao@123", PerfilUsuario.RECEPCIONISTA);
        CategoriaQuarto std = new CategoriaQuarto(1, "Standard", new BigDecimal("200.00"));
        Quarto quarto = new Quarto(7, "301", 3, 2, std, StatusOcupacao.DISPONIVEL, StatusLimpeza.LIMPO);
        Hospede hospede = new Hospede(3, "Lucas Mendes", "39148271805", "47999990000", "lucas@email.com", "Rua XV");

        Reserva reserva = new Reserva(40, hospede, quarto,
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12), null, null, StatusReserva.PENDENTE);
        reserva.confirmar();
        reserva.realizarCheckIn(LocalDateTime.of(2026, 11, 10, 14, 0));

        Fatura fatura = new Fatura(reserva, LocalDateTime.of(2026, 11, 12, 10, 0));
        fatura.setIdFatura(99);

        List<String> ordemBloqueioELeitura = new ArrayList<>();

        ReservaDAO reservaDaoFake = new ReservaDAO(null) {
            @Override
            public Integer bloquearObterIdQuarto(Connection conn, int idReserva) {
                ordemBloqueioELeitura.add("LOCK_RESERVA_" + idReserva);
                return quarto.getIdQuarto();
            }

            @Override
            public Reserva buscarPorId(Connection conn, Integer id) {
                ordemBloqueioELeitura.add("READ_SALDO_APOS_LOCK");
                return reserva;
            }

            @Override
            public void atualizar(Reserva r) {
                ordemBloqueioELeitura.add("UPDATE_RESERVA_" + r.getStatus().name());
            }
        };

        QuartoDAO quartoDaoFake = new QuartoDAO(null) {
            @Override
            public void bloquearPorId(Connection conn, int idQuarto) {
                ordemBloqueioELeitura.add("LOCK_QUARTO_" + idQuarto);
            }

            @Override
            public void atualizar(Quarto q) {
                ordemBloqueioELeitura.add("UPDATE_QUARTO_" + q.getStatusOcupacao().name());
            }
        };

        FaturaDAO faturaDaoFake = new FaturaDAO(null) {
            @Override
            public void bloquearPorReserva(Connection conn, int idReserva) {
                ordemBloqueioELeitura.add("LOCK_FATURA_RESERVA_" + idReserva);
            }

            @Override
            public void atualizar(Fatura f) {
                ordemBloqueioELeitura.add("UPDATE_FATURA_" + f.getStatus().name());
            }
        };

        PagamentoDAO pagamentoDaoFake = new PagamentoDAO(null) {
            @Override
            public Pagamento salvar(int idFatura, Pagamento p) {
                p.setIdPagamento(1);
                return p;
            }
        };

        CheckOutService service = new CheckOutService(
                null, reservaDaoFake, faturaDaoFake, pagamentoDaoFake,
                quartoDaoFake, new CupomDescontoDAO(null), new GeradorReciboService()
        );

        service.registrarPagamento(recepcionista, 40, new BigDecimal("400.00"),
                FormaPagamento.PIX, LocalDateTime.of(2026, 11, 12, 10, 5));

        assertEquals("LOCK_RESERVA_40", ordemBloqueioELeitura.get(0));
        assertEquals("LOCK_QUARTO_7", ordemBloqueioELeitura.get(1));
        assertEquals("LOCK_FATURA_RESERVA_40", ordemBloqueioELeitura.get(2));
        assertEquals("READ_SALDO_APOS_LOCK", ordemBloqueioELeitura.get(3));

        assertThrows(RegraNegocioException.class, () -> service.registrarPagamento(
                recepcionista, 40, new BigDecimal("400.00"),
                FormaPagamento.CARTAO_CREDITO, LocalDateTime.of(2026, 11, 12, 10, 6)));
    }
}
