package br.senac.hotelflow;

import br.senac.hotelflow.exception.AutenticacaoException;
import br.senac.hotelflow.exception.AutorizacaoException;
import br.senac.hotelflow.model.Usuario;
import br.senac.hotelflow.model.enums.PerfilUsuario;
import br.senac.hotelflow.service.AutenticacaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários de autenticação SHA-256 e matriz hierárquica de controle de acesso por perfil.
 *
 * <p>Rastreabilidade: {@code [Atende RN01, RN12, RN14, RN20, D04]}
 */
class AutenticacaoEPermissoesTest {

    @Test
    @DisplayName("RN01: Deve autenticar colaborador com senha correta (hash SHA-256) e rejeitar senha incorreta")
    void deveAutenticarComHashSha256Correto() {
        Usuario gerente = new Usuario("Carlos Mendes", "gerente", "Gerente@123", PerfilUsuario.GERENTE);

        assertTrue(gerente.autenticar("Gerente@123"));
        assertFalse(gerente.autenticar("SenhaErrada"));
        assertFalse(gerente.autenticar(""));
        assertFalse(gerente.autenticar(null));
    }

    @Test
    @DisplayName("RN01 / D04: Gerente deve herdar todas as permissões de Recepcionista além das exclusivas")
    void gerenteDevePossuirPermissoesRecepcaoEGerencia() {
        Usuario gerente = new Usuario("Carlos", "gerente", "Gerente@123", PerfilUsuario.GERENTE);

        // Herdadas de Recepcionista (D04)
        assertTrue(gerente.possuiPermissao("MANTER_HOSPEDES"));
        assertTrue(gerente.possuiPermissao("CRIAR_RESERVA"));
        assertTrue(gerente.possuiPermissao("PROCESSAR_CHECKOUT"));

        // Exclusivas do Gerente (RN03, RN04, RN14, RN20)
        assertTrue(gerente.possuiPermissao("MANTER_QUARTOS"));
        assertTrue(gerente.possuiPermissao("MANTER_CATEGORIAS_TARIFAS"));
        assertTrue(gerente.possuiPermissao("APLICAR_DESCONTO"));
        assertTrue(gerente.possuiPermissao("GERAR_RELATORIO_OCUPACAO"));
    }

    @Test
    @DisplayName("RN01 / RN14 / RN20: Recepcionista deve ser bloqueada em operações exclusivas do Gerente e Camareira")
    void recepcionistaNaoDeveAcessarOperacoesGerenciaisOuGovernanca() {
        Usuario recepcionista = new Usuario("Fernanda", "recepcao", "Recepcao@123", PerfilUsuario.RECEPCIONISTA);

        assertDoesNotThrow(() -> AutenticacaoService.exigirPermissao(recepcionista, "REALIZAR_CHECKIN"));
        assertThrows(AutorizacaoException.class,
                () -> AutenticacaoService.exigirPermissao(recepcionista, "APLICAR_DESCONTO"));
        assertThrows(AutorizacaoException.class,
                () -> AutenticacaoService.exigirPermissao(recepcionista, "GERAR_RELATORIO_OCUPACAO"));
        assertThrows(AutorizacaoException.class,
                () -> AutenticacaoService.exigirPermissao(recepcionista, "ATUALIZAR_LIMPEZA"));
    }

    @Test
    @DisplayName("RN01 / RN12: Camareira deve acessar apenas governança de limpeza e operador nulo deve ser barrado")
    void camareiraRestritaAGovernancaEOperadorNuloBloqueado() {
        Usuario camareira = new Usuario("Luciana", "camareira", "Camareira@123", PerfilUsuario.CAMAREIRA);

        assertDoesNotThrow(() -> AutenticacaoService.exigirPermissao(camareira, "ATUALIZAR_LIMPEZA"));
        assertThrows(AutorizacaoException.class,
                () -> AutenticacaoService.exigirPermissao(camareira, "CRIAR_RESERVA"));
        assertThrows(AutenticacaoException.class,
                () -> AutenticacaoService.exigirPermissao(null, "ATUALIZAR_LIMPEZA"));
    }
}
