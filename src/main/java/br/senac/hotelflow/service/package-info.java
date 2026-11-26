/**
 * Camada de Serviços de Aplicação ({@code service}): orquestra casos de uso multi-entidade,
 * valida permissões de perfil ({@code RN01, D04}), coordena transações atômicas JDBC
 * ({@code RN09, RN16, RN17}), emissão textual de recibos ({@code RN18}) e relatórios ({@code RN20}).
 *
 * <p>Rastreabilidade: {@code [Atende RN01..RN20, D01..D06]}
 */
package br.senac.hotelflow.service;
