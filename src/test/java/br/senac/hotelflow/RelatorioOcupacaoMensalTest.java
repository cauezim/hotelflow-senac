package br.senac.hotelflow;

import br.senac.hotelflow.model.CategoriaQuarto;
import br.senac.hotelflow.model.Hospede;
import br.senac.hotelflow.model.Quarto;
import br.senac.hotelflow.model.Reserva;
import br.senac.hotelflow.model.enums.StatusLimpeza;
import br.senac.hotelflow.model.enums.StatusOcupacao;
import br.senac.hotelflow.model.enums.StatusReserva;
import br.senac.hotelflow.service.RelatorioOcupacaoMensal;
import br.senac.hotelflow.service.RelatorioOcupacaoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários do cálculo gerencial de Taxa de Ocupação Mensal ({@code RN20}), cobrindo
 * estadias contidas no mês, estadias que atravessam virada de mês e estadias {@code EM_ANDAMENTO}.
 *
 * <p>Rastreabilidade: {@code [Atende RN20, D05]}
 */
class RelatorioOcupacaoMensalTest {

    @Test
    @DisplayName("RN20 / D05: Deve recortar com exatidão estadias que atravessam meses e estadias ainda Em Andamento")
    void deveCalcularOcupacaoComViradaDeMesEReservaEmAndamento() {
        CategoriaQuarto std = new CategoriaQuarto(1, "Standard", new BigDecimal("200.00"));
        Quarto q101 = new Quarto(1, "101", 1, 2, std, StatusOcupacao.DISPONIVEL, StatusLimpeza.LIMPO);
        Quarto q102 = new Quarto(2, "102", 1, 2, std, StatusOcupacao.OCUPADO, StatusLimpeza.LIMPO);
        Hospede hospede = new Hospede(1, "Cliente Teste", "12345678909", "11999990000", "c@email.com", "Rua X");

        // Estadia 1 (FINALIZADA atravessando Outubro -> Novembro: 29/10 a 03/11)
        // Outubro = 3 diárias (29, 30, 31); Novembro = 2 diárias (01, 02)
        Reserva rViradaMes = new Reserva(1, hospede, q101,
                LocalDate.of(2026, 10, 29), LocalDate.of(2026, 11, 3),
                LocalDateTime.of(2026, 10, 29, 14, 0), LocalDateTime.of(2026, 11, 3, 11, 0),
                StatusReserva.FINALIZADA);

        // Estadia 2 (EM_ANDAMENTO iniciada em 24/11, sem checkout ainda; referência hoje = 27/11 -> 3 diárias)
        Reserva rEmAndamento = new Reserva(2, hospede, q102,
                LocalDate.of(2026, 11, 24), LocalDate.of(2026, 11, 30),
                LocalDateTime.of(2026, 11, 24, 15, 0), null,
                StatusReserva.EM_ANDAMENTO);

        RelatorioOcupacaoService service = new RelatorioOcupacaoService(null, null);
        LocalDate hojeRef = LocalDate.of(2026, 11, 27);

        // Valida Novembro/2026 (30 dias * 2 quartos = 60 quartos-dia; ocupados = 2 + 3 = 5 quartos-dia -> 8,33%)
        RelatorioOcupacaoMensal relNov = service.calcularOcupacaoEmMemoria(
                List.of(q101, q102), List.of(rViradaMes, rEmAndamento), YearMonth.of(2026, 11), hojeRef);

        assertEquals(60L, relNov.capacidadeTotalQuartosDia());
        assertEquals(5L, relNov.totalQuartosDiaOcupados());
        assertEquals(new BigDecimal("8.33"), relNov.taxaOcupacaoPercentual());

        // Valida Outubro/2026 (31 dias * 2 quartos = 62 quartos-dia; ocupados = 3 quartos-dia da rViradaMes -> 4,84%)
        RelatorioOcupacaoMensal relOut = service.calcularOcupacaoEmMemoria(
                List.of(q101, q102), List.of(rViradaMes, rEmAndamento), YearMonth.of(2026, 10), hojeRef);

        assertEquals(62L, relOut.capacidadeTotalQuartosDia());
        assertEquals(3L, relOut.totalQuartosDiaOcupados());
        assertEquals(new BigDecimal("4.84"), relOut.taxaOcupacaoPercentual());
    }
}
