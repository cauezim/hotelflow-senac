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
import java.util.Arrays;
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

        Reserva rViradaMes = new Reserva(1, hospede, q101,
                LocalDate.of(2026, 10, 29), LocalDate.of(2026, 11, 3),
                LocalDateTime.of(2026, 10, 29, 14, 0), LocalDateTime.of(2026, 11, 3, 11, 0),
                StatusReserva.FINALIZADA);

        Reserva rEmAndamento = new Reserva(2, hospede, q102,
                LocalDate.of(2026, 11, 24), LocalDate.of(2026, 11, 30),
                LocalDateTime.of(2026, 11, 24, 15, 0), null,
                StatusReserva.EM_ANDAMENTO);

        RelatorioOcupacaoService service = new RelatorioOcupacaoService(null, null);
        LocalDate hojeRef = LocalDate.of(2026, 11, 27);

        RelatorioOcupacaoMensal relNov = service.calcularOcupacaoEmMemoria(
                Arrays.asList(q101, q102), Arrays.asList(rViradaMes, rEmAndamento), YearMonth.of(2026, 11), hojeRef);

        assertEquals(60L, relNov.getCapacidadeTotalQuartosDia());
        assertEquals(5L, relNov.getTotalQuartosDiaOcupados());
        assertEquals(new BigDecimal("8.33"), relNov.getTaxaOcupacaoPercentual());

        RelatorioOcupacaoMensal relOut = service.calcularOcupacaoEmMemoria(
                Arrays.asList(q101, q102), Arrays.asList(rViradaMes, rEmAndamento), YearMonth.of(2026, 10), hojeRef);

        assertEquals(62L, relOut.getCapacidadeTotalQuartosDia());
        assertEquals(3L, relOut.getTotalQuartosDiaOcupados());
        assertEquals(new BigDecimal("4.84"), relOut.getTaxaOcupacaoPercentual());
    }
}
