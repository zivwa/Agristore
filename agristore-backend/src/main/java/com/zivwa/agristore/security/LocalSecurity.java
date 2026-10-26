package com.zivwa.agristore.security;
import com.zivwa.agristore.api.LocalStore;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import com.google.firebase.auth.*;
import org.springframework.web.server.ResponseStatusException;

@Configuration
@ConditionalOnExpression("'${app.auth-mode:local}' == 'local' or '${app.auth-mode:local}' == 'firebase-commerce'")
public class LocalSecurity {
 @Bean SecurityFilterChain localChain(HttpSecurity http,LocalStore store,ObjectProvider<FirebaseAuth> firebaseProvider) throws Exception {
  return http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(a->a
    .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()
    .requestMatchers(org.springframework.http.HttpMethod.OPTIONS,"/**").permitAll()
    .requestMatchers(org.springframework.http.HttpMethod.GET,"/api/v1/health","/api/v1/products","/api/v1/products/**","/api/v1/categories","/api/v1/store").permitAll()
    .requestMatchers(org.springframework.http.HttpMethod.POST,"/api/v1/auth/login","/api/v1/auth/register","/api/v1/contact").permitAll()
    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN").anyRequest().authenticated())
   .exceptionHandling(e->e.authenticationEntryPoint((q,r,x)->{r.setStatus(401);r.setContentType("application/json");r.getWriter().write("{\"message\":\"Please log in\"}");})
    .accessDeniedHandler((q,r,x)->{r.setStatus(403);r.setContentType("application/json");r.getWriter().write("{\"message\":\"Access denied\"}");}))
   .addFilterBefore(new OncePerRequestFilter(){protected void doFilterInternal(HttpServletRequest q,HttpServletResponse r,FilterChain chain)throws ServletException,IOException{
    String path=q.getRequestURI();
    try {
      if(store.firebaseMode && !path.equals("/api/v1/health") && !q.getMethod().equals("OPTIONS"))store.refreshCloud();
      String header=q.getHeader("Authorization");
      if(header!=null&&header.startsWith("Bearer ")){
       String uid;
       if(store.firebaseMode){FirebaseAuth verifier=firebaseProvider.getIfAvailable();if(verifier==null)throw new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Firebase Authentication is not configured");uid=store.acceptFirebase(verifier.verifyIdToken(header.substring(7),true));}
       else uid=store.verify(header.substring(7));
       if(uid!=null){String role=store.text(store.profile(uid),"role");SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(uid,null,List.of(new SimpleGrantedAuthority("ROLE_"+role))));}
      }
    }catch(FirebaseAuthException|IllegalArgumentException e){r.setStatus(401);r.setContentType("application/json");r.getWriter().write("{\"message\":\"Invalid or expired Firebase session\"}");return;}
    catch(ResponseStatusException e){r.setStatus(e.getStatusCode().value());r.setContentType("application/json");r.getWriter().write(store.mapper.writeValueAsString(java.util.Map.of("message",e.getReason()==null?"Firebase request failed":e.getReason())));return;}    chain.doFilter(q,r);
   }},UsernamePasswordAuthenticationFilter.class).build();
 }
}
