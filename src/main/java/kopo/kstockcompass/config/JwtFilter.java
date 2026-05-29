package kopo.kstockcompass.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// 모든 요청마다 JWT 토큰을 검사하는 필터
// @Component: Spring Bean으로 등록하여 IoC 컨테이너가 관리
// OncePerRequestFilter: 요청당 딱 한 번만 실행되도록 보장
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    // JwtProvider를 new로 생성하지 않고 DI로 주입받음 (IoC/DI 원칙)
    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Authorization 헤더에서 토큰 추출
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7); // "Bearer " 제거

            // 2. 토큰 유효성 검사
            if (jwtProvider.validateToken(token)) {
                String email = jwtProvider.getEmail(token);

                // 3. SecurityContext에 인증 정보 저장
                // 이후 컨트롤러에서 @AuthenticationPrincipal로 꺼낼 수 있음
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(email, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JWT 인증 성공: {}", email);
            } else {
                log.warn("JWT 토큰 유효하지 않음");
            }
        }

        // 4. 다음 필터로 넘김 (인증 실패해도 통과 - permitAll 설정이므로)
        filterChain.doFilter(request, response);
    }
}