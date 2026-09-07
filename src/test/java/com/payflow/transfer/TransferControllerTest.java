package com.payflow.transfer;

import com.payflow.exception.BusinessException;
import com.payflow.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransferController.class)
public class TransferControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TransferService transferService;

    @Test
    void 송금금액이_음수이면_400을_반환한다() throws Exception {

        mockMvc.perform(post("/api/transfers")
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
                .when(transferService).transfer(999L,2L,1000L);
        mockMvc.perform(post("/api/transfers").contentType(APPLICATION_JSON).
                content("""
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
                .transfer(1L,1L,1000L);

        mockMvc.perform(post("/api/transfers").contentType(APPLICATION_JSON).
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
                .transfer(1L, 2L, 999999L);

        mockMvc.perform(post("/api/transfers")
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
}
