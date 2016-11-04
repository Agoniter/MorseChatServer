package com.hallv.morsechatserver;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author hallv
 */
@Path("morsechat")
@Produces(MediaType.APPLICATION_JSON)
@Stateless
public class MorseChatService {
    @PersistenceContext
    EntityManager em;
    
    @Resource(mappedName ="jdbc/MorseChat")
    DataSource dataSource;
    
    @GET
    @Path("test")
    public String test(){
        return "Hello World!";
    }
    @POST
    @Path("createuser")
    @Consumes(MediaType.APPLICATION_JSON)
    public void createUser( ChatUser usr){
        usr = new ChatUser();
        em.persist(usr);
    }
    
}
