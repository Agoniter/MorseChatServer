package com.hallv.morsechatserver;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.Key;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.crypto.spec.SecretKeySpec;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;

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
    
     
    @GET
    @Path("test")
    public Response test(){
        return Response.ok("Hello World!").build();
       
    }
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
    @Secured
    @Path("message/sendgroupmessage")
    public Response sendGroupMessage(@QueryParam("senderid") long senderid,
                                     @QueryParam("message") List<Long> message,
                                     @QueryParam("recipientlist") List<Long> recipientList){
                            ChatUser sender = em.getReference(ChatUser.class, senderid);
                            if(sender == null || recipientList.isEmpty()){
                                return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
                            }
                            for(long i : recipientList){
                                ChatUser recipient = em.getReference(ChatUser.class, i);
                                Message msg = new Message((ArrayList)message, recipient,sender);
                                em.persist(msg);
                            }
                            return Response.ok().build();
    }

    @GET
    @Secured
    @Path("message/messages")
    public List<Message> getMessages(@QueryParam("recipientid") long recipientid){
          return em.createQuery("select m from Message m where m.recipient.id = :recipientid",Message.class)
                  .setParameter("recipientid",recipientid)
                  .getResultList();
    }
    @GET
    @Secured
    @Path("user/all")
    public List<ChatUser> getUsers(){
        return em.createQuery("select c from ChatUser c", ChatUser.class).getResultList();
    }
    
    @DELETE
    @Secured
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
    @Secured
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
    @Secured
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
    @Secured
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
    

    
    @POST
    public String createUser(@FormParam("email")String email,
                             @FormParam("password")String password,
                             @FormParam("username") String username) {
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
    public Response authenticateUser(@FormParam("username") String username,
                                     @FormParam("password") String password) {

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
