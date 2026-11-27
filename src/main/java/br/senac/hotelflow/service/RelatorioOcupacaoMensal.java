package br.senac.hotelflow.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

/**
 * Classe de resultado consolidado e detalhado por quarto do Relatório Gerencial
 * de Taxa de Ocupação Mensal.
 *
 * <p>Rastreabilidade: {@code [Atende RN20, D05]}
 */
public class RelatorioOcupacaoMensal {

    private final YearMonth competencia;
    private final int totalQuartosCadastrados;
    private final int diasNoMesConsiderados;
    private final long capacidadeTotalQuartosDia;
    private final long totalQuartosDiaOcupados;
    private final BigDecimal taxaOcupacaoPercentual;
    private final List<OcupacaoPorQuarto> detalhamentoQuartos;

    public RelatorioOcupacaoMensal(YearMonth competencia,
                                   int totalQuartosCadastrados,
                                   int diasNoMesConsiderados,
                                   long capacidadeTotalQuartosDia,
                                   long totalQuartosDiaOcupados,
                                   BigDecimal taxaOcupacaoPercentual,
                                   List<OcupacaoPorQuarto> detalhamentoQuartos) {
        this.competencia = competencia;
        this.totalQuartosCadastrados = totalQuartosCadastrados;
        this.diasNoMesConsiderados = diasNoMesConsiderados;
        this.capacidadeTotalQuartosDia = capacidadeTotalQuartosDia;
        this.totalQuartosDiaOcupados = totalQuartosDiaOcupados;
        this.taxaOcupacaoPercentual = taxaOcupacaoPercentual;
        this.detalhamentoQuartos = Collections.unmodifiableList(detalhamentoQuartos);
    }

    public YearMonth getCompetencia() {
        return competencia;
    }

    public int getTotalQuartosCadastrados() {
        return totalQuartosCadastrados;
    }

    public int getDiasNoMesConsiderados() {
        return diasNoMesConsiderados;
    }

    public long getCapacidadeTotalQuartosDia() {
        return capacidadeTotalQuartosDia;
    }

    public long getTotalQuartosDiaOcupados() {
        return totalQuartosDiaOcupados;
    }

    public BigDecimal getTaxaOcupacaoPercentual() {
        return taxaOcupacaoPercentual;
    }

    public List<OcupacaoPorQuarto> getDetalhamentoQuartos() {
        return detalhamentoQuartos;
    }

    /**
     * Linha analítica de ocupação individual de um quarto no mês.
     */
    public static class OcupacaoPorQuarto {

        private final String numeroQuarto;
        private final String categoria;
        private final long diariasOcupadas;
        private final BigDecimal percentualQuarto;

        public OcupacaoPorQuarto(String numeroQuarto, String categoria,
                                 long diariasOcupadas, BigDecimal percentualQuarto) {
            this.numeroQuarto = numeroQuarto;
            this.categoria = categoria;
            this.diariasOcupadas = diariasOcupadas;
            this.percentualQuarto = percentualQuarto;
        }

        public String getNumeroQuarto() {
            return numeroQuarto;
        }

        public String getCategoria() {
            return categoria;
        }

        public long getDiariasOcupadas() {
            return diariasOcupadas;
        }

        public BigDecimal getPercentualQuarto() {
            return percentualQuarto;
        }
    }
}
