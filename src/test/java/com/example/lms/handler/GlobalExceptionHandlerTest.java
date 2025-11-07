package com.example.lms.handler;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.TransactionSystemException;

import com.example.lms.exception.DuplicateEmailException;
import com.example.lms.exception.GlobalExceptionHandler;
import com.example.lms.exception.InvalidCredentialsException;

import org.springframework.http.ResponseEntity;

import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleDuplicateEmail_returnsBadRequestAndMessage() {
        DuplicateEmailException ex = new DuplicateEmailException("email already used");
        ResponseEntity<?> resp = handler.handleDuplicateEmail(ex);

        assertEquals(400, resp.getStatusCode().value());

        Object body = resp.getBody();
        assertTrue(body instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) body;
        assertEquals("email already used", map.get("error"));
    }

    @Test
    void handleInvalidCredentials_returnsUnauthorizedAndMessage() {
        InvalidCredentialsException ex = new InvalidCredentialsException("bad creds");
        ResponseEntity<?> resp = handler.handleInvalidCredentials(ex);

        assertEquals(401, resp.getStatusCode().value());

        Object body = resp.getBody();
        assertTrue(body instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) body;
        assertEquals("bad creds", map.get("error"));
    }

    @Test
    void handleConstraintErrors_withDataIntegrityViolation_returnsBadRequestAndDetail() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("unique violation");
        ResponseEntity<?> resp = handler.handleConstraintErrors(ex);

        assertEquals(400, resp.getStatusCode().value());

        Object body = resp.getBody();
        assertTrue(body instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) body;
        assertEquals("Email already exists or violates unique constraint.", map.get("error"));
        assertNotNull(map.get("detail"));
        assertTrue(map.get("detail").toString().toLowerCase().contains("unique violation"));
    }

    @Test
    void handleConstraintErrors_withConstraintViolationException_returnsBadRequestAndDetail() {
        // ConstraintViolationException requires SQL exception and constraint name in
        // constructor
        ConstraintViolationException ex = new ConstraintViolationException("constraint fail", new SQLException("sql"),
                "uq_email");
        ResponseEntity<?> resp = handler.handleConstraintErrors(ex);

        assertEquals(400, resp.getStatusCode().value());

        Object body = resp.getBody();
        assertTrue(body instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) body;
        assertEquals("Email already exists or violates unique constraint.", map.get("error"));
        assertNotNull(map.get("detail"));
        assertTrue(map.get("detail").toString().toLowerCase().contains("constraint fail"));
    }

    @Test
    void handleConstraintErrors_withTransactionSystemException_returnsBadRequestAndDetail() {
        TransactionSystemException ex = new TransactionSystemException("tx failed");
        ResponseEntity<?> resp = handler.handleConstraintErrors(ex);

        assertEquals(400, resp.getStatusCode().value());

        Object body = resp.getBody();
        assertTrue(body instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) body;
        assertEquals("Email already exists or violates unique constraint.", map.get("error"));
        assertNotNull(map.get("detail"));
        assertTrue(map.get("detail").toString().toLowerCase().contains("tx failed"));
    }

    @Test
    void handleConstraintErrors_withJpaSystemException_returnsBadRequestAndDetail() {
        JpaSystemException ex = new JpaSystemException(new RuntimeException("jpa fail"));
        ResponseEntity<?> resp = handler.handleConstraintErrors(ex);

        assertEquals(400, resp.getStatusCode().value());

        Object body = resp.getBody();
        assertTrue(body instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) body;
        assertEquals("Email already exists or violates unique constraint.", map.get("error"));
        assertNotNull(map.get("detail"));
        assertTrue(map.get("detail").toString().toLowerCase().contains("jpa fail"));
    }

    @Test
    void handleGeneralError_returnsInternalServerErrorAndDetail() {
        RuntimeException ex = new RuntimeException("unexpected boom");
        ResponseEntity<?> resp = handler.handleGeneralError(ex);

        assertEquals(500, resp.getStatusCode().value());

        Object body = resp.getBody();
        assertTrue(body instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) body;
        assertEquals("Internal server error", map.get("error"));
        assertNotNull(map.get("detail"));
        assertTrue(map.get("detail").toString().toLowerCase().contains("unexpected boom"));
    }
}
