package com.logistica.roteirizacao.exception;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.logistica.roteirizacao.dto.ErroResponse;

/**
 * Converte qualquer falha em {@code {status, erro, campo, mensagem}}.
 * As mensagens dizem o que está errado e como corrigir, para a GUI poder
 * mostrá-las diretamente ao usuário.
 */
@RestControllerAdvice
public class TratadorErrosApi {

    private static final Logger log = LoggerFactory.getLogger(TratadorErrosApi.class);

    /** Exceções da própria aplicação (400, 404, 409...). */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErroResponse> tratarApi(ApiException e) {
        return responder(e.getTipo(), e.getCampo(), e.getMessage());
    }

    /** Bean Validation no corpo (@Valid). Devolve o primeiro campo, na ordem em que foi declarado. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException e) {
        Object alvo = e.getBindingResult().getTarget();
        List<String> ordem = alvo == null ? List.of()
                : Arrays.stream(alvo.getClass().getDeclaredFields()).map(Field::getName).toList();
        FieldError primeiro = e.getBindingResult().getFieldErrors().stream()
                .min(Comparator.comparingInt(f -> posicao(ordem, f.getField())))
                .orElse(null);
        if (primeiro == null) {
            return responder(TipoErro.VALIDACAO, null, "Requisição inválida.");
        }
        return responder(TipoErro.VALIDACAO, primeiro.getField(), primeiro.getDefaultMessage());
    }

    /** JSON mal formado ou valor de tipo errado (ex.: critério que não existe). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarCorpoIlegivel(HttpMessageNotReadableException e) {
        if (e.getCause() instanceof InvalidFormatException invalido && !invalido.getPath().isEmpty()) {
            String campo = caminho(invalido);
            Class<?> tipo = invalido.getTargetType();
            if (tipo != null && tipo.isEnum()) {
                String aceitos = Arrays.stream(tipo.getEnumConstants())
                        .map(Object::toString)
                        .collect(Collectors.joining(" ou "));
                String mensagem = "criterio".equals(campo)
                        ? "Critério inválido. Use " + aceitos + "."
                        : "Valor inválido para " + campo + ". Use " + aceitos + ".";
                return responder(TipoErro.VALIDACAO, campo, mensagem);
            }
            return responder(TipoErro.VALIDACAO, campo, "Valor inválido para " + campo + ".");
        }
        if (e.getCause() instanceof JsonMappingException mapeamento && !mapeamento.getPath().isEmpty()) {
            String campo = caminho(mapeamento);
            return responder(TipoErro.VALIDACAO, campo, "Valor com formato inválido para " + campo + ".");
        }
        return responder(TipoErro.VALIDACAO, null,
                "Corpo da requisição ausente ou mal formado. Envie um JSON válido.");
    }

    /** Ex.: GET /rotas/abc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tratarTipoParametro(MethodArgumentTypeMismatchException e) {
        return responder(TipoErro.VALIDACAO, e.getName(),
                "O valor '" + e.getValue() + "' não é válido para " + e.getName() + ". Informe um número inteiro.");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErroResponse> tratarArquivoAusente(MissingServletRequestPartException e) {
        String parte = e.getRequestPartName();
        return responder(TipoErro.VALIDACAO, parte,
                "Envie o arquivo " + parte + ".csv no campo '" + parte + "' do formulário.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErroResponse> tratarArquivoGrande(MaxUploadSizeExceededException e) {
        return responder(TipoErro.VALIDACAO, null, "Arquivo maior que o limite permitido para upload.");
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErroResponse> tratarMultipart(MultipartException e) {
        return responder(TipoErro.VALIDACAO, null,
                "Envie os arquivos como multipart/form-data, nos campos 'pontos' e 'vias'.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResponse> tratarMediaType(HttpMediaTypeNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ErroResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), TipoErro.VALIDACAO.name(), null,
                        "Tipo de conteúdo não suportado: " + e.getContentType() + "."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> tratarMetodo(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErroResponse(HttpStatus.METHOD_NOT_ALLOWED.value(), TipoErro.VALIDACAO.name(), null,
                        "Método " + e.getMethod() + " não é aceito neste endereço."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> tratarRotaInexistente(NoResourceFoundException e) {
        return responder(TipoErro.NAO_ENCONTRADO, null, "Endereço /" + e.getResourcePath() + " não existe.");
    }

    /** Banco fora do ar, conexão recusada etc. */
    @ExceptionHandler({DataAccessResourceFailureException.class, CannotCreateTransactionException.class})
    public ResponseEntity<ErroResponse> tratarBancoIndisponivel(Exception e) {
        log.error("Banco de dados indisponível", e);
        return responder(TipoErro.INDISPONIVEL, null, "Banco de dados indisponível. Tente novamente em instantes.");
    }

    /** Qualquer outra falha: não expõe detalhes internos, mas registra no log. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarInesperado(Exception e) {
        log.error("Erro inesperado", e);
        return responder(TipoErro.INTERNO, null, "Erro inesperado no servidor. Tente novamente; se persistir, avise a equipe.");
    }

    private static ResponseEntity<ErroResponse> responder(TipoErro tipo, String campo, String mensagem) {
        return ResponseEntity.status(tipo.getStatus()).body(ErroResponse.de(tipo, campo, mensagem));
    }

    private static String caminho(JsonMappingException e) {
        return e.getPath().stream()
                .map(r -> r.getFieldName() != null ? r.getFieldName() : "[" + r.getIndex() + "]")
                .collect(Collectors.joining("."));
    }

    private static int posicao(List<String> ordem, String campo) {
        int i = ordem.indexOf(campo);
        return i < 0 ? Integer.MAX_VALUE : i;
    }
}
