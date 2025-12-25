package back.kalender.domain.payment.service;

import back.kalender.domain.payment.dto.request.TossPaymentCancelRequest;
import back.kalender.domain.payment.dto.request.TossPaymentConfirmRequest;
import back.kalender.domain.payment.dto.response.PaymentGatewayCancelResponse;
import back.kalender.domain.payment.dto.response.PaymentGatewayConfirmResponse;
import back.kalender.domain.payment.dto.response.TossPaymentCancelResponse;
import back.kalender.domain.payment.dto.response.TossPaymentConfirmResponse;
import back.kalender.domain.payment.dto.response.TossPaymentErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

// 토스페이먼츠 결제 게이트웨이 구현체
@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentGateway implements PaymentGateway {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${custom.payment.toss.secretKey:}")
    private String secretKey;

    @Value("${custom.payment.toss.baseUrl:https://api.tosspayments.com/v1}")
    private String baseUrl;

    private static final String CONFIRM_ENDPOINT = "/payments/confirm";
    private static final String CANCEL_ENDPOINT = "/payments/{paymentKey}/cancel";
    
    /**
     * 시크릿 키 유효성 검증
     * 애플리케이션 시작 시 또는 첫 API 호출 시 검증
     */
    private void validateSecretKey() {
        if (secretKey == null || secretKey.trim().isEmpty()) {
            log.error("[TossPaymentGateway] 시크릿 키가 설정되지 않았습니다. TOSS_PAYMENT_SECRET_KEY 환경 변수를 설정해주세요.");
            throw new IllegalStateException("토스페이먼츠 시크릿 키가 설정되지 않았습니다. TOSS_PAYMENT_SECRET_KEY 환경 변수를 확인해주세요.");
        }
        
        // 토스페이먼츠 시크릿 키 형식 검증 (test_sk_ 또는 live_sk_로 시작)
        if (!secretKey.startsWith("test_sk_") && !secretKey.startsWith("live_sk_")) {
            log.warn("[TossPaymentGateway] 시크릿 키 형식이 올바르지 않습니다. test_sk_ 또는 live_sk_로 시작해야 합니다.");
            // 경고만 하고 계속 진행 (실제 API 호출 시 검증됨)
        }
    }

    @Override
    public PaymentGatewayConfirmResponse confirm(String paymentKey, String orderId, Integer amount) {
        // 시크릿 키 검증
        validateSecretKey();
        
        String url = baseUrl + CONFIRM_ENDPOINT;
        
        TossPaymentConfirmRequest request = new TossPaymentConfirmRequest(
                paymentKey,
                orderId,
                amount
        );

        HttpHeaders headers = createHeaders();
        HttpEntity<TossPaymentConfirmRequest> entity = new HttpEntity<>(request, headers);

        try {
            log.info("[TossPaymentGateway] 결제 승인 요청 - paymentKey: {}, orderId: {}, amount: {}", 
                    paymentKey, orderId, amount);
            
            ResponseEntity<TossPaymentConfirmResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    TossPaymentConfirmResponse.class
            );

            TossPaymentConfirmResponse responseBody = response.getBody();
            if (responseBody == null) {
                log.error("[TossPaymentGateway] 결제 승인 응답이 null입니다 - paymentKey: {}", paymentKey);
                return PaymentGatewayConfirmResponse.ofFailure("RESPONSE_NULL", "응답이 null입니다");
            }

            // 성공 응답 처리
            if ("DONE".equals(responseBody.status())) {
                log.info("[TossPaymentGateway] 결제 승인 성공 - paymentKey: {}, orderId: {}", 
                        responseBody.paymentKey(), responseBody.orderId());
                return PaymentGatewayConfirmResponse.ofSuccess(responseBody.paymentKey());
            } else {
                // 실패 응답 처리
                String failCode = responseBody.failReason() != null ? responseBody.failReason().code() : "UNKNOWN";
                String failMessage = responseBody.failReason() != null ? responseBody.failReason().message() : "결제 승인 실패";
                log.warn("[TossPaymentGateway] 결제 승인 실패 - paymentKey: {}, failCode: {}, failMessage: {}", 
                        paymentKey, failCode, failMessage);
                return PaymentGatewayConfirmResponse.ofFailure(failCode, failMessage);
            }

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // 4xx, 5xx 에러 처리
            log.error("[TossPaymentGateway] 결제 승인 HTTP 에러 - paymentKey: {}, status: {}, body: {}", 
                    paymentKey, e.getStatusCode(), e.getResponseBodyAsString(), e);
            
            // 401 Unauthorized: 시크릿 키가 잘못된 경우
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.error("[TossPaymentGateway] 인증 실패 - 시크릿 키가 유효하지 않습니다. TOSS_PAYMENT_SECRET_KEY를 확인해주세요.");
                return PaymentGatewayConfirmResponse.ofFailure(
                        "UNAUTHORIZED", 
                        "토스페이먼츠 인증에 실패했습니다. 시크릿 키를 확인해주세요."
                );
            }
            
            String failCode = "HTTP_ERROR";
            String failMessage = "결제 게이트웨이 오류가 발생했습니다";
            
            try {
                // 에러 응답 파싱 시도
                String responseBody = e.getResponseBodyAsString();
                if (responseBody != null && !responseBody.isEmpty()) {
                    TossPaymentErrorResponse errorResponse = objectMapper.readValue(
                            responseBody, TossPaymentErrorResponse.class);
                    failCode = errorResponse.code();
                    failMessage = errorResponse.message();
                }
            } catch (Exception parseException) {
                log.warn("[TossPaymentGateway] 에러 응답 파싱 실패", parseException);
            }
            
            return PaymentGatewayConfirmResponse.ofFailure(failCode, failMessage);
            
        } catch (ResourceAccessException e) {
            // 타임아웃 또는 네트워크 에러
            log.error("[TossPaymentGateway] 결제 승인 타임아웃/네트워크 에러 - paymentKey: {}", paymentKey, e);
            throw new RuntimeException("결제 게이트웨이 타임아웃", e);
            
        } catch (Exception e) {
            log.error("[TossPaymentGateway] 결제 승인 예외 발생 - paymentKey: {}", paymentKey, e);
            return PaymentGatewayConfirmResponse.ofFailure("UNKNOWN_ERROR", "알 수 없는 오류가 발생했습니다");
        }
    }

    @Override
    public PaymentGatewayCancelResponse cancel(String paymentKey, String cancelReason) {
        // 시크릿 키 검증
        validateSecretKey();
        
        String url = baseUrl + CANCEL_ENDPOINT.replace("{paymentKey}", paymentKey);
        
        TossPaymentCancelRequest request = new TossPaymentCancelRequest(cancelReason);

        HttpHeaders headers = createHeaders();
        HttpEntity<TossPaymentCancelRequest> entity = new HttpEntity<>(request, headers);

        try {
            log.info("[TossPaymentGateway] 결제 취소 요청 - paymentKey: {}, cancelReason: {}", 
                    paymentKey, cancelReason);
            
            ResponseEntity<TossPaymentCancelResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    TossPaymentCancelResponse.class
            );

            TossPaymentCancelResponse responseBody = response.getBody();
            if (responseBody == null) {
                log.error("[TossPaymentGateway] 결제 취소 응답이 null입니다 - paymentKey: {}", paymentKey);
                return PaymentGatewayCancelResponse.ofFailure("RESPONSE_NULL", "응답이 null입니다");
            }

            // 성공 응답 처리
            if ("CANCELED".equals(responseBody.status()) || "DONE".equals(responseBody.status())) {
                log.info("[TossPaymentGateway] 결제 취소 성공 - paymentKey: {}, orderId: {}", 
                        responseBody.paymentKey(), responseBody.orderId());
                return PaymentGatewayCancelResponse.ofSuccess();
            } else {
                // 실패 응답 처리
                String failCode = responseBody.failReason() != null ? responseBody.failReason().code() : "UNKNOWN";
                String failMessage = responseBody.failReason() != null ? responseBody.failReason().message() : "결제 취소 실패";
                log.warn("[TossPaymentGateway] 결제 취소 실패 - paymentKey: {}, failCode: {}, failMessage: {}", 
                        paymentKey, failCode, failMessage);
                return PaymentGatewayCancelResponse.ofFailure(failCode, failMessage);
            }

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // 4xx, 5xx 에러 처리
            log.error("[TossPaymentGateway] 결제 취소 HTTP 에러 - paymentKey: {}, status: {}, body: {}", 
                    paymentKey, e.getStatusCode(), e.getResponseBodyAsString(), e);
            
            // 401 Unauthorized: 시크릿 키가 잘못된 경우
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.error("[TossPaymentGateway] 인증 실패 - 시크릿 키가 유효하지 않습니다. TOSS_PAYMENT_SECRET_KEY를 확인해주세요.");
                return PaymentGatewayCancelResponse.ofFailure(
                        "UNAUTHORIZED", 
                        "토스페이먼츠 인증에 실패했습니다. 시크릿 키를 확인해주세요."
                );
            }
            
            String failCode = "HTTP_ERROR";
            String failMessage = "결제 게이트웨이 오류가 발생했습니다";
            
            try {
                // 에러 응답 파싱 시도
                String responseBody = e.getResponseBodyAsString();
                if (responseBody != null && !responseBody.isEmpty()) {
                    TossPaymentErrorResponse errorResponse = objectMapper.readValue(
                            responseBody, TossPaymentErrorResponse.class);
                    failCode = errorResponse.code();
                    failMessage = errorResponse.message();
                }
            } catch (Exception parseException) {
                log.warn("[TossPaymentGateway] 에러 응답 파싱 실패", parseException);
            }
            
            return PaymentGatewayCancelResponse.ofFailure(failCode, failMessage);
            
        } catch (ResourceAccessException e) {
            // 타임아웃 또는 네트워크 에러
            log.error("[TossPaymentGateway] 결제 취소 타임아웃/네트워크 에러 - paymentKey: {}", paymentKey, e);
            throw new RuntimeException("결제 게이트웨이 타임아웃", e);
            
        } catch (Exception e) {
            log.error("[TossPaymentGateway] 결제 취소 예외 발생 - paymentKey: {}", paymentKey, e);
            return PaymentGatewayCancelResponse.ofFailure("UNKNOWN_ERROR", "알 수 없는 오류가 발생했습니다");
        }
    }

    /**
     * 토스페이먼츠 API 인증 헤더 생성
     * Basic Auth: Secret Key를 Base64 인코딩하여 사용
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8)));
        return headers;
    }
}
