package com.group1.banking.controller;

import java.util.List;
import java.util.UUID;

import com.group1.banking.dto.chat.DeveloperChatRequest;
import com.group1.banking.dto.chat.DeveloperChatResponse;
import com.group1.banking.repository.UserRepository;
import com.group1.banking.security.JwtService;
import com.group1.banking.service.impl.DeveloperChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.json.JsonMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeveloperChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class DeveloperChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private DeveloperChatService developerChatService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithCustomUser
    void ask_rejectsRetailCustomer() throws Exception {
        DeveloperChatRequest request = request();

        mockMvc.perform(post("/api/developer/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(developerChatService);
    }

    @Test
    @WithCustomUser(role = com.group1.banking.enums.RoleName.DEVELOPER)
    void ask_returnsAnswerForDeveloper() throws Exception {
        DeveloperChatRequest request = request();
        DeveloperChatResponse response = new DeveloperChatResponse(
                UUID.randomUUID().toString(), "A documented answer.", List.of(), "NO_EVIDENCE");
        when(developerChatService.ask(any(UUID.class), any(DeveloperChatRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/developer/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("A documented answer."));

        verify(developerChatService).ask(
                any(UUID.class),
                argThat(actual -> "How do I run the project?".equals(actual.getMessage())));
    }

    @Test
    @WithCustomUser(role = com.group1.banking.enums.RoleName.DEVELOPER)
    void ask_rejectsBlankQuestion() throws Exception {
        DeveloperChatRequest request = request();
        request.setMessage(" ");

        mockMvc.perform(post("/api/developer/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableContent());

        verifyNoInteractions(developerChatService);
    }

    private DeveloperChatRequest request() {
        DeveloperChatRequest request = new DeveloperChatRequest();
        request.setMessage("How do I run the project?");
        request.setSessionId(null);
        return request;
    }
}
