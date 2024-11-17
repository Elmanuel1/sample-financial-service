package com.spherelabs.controllers

import com.spherelabs.AbstractSpecification
import com.spherelabs.repository.ExchangeRateRepository;
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Unroll

import java.time.OffsetDateTime

import static org.springframework.http.MediaType.APPLICATION_JSON

@SpringBootTest
@AutoConfigureMockMvc
class FxRateControllerSpec extends AbstractSpecification {
    @Autowired
    ExchangeRateRepository exchangeRateRepository
    @Autowired
    MockMvc mockMvc

    def setup() {
        exchangeRateRepository.addRate("EUR/USD", 82.6666, OffsetDateTime.now().plusDays(2))
        .getOrElseGet { throw new RuntimeException("Failed to add rate") }
    }

    @Unroll
    def "POST /fx-rate - #description"() {
        given:
        def requestBody =  """
            {
                "pair": $pair,
                "rate": $rate,
                "timestamp": "$timestamp"
            }
        """

        expect:
            def response = mockMvc.perform(MockMvcRequestBuilders.post("/fx-rate")
                    .contentType(APPLICATION_JSON)
                    .content(requestBody as String))
            response.andExpect(MockMvcResultMatchers.status().is(statusCode))
            if (code != null) {
                response.andExpect(MockMvcResultMatchers.jsonPath('$.code').value(code))
            }

        // TODO: Asset the messages here because of 400 errors
        where:
            description                       | pair            | rate       | timestamp                      || statusCode                                  | code
            "Valid pair AUD/JPY with rate"    | "\"AUD/JPY\""   | "82.6666"  | "2024-11-11T11:22:18.123Z"     || HttpStatus.NO_CONTENT.value()               | null
            "Invalid currency pair format"    | "\"INVALID\""   | "100.1234" | "2024-11-12T10:15:00.123Z"     || HttpStatus.BAD_REQUEST.value()              | "api.validation.error"
            "No pair"                         | null            | "82.6666"  | "2024-11-11T11:22:18.123Z"     || HttpStatus.BAD_REQUEST.value()              | "api.validation.error"
            "Negative rate "                  | "\"EUR/USD\""   | "-1.0"     | "2024-11-12T10:15:00.123Z"     || HttpStatus.BAD_REQUEST.value()            | "api.validation.error"
            "Zero rate "                      | "\"EUR/USD\""   | "0.00"     | "2024-11-12T10:15:00.123Z"     || HttpStatus.BAD_REQUEST.value()            | "api.validation.error"
            "No timestamp"                    | "\"EUR/USD\""   | "0.00"     | null                           || HttpStatus.BAD_REQUEST.value()            | "api.validation.error"

    }
}
