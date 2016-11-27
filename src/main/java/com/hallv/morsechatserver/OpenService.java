/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hallv.morsechatserver;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.security.Key;
import io.jsonwebtoken.*;
import java.util.Date;    
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 *
 * @author Vegard
 */

@Path("open")
@Stateless
public class OpenService {
    @PersistenceContext
    EntityManager em;
    private static final Random RANDOM = new SecureRandom();
    
    @GET 
    public String createUser(@QueryParam("email")String email,
                             @QueryParam("password")String password,
                             @QueryParam("username") String username) {
        ChatUser user = null;
        try {
            byte[] pass = password.getBytes("UTF-8");
            
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(pass);
            user = new ChatUser(username, email, Base64.getEncoder().encodeToString(hash));
            em.persist(user);
            em.persist(new Group(Group.USER, username));
        } catch(Exception e) {
            Logger.getLogger(OpenService.class.getName()).log(Level.SEVERE, "Failed to add user",e);
        }
        
        return user != null ? user.toString() : "Error"; 
    }
    
    @POST
    @Produces("application/json")
    @Consumes("application/json")
    public Response authenticateUser(Credentials credentials) {

        String username = credentials.getUsername();
        String password = credentials.getPassword();
        try{
            password = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(password.getBytes()));
        }
        catch(Exception e){
            Logger.getLogger(OpenService.class.getName()).log(Level.SEVERE, "Failed to log in",e);
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        }
        
        List<ChatUser> result = em.createQuery("select c from ChatUser c where c.username = :uname and c.password = :psw").setParameter("uname", username).setParameter("psw",password).getResultList();
        
        // Authenticate the user, issue a token and return a response
        if(!result.isEmpty()){
            String token = issueToken(username);
            return Response.ok(token).build();
        }
        else{
            return Response.status(Response.Status.CONFLICT).build();
        }
        
    }
    
    private String issueToken(String username) {
        return createJWT(username, 7*24*60*60*1000);
    }
    
    private String createJWT(String subject, long ttlMillis) {
 
    //The JWT signature algorithm we will be using to sign the token
    SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
 
    long nowMillis = System.currentTimeMillis();
    Date now = new Date(nowMillis);
 
    //We will sign our JWT with our ApiKey secret
    byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary("test");
    Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
 
    //Let's set the JWT Claims
    JwtBuilder builder = Jwts.builder().setIssuedAt(now)
                                .setSubject(subject)
                                .signWith(signatureAlgorithm, signingKey);
 
    //if it has been specified, let's add the expiration
    if (ttlMillis >= 0) {
    long expMillis = nowMillis + ttlMillis;
        Date exp = new Date(expMillis);
        builder.setExpiration(exp);
    }
 
    //Builds the JWT and serializes it to a compact, URL-safe string
    return builder.compact();
}
    
    
}

