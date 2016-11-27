package com.hallv.morsechatserver;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.FormParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
    
    
    @POST
    @Secured
    @Path("message/sendmessage")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Message sendMessage(Message message){    
    message = new Message();
    em.persist(message);
    return message;
    }
    @POST
    @Path("message/sendgroupmessage")
    public Response sendGroupMessage(@FormParam("senderid") long senderid,
                                     @FormParam("text") String text,
                                     @FormParam("recipientlist") List<Long> recipientList){
                            ChatUser sender = em.getReference(ChatUser.class, senderid);
                            if(sender == null || recipientList.isEmpty()){
                                return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
                            }
                            for(long i : recipientList){
                                ChatUser recipient = em.getReference(ChatUser.class, i);
                                Message msg = new Message(text, recipient,sender);
                                em.persist(msg);
                            }
                            return Response.ok().build();
    }
    @GET
    @Path("message/messages")
    public List<Message> getMessages(@QueryParam("recipientid") long recipientid){
          return em.createQuery("select m from Message m where m.recipient.id = :recipientid",Message.class)
                  .setParameter("recipientid",recipientid)
                  .getResultList();
    }
    @GET
    @Path("user/all")
    public List<ChatUser> getUsers(){
        return em.createQuery("select c from ChatUser c", ChatUser.class).getResultList();
    }
    
    @DELETE
    @Path("message/delete")
    public Response deleteMessage(@FormParam("messageid") long messageid){
        Message msg = em.getReference(Message.class,messageid);
        if(msg != null){
            em.remove(msg);
            return Response.ok().build();
        }
        return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
    }
    
    @POST
    @Path("user/addfriend")
    public Response addFriend(@FormParam("ownerid") long ownerid,
                          @FormParam("friendid") long friendid  ){
                ChatUser owner = em.getReference(ChatUser.class, ownerid);
                ChatUser friend = em.getReference(ChatUser.class, friendid);
                if(owner != null && friend != null){ 
                Friend newFriend = new Friend(owner, 0, friend);
                em.persist(newFriend);
                return Response.ok().build();
                }
                return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
    }
                
    @POST
    @Path("user/confirmfriend")
    public Response confirmFriend(@FormParam("ownerid") long ownerid,
                              @FormParam("friendid") long friendid){
                List<Friend> list= em.createQuery("select f from Friend f where f.owner.id = :ownerid and f.friend.id = :friendid",Friend.class)
                        .setParameter("ownerid",ownerid).setParameter("friendid",friendid).getResultList();
                 if(!list.isEmpty()){
                     Friend friend = list.get(0);
                     friend.setConfirmed(1);
                     em.persist(friend);
                     return Response.ok().build();
                 }
                 else{
                 return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
                 }
    }
    @GET
    @Path("user/friends")
    public List<ChatUser> showFriends(@QueryParam("ownerid") long ownerid){
            List<Friend> friendZone = em.createQuery("Select f from Friend f where f.owner.id = :ownerid", Friend.class)
                    .setParameter("ownerid",ownerid).getResultList();
            ArrayList<ChatUser> friendList = new ArrayList<>();
            for(Friend f : friendZone){
                friendList.add(f.getFriend());
            }
            return friendList;
    }
}
