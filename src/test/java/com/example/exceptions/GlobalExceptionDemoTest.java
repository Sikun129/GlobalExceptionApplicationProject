package com.example.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionDemoTest {
    @Autowired MockMvc mvc;

    @Test void successBypassesAdvice() throws Exception {
        mvc.perform(get("/api/products/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Wireless Keyboard"))
                .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test void missingProductHasUsefulProblemAndMatchingRequestId() throws Exception {
        var response = mvc.perform(get("/api/products/99")).andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.instance").value("/api/products/99"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty()).andReturn().getResponse();
        assertThat(response.getContentAsString()).contains(response.getHeader("X-Request-Id"));
    }

    @Test void validationHasFieldErrors() throws Exception {
        mvc.perform(post("/api/orders/preview").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":1,\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("quantity"));
    }

    @Test void missingFieldsAreValidationErrors() throws Exception {
        mvc.perform(post("/api/orders/preview").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test void sameAdviceHandlesTheOtherController() throws Exception {
        mvc.perform(post("/api/orders/preview").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":99,\"quantity\":1}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test void insufficientStockIsConflict() throws Exception {
        mvc.perform(post("/api/orders/preview").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":1,\"quantity\":10}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }

    @Test void validPreviewIsRepeatable() throws Exception {
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/orders/preview").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"productId\":1,\"quantity\":5}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.product.stock").value(5));
        }
    }

    @Test void malformedJsonStays400() throws Exception {
        mvc.perform(post("/api/orders/preview").contentType(MediaType.APPLICATION_JSON).content("{\"productId\":"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("HTTP_400"));
    }

    @Test void invalidPathParameterStays400() throws Exception {
        mvc.perform(get("/api/products/abc")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("HTTP_400"));
    }

    @Test void methodNotAllowedKeepsHeaderAndStatus() throws Exception {
        mvc.perform(delete("/api/products/1")).andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.code").value("HTTP_405"));
    }

    @Test void unexpectedFailureDoesNotLeakDetails() throws Exception {
        mvc.perform(get("/api/products/demo-failure")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString("database"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test void presentationPageLoads() throws Exception {
        mvc.perform(get("/index.html")).andExpect(status().isOk())
                .andExpect(content().string(containsString("One handler. Consistent errors.")));
    }
}
