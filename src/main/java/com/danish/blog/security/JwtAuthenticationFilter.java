package com.danish.blog.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtTokenHelper jwtTokenHelper;

    Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // DEBUG: list all headers (very helpful to confirm header arrival)
        if (request.getHeaderNames() != null) {
            java.util.Enumeration<String> names = request.getHeaderNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                logger.debug("Incoming header: {} = {}", name, request.getHeader(name));
            }
        } else {
            logger.debug("No headers on request");
        }

        //Get Token from request
        String requestToken=request.getHeader("Authorization");

        //Bearer 22334dfvc
        System.out.println("Requested Token: \n" +requestToken);

        String username=null;

        String token = null;

        if(requestToken != null && requestToken.startsWith("Bearer ")){
            token = requestToken.substring(7);
            try {
                username = jwtTokenHelper.getUsernameFromToken(token);
            }
            catch (IllegalArgumentException e){
                System.out.println("Unable to get Jwt Token");
            } catch (ExpiredJwtException e) {
                System.out.println("Jwt Token has Expired !!");
            } catch (MalformedJwtException e) {
                System.out.println("Invalid Jwt Exception ");
            }
        }else{
            System.out.println("Jwt Token Doesn't Begin with: Bearer");
        }

        //Once we get the token now will validate token
        if(username!=null && SecurityContextHolder.getContext().getAuthentication()==null){
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if(jwtTokenHelper.validateToken(token, userDetails)){
                //everything is correct
                //Needs to do authentication

                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            }else{
                System.out.println("Invalid Jwt Token");
            }
        }else{
            System.out.println("Username is null or Context is not null");
        }

        filterChain.doFilter(request,response);
    }
}
