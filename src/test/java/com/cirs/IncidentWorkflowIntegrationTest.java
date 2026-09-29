package com.cirs;

import com.cirs.dto.request.*;
import com.cirs.entity.IncidentCategory;
import com.cirs.entity.User;
import com.cirs.enums.IncidentStatus;
import com.cirs.enums.NoteType;
import com.cirs.enums.Role;
import com.cirs.repository.IncidentCategoryRepository;
import com.cirs.repository.IncidentRepository;
import com.cirs.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IncidentWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IncidentCategoryRepository categoryRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long categoryId;
    private String citizenToken;
    private String investigatorToken;
    private String adminToken;
    private Long investigatorId;

    @BeforeEach
    void setUp() throws Exception {
        // Ensure standard category exists
        IncidentCategory cat = categoryRepository.findByNameIgnoreCase("Phishing")
                .orElseGet(() -> categoryRepository.save(new IncidentCategory("Phishing", "Email phishing attacks")));
        this.categoryId = cat.getId();

        // 1. Citizen User
        if (userRepository.findByUsername("test_citizen").isEmpty()) {
            User citizen = new User("test_citizen", "citizen@test.com", passwordEncoder.encode("Password123!"), "Citizen Jane", null, Role.ROLE_USER);
            userRepository.save(citizen);
        }
        citizenToken = obtainToken("test_citizen", "Password123!");

        // 2. Investigator
        User inv = userRepository.findByUsername("test_inv").orElseGet(() -> {
            User investigator = new User("test_inv", "inv@test.com", passwordEncoder.encode("Password123!"), "Agent Smith", null, Role.ROLE_INVESTIGATOR);
            return userRepository.save(investigator);
        });
        this.investigatorId = inv.getId();
        investigatorToken = obtainToken("test_inv", "Password123!");

        // 3. Administrator
        if (userRepository.findByUsername("test_admin").isEmpty()) {
            User admin = new User("test_admin", "admin@test.com", passwordEncoder.encode("Password123!"), "Admin Director", null, Role.ROLE_ADMIN);
            userRepository.save(admin);
        }
        adminToken = obtainToken("test_admin", "Password123!");
    }

    private String obtainToken(String username, String password) throws Exception {
        LoginRequest login = new LoginRequest(username, password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("token").asText();
    }

    @Test
    @DisplayName("End-to-End: Submission, IDOR defense, Assignment, Internal Notes Segregation, Resolution")
    void testCompleteIncidentLifecycleAndSecurity() throws Exception {
        // Step 1: Citizen submits report with evidence file
        MockMultipartFile evidenceFile = new MockMultipartFile(
                "files",
                "phish_log.txt",
                "text/plain",
                "Header info: Return-Path: <spoofer@malicious.com>".getBytes(StandardCharsets.UTF_8)
        );

        MvcResult createResult = mockMvc.perform(multipart("/api/v1/incidents")
                        .file(evidenceFile)
                        .param("categoryId", categoryId.toString())
                        .param("title", "Credential Harvesting Portal on internal subnet")
                        .param("description", "A spoofed intranet login was deployed targeting user accounts.")
                        .param("incidentDate", "2026-09-28T10:00:00")
                        .param("severity", "HIGH")
                        .param("locationIp", "10.0.5.22")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isCreated())
                .andExpect(jsonNode -> assertTrue(jsonNode.getResponse().getContentAsString().contains("CIR-")))
                .andReturn();

        JsonNode createNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String incidentNumber = createNode.path("data").path("incidentNumber").asText();
        assertNotNull(incidentNumber);

        // Step 2: Test IDOR protection - Another citizen user cannot view this incident
        if (userRepository.findByUsername("unauthorized_citizen").isEmpty()) {
            userRepository.save(new User("unauthorized_citizen", "other@test.com", passwordEncoder.encode("Password123!"), "Other User", null, Role.ROLE_USER));
        }
        String otherUserToken = obtainToken("unauthorized_citizen", "Password123!");

        mockMvc.perform(get("/api/v1/incidents/" + incidentNumber)
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isForbidden()); // IDOR BLOCKED!

        // Step 3: Admin assigns incident to Investigator
        AssignInvestigatorRequest assignReq = new AssignInvestigatorRequest(investigatorId);
        mockMvc.perform(put("/api/v1/admin/incidents/" + incidentNumber + "/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"));

        // Step 4: Investigator views assigned case and marks as INVESTIGATING
        StatusUpdateRequest statusReq = new StatusUpdateRequest(IncidentStatus.INVESTIGATING, "Forensic imaging underway");
        mockMvc.perform(patch("/api/v1/incidents/" + incidentNumber + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq))
                        .header("Authorization", "Bearer " + investigatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INVESTIGATING"));

        // Step 5: Investigator adds a confidential INTERNAL_NOTE
        NoteCreateRequest internalNoteReq = new NoteCreateRequest(NoteType.INTERNAL_NOTE, "SOC Intel: C2 communication identified to external IP 203.0.113.5");
        mockMvc.perform(post("/api/v1/incidents/" + incidentNumber + "/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(internalNoteReq))
                        .header("Authorization", "Bearer " + investigatorToken))
                .andExpect(status().isOk());

        // Step 6: Verify information segregation:
        // Investigator sees the internal note
        MvcResult invViewResult = mockMvc.perform(get("/api/v1/incidents/" + incidentNumber)
                        .header("Authorization", "Bearer " + investigatorToken))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(invViewResult.getResponse().getContentAsString().contains("SOC Intel: C2 communication"));

        // Citizen does NOT see the internal note
        MvcResult citizenViewResult = mockMvc.perform(get("/api/v1/incidents/" + incidentNumber)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andReturn();
        assertFalse(citizenViewResult.getResponse().getContentAsString().contains("SOC Intel: C2 communication"));

        // Step 7: Investigator resolves the incident
        StatusUpdateRequest resolveReq = new StatusUpdateRequest(IncidentStatus.RESOLVED, "Spoofed portal taken offline and domain sinkholed");
        mockMvc.perform(patch("/api/v1/incidents/" + incidentNumber + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveReq))
                        .header("Authorization", "Bearer " + investigatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));

        // Step 8: Admin closes the case
        StatusUpdateRequest closeReq = new StatusUpdateRequest(IncidentStatus.CLOSED, "Final administrative sign-off");
        mockMvc.perform(patch("/api/v1/incidents/" + incidentNumber + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeReq))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }
}
