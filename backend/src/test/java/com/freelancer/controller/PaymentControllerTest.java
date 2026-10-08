package com.freelancer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.freelancer.dto.PaymentTransactionResponse;
import com.freelancer.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void refundByTransactionIdReturnsRefundedPaymentAsJson() throws Exception {
        LocalDateTime refundDate = LocalDateTime.parse("2026-10-07T17:00:00");
        PaymentTransactionResponse payment = new PaymentTransactionResponse(
                1L, 20L, "Project", "Client", "Freelancer", new BigDecimal("450.00"),
                LocalDateTime.parse("2026-10-06T17:00:00"), "REFUNDED",
                "TXN-12345678", "CARD", refundDate);
        when(paymentService.refund(5L, null, "TXN-12345678")).thenReturn(payment);

        mockMvc.perform(post("/api/payments/refund")
                        .requestAttr("userId", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transactionId\":\"TXN-12345678\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.transactionId").value("TXN-12345678"))
                .andExpect(jsonPath("$.refundDate").value("2026-10-07T17:00:00"));

        verify(paymentService).refund(5L, null, "TXN-12345678");
    }
}
