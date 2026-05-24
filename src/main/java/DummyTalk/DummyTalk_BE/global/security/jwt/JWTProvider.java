package DummyTalk.DummyTalk_BE.global.security.jwt;

import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.GeneralException;
import DummyTalk.DummyTalk_BE.global.security.userDetails.CustomUserDetailsService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
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

    private static final Long ACCESS_TOKEN_EXPIRE_TIME = (long) 1000 * 60 * 60;       // 1시간
    // Cookie maxAge & Redis TTL과 통일
    private static final Long REFRESH_TOKEN_EXPIRE_TIME = (long) 1000 * 60 * 60 * 24 * 7; // 7일

    private final Key key;
    private final CustomUserDetailsService customUserDetailsService;


    public JWTProvider(@Value("${jwt.secret.key}") String key, CustomUserDetailsService customUserDetailsService) {
        byte[] encodeKey = Base64.getEncoder().encode(key.getBytes());
        this.key = Keys.hmacShaKeyFor(encodeKey);
        this.customUserDetailsService = customUserDetailsService; // 커스텀 UserDetailsService 를 통한 DB 조회
    }

    /**
     * 실질적인 JWT 반환 메서드!
     * Service -> login 메서드에서 사용
     * */
    public JWT generateToken (Authentication authentication){
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        String username = authentication.getName();

        long now = (new Date()).getTime();

        Date accessTokenExpire = new Date(now + ACCESS_TOKEN_EXPIRE_TIME);
        String accessToken = createNewToken(username, authorities, accessTokenExpire);

        Date refreshTokenExpire = new Date(now + REFRESH_TOKEN_EXPIRE_TIME);
        String refreshToken = createNewToken(username, authorities, refreshTokenExpire);

        return JWT.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public String createNewToken(String email, String authorities, Date expireDate){
        return Jwts.builder()
//                .claim("username", email)
                .subject(email) // 표준 필드로 수정
                .claim("auth", authorities != null ? authorities : "ROLE_MEMBER") // authorities 있는 경우 Member 필드의 Role 반환
                .claim("role", authorities != null ? authorities : "ROLE_MEMBER") // authorities 있는 경우 Member 필드의 Role 반환
                .expiration(expireDate)
                .signWith(key)
                .compact();
    }


    public boolean validateToken(String accessToken) {
        try{
            Jwts.parser()
                    .verifyWith((SecretKey) key)
                    .build()
                    .parseSignedClaims(accessToken);
            return true;
        }
        catch (SecurityException | MalformedJwtException e){
            log.warn("[JWTProvider-ValidateToken()] : 잘못된 토큰입니다 ", e);
        }
        catch (ExpiredJwtException e){ // 실패 응답을 통한 로그인 요청 로직
            log.warn("[JWTProvider-ValidateToken()] : 만료된 토큰입니다 ", e);
            throw e;
        }
        catch (UnsupportedJwtException e) {
            log.warn("[JWTProvider-ValidateToken()] : 지원되지 않는 토큰 형식입니다 ", e);
        }
        catch (IllegalArgumentException e) {
            log.warn("[JWTProvider-ValidateToken()] : JWT claims String이 비어있습니다. ", e);
        }
        return false;
    }

    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);

        Object authClaimObject = claims.get("auth") != null ? claims.get("auth") : "";

//        log.info("authClaimObject: {}, ", authClaimObject.toString());


        String authoritiesString = (authClaimObject != null) ? authClaimObject.toString() : "";

//        log.info("authoritiesString: {}", authoritiesString);

        if (authoritiesString.isEmpty() || claims.get("auth") == null) {
            ///  GenerationException 으로 수정하기
            throw new GeneralException(ErrorCode.CANT_FIND_AUTHORITIES);
        }

        // 표준 필드로 변경
//        log.info("[JWTProvider - getAuthentication()] email: {}", claims.get("username", String.class));
        log.info("[JWTProvider - getAuthentication()] email: {}", claims.getSubject());

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(claims.getSubject());
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    private Claims parseClaims(String accessToken) {
        try{
            Claims claims = Jwts.parser()
                    .verifyWith((SecretKey) key)
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();

//            log.info ("[JWTProvider - parseClaims()] claims 파싱 발생 -> {}, {}", claims.toString(), claims.getSubject());
            return claims;
        }
        catch (Exception e){
            ///  GenerationException 으로 수정하기
            throw new RuntimeException("파싱이 잘못되었습니다.");
        }
    }

    public Long getRemainingTime(String token) {
        Claims claims = parseClaims(token);
        return claims.getExpiration().getTime() - System.currentTimeMillis(); // 만료 시간 - 남은 시간
    }
}
