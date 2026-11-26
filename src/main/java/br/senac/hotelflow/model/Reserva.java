package br.senac.hotelflow.model;

import br.senac.hotelflow.exception.RegraNegocioException;
import br.senac.hotelflow.model.enums.StatusLimpeza;
import br.senac.hotelflow.model.enums.StatusReserva;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Agregado central da hospedagem, controlando o ciclo de estados ({@code E6}), conflitos em
 * intervalo semiaberto ({@code D02}), estadia efetiva ({@code D05}) e despesas de estadia.
 *
 * <p>Rastreabilidade: {@code [Atende RN05..RN11, RN13, RN17, RN19, RN20, D01, D02, D05]}
 */
public class Reserva {

    private Integer idReserva;
    private Hospede hospede;
    private Quarto quarto;
    private LocalDate dataCheckInPrevista;
    private LocalDate dataCheckOutPrevista;
    private LocalDateTime dataHoraCheckIn;
    private LocalDateTime dataHoraCheckOut;
    private StatusReserva status;

    private final List<ConsumoFrigobar> consumos = new ArrayList<>();
    private final List<ServicoQuarto> servicos = new ArrayList<>();
    private Fatura fatura;

    /**
     * Construtor de negócio para criação inicial de uma reserva em status {@code PENDENTE}.
     *
     * <p>Rastreabilidade: {@code [Atende RN05, RN07]}
     */
    public Reserva(Hospede hospede, Quarto quarto,
                   LocalDate dataCheckInPrevista, LocalDate dataCheckOutPrevista) {
        this(null, hospede, quarto, dataCheckInPrevista, dataCheckOutPrevista,
                null, null, StatusReserva.PENDENTE);
    }

    /**
     * Construtor completo para hidratação a partir do banco de dados ({@code ReservaDAO}).
     */
    public Reserva(Integer idReserva, Hospede hospede, Quarto quarto,
                   LocalDate dataCheckInPrevista, LocalDate dataCheckOutPrevista,
                   LocalDateTime dataHoraCheckIn, LocalDateTime dataHoraCheckOut,
                   StatusReserva status) {
        if (hospede == null) {
            throw new RegraNegocioException("O hóspede titular da reserva é obrigatório.");
        }
        if (quarto == null) {
            throw new RegraNegocioException("O quarto vinculado à reserva é obrigatório.");
        }
        if (dataCheckInPrevista == null || dataCheckOutPrevista == null) {
            throw new RegraNegocioException("As datas previstas de check-in e check-out são obrigatórias.");
        }
        if (dataCheckOutPrevista.isBefore(dataCheckInPrevista)) {
            throw new RegraNegocioException("A data prevista de check-out não pode ser anterior à data prevista de check-in.");
        }
        if (status == null) {
            throw new RegraNegocioException("O status da reserva é obrigatório.");
        }
        if (dataHoraCheckOut != null && (dataHoraCheckIn == null || dataHoraCheckOut.isBefore(dataHoraCheckIn))) {
            throw new RegraNegocioException("A data/hora efetiva de saída não pode anteceder o check-in.");
        }

        this.idReserva = idReserva;
        this.hospede = hospede;
        this.quarto = quarto;
        this.dataCheckInPrevista = dataCheckInPrevista;
        this.dataCheckOutPrevista = dataCheckOutPrevista;
        this.dataHoraCheckIn = dataHoraCheckIn;
        this.dataHoraCheckOut = dataHoraCheckOut;
        this.status = status;

        this.hospede.vincularReserva(this);
    }

    /**
     * Confirma uma reserva {@code PENDENTE} após verificação externa de conflito na agenda.
     *
     * <p>Rastreabilidade: {@code [Atende RN06, RN07]}
     */
    public void confirmar() {
        if (this.status != StatusReserva.PENDENTE) {
            throw new RegraNegocioException(
                    "Somente reservas Pendentes podem ser confirmadas (status atual: " + status.getDescricao() + ").");
        }
        this.status = StatusReserva.CONFIRMADA;
    }

    /**
     * Cancela uma reserva antes do início da estadia ({@code PENDENTE} ou {@code CONFIRMADA}).
     *
     * <p>Rastreabilidade: {@code [Atende RN07]}
     */
    public void cancelar() {
        if (this.status != StatusReserva.PENDENTE && this.status != StatusReserva.CONFIRMADA) {
            throw new RegraNegocioException(
                    "Não é permitido cancelar uma reserva em status '" + status.getDescricao() + "'.");
        }
        this.status = StatusReserva.CANCELADA;
    }

