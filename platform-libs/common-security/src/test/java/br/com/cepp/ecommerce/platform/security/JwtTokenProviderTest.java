package br.com.cepp.ecommerce.platform.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @Test
    void GivenJWTShouldReturnUserName() {
//        Authentication authentication = Mockito.mock(Authentication.class);
//        when(authentication.getPrincipal())
//                .thenReturn(new UsernamePasswordAuthenticationToken("admin", "admin"));
//        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
//                "YourSuperLongAndSecureSecretKeyHereWhichIsMoreThan256BitsLong", 1L);
//        String jwt = "some-jwt-token";
//        String userName = jwtTokenProvider.getUsernameFromJWT(jwt);
//        assertEquals("admin", userName);
    }
}