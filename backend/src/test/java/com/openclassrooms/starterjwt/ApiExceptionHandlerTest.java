package com.openclassrooms.starterjwt;

import com.openclassrooms.starterjwt.exception.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApiExceptionHandlerTest {
    @RestController
    static class ConstraintFailureEndpoint {
        @GetMapping("/constraint-failure")
        void fail() {
            throw new DataIntegrityViolationException("SQL statement containing a test-only-sensitive-marker");
        }
    }

    @Test
    void constraintFailuresReturnConflictWithoutExposingSql() throws Exception {
        MockMvcBuilders.standaloneSetup(new ConstraintFailureEndpoint())
                .setControllerAdvice(new ApiExceptionHandler()).build()
                .perform(get("/constraint-failure"))
                .andExpect(status().is(HttpStatus.CONFLICT.value())).andExpect(content().string(""));
    }
}
