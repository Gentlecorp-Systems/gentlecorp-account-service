package com.gentlecorp.account.security;

import com.gentlecorp.account.security.service.JwtUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Konvertiert ein JWT in ein `UserDetails`-Objekt für die Authentifizierung.
 * <p>
 * Diese Klasse wird von Spring Security verwendet, um JWTs in `UserDetails` zu übersetzen.
 * </p>
 *
 * @since 14.02.2025
 * @author <a href="mailto:caleb-script@outlook.de">Caleb Gyamfi</a>
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class JwtToUserDetailsConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtUserDetailsService jwtUserDetailsService;

    @Override
    @NonNull
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        CustomUserDetails userDetails = (CustomUserDetails) jwtUserDetailsService.loadUserDetailsFromJwt(jwt);
        return new CustomAuthenticationToken(userDetails, jwt, userDetails.getAuthorities());
    }
}
