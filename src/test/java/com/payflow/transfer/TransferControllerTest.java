package com.payflow.transfer;

import com.payflow.exception.BusinessException;
import com.payflow.exception.ErrorCode;
import com.payflow.transfer.dto.TransferRequest;
import com.payflow.transfer.dto.TransferResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransferController.class)
public class TransferControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TransferService transferService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 송금_성공시_transferId와_SUCCESS를_반환한다() throws Exception {

        // given
        TransferRequest request = new TransferRequest(1L,2L,1000L);

        when(transferService.transfer(anyLong(),anyLong(),anyLong(),anyString())).thenReturn(1L);

        // when & then
        mockMvc.perform(
                        post("/api/transfers")
                                .header("Idempotency-Key", "test-key-001")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferId").value(1L))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void 송금금액이_음수이면_400을_반환한다() throws Exception {

        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "test-key")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "fromAccountId": 1,
                                  "toAccountId": 2,
                                  "amount": -1000
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("송금금액은 0보다 커야합니다."));
    }

    @Test
    void 송금금액이_없으면_400을_반환한다() throws Exception {

        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "test-key")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "fromAccountId": 1,
                                  "toAccountId": 2
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("송금금액은 필수입니다."));
    }

    @Test
    void 출금계좌ID가_없으면_400을_반환한다() throws Exception {

        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "test-key")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "toAccountId": 2,
                                  "amount": 1000
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("출금계좌 ID는 필수입니다."));
    }

    @Test
    void 존재하지_않는_계좌면_404를_반환한다() throws Exception{
        doThrow(new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND))
                .when(transferService).transfer(999L,2L,1000L, "test-key");
        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "test-key")
                        .contentType(APPLICATION_JSON)
                        .content("""
                            {
                              "fromAccountId": 999,
                              "toAccountId": 2,
                              "amount": 1000
                            }
                            """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("계좌가 존재하지 않습니다."));
    }

    @Test
    void 같은_계좌로_송금하면_400을_반환한다() throws Exception{
        doThrow(new BusinessException(ErrorCode.SAME_ACCOUNT_TRANSFER))
                .when(transferService)
                .transfer(1L,1L,1000L, "test-key");

        mockMvc.perform(post("/api/transfers")
                .header("Idempotency-Key", "test-key")
                        .contentType(APPLICATION_JSON).
                        content("""
                            {
                              "fromAccountId": 1,
                              "toAccountId": 1,
                              "amount": 1000
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("SAME_ACCOUNT_TRANSFER"))
                .andExpect(jsonPath("$.message")
                        .value("같은 계좌로는 송금할 수 없습니다."));
    }

    @Test
    void 잔액이_부족하면_400을_반환한다() throws Exception {

        doThrow(new BusinessException(ErrorCode.INSUFFICIENT_BALANCE))
                .when(transferService)
                .transfer(1L, 2L, 999999L, "test-key");

        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "test-key")
                        .contentType(APPLICATION_JSON)
                        .content("""
                            {
                              "fromAccountId": 1,
                              "toAccountId": 2,
                              "amount": 999999
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INSUFFICIENT_BALANCE"))
                .andExpect(jsonPath("$.message")
                        .value("잔액이 부족합니다."));
    }

    @Test
    void 거래_ID로_단건_조회하면_200과_거래정보를_반환한다() throws Exception {
        // given
        TransferResponse response =new TransferResponse(
                1L,1L,2L,3000L,
                TransferStatus.SUCCESS,
                LocalDateTime.of(2026, 9, 12, 16, 0)
        );

        when(transferService.findById(1L)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/transfers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fromAccountId").value(1))
                .andExpect(jsonPath("$.toAccountId").value(2))
                .andExpect(jsonPath("$.amount").value(3000))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void 존재하지_않는_거래를_조회하면_404를_반환한다() throws Exception {
        // given
        when(transferService.findById(999L))
                .thenThrow(new BusinessException(ErrorCode.TRANSFER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/transfers/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("TRANSFER_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("거래가 존재하지 않습니다."));
    }

    @Test
    void 전체_거래내역을_페이지단위로_조회한다() throws Exception {
        // given
        TransferResponse response = new TransferResponse(
                        1L,
                        1L,
                        2L,
                        3000L,
                        TransferStatus.SUCCESS,
                        LocalDateTime.now()
                );

        Page<TransferResponse> page = new PageImpl<>(List.of(response));

        when(transferService.findAll(0, 20)).thenReturn(page);

        // when & then
        mockMvc.perform(
                        get("/api/transfers")
                                .param("page", "0")
                                .param("size", "20")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].amount").value(3000))
                .andExpect(jsonPath("$.content[0].status").value("SUCCESS"));
    }
}
