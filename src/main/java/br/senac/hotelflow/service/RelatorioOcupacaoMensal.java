package br.senac.hotelflow.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * DTO imutável (Java 17 {@code record}) com o resultado consolidado e detalhado por quarto
 * do Relatório Gerencial de Taxa de Ocupação Mensal.
 *
 * <p>Rastreabilidade: {@code [Atende RN20, D05]}
 */
public record RelatorioOcupacaoMensal(
        YearMonth competencia,
        int totalQuartosCadastrados,
        int diasNoMesConsiderados,
        long capacidadeTotalQuartosDia,
        long totalQuartosDiaOcupados,
        BigDecimal taxaOcupacaoPercentual,
        List<OcupacaoPorQuarto> detalhamentoQuartos
) {

    public record OcupacaoPorQuarto(
            String numeroQuarto,
            String categoria,
            long diariasOcupadas,
            BigDecimal percentualQuarto
    ) {
    }
}
