package br.senac.hotelflow.model.enums;

import java.util.Arrays;
import java.util.List;

/**
 * Perfis de acesso dos colaboradores autenticáveis no sistema, encapsulando a matriz
 * hierárquica de permissões aprovada na decisão {@code D04}.
 *
 * <p>Rastreabilidade: {@code [Atende RN01, RN12, RN14, RN20, D04]}
 */
public enum PerfilUsuario {

    CAMAREIRA("Camareira", Arrays.asList(
            "AUTENTICAR",
            "ATUALIZAR_LIMPEZA"
    )),

    RECEPCIONISTA("Recepcionista", Arrays.asList(
            "AUTENTICAR",
            "MANTER_HOSPEDES",
            "CONSULTAR_HISTORICO",
            "CRIAR_RESERVA",
            "CANCELAR_RESERVA",
            "CONFIRMAR_RESERVA",
            "REALIZAR_CHECKIN",
            "LANCAR_CONSUMO",
            "REGISTRAR_SERVICO",
            "PROCESSAR_CHECKOUT",
            "REGISTRAR_PAGAMENTO",
            "EMITIR_RECIBO"
    )),

    GERENTE("Gerente", Arrays.asList(
            // Herda integralmente as operações de Recepcionista [Atende D04]
            "AUTENTICAR",
            "MANTER_HOSPEDES",
            "CONSULTAR_HISTORICO",
            "CRIAR_RESERVA",
            "CANCELAR_RESERVA",
            "CONFIRMAR_RESERVA",
            "REALIZAR_CHECKIN",
            "LANCAR_CONSUMO",
            "REGISTRAR_SERVICO",
            "PROCESSAR_CHECKOUT",
            "REGISTRAR_PAGAMENTO",
            "EMITIR_RECIBO",
            // Operações exclusivas da Gerência [Atende RN03, RN04, RN14, RN20]
            "MANTER_QUARTOS",
            "MANTER_CATEGORIAS_TARIFAS",
            "MANTER_CUPONS",
            "APLICAR_DESCONTO",
            "GERAR_RELATORIO_OCUPACAO"
    ));

    private final String descricao;
    private final List<String> operacoesPermitidas;

    PerfilUsuario(String descricao, List<String> operacoesPermitidas) {
        this.descricao = descricao;
        this.operacoesPermitidas = operacoesPermitidas;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean possuiPermissao(String operacao) {
        if (operacao == null || operacao.trim().isEmpty()) {
            return false;
        }
        return operacoesPermitidas.contains(operacao.trim().toUpperCase());
    }
}
