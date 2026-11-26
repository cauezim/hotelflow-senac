package br.senac.hotelflow;

import br.senac.hotelflow.dao.HospedeDAO;
import br.senac.hotelflow.dao.QuartoDAO;
import br.senac.hotelflow.dao.ReservaDAO;
import br.senac.hotelflow.exception.RegraNegocioException;
import br.senac.hotelflow.model.CategoriaQuarto;
import br.senac.hotelflow.model.ConsumoFrigobar;
import br.senac.hotelflow.model.Hospede;
import br.senac.hotelflow.model.Quarto;
import br.senac.hotelflow.model.Reserva;
import br.senac.hotelflow.model.Usuario;
import br.senac.hotelflow.model.enums.PerfilUsuario;
import br.senac.hotelflow.model.enums.StatusReserva;
import br.senac.hotelflow.service.ReservaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários de regras de sobreposição de agenda ({@code RN06, D02}), confirmação com
 * e-mail automático ({@code RN08}), transições de estado ({@code RN07, RN09}) e histórico ({@code RN19}).
 *
 * <p>Rastreabilidade: {@code [Atende RN05, RN06, RN07, RN08, RN09, RN10, RN11, RN19, D01, D02, D05]}
 */
class ReservaConflitosECicloVidaTest {

    private Hospede criarHospedePadrao() {
        return new Hospede(1, "Mariana Santos", "39148271805", "11999998888", "mariana@email.com", "Av. Paulista, 100");
    }

    private Quarto criarQuartoPadrao() {
        CategoriaQuarto std = new CategoriaQuarto(1, "Standard", new BigDecimal("200.00"));
        return new Quarto(1, "101", 1, 2, std,
                br.senac.hotelflow.model.enums.StatusOcupacao.DISPONIVEL,
                br.senac.hotelflow.model.enums.StatusLimpeza.LIMPO);
    }

    @Test
    @DisplayName("RN06 / D02: Deve detectar sobreposição de datas e permitir check-in no mesmo dia de check-out anterior")
    void deveAvaliarConflitoIntervaloSemiabertoEDayUse() {
        Reserva confirmada = new Reserva(criarHospedePadrao(), criarQuartoPadrao(),
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 15));
        confirmada.confirmar();

        // Conflito interno: [12/11, 14/11) sobrepõe [10/11, 15/11)
        assertTrue(confirmada.possuiConflitoPeriodo(
                LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 14)));

        // Fronteira hoteleira D02: saída dia 15 permite nova entrada dia 15 sem conflito!
        assertFalse(confirmada.possuiConflitoPeriodo(
                LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 18)));

        // Ponto 1 da Revisão: conflito de reserva day-use no mesmo dia (15/11 a 15/11)
        Reserva dayUse = new Reserva(criarHospedePadrao(), criarQuartoPadrao(),
                LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 20));
        dayUse.confirmar();
        assertTrue(dayUse.possuiConflitoPeriodo(
                LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 20)));
        assertFalse(dayUse.possuiConflitoPeriodo(
                LocalDate.of(2026, 11, 21), LocalDate.of(2026, 11, 22)));
    }

    @Test
    @DisplayName("RN06 / RN08: Confirmar reserva deve disparar e-mail automático quando sem conflito e bloquear se houver conflito")
    void deveDispararEmailNaConfirmacaoEBloquearSeConflitante() {
        Usuario recepcionista = new Usuario("Fernanda", "recepcao", "Recepcao@123", PerfilUsuario.RECEPCIONISTA);
        Reserva reservaStub = new Reserva(10, criarHospedePadrao(), criarQuartoPadrao(),
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 4), null, null, StatusReserva.PENDENTE);

        AtomicInteger emailsEnviados = new AtomicInteger(0);

        ReservaDAO reservaDaoFake = new ReservaDAO(null) {
            boolean simularConflito = true;

            @Override
            public Optional<Reserva> buscarPorId(Integer id) {
                return Optional.of(reservaStub);
            }

            @Override
            public boolean existeConflitoPeriodo(int idQuarto, LocalDate ini, LocalDate fim, Integer ign) {
                if (simularConflito) {
                    simularConflito = false;
                    return true;
                }
                return false;
            }

            @Override
            public void atualizar(Reserva reserva) {
                // Stub em memória
            }
        };

        ReservaService service = new ReservaService(
                null, reservaDaoFake, new HospedeDAO(null), new QuartoDAO(null),
                (dest, assunto, corpo) -> emailsEnviados.incrementAndGet()
        );

        // 1ª tentativa com conflito: bloqueia e NÃO envia e-mail (RN06)
        assertThrows(RegraNegocioException.class, () -> service.confirmarReserva(recepcionista, 10));
        assertEquals(0, emailsEnviados.get());
        assertEquals(StatusReserva.PENDENTE, reservaStub.getStatus());

        // 2ª tentativa sem conflito: confirma e dispara 1 e-mail (RN07, RN08)
        service.confirmarReserva(recepcionista, 10);
        assertEquals(StatusReserva.CONFIRMADA, reservaStub.getStatus());
        assertEquals(1, emailsEnviados.get());
    }

    @Test
    @DisplayName("RN07 / RN09 / RN10 / RN19: Deve proteger transições inválidas e filtrar histórico apenas com FINALIZADA")
    void deveProtegerCicloDeVidaEConsultarHistorico() {
        Hospede hospede = criarHospedePadrao();
        Quarto quarto = criarQuartoPadrao();

        Reserva rAtiva = new Reserva(1, hospede, quarto,
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12), null, null, StatusReserva.PENDENTE);

        // Não permite check-in nem lançamento de consumo em reserva PENDENTE
        assertThrows(RegraNegocioException.class,
                () -> rAtiva.realizarCheckIn(LocalDateTime.of(2026, 11, 10, 14, 0)));
        assertThrows(RegraNegocioException.class,
                () -> rAtiva.adicionarConsumo(new ConsumoFrigobar("Água", 1, new BigDecimal("8.00"))));

        rAtiva.confirmar();
        rAtiva.realizarCheckIn(LocalDateTime.of(2026, 11, 10, 14, 0));
        assertEquals(StatusReserva.EM_ANDAMENTO, rAtiva.getStatus());

        // Reserva em andamento NÃO pode ser cancelada nem entra ainda no histórico de finalizadas (RN19)
        assertThrows(RegraNegocioException.class, rAtiva::cancelar);
        assertTrue(hospede.consultarHistorico().isEmpty());

        Reserva rFinalizada = new Reserva(2, hospede, criarQuartoPadrao(),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3),
                LocalDateTime.of(2026, 10, 1, 14, 0), LocalDateTime.of(2026, 10, 3, 11, 0),
                StatusReserva.FINALIZADA);

        assertEquals(1, hospede.consultarHistorico().size());
        assertEquals(rFinalizada, hospede.consultarHistorico().get(0));
    }
}
