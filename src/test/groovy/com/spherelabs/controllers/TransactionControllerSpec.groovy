package com.spherelabs.controllers

import com.spherelabs.AbstractSpecification
import com.spherelabs.model.Transaction
import com.spherelabs.repository.ExchangeRateRepository
import com.spherelabs.repository.LedgerRepository
import com.spherelabs.repository.LiquidityRepository
import com.spherelabs.repository.TransactionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Unroll

import java.math.RoundingMode
import java.time.OffsetDateTime

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerSpec extends AbstractSpecification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    ExchangeRateRepository exchangeRateRepository

    @Autowired
    TransactionRepository transactionRepository

    @Autowired
    LiquidityRepository liquidityRepository

    @Autowired
    LedgerRepository ledgerRepository

    def setup() {
        //check the second one is used since the 1st is not yet effective as at today
        exchangeRateRepository.addRate("USD/EUR", 1.15, OffsetDateTime.now().plusDays(2))
                .getOrElseGet { throw new RuntimeException("Failed to add rate") }
        exchangeRateRepository.addRate("USD/EUR", 1.16, OffsetDateTime.now().minusDays(2))
                .getOrElseGet { throw new RuntimeException("Failed to add rate") }

        //this should throw a rate not found since the effective date is in the future
        exchangeRateRepository.addRate("USD/GBP", 1.80, OffsetDateTime.now().plusDays(2))
                .getOrElseGet { throw new RuntimeException("Failed to add rate") }
    }

    @Unroll
    def "POST /transfer - #testCase"() {
        given:
        def requestBody = """
            {
                "sender_account": "$senderAccount",
                "receiver_account": "$receiverAccount",
                "from_currency": "$fromCurrency",
                "to_currency": "$toCurrency",
                "amount": $amount,
                "description": "$description",
                "reference":  "$reference"
            }
        """

        expect:
        def balance  = liquidityRepository.getBalance(toCurrency).get()

        def result = mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                result.andExpect(status().is(code))
        // assert the status

        if (dbStatus != null) {
            def transaction = transactionRepository.getTransaction(reference, fromCurrency, toCurrency).get()
            transaction.status == dbStatus
            transaction.createdAt != null
            transaction.updatedAt != null
            transaction.fromAmount == amount
            transaction.fromCurrency == fromCurrency
            transaction.toCurrency == toCurrency
            transaction.toAmount == ((amount * 1.16) - (0.03 * (amount * 1.16))).setScale(2, RoundingMode.HALF_UP)
            transaction.margin == (0.03 * (amount * 1.16)).setScale(2, RoundingMode.HALF_DOWN)
            transaction.marginCurrency == "EUR"
            transaction.fxRate == 1.16
            transaction.marginRate == 0.03
            transaction.description == description
            transaction.transferId == reference
            transaction.senderAccount == senderAccount
            transaction.receiverAccount == receiverAccount

            // expects the balance to be updated
            if (transaction.status <= Transaction.Status.PROCESSING) {
                liquidityRepository.getBalance(toCurrency).get() == balance - transaction.toAmount
                def lock = ledgerRepository.getByLockId(transaction.lockedId).get()
                lock.amount() == transaction.toAmount
                lock.currencyCode() == transaction.toCurrency
            }
        }


        // add more test case when there is time.
        where:
        testCase              |  senderAccount | receiverAccount | fromCurrency | toCurrency | amount        | description         | reference                    | apiStatus                               | dbStatus                          | code
        "Success"             | "123456789"   | "987654321"     | "USD"        | "EUR"      | 100.50         | "Payment for goods"  |  "unique ref "              | Transaction.Status.PROCESSING           |Transaction.Status.FUNDS_LOCKED    | 200
        "Insufficient Funds"  | "123456789"   | "987654321"     | "USD"        | "EUR"      | 100000000.50   | "Payment for goods"  |  "new ref "                 | Transaction.Status.FAILED               | Transaction.Status.FAILED         | 200
        "Unsupported from"    | "123456789"   | "987654321"     | "XXX"        | "EUR"      | 100000000.50   | "Payment for goods"  |  "new ref "                 | Transaction.Status.FAILED               | null         | 422
        "Unsupported to"      | "123456789"   | "987654321"      | "USD"        | "XXX"      | 100000000.50   | "Payment for goods"  |  "new ref "                 | Transaction.Status.FAILED              | null         | 422
        "Same currency"       | "123456789"   | "987654321"      | "USD"        | "USD"      | 100.50         | "Payment for goods"  |  "new ref "                 | Transaction.Status.FAILED              | null         | 500
    }
}
