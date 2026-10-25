package com.lias.gestion_laboratoire.security;

import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.Mandat;
import com.lias.gestion_laboratoire.models.enums.Role;
import com.lias.gestion_laboratoire.repositories.MandatRepository;
import com.lias.gestion_laboratoire.models.enums.Statut;
import com.lias.gestion_laboratoire.repositories.MembreRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final MembreRepository membreRepository;
    private final MandatRepository mandatRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String email = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                email = jwtUtil.extractEmail(jwt);
            } catch (Exception e) {
                // Token invalide ou expiré
            }
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Membre membre = membreRepository.findByEmail(email).orElse(null);

            if (membre != null && jwtUtil.validateToken(jwt, membre.getEmail())
                    && jwtUtil.extractSessionVersion(jwt) == membre.getSessionVersion()) {
                if (!membre.isCompteActif() || membre.isCompteGele()
                        || membre.getStatut() == Statut.RETRAITE
                        || membre.getStatut() == Statut.ANCIEN) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(
                            "{\"status\":403,\"message\":\"Votre compte est inactif ou gelé. Contactez l’administration.\"}");
                    return;
                }
                Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
                if (membre.getRoleSysteme() != null) {
                    authorities.add(new SimpleGrantedAuthority(membre.getRoleSysteme().name()));
                }
                if (membre.getRole() != null) {
                    authorities.add(new SimpleGrantedAuthority(membre.getRole().name()));
                }
                for (Mandat mandat : mandatRepository.findMandatsMembreValidesAu(membre.getId(), LocalDate.now())) {
                    Role role = mandat.getResponsabilite();
                    if (role != null) {
                        authorities.add(new SimpleGrantedAuthority(role.name()));
                    }
                }
                if (authorities.isEmpty()) authorities.add(new SimpleGrantedAuthority("USER"));

                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        membre, null, authorities);

                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
