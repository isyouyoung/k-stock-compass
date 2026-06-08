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

        // 1. 요청 헤더에서 Authorization 값을 꺼냄
        // 프론트에서 authFetch() 호출 시 "Bearer {토큰}" 형태로 전달됨
        String header = request.getHeader("Authorization");

        // 2. Authorization 헤더가 있고 "Bearer "로 시작할 때만 처리
        // 없으면 비로그인 요청이므로 그냥 통과
        if (header != null && header.startsWith("Bearer ")) {

            // 3. "Bearer " 7글자를 제거하고 순수 토큰 문자열만 추출
            String token = header.substring(7);

            try {
                // 4. 토큰 유효성 검사
                // 서명 위조, 형식 오류 등은 예외가 발생하며
                // 정상적인 토큰이면 true를 반환 (비정상시 false)
                if (jwtProvider.validateToken(token)) {

                    // 5. 토큰에서 사용자 이메일 추출
                    String email = jwtProvider.getEmail(token);

                    // 6. SecurityContext에 인증 정보 저장
                    // 스프링 시큐리티가 이 요청을 "인증된 사용자"로 인식하게 해줌
                    // 이후 컨트롤러에서 SecurityContextHolder로 이메일을 꺼낼 수 있음
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(email, null, List.of());
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("JWT 인증 성공: {}", email);
                } else {
                    // 7. 토큰이 유효하지 않으면 (가짜, 변조 등) 경고 로그만 남기고 통과
                    // SecurityContext에 인증 정보가 없으니 인증 필요 API는 403 반환됨
                    log.warn("JWT 토큰 유효하지 않음");
                }
            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                // 8. 토큰이 만료된 경우 → 401 반환
                // 프론트의 authFetch()가 401을 감지하고 자동으로 Refresh Token 재발급 시도
                log.warn("JWT 토큰 만료됨");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 상태코드 설정
                response.getWriter().write("Token expired"); // 응답 본문 작성
                return; // 필터 체인 중단 (컨트롤러까지 요청이 가지 않음)
            } catch (Exception e) {
                // 9. 그 외 예외 (서명 오류 등) → 경고 로그만 남기고 통과
                log.warn("JWT 처리 중 오류: {}", e.getMessage());
            }
        }
        // 10. 다음 필터로 요청을 넘김
        // 인증 성공이든 실패든 여기까지 오면 다음 필터로 진행
        // 인증 실패 시엔 SecurityContext가 비어있어서 인증 필요 API 접근 시 403 반환
        filterChain.doFilter(request, response);
    }
}