    /**
     * Efetiva a entrada do hóspede, exigindo reserva {@code CONFIRMADA} e quarto {@code DISPONIVEL} e {@code LIMPO}.
     *
     * <p>Rastreabilidade: {@code [Atende RN07, RN09, D01, D05]}
     */
    public void realizarCheckIn(LocalDateTime entrada) {
        if (this.status != StatusReserva.CONFIRMADA) {
            throw new RegraNegocioException(
                    "O Check-in exige reserva Confirmada (status atual: " + status.getDescricao() + ").");
        }
        if (entrada == null) {
            throw new RegraNegocioException("A data/hora efetiva de check-in é obrigatória.");
        }
        this.quarto.ocupar();
        this.dataHoraCheckIn = entrada;
        this.status = StatusReserva.EM_ANDAMENTO;
    }

    /**
     * Finaliza a estadia após quitação integral da fatura, liberando o quarto como {@code SUJO}.
     *
     * <p>Rastreabilidade: {@code [Atende RN07, RN16, RN17, RN19, D05]}
     */
    public void finalizar(LocalDateTime saida) {
        if (this.status != StatusReserva.EM_ANDAMENTO) {
            throw new RegraNegocioException(
                    "Somente reservas Em Andamento podem ser finalizadas (status atual: " + status.getDescricao() + ").");
        }
        if (this.fatura == null || !this.fatura.estaQuitada()) {
            throw new RegraNegocioException(
                    "Não é permitido finalizar o Check-out enquanto a fatura não estiver integralmente quitada.");
        }
        if (saida == null || (dataHoraCheckIn != null && saida.isBefore(dataHoraCheckIn))) {
            throw new RegraNegocioException("A data/hora de check-out informada é inválida.");
        }
        this.dataHoraCheckOut = saida;
        this.status = StatusReserva.FINALIZADA;
        this.quarto.liberar();
        this.quarto.atualizarLimpeza(StatusLimpeza.SUJO);
    }

    /**
     * Avalia sobreposição de agendamento segundo a regra de intervalo semiaberto {@code [ini, fim)} ({@code D02}).
     * Saída no dia X permite nova entrada no mesmo dia X sem conflito.
     *
     * <p>Rastreabilidade: {@code [Atende RN06, D02]}
     */
    public boolean possuiConflitoPeriodo(LocalDate inicioProposto, LocalDate fimProposto) {
        if (inicioProposto == null || fimProposto == null || fimProposto.isBefore(inicioProposto)) {
            throw new RegraNegocioException("Período inválido para verificação de disponibilidade.");
        }
        if (this.status != StatusReserva.CONFIRMADA && this.status != StatusReserva.EM_ANDAMENTO) {
            return false;
        }
        LocalDate fimEfetivoExistente = this.dataCheckOutPrevista.equals(this.dataCheckInPrevista)
                ? this.dataCheckInPrevista.plusDays(1)
                : this.dataCheckOutPrevista;
        LocalDate fimEfetivoProposto = fimProposto.equals(inicioProposto)
                ? inicioProposto.plusDays(1)
                : fimProposto;

        return this.dataCheckInPrevista.isBefore(fimEfetivoProposto)
                && fimEfetivoExistente.isAfter(inicioProposto);
    }

    /**
     * Calcula a quantidade de diárias considerando a entrada efetiva ({@code D05}) e mínimo de 1 diária ({@code D02}).
     *
     * <p>Rastreabilidade: {@code [Atende RN13, D02, D05]}
     */
    public long calcularDiarias(LocalDate dataSaidaReferencia) {
        LocalDate dataBaseEntrada = (this.dataHoraCheckIn != null)
                ? this.dataHoraCheckIn.toLocalDate()
                : this.dataCheckInPrevista;
        LocalDate dataBaseSaida = (dataSaidaReferencia != null)
                ? dataSaidaReferencia
                : ((this.dataHoraCheckOut != null) ? this.dataHoraCheckOut.toLocalDate() : this.dataCheckOutPrevista);

        long dias = ChronoUnit.DAYS.between(dataBaseEntrada, dataBaseSaida);
        return Math.max(1L, dias);
    }

