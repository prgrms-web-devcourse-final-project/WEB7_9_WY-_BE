package back.kalender.domain.payment.service;

import back.kalender.domain.payment.dto.response.PaymentGatewayCancelResponse;
import back.kalender.domain.payment.dto.response.PaymentGatewayConfirmResponse;
import back.kalender.domain.payment.dto.response.TossPaymentCancelResponse;
import back.kalender.domain.payment.dto.response.TossPaymentConfirmResponse;
import back.kalender.domain.payment.dto.response.TossPaymentErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TossPaymentGateway 테스트")
class TossPaymentGatewayTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TossPaymentGateway tossPaymentGateway;

    private static final String TEST_SECRET_KEY = "test_secret_key";
    private static final String TEST_BASE_URL = "https://api.tosspayments.com/v1";
    private static final String TEST_PAYMENT_KEY = "tgen_20250101_abc123";
    private static final String TEST_ORDER_ID = "123";
    private static final Integer TEST_AMOUNT = 50000;
    private static final String TEST_CANCEL_REASON = "고객 요청";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tossPaymentGateway, "secretKey", TEST_SECRET_KEY);
        ReflectionTestUtils.setField(tossPaymentGateway, "baseUrl", TEST_BASE_URL);
    }

    @Nested
    @DisplayName("결제 승인 (confirm) 테스트")
    class ConfirmTest {

        @Test
        @DisplayName("결제 승인 성공")
        void confirm_Success() {
            // given
            TossPaymentConfirmResponse responseBody = new TossPaymentConfirmResponse(
                    TEST_PAYMENT_KEY,
                    TEST_ORDER_ID,
                    "DONE",
                    TEST_AMOUNT,
                    "카드",
                    "2025-01-01T12:00:00",
                    null
            );
            ResponseEntity<TossPaymentConfirmResponse> response = ResponseEntity.ok(responseBody);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(),
                    eq(TossPaymentConfirmResponse.class)
            )).thenReturn(response);

            // when
            PaymentGatewayConfirmResponse result = tossPaymentGateway.confirm(
                    TEST_PAYMENT_KEY, TEST_ORDER_ID, TEST_AMOUNT);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.paymentKey()).isEqualTo(TEST_PAYMENT_KEY);
            assertThat(result.failCode()).isNull();
            assertThat(result.failMessage()).isNull();

            verify(restTemplate, times(1)).exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(),
                    eq(TossPaymentConfirmResponse.class)
            );
        }

        @Test
        @DisplayName("결제 승인 실패 - 상태가 DONE이 아님")
        void confirm_Failure_StatusNotDone() {
            // given
            TossPaymentConfirmResponse.FailReason failReason = 
                    new TossPaymentConfirmResponse.FailReason("CARD_AUTH_FAILED", "카드 인증 실패");
            TossPaymentConfirmResponse responseBody = new TossPaymentConfirmResponse(
                    TEST_PAYMENT_KEY,
                    TEST_ORDER_ID,
                    "FAILED",
                    TEST_AMOUNT,
                    "카드",
                    null,
                    failReason
            );
            ResponseEntity<TossPaymentConfirmResponse> response = ResponseEntity.ok(responseBody);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(),
                    eq(TossPaymentConfirmResponse.class)
            )).thenReturn(response);

            // when
            PaymentGatewayConfirmResponse result = tossPaymentGateway.confirm(
                    TEST_PAYMENT_KEY, TEST_ORDER_ID, TEST_AMOUNT);

            // then
            assertThat(result.success()).isFalse();
            assertThat(result.paymentKey()).isNull();
            assertThat(result.failCode()).isEqualTo("CARD_AUTH_FAILED");
            assertThat(result.failMessage()).isEqualTo("카드 인증 실패");
        }

        @Test
        @DisplayName("결제 승인 실패 - 응답이 null")
        void confirm_Failure_NullResponse() {
            // given
            ResponseEntity<TossPaymentConfirmResponse> response = ResponseEntity.ok(null);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(),
                    eq(TossPaymentConfirmResponse.class)
            )).thenReturn(response);

            // when
            PaymentGatewayConfirmResponse result = tossPaymentGateway.confirm(
                    TEST_PAYMENT_KEY, TEST_ORDER_ID, TEST_AMOUNT);

            // then
            assertThat(result.success()).isFalse();
            assertThat(result.failCode()).isEqualTo("RESPONSE_NULL");
            assertThat(result.failMessage()).isEqualTo("응답이 null입니다");
        }

        @Test
        @DisplayName("결제 승인 실패 - HTTP 4xx 에러")
        void confirm_Failure_HttpClientError() throws Exception {
            // given
            String errorBody = "{\"code\":\"INVALID_PAYMENT_KEY\",\"message\":\"유효하지 않은 결제 키입니다\"}";
            HttpClientErrorException exception = new HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "Bad Request",
                    errorBody.getBytes(),
                    null
            );

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(),
                    eq(TossPaymentConfirmResponse.class)
            )).thenThrow(exception);

            TossPaymentErrorResponse errorResponse = new TossPaymentErrorResponse(
                    "INVALID_PAYMENT_KEY",
                    "유효하지 않은 결제 키입니다"
            );
            when(objectMapper.readValue(errorBody, TossPaymentErrorResponse.class))
                    .thenReturn(errorResponse);

            // when
            PaymentGatewayConfirmResponse result = tossPaymentGateway.confirm(
                    TEST_PAYMENT_KEY, TEST_ORDER_ID, TEST_AMOUNT);

            // then
            assertThat(result.success()).isFalse();
            assertThat(result.failCode()).isEqualTo("INVALID_PAYMENT_KEY");
            assertThat(result.failMessage()).isEqualTo("유효하지 않은 결제 키입니다");
        }

        @Test
        @DisplayName("결제 승인 실패 - 타임아웃")
        void confirm_Failure_Timeout() {
            // given
            ResourceAccessException exception = new ResourceAccessException("Connection timeout");

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(),
                    eq(TossPaymentConfirmResponse.class)
            )).thenThrow(exception);

            // when & then
            assertThatThrownBy(() -> tossPaymentGateway.confirm(
                    TEST_PAYMENT_KEY, TEST_ORDER_ID, TEST_AMOUNT))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("결제 게이트웨이 타임아웃");
        }

        @Test
        @DisplayName("결제 승인 실패 - HTTP 5xx 에러")
        void confirm_Failure_HttpServerError() throws Exception {
            // given
            String errorBody = "{\"code\":\"INTERNAL_SERVER_ERROR\",\"message\":\"서버 오류가 발생했습니다\"}";
            HttpServerErrorException exception = new HttpServerErrorException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal Server Error",
                    errorBody.getBytes(),
                    null
            );

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(),
                    eq(TossPaymentConfirmResponse.class)
            )).thenThrow(exception);

            TossPaymentErrorResponse errorResponse = new TossPaymentErrorResponse(
                    "INTERNAL_SERVER_ERROR",
                    "서버 오류가 발생했습니다"
            );
            when(objectMapper.readValue(errorBody, TossPaymentErrorResponse.class))
                    .thenReturn(errorResponse);

            // when
            PaymentGatewayConfirmResponse result = tossPaymentGateway.confirm(
                    TEST_PAYMENT_KEY, TEST_ORDER_ID, TEST_AMOUNT);

            // then
            assertThat(result.success()).isFalse();
            assertThat(result.failCode()).isEqualTo("INTERNAL_SERVER_ERROR");
            assertThat(result.failMessage()).isEqualTo("서버 오류가 발생했습니다");
        }
    }

    @Nested
    @DisplayName("시크릿 키 검증 테스트")
    class SecretKeyValidationTest {

        @Test
        @DisplayName("시크릿 키가 비어있을 때 예외 발생")
        void confirm_Failure_EmptySecretKey() {
            // given
            ReflectionTestUtils.setField(tossPaymentGateway, "secretKey", "");

            // when & then
            assertThatThrownBy(() -> tossPaymentGateway.confirm(
                    TEST_PAYMENT_KEY, TEST_ORDER_ID, TEST_AMOUNT))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("토스페이먼츠 시크릿 키가 설정되지 않았습니다");
        }

        @Test
        @DisplayName("시크릿 키가 null일 때 예외 발생")
        void confirm_Failure_NullSecretKey() {
            // given
            ReflectionTestUtils.setField(tossPaymentGateway, "secretKey", null);

            // when & then
            assertThatThrownBy(() -> tossPaymentGateway.confirm(
                    TEST_PAYMENT_KEY, TEST_ORDER_ID, TEST_AMOUNT))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("토스페이먼츠 시크릿 키가 설정되지 않았습니다");
        }

        @Test
        @DisplayName("잘못된 시크릿 키로 API 호출 시 401 에러 처리")
        void confirm_Failure_InvalidSecretKey() throws Exception {
            // given
            ReflectionTestUtils.setField(tossPaymentGateway, "secretKey", "invalid_key");
            
            String errorBody = "{\"code\":\"UNAUTHORIZED\",\"message\":\"인증에 실패했습니다\"}";
            HttpClientErrorException exception = new HttpClientErrorException(
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized",
                    errorBody.getBytes(),
                    null
            );

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(),
                    eq(TossPaymentConfirmResponse.class)
            )).thenThrow(exception);

            // when
            PaymentGatewayConfirmResponse result = tossPaymentGateway.confirm(
                    TEST_PAYMENT_KEY, TEST_ORDER_ID, TEST_AMOUNT);

            // then
            assertThat(result.success()).isFalse();
            assertThat(result.failCode()).isEqualTo("UNAUTHORIZED");
            assertThat(result.failMessage()).isEqualTo("토스페이먼츠 인증에 실패했습니다. 시크릿 키를 확인해주세요.");
        }
    }

    @Nested
    @DisplayName("결제 취소 (cancel) 테스트")
    class CancelTest {

        @Test
        @DisplayName("결제 취소 성공 - CANCELED 상태")
        void cancel_Success_CanceledStatus() {
            // given
            TossPaymentCancelResponse responseBody = new TossPaymentCancelResponse(
                    TEST_PAYMENT_KEY,
                    TEST_ORDER_ID,
                    "CANCELED",
                    TEST_CANCEL_REASON,
                    "2025-01-01T13:00:00",
                    null
            );
            ResponseEntity<TossPaymentCancelResponse> response = ResponseEntity.ok(responseBody);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(),
                    eq(TossPaymentCancelResponse.class)
            )).thenReturn(response);

            // when
            PaymentGatewayCancelResponse result = tossPaymentGateway.cancel(
                    TEST_PAYMENT_KEY, TEST_CANCEL_REASON);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.failCode()).isNull();
            assertThat(result.failMessage()).isNull();

            verify(restTemplate, times(1)).exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(),
                    eq(TossPaymentCancelResponse.class)
            );
        }

        @Test
        @DisplayName("결제 취소 성공 - DONE 상태")
        void cancel_Success_DoneStatus() {
            // given
            TossPaymentCancelResponse responseBody = new TossPaymentCancelResponse(
                    TEST_PAYMENT_KEY,
                    TEST_ORDER_ID,
                    "DONE",
                    TEST_CANCEL_REASON,
                    "2025-01-01T13:00:00",
                    null
            );
            ResponseEntity<TossPaymentCancelResponse> response = ResponseEntity.ok(responseBody);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(),
                    eq(TossPaymentCancelResponse.class)
            )).thenReturn(response);

            // when
            PaymentGatewayCancelResponse result = tossPaymentGateway.cancel(
                    TEST_PAYMENT_KEY, TEST_CANCEL_REASON);

            // then
            assertThat(result.success()).isTrue();
        }

        @Test
        @DisplayName("결제 취소 실패 - 상태가 CANCELED/DONE이 아님")
        void cancel_Failure_InvalidStatus() {
            // given
            TossPaymentCancelResponse.FailReason failReason = 
                    new TossPaymentCancelResponse.FailReason("ALREADY_CANCELED", "이미 취소된 결제입니다");
            TossPaymentCancelResponse responseBody = new TossPaymentCancelResponse(
                    TEST_PAYMENT_KEY,
                    TEST_ORDER_ID,
                    "FAILED",
                    TEST_CANCEL_REASON,
                    null,
                    failReason
            );
            ResponseEntity<TossPaymentCancelResponse> response = ResponseEntity.ok(responseBody);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(),
                    eq(TossPaymentCancelResponse.class)
            )).thenReturn(response);

            // when
            PaymentGatewayCancelResponse result = tossPaymentGateway.cancel(
                    TEST_PAYMENT_KEY, TEST_CANCEL_REASON);

            // then
            assertThat(result.success()).isFalse();
            assertThat(result.failCode()).isEqualTo("ALREADY_CANCELED");
            assertThat(result.failMessage()).isEqualTo("이미 취소된 결제입니다");
        }

        @Test
        @DisplayName("결제 취소 실패 - HTTP 4xx 에러")
        void cancel_Failure_HttpClientError() throws Exception {
            // given
            String errorBody = "{\"code\":\"INVALID_PAYMENT_KEY\",\"message\":\"유효하지 않은 결제 키입니다\"}";
            HttpClientErrorException exception = new HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "Bad Request",
                    errorBody.getBytes(),
                    null
            );

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(),
                    eq(TossPaymentCancelResponse.class)
            )).thenThrow(exception);

            TossPaymentErrorResponse errorResponse = new TossPaymentErrorResponse(
                    "INVALID_PAYMENT_KEY",
                    "유효하지 않은 결제 키입니다"
            );
            when(objectMapper.readValue(errorBody, TossPaymentErrorResponse.class))
                    .thenReturn(errorResponse);

            // when
            PaymentGatewayCancelResponse result = tossPaymentGateway.cancel(
                    TEST_PAYMENT_KEY, TEST_CANCEL_REASON);

            // then
            assertThat(result.success()).isFalse();
            assertThat(result.failCode()).isEqualTo("INVALID_PAYMENT_KEY");
            assertThat(result.failMessage()).isEqualTo("유효하지 않은 결제 키입니다");
        }

        @Test
        @DisplayName("결제 취소 실패 - 타임아웃")
        void cancel_Failure_Timeout() {
            // given
            ResourceAccessException exception = new ResourceAccessException("Connection timeout");

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(),
                    eq(TossPaymentCancelResponse.class)
            )).thenThrow(exception);

            // when & then
            assertThatThrownBy(() -> tossPaymentGateway.cancel(
                    TEST_PAYMENT_KEY, TEST_CANCEL_REASON))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("결제 게이트웨이 타임아웃");
        }
    }
}

