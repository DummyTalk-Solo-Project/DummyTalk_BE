package DummyTalk.DummyTalk_BE.global.security.jwt;

import DummyTalk.DummyTalk_BE.global.security.userDetails.CustomUserDetailsService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Component
public class JWTProvider {

    private static final Long ACCESS_TOKEN_EXPIRE_TIME = (long) 1000 * 60 * 60;
    private static final Long REFRESH_TOKEN_EXPIRE_TIME = (long) 1000 * 60 * 60;

    private final CustomUserDetailsService customUserDetailsService;
    private final Key key;


    public JWTProvider(@Value("${jwt.secret.key}") String key, CustomUserDetailsService customUserDetailsService) {
        byte[] encodeKey = Base64.getEncoder().encode(key.getBytes());
        this.key = Keys.hmacShaKeyFor(encodeKey);
        this.customUserDetailsService = customUserDetailsService;
    }

    // 사용자 정보를 통해 토큰 생성.
    public JwtToken generateToken(Authentication authentication) {

        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));


        String username = authentication.getName();
        log.info("generateToken.authorities: {}", authorities);
        log.info ("generateToken.username: {}", username);

        long now = (new Date()).getTime();
        Date accessTokenExpire = new Date(now + ACCESS_TOKEN_EXPIRE_TIME);
        String accessToken = generateNewToken(username, authorities, accessTokenExpire);
        Date refreshTokenExpire = new Date(now + REFRESH_TOKEN_EXPIRE_TIME);
        String refreshToken = generateNewToken(username, null, refreshTokenExpire);

        return JwtToken.builder()
                .grantType("ROLE_USER")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private String generateNewToken(String email, String authorities, Date expireDate) {

        String authClaim = authorities != null ? authorities : "ROLE_USER";

        return Jwts.builder()
                .claim("username", email)
                .claim("role", "ROLE_USER")
                .claim("auth", authClaim)
                .expiration(expireDate)
                .signWith(key)
                .compact();
    }

    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);

        Object authClaimObject = claims.get("auth");
        String authoritiesString = (authClaimObject != null) ? authClaimObject.toString() : "";

        if (authoritiesString.isEmpty() || claims.get("auth") == null) {
            throw new RuntimeException("권한 정보가 없는 이상한 토큰입니다");
        }

        Collection<? extends GrantedAuthority> authorities = Arrays.stream(claims.get("auth").toString().split(","))
                .map(SimpleGrantedAuthority::new)
                .toList();

        String email = claims.get("username", String.class);

        log.info("email: {}", email);

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    private Claims parseClaims(String accessToken) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith((SecretKey) this.key)
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();

            log.info("claims! {}", claims.toString());
            log.info("claims.getSubject: {}", claims.getSubject());
            return claims;
        } catch (Exception e) {
            throw new RuntimeException("파싱이 잘못되었습니다");
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith((SecretKey) key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT Token", e);
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT Token", e);
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT Token", e);
        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty", e);
        }
        return false;
    }
}
