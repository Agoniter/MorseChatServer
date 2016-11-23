/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hallv.morsechatserver;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

/**
 *
 * @author Vegard
 */

@Path("open")
@Stateless
public class OpenService {
    @PersistenceContext
    EntityManager em;
    
    @GET 
    public String createUser(@QueryParam("email")String email,
                             @QueryParam("password")String password,
                             @QueryParam("username") String username) {
        ChatUser user = null;
                
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(password.getBytes("UTF-8"));
            user = new ChatUser(username, email, Base64.getEncoder().encodeToString(hash));
            em.persist(user);
            em.persist(new Group(Group.USER, username));
        } catch(Exception e) {
            Logger.getLogger(OpenService.class.getName()).log(Level.SEVERE, "Failed to add user",e);
        }
        
        return user != null ? user.toString() : "Error"; 
    }
}

