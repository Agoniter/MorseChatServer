package com.hallv.morsechatserver;

import java.util.List;
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
import javax.ws.rs.QueryParam;
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
    @Path("message/sendmessage")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Message sendMessage(Message message){
    message = new Message();
    em.persist(message);
    return message;
    }
    
    @POST
    @Path("message/getmessages")
    public List<Message> getMessages(@QueryParam("recipientid") long recipientid){
          return em.createQuery("select m from Message m where m.recipient.id = :recipientid",Message.class)
                  .setParameter("recipientid",recipientid)
                  .getResultList();
    }
    
}
