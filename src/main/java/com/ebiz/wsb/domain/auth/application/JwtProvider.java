package com.ebiz.wsb.domain.auth.application;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtProvider {
    private final Key key;
    public static final int ACCESS_TOKEN_EXPIRE = 1000 * 60 * 60 * 24;
    public static final int REFRESH_TOKEN_EXPIRE = 1000 * 60 * 60 * 24 * 15;

    /**
     * JwtProvider 생성자
     * @param secretKey JWT 비밀키를 Base64로 인코딩한 값
     */
    public JwtProvider(@Value("${jwt.secretKey}") String secretKey){
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 토큰을 생성하는 메서드
     * @param authentication 인증 정보 (사용자 이름과 권한 정보)
     * @param expireMills 토큰 만료 시간 (밀리초 단위)
     * @return 생성된 JWT 토큰
     */
    public String generateToken(Authentication authentication, long expireMills){
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();

        Date accessTokenExpire = new Date(now + expireMills);
        String token = Jwts.builder()
                .setSubject(authentication.getName())
                .claim("auth", authorities)
                .setExpiration(accessTokenExpire)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        return token;
    }

    /**
     * 액세스 토큰을 생성하는 메서드
     * @param authentication 인증 정보 (사용자 이름과 권한 정보)
     * @return 생성된 액세스 토큰
     */
    public String generateAccessToken(Authentication authentication){
        return generateToken(authentication, ACCESS_TOKEN_EXPIRE);
    }

    /**
     * 리프레시 토큰을 생성하는 메서드
     * @param authentication 인증 정보 (사용자 이름과 권한 정보)
     * @return 생성된 리프레시 토큰
     */
    public String generateRefreshToken(Authentication authentication){
        return generateToken(authentication, REFRESH_TOKEN_EXPIRE);
    }

    /**
     * JMT 토큰을 복호화해 토큰에 들어있는 정보를 꺼내는 메서드
     * @param accessToken JWT 토큰
     */
    public Authentication getAuthentication(String accessToken){
        Claims claims = parseClaims(accessToken);
        if (claims.get("auth") == null ){
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        /**
         * claim에서 권한 정보 가져오기
         * @param claim : 토큰을 복호화한 것
         */
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get("auth").toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        // UserDetails 객체를 만들어서 Authentication 리턴 / Authentication
        UserDetails pricipal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(pricipal, "", authorities);
    }

    /**
     * 토큰의 유효성을 검증하는 메서드
     * @param token JWT 토큰
     * @return 토큰이 유효하면 true, 그렇지 않으면 예외를 발생
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException e) {
            throw new MalformedJwtException("위조된 토큰");
        } catch (ExpiredJwtException e) {
            throw new ExpiredJwtException(e.getHeader(), e.getClaims(), e.getMessage());
        } catch (UnsupportedJwtException e) {
            throw new UnsupportedJwtException(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("토큰 에러");
        } catch (io.jsonwebtoken.security.SecurityException e) {
            throw new SecurityException("security exception");
        }
    }

    /**
     * 토큰을 파싱하여 Claims 객체를 반환하는 메서드
     * @param accessToken JWT 액세스 토큰
     * @return Claims 객체
     */
    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).getBody();

        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    /**
     * 비밀키를 Base64로 인코딩하는 메서드
     * @param secretKey 비밀키
     * @return Base64로 인코딩된 비밀키
     */
    public String encodeBase64SecretKey(@Value("${jwt.secretKey}") String secretKey){
        return Encoders.BASE64.encode(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}
