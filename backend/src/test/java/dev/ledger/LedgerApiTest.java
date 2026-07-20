package dev.ledger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LedgerApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsAndListsAccounts() throws Exception {
        mvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Checking\",\"type\":\"ASSET\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.balance").value(0));

        mvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Checking"));
    }

    @Test
    void blankAccountNameIsA400() throws Exception {
        mvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"type\":\"ASSET\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").exists());
    }

    @Test
    void unknownAccountBalanceIsA404() throws Exception {
        mvc.perform(get("/api/accounts/999999/balance"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unbalancedTransactionIsA422() throws Exception {
        long checking = createAccount("Checking", "ASSET");
        long salary = createAccount("Salary", "INCOME");

        mvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Off by a cent","legs":[
                                  {"accountId":%d,"direction":"DEBIT","amount":"10.00"},
                                  {"accountId":%d,"direction":"CREDIT","amount":"10.01"}]}
                                """.formatted(checking, salary)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void idempotentReplayIsA200WithTheOriginalId() throws Exception {
        long checking = createAccount("Checking", "ASSET");
        long salary = createAccount("Salary", "INCOME");
        String body = """
                {"description":"Paycheck","idempotencyKey":"pay-1","legs":[
                  {"accountId":%d,"direction":"DEBIT","amount":"2500.00"},
                  {"accountId":%d,"direction":"CREDIT","amount":"2500.00"}]}
                """.formatted(checking, salary);

        String first = mvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long firstId = objectMapper.readTree(first).get("id").asLong();

        mvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstId));
    }

    private long createAccount(String name, String type) throws Exception {
        String json = mvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"type\":\"%s\"}".formatted(name, type)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(json);
        return node.get("id").asLong();
    }
}