    /**
     * Soma todos os subtotais de consumo de frigobar lançados na reserva.
     *
     * <p>Rastreabilidade: {@code [Atende RN10, RN13]}
     */
    public BigDecimal calcularTotalConsumos() {
        return consumos.stream()
                .map(ConsumoFrigobar::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Soma todos os valores de serviços de quarto solicitados na reserva.
     *
     * <p>Rastreabilidade: {@code [Atende RN11, RN13]}
     */
    public BigDecimal calcularTotalServicos() {
        return servicos.stream()
                .map(ServicoQuarto::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Lança um item de frigobar na estadia ativa (bloqueado se a fatura já tiver sido emitida).
     *
     * <p>Rastreabilidade: {@code [Atende RN10, D06]}
     */
    public void adicionarConsumo(ConsumoFrigobar consumo) {
        if (consumo == null) {
            throw new RegraNegocioException("O consumo de frigobar não pode ser nulo.");
        }
        if (this.status != StatusReserva.EM_ANDAMENTO) {
            throw new RegraNegocioException("Consumos de frigobar só podem ser lançados em reservas Em Andamento.");
        }
        if (this.fatura != null) {
            throw new RegraNegocioException("Não é possível lançar novos consumos após a emissão da fatura de check-out.");
        }
        this.consumos.add(consumo);
    }

    /**
     * Registra um serviço de quarto na estadia ativa (bloqueado se a fatura já tiver sido emitida).
     *
     * <p>Rastreabilidade: {@code [Atende RN11, D06]}
     */
    public void adicionarServico(ServicoQuarto servico) {
        if (servico == null) {
            throw new RegraNegocioException("O serviço de quarto não pode ser nulo.");
        }
        if (this.status != StatusReserva.EM_ANDAMENTO) {
            throw new RegraNegocioException("Serviços de quarto só podem ser lançados em reservas Em Andamento.");
        }
        if (this.fatura != null) {
            throw new RegraNegocioException("Não é possível lançar novos serviços após a emissão da fatura de check-out.");
        }
        this.servicos.add(servico);
    }

    /**
     * Métodos auxiliares de hidratação para reconstrução do agregado pelos DAOs a partir do MySQL.
     */
    public void hidratarConsumo(ConsumoFrigobar consumo) {
        if (consumo != null) {
            this.consumos.add(consumo);
        }
    }

    public void hidratarServico(ServicoQuarto servico) {
        if (servico != null) {
            this.servicos.add(servico);
        }
    }

    void vincularFaturaInterna(Fatura fatura) {
        if (this.fatura != null && this.fatura != fatura) {
            throw new RegraNegocioException("Esta reserva já possui uma fatura vinculada.");
        }
        this.fatura = fatura;
    }

    public Integer getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(Integer idReserva) {
        this.idReserva = idReserva;
    }

    public Hospede getHospede() {
        return hospede;
    }

    public Quarto getQuarto() {
        return quarto;
    }

    public LocalDate getDataCheckInPrevista() {
        return dataCheckInPrevista;
    }

    public LocalDate getDataCheckOutPrevista() {
        return dataCheckOutPrevista;
    }

    public LocalDateTime getDataHoraCheckIn() {
        return dataHoraCheckIn;
    }

    public LocalDateTime getDataHoraCheckOut() {
        return dataHoraCheckOut;
    }

    public StatusReserva getStatus() {
        return status;
    }

    public List<ConsumoFrigobar> getConsumos() {
        return Collections.unmodifiableList(consumos);
    }

    public List<ServicoQuarto> getServicos() {
        return Collections.unmodifiableList(servicos);
    }

    public Fatura getFatura() {
        return fatura;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reserva that)) return false;
        if (idReserva != null && that.idReserva != null) {
            return Objects.equals(idReserva, that.idReserva);
        }
        return Objects.equals(hospede, that.hospede)
                && Objects.equals(quarto, that.quarto)
                && Objects.equals(dataCheckInPrevista, that.dataCheckInPrevista);
    }

    @Override
    public int hashCode() {
        return idReserva != null
                ? Objects.hash(idReserva)
                : Objects.hash(hospede, quarto, dataCheckInPrevista);
    }

    @Override
    public String toString() {
        return "Reserva{id=" + idReserva + ", quarto=" + quarto.getNumero()
                + ", hospede='" + hospede.getNomeCompleto() + "', status=" + status + "}";
    }
}
