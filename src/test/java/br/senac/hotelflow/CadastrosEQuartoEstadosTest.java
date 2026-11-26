package br.senac.hotelflow;

import br.senac.hotelflow.exception.RegraNegocioException;
import br.senac.hotelflow.model.CategoriaQuarto;
import br.senac.hotelflow.model.Hospede;
import br.senac.hotelflow.model.Quarto;
import br.senac.hotelflow.model.enums.StatusLimpeza;
import br.senac.hotelflow.model.enums.StatusOcupacao;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários de cadastros estruturais, normalização de documentos e máquina de estados
 * ortogonal de {@link Quarto}.
 *
 * <p>Rastreabilidade: {@code [Atende RN02, RN03, RN04, RN09, RN12, RN17, D01]}
 */
class CadastrosEQuartoEstadosTest {

    @Test
    @DisplayName("RN02: Deve padronizar CPF e Passaporte removendo pontuação para evitar duplicidade")
    void deveNormalizarDocumentoHospede() {
        Hospede h1 = new Hospede("Mariana Santos", "391.482.718-05", "11999998888", "mariana@email.com", "Rua A, 10");
        Hospede h2 = new Hospede("Mariana S.", "39148271805", "11999998888", "mariana@email.com", "Rua A, 10");

        assertEquals("39148271805", h1.getDocumento());
        assertEquals(h1, h2);
        assertEquals("ESPAA849201", Hospede.normalizarDocumento("esp-aa849201"));
    }

    @Test
    @DisplayName("RN03 / RN04: Deve aceitar apenas categorias Standard, Luxo e Suíte com tarifa positiva")
    void deveValidarCategoriasETarifas() {
        CategoriaQuarto luxo = new CategoriaQuarto("luxo", new BigDecimal("350.00"));
        assertEquals("Luxo", luxo.getNome());

        luxo.atualizarTarifa(new BigDecimal("390.50"));
        assertEquals(new BigDecimal("390.50"), luxo.getTarifaDiaria());

        assertThrows(RegraNegocioException.class,
                () -> new CategoriaQuarto("Presidencial", new BigDecimal("800.00")));
        assertThrows(RegraNegocioException.class,
                () -> luxo.atualizarTarifa(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("RN09 / RN12 / D01: Quarto sujo ou em limpeza deve bloquear check-in até higienização da Camareira")
    void deveControlarEstadosOrtogonaisDeQuarto() {
        CategoriaQuarto std = new CategoriaQuarto("Standard", new BigDecimal("200.00"));
        Quarto quarto = new Quarto(1, "102", 1, 2, std, StatusOcupacao.DISPONIVEL, StatusLimpeza.SUJO);

        // Quarto Disponível porém Sujo bloqueia Check-in (D01)
        assertFalse(quarto.estaAptoParaCheckIn());
        assertThrows(RegraNegocioException.class, quarto::ocupar);

        // Ciclo da Camareira: SUJO -> EM_LIMPEZA -> LIMPO (RN12)
        quarto.atualizarLimpeza(StatusLimpeza.EM_LIMPEZA);
        assertFalse(quarto.estaAptoParaCheckIn());

        quarto.atualizarLimpeza(StatusLimpeza.LIMPO);
        assertTrue(quarto.estaAptoParaCheckIn());

        // Check-in ocupa o quarto (RN09)
        quarto.ocupar();
        assertEquals(StatusOcupacao.OCUPADO, quarto.getStatusOcupacao());
        assertFalse(quarto.estaAptoParaCheckIn());
    }
}
